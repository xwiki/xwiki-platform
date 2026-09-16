/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.security.internal;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.AbstractValidatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A custom implementation of Spring's PasswordEncoder dedicated to handle XWiki legacy passwords.
 * The purpose of this encoder is two-fold: it aims at re-encode legacy passwords using argon2 algorithm. And it's
 * also able to perform the password matching check between a raw password and the reencoded legacy passwords.
 *
 * @version $Id$
 * @since 18.8.0RC1
 * @since 18.4.5
 */
public class XWikiLegacyPasswordEncoder extends AbstractValidatingPasswordEncoder
{
    /**
     * The name of the encoder to be used in returned encoded password format, and reused in the delegate encoder.
     */
    public static final String ALGORITHM_ID = "XWikiLegacy";
    private static final String HASH_IDENTIFIER = "hash";
    private static final String SEPARATOR = ":";
    private static final Pattern PASSWORD_FORMAT_PATTERN =
        Pattern.compile("^(hash:)?(?<algorithmName>[\\w-]+):((?<salt>\\p{XDigit}+):)?(?<passwordHash>.*)$");

    private final PasswordEncoder passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    private record LegacyPasswordInformation(String algorithmName, String salt, String passwordHash)
    {
    }

    private LegacyPasswordInformation getLegacyPasswordInformation(String encodedPassword)
        throws IllegalArgumentException
    {
        Matcher matcher = PASSWORD_FORMAT_PATTERN.matcher(encodedPassword);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("The given encoded password doesn't match the expected format "
                + "<algorithmName>:<salt>:<hash>.");
        }
        String salt = matcher.group("salt");
        if (salt == null) {
            salt = "";
        }
        return new LegacyPasswordInformation(
            matcher.group("algorithmName"),
            salt,
            matcher.group("passwordHash"));
    }

    /**
     * @param password the encoded password
     * @return The algorithm used for the given password.
     */
    public String getAlgorithmFromPassword(String password)
    {
        return getLegacyPasswordInformation(password).algorithmName;
    }

    /**
     * @param password the password to hash.
     * @param algorithmName the name of the hashing algorithm to use. See {@link MessageDigest#getInstance(String)}.
     * @param salt the string to pad the password with before hashing. If {@code null}, a random 64 character
     * salt will be used. To disable salting, use an empty ({@code ""}) salt string.
     * @return a string of the form {@code hash:<algorithmName>:<salt>:<hexStrignHash>}, where {@code <hexStrignHash>}
     * is the salted hash of the given password, using the given hashing algorithm.
     * @since 6.3M2
     */
    private String getLegacyPasswordHash(String password, String algorithmName, String salt)
        throws NoSuchAlgorithmException
    {
        String saltedPassword = salt + password;

        MessageDigest hashAlgorithm = MessageDigest.getInstance(algorithmName);
        hashAlgorithm.update(saltedPassword.getBytes());
        byte[] digest = hashAlgorithm.digest();

        // Build the result.
        StringBuilder sb = new StringBuilder();
        // Metadata
        sb.append(HASH_IDENTIFIER);
        sb.append(SEPARATOR);
        sb.append(algorithmName);
        sb.append(SEPARATOR);
        // Backward compatibility concern : let's keep unsalted password the way they are.
        if (!salt.isEmpty()) {
            sb.append(salt);
            sb.append(SEPARATOR);
        }
        // The actual password hash.
        for (byte element : digest) {
            int b = element & 0xFF;
            if (b < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(b));
        }

        return sb.toString();
    }

    /**
     * Re-encode the provided legacy encoded password with an Argon2 algorithm to enhance it's security.
     * The provided legacy encoded password should be on the format {@code hash:<algorithm>:<salt>:<hashed password>}
     * knowing that the salt is optional.
     * <br/>
     * The re-encoded password will be on the format: {@code {XWikiLegacy}:
     * <algorithm>:<salt>:<argon2 hashed password>}.The original algorithm name and salt need to be kept decoded to
     * ensure password matching remains possible.
     *
     * @param legacyEncodedPassword the legacy password that needs to be re-encoded.
     * @return a re-encoded password using Argon2.
     * @throws IllegalArgumentException if the provided legacy encoded password doesn't match the expected format.
     */
    public String reencodePassword(String legacyEncodedPassword) throws IllegalArgumentException
    {
        LegacyPasswordInformation legacyPasswordInformation = getLegacyPasswordInformation(legacyEncodedPassword);

        return String.format("{%s}%s:%s:%s", ALGORITHM_ID,
            legacyPasswordInformation.algorithmName(),
            legacyPasswordInformation.salt(),
            passwordEncoder.encode(legacyEncodedPassword)
        );
    }

    /**
     * Check if the given raw password matches the provided legacy encoded password which should use the format
     * {@code hash:<algorithm>:<salt>:<hashed password>}.
     *
     * @param rawPassword the raw password to check for matching
     * @param encodedPassword the legacy encoded password
     * @return {@code true} only if the passwords are matching.
     */
    public boolean matchesLegacy(String rawPassword, String encodedPassword)
    {
        LegacyPasswordInformation legacyPasswordInformation = getLegacyPasswordInformation(encodedPassword);
        try {
            String legacyPasswordHash =
                getLegacyPasswordHash(rawPassword, legacyPasswordInformation.algorithmName(),
                    legacyPasswordInformation.salt());
            return encodedPassword.equals(legacyPasswordHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected String encodeNonNullPassword(String rawPassword)
    {
        throw new RuntimeException("This encoder is provided only to reencode legacy passwords, it should never"
            + " be used for encoding new passwords.");
    }

    @Override
    protected boolean matchesNonNull(String rawPassword, String encodedPassword)
    {
        LegacyPasswordInformation legacyPasswordInformation = getLegacyPasswordInformation(encodedPassword);
        try {
            String legacyPasswordHash = getLegacyPasswordHash(rawPassword, legacyPasswordInformation.algorithmName(),
                    legacyPasswordInformation.salt());
            return passwordEncoder.matches(legacyPasswordHash, legacyPasswordInformation.passwordHash());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean upgradeEncodingNonNull(String encodedPassword)
    {
        return true;
    }
}

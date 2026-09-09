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
package com.xpn.xwiki.objects.classes;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.ecs.xhtml.input;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.xwiki.security.internal.XWikiLegacyPasswordEncoder;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.xml.XMLAttributeValueFilter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.PasswordProperty;
import com.xpn.xwiki.objects.meta.PasswordMetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

/**
 * Define a property field to hold a password.
 * Password hashing in that class relies on the following set of algorithms provided by Spring Security framework:
 * <ul>
 *     <li>argon2: Default algorithm when nothing is specified. The provided implementation is the one for Spring
 *     Security v5.8.</li>
 *     <li>bcrypt: Note that this algorithm has a limit of 72 characters.</li>
 *     <li>scrypt: The provided implementation is the one for Spring Security v5.8.</li>
 *     <li>pbkdf2: The provided implementation is the one for Spring Security v5.8.</li>
 *     <li>SHA-1: Deprecated. Only provided for legacy purpose.</li>
 *     <li>SHA-256: Deprecated. Only provided for legacy purpose.</li>
 *     <li>SHA-512: Deprecated. Only provided for legacy purpose.</li>
 * </ul>
 *
 * @version $Id$
 */
public class PasswordClass extends StringClass
{
    /**
     * The type used as a hint to find the class.
     * @since 18.2.0RC1
     */
    @Unstable
    public static final String PROPERTY_TYPE = "Password";

    /**
     * The key for the argon2 algorithm which is the default hashing algorithm used in the class.
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public static final String ARGON2_ALGORITHM = "argon2";

    /**
     * The key for the bcrypt hash algorithm.
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public static final String BCRYPT_ALGORITHM = "bcrypt";

    /**
     * The key for the scrypt hash algorithm.
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public static final String SCRYPT_ALGORITHM = "scrypt";

    /**
     * The key for the pbkdf2 hash algorithm.
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public static final String PBKDF2_ALGORITHM = "pbkdf2";

    /**
     * The key for the SHA-1 hash algorithm: note that this algorithm is deprecated and only provided for legacy
     * reasons.
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public static final String SHA_1_ALGORITHM = "SHA-1";

    /**
     * The key for the SHA-256 hash algorithm: note that this algorithm is deprecated and only provided for legacy
     * reasons.
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public static final String SHA_256_ALGORITHM = "SHA-256";

    /**
     * The key for the SHA-512 hash algorithm: note that this algorithm is deprecated and only provided for legacy
     * reasons.
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public static final String SHA_512_ALGORITHM = "SHA-512";

    /**
     * Expose the full list of supported hash algorithms.
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public static final List<String> SUPPORTED_ALGORITHMS = List.of(
        XWikiLegacyPasswordEncoder.ALGORITHM_ID,
        ARGON2_ALGORITHM,
        BCRYPT_ALGORITHM,
        SCRYPT_ALGORITHM,
        PBKDF2_ALGORITHM,
        SHA_1_ALGORITHM,
        SHA_256_ALGORITHM,
        SHA_512_ALGORITHM
    );

    protected static final Logger LOGGER = LoggerFactory.getLogger(PasswordClass.class);

    protected static final String DEFAULT_STORAGE = PasswordMetaClass.HASH;

    protected static final String DEFAULT_HASH_ALGORITHM = ARGON2_ALGORITHM;

    protected static final String HASH_IDENTIFIER = "hash";

    protected static final String SEPARATOR = ":";

    protected static final String FORM_PASSWORD_PLACEHODLER = "********";

    private static final long serialVersionUID = 1L;

    private static final String ALGORITHM_ID_PATTERN_GROUP = "algorithmId";
    private static final String PASSWORD_HASH_PATTERN_GROUP = "passwordHash";
    private static final Pattern HASH_PATTERN = Pattern.compile(
        String.format("^((hash:)|(\\{(?<%s>%s)}))(?<%s>.*)$",
            ALGORITHM_ID_PATTERN_GROUP,
            String.join("|", SUPPORTED_ALGORITHMS),
            PASSWORD_HASH_PATTERN_GROUP
        ));

    // "password" is used for both the field type and the xclass name:
    // we use a single constant to comply with checkstyle here.
    private static final String PASSWORD_FIELD_TYPE = "password";
    private static final String XCLASSNAME = PASSWORD_FIELD_TYPE;
    private static final XWikiLegacyPasswordEncoder LEGACY_PASSWORD_ENCODER = new XWikiLegacyPasswordEncoder();
    private static final Map<String, PasswordEncoder> ENCODERS_MAP = Map.of(
        BCRYPT_ALGORITHM, new BCryptPasswordEncoder(),
        PBKDF2_ALGORITHM, Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
        SCRYPT_ALGORITHM, SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8(),
        ARGON2_ALGORITHM, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
        SHA_1_ALGORITHM,
        new org.springframework.security.crypto.password.MessageDigestPasswordEncoder(SHA_1_ALGORITHM),
        SHA_256_ALGORITHM,
        new org.springframework.security.crypto.password.MessageDigestPasswordEncoder(SHA_256_ALGORITHM),
        SHA_512_ALGORITHM,
        new org.springframework.security.crypto.password.MessageDigestPasswordEncoder(SHA_512_ALGORITHM),
        XWikiLegacyPasswordEncoder.ALGORITHM_ID, LEGACY_PASSWORD_ENCODER
    );

    private static final IllegalArgumentException ILLEGAL_ARGUMENT_EXCEPTION_UNKNOWN_FORMAT =
        new IllegalArgumentException("The provided encoded password doesn't match any known hash "
            + "encoded password format.");

    /**
     * Default constructor with a metaclass.
     * @param wclass the metaclass value.
     */
    public PasswordClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, PROPERTY_TYPE, wclass);
    }

    /**
     * Empty constructor with a null metaclass.
     */
    public PasswordClass()
    {
        this(null);
    }

    @Override
    public BaseProperty fromString(String value) throws XWikiException
    {
        BaseProperty property = newProperty();
        if (value.isEmpty() || isPasswordHashed(value)) {
            property.setValue(value);
        } else {
            property.setValue(getProcessedPassword(value));
        }
        return property;
    }

    /**
     * Compute if a value is a hash password or not based only on its prefix.
     * The method will return {@code true} either if value starts with {@code hash:} or if it starts with
     * {@code {algorithmName}} where the provided algorithm name is a supported hash algorithm.
     *
     * @param value the value to check if it's a hashed password or not
     * @return {@code true} if it's recognized as a hash.
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public static boolean isPasswordHashed(String value)
    {
        return value != null && value.startsWith(HASH_IDENTIFIER + SEPARATOR) || HASH_PATTERN.matcher(value).matches();
    }

    @Override
    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        // Passwords cannot go through the preview interface, so we don't do something here..
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        ElementInterface prop = object.safeget(name);
        if (prop != null) {
            buffer.append(FORM_PASSWORD_PLACEHODLER);
        }
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        input input = new input();
        input.setAttributeFilter(new XMLAttributeValueFilter());
        BaseProperty prop = (BaseProperty) object.safeget(name);
        // Only display the obfuscation placeholder is the value is not empty to not confuse users into thinking that
        // the property is set.
        if (prop != null && !StringUtils.isEmpty(prop.toText())) {
            input.setValue(FORM_PASSWORD_PLACEHODLER);
        }

        input.setType(PASSWORD_FIELD_TYPE);
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        input.setDisabled(isDisabled());
        setAriaLabelFallback(input, context);
        buffer.append(input.toString());
    }

    /**
     * @return 'Clear' or 'Hash'.
     */
    public String getStorageType()
    {
        BaseProperty st = (BaseProperty) this.getField(PasswordMetaClass.STORAGE_TYPE);
        if (st != null) {
            Object value = st.getValue();
            if (value != null) {
                String type = value.toString().trim();
                if (!type.isEmpty()) {
                    return type;
                }
            }
        }
        return DEFAULT_STORAGE;
    }

    /**
     * @param storageType One of 'Clear' or 'Hash'
     * @since 10.7RC1
     */
    public void setStorageType(String storageType)
    {
        setStringValue(PasswordMetaClass.STORAGE_TYPE, storageType);
    }

    /**
     * Transforms a plain text password so that it has the same encryption as a password stored in the database. The
     * current configuration for this password XProperty cannot be used, as the user might have a different encryption
     * mechanism (for example, if the user was imported, or the password was not yet upgraded).
     *
     * @param storedPassword The stored password, which gives the storage type and algorithm.
     * @param plainPassword The plain text password to be encrypted.
     * @return The input password, encrypted with the same mechanism as the stored password.
     * @deprecated Contrarily to what the original doc says this method only returns the stored password value, if
     * there's a match between both values, else it returns an empty string. It shouldn't be used at all anymore and
     * instead {@link #arePasswordsMatching(String, String)} should be used.
     */
    @Deprecated(since = "18.8.0RC1, 18.4.5")
    public String getEquivalentPassword(String storedPassword, String plainPassword)
    {
        return (arePasswordsMatching(plainPassword, storedPassword)) ? storedPassword : "";
    }

    /**
     * Process the given password depending on the storage type and the defined algorithm.
     * @param password the password to be processed
     * @return a hashed password
     */
    public String getProcessedPassword(String password)
    {
        String storageType = getStorageType();
        String result = password;
        if (PasswordMetaClass.HASH.equals(storageType)) {
            result = getPasswordHash(result);
        }
        return result;
    }

    /**
     * Compute a hash for the given password based on the algorithm contained in the class property information, or
     * based on the {@link #DEFAULT_HASH_ALGORITHM} if not provided.
     * The returned hash is using the format {@code {algorithmKey}passwordHash}.
     *
     * @param password the password to hash.
     * @return an encoded password hash containing the name of the algorithm to use for matching.
     */
    public String getPasswordHash(String password)
    {
        return getPasswordHash(password, getHashAlgorithm());
    }

    /**
     * {@return the hash algorithm configured for this XProperty}
     */
    public String getHashAlgorithm()
    {
        BaseProperty alg = (BaseProperty) this.getField(PasswordMetaClass.ALGORITHM_KEY);
        if (alg != null && !StringUtils.isEmpty(String.valueOf(alg.getValue()))) {
            return alg.getValue().toString();
        }
        return DEFAULT_HASH_ALGORITHM;
    }

    /**
     * @param password the encoded password
     * @return The algorithm used for the given password or {@code null} if the storage type is clear.
     */
    public String getAlgorithmFromPassword(String password)
    {
        if (PasswordMetaClass.CLEAR.equals(getStorageType())) {
            return null;
        } else if (password.startsWith(HASH_IDENTIFIER + SEPARATOR)) {
            warnAboutOutdatedAlgorithm(XWikiLegacyPasswordEncoder.ALGORITHM_ID);
            return LEGACY_PASSWORD_ENCODER.getAlgorithmFromPassword(password);
        } else {
            Matcher hashMatcher = HASH_PATTERN.matcher(password);
            if (hashMatcher.matches()) {
                return hashMatcher.group(ALGORITHM_ID_PATTERN_GROUP);
            } else {
                throw ILLEGAL_ARGUMENT_EXCEPTION_UNKNOWN_FORMAT;
            }
        }
    }

    /**
     * Compute a password hash based on the given algorithm.
     *
     * @param password the password to hash.
     * @param algorithmName the name of the algorithm to use for performing the hash (e.g. "argon2", "scrypt", etc).
     * @return a hash whose format is {@code {algorithmName}hash}
     */
    public String getPasswordHash(String password, String algorithmName)
    {
        PasswordEncoder passwordEncoder = getPasswordEncoder(algorithmName);
        String encodedPassword = this.ENCODERS_MAP.get(algorithmName).encode(password);
        if (passwordEncoder.upgradeEncoding(encodedPassword)
            || isDeprecatedEncoder(passwordEncoder.getClass())) {
            warnAboutOutdatedAlgorithm(algorithmName);
        }
        return String.format("{%s}%s", algorithmName, encodedPassword);
    }

    private @NonNull PasswordEncoder getPasswordEncoder(String algorithmName)
    {
        if (this.ENCODERS_MAP.containsKey(algorithmName)) {
            PasswordEncoder passwordEncoder = this.ENCODERS_MAP.get(algorithmName);
            if (isDeprecatedEncoder(passwordEncoder.getClass())) {
                warnAboutOutdatedAlgorithm(algorithmName);
            }
            return passwordEncoder;
        } else {
            throw new IllegalArgumentException(String.format("The algorithm [%s] is not supported for password hash.",
                algorithmName));
        }
    }

    /**
     * Verify if the given raw passwords is matching the given encoded password.
     * This method directly checks a case-sensitive equality of the passwords if the storage type is set to clear.
     * Otherwise, the method will check a match with a password hash: the method still supports legacy encoded
     * passwords (using the format {@code hash:<algorithmName>:<salt>:<hash>}) but it will warn to reencode them.
     * In a similar way, if the provided encode passwords uses a deprecated algorithm a warning will be issued.
     *
     * @param rawPassword the raw password to test for a match
     * @param encodedPassword the encoded (or not if the storage type is clear) password to match with
     * @return {@code true} only if there's match between the passwords
     * @since 18.8.0RC1
     * @since 18.4.5
     */
    @Unstable
    public boolean arePasswordsMatching(String rawPassword, String encodedPassword)
    {
        if (PasswordMetaClass.CLEAR.equals(getStorageType())) {
            return Strings.CS.equals(rawPassword, encodedPassword);
        } else if (encodedPassword.startsWith(HASH_IDENTIFIER + SEPARATOR)) {
            warnAboutOutdatedAlgorithm(XWikiLegacyPasswordEncoder.ALGORITHM_ID);
            return LEGACY_PASSWORD_ENCODER.matchesLegacy(rawPassword, encodedPassword);
        } else {
            Matcher hashMatcher = HASH_PATTERN.matcher(encodedPassword);
            if (hashMatcher.matches()) {
                String algorithmId = hashMatcher.group(ALGORITHM_ID_PATTERN_GROUP);
                String passwordHash = hashMatcher.group(PASSWORD_HASH_PATTERN_GROUP);
                PasswordEncoder passwordEncoder = getPasswordEncoder(algorithmId);
                if (passwordEncoder.upgradeEncoding(passwordHash)) {
                    warnAboutOutdatedAlgorithm(algorithmId);
                }
                return passwordEncoder.matches(rawPassword, passwordHash);
            } else {
                throw ILLEGAL_ARGUMENT_EXCEPTION_UNKNOWN_FORMAT;
            }
        }
    }

    private <X extends PasswordEncoder> boolean isDeprecatedEncoder(Class<X> encoderClass)
    {
        return encoderClass.getAnnotation(Deprecated.class) != null;
    }

    private void warnAboutOutdatedAlgorithm(String algorithmName)
    {
        if (getObject() != null) {
            LOGGER.warn("The password located in [{}] uses an outdated algorithm [{}] (or an outdated version of it) "
                    + "and should be re-encoded.",
                getReference(), algorithmName);
        } else {
            LOGGER.error("An outdated algorithm [{}] (or an outdated version of it) is used in a PasswordClass "
                    + "property not yet attached to an object",
                algorithmName, new Exception());
        }
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new PasswordProperty();
        property.setName(getName());
        return property;
    }

    /**
     * {@inheritDoc}
     * @return {@code true} as this property is always sensitive.
     */
    @Override
    public boolean isSensitive(XWikiContext context)
    {
        return true;
    }

    @Override
    public String getPropertyType()
    {
        return PROPERTY_TYPE;
    }
}

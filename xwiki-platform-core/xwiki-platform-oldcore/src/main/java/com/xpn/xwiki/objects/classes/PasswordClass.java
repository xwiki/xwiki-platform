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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.xml.XMLAttributeValueFilter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.meta.PasswordMetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public class PasswordClass extends StringClass
{
    private static final String XCLASSNAME = "password";

    protected static Logger LOGGER = LoggerFactory.getLogger(PasswordClass.class);

    protected static final String DEFAULT_STORAGE = PasswordMetaClass.HASH;

    protected static final String DEFAULT_HASH_ALGORITHM = "SHA-512";

    protected static final String DEFAULT_CRYPT_ALGORITHM = "AES";

    protected static final String HASH_IDENTIFIER = "hash";

    protected static final String CRYPT_IDENTIFIER = "crypt";

    protected static final String SEPARATOR = ":";

    protected static final String FORM_PASSWORD_PLACEHODLER = "********";

    public PasswordClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, "Password", wclass);
    }

    public PasswordClass()
    {
        this(null);
    }

    @Override
    public BaseProperty fromString(String value)
    {
        if (value.equals(FORM_PASSWORD_PLACEHODLER)) {
            return null;
        }
        BaseProperty property = newProperty();
        if (value.isEmpty() || value.startsWith(HASH_IDENTIFIER + SEPARATOR)
            || value.startsWith(CRYPT_IDENTIFIER + SEPARATOR)) {
            property.setValue(value);
        } else {
            property.setValue(getProcessedPassword(value));
        }
        return property;
    }

    @Override
    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        // Passwords cannot go through the preview interface, so we don't do something here..
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        ElementInterface prop = object.safeget(name);
        if (prop != null) {
            buffer.append(FORM_PASSWORD_PLACEHODLER);
        }
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        input input = new input();
        input.setAttributeFilter(new XMLAttributeValueFilter());
        BaseProperty prop = (BaseProperty) object.safeget(name);
        // Only display the obfuscation placeholder is the value is not empty to not confuse users into thinking that
        // the property is set.
        if (prop != null && !StringUtils.isEmpty(prop.toText())) {
            input.setValue(FORM_PASSWORD_PLACEHODLER);
        }

        input.setType("password");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        input.setDisabled(isDisabled());
        buffer.append(input.toString());
    }

    /**
     * @return One of 'Clear', 'Hash' or 'Encrypt'.
     */
    public String getStorageType()
    {
        BaseProperty st = (BaseProperty) this.getField(PasswordMetaClass.STORAGE_TYPE);
        if (st != null) {
            Object value = st.getValue();
            if (value != null) {
                String type = value.toString().trim();
                if (!type.equals("")) {
                    return type;
                }
            }
        }
        return DEFAULT_STORAGE;
    }

    /**
     * @param storageType One of 'Clear', 'Hash' or 'Encrypt'
     * @since 10.7RC1
     */
    public void setStorageType(String storageType)
    {
        setStringValue(PasswordMetaClass.STORAGE_TYPE, storageType);
    }

    /**
     * @return The hash algorithm configured for this XProperty.
     */
    public String getHashAlgorithm()
    {
        BaseProperty alg = (BaseProperty) this.getField(PasswordMetaClass.ALGORITHM_KEY);
        if (alg != null && alg.getValue() != null && !alg.getValue().toString().trim().equals("")) {
            return alg.getValue().toString();
        }
        return DEFAULT_HASH_ALGORITHM;
    }

    /**
     * @return The encryption algorithm configured for this XProperty.
     */
    public String getCryptAlgorithm()
    {
        BaseProperty alg = (BaseProperty) this.getField(PasswordMetaClass.ALGORITHM_KEY);
        if (alg != null && alg.getValue() != null && !alg.getValue().toString().trim().equals("")) {
            return alg.getValue().toString();
        }
        return DEFAULT_CRYPT_ALGORITHM;
    }

    /**
     * @param password
     * @return The algorithm used for the given password.
     */
    public String getAlgorithmFromPassword(String password)
    {
        int beginIndex = password.indexOf(SEPARATOR) + 1;
        if (beginIndex >= 0) {
            int endIndex = password.indexOf(SEPARATOR, beginIndex);
            if (endIndex >= 0) {
                return password.substring(beginIndex, endIndex);
            }
        }
        return DEFAULT_HASH_ALGORITHM;
    }

    /**
     * @param password
     * @return The salt used for the given password. If this is an unsalted password, let it be known by returning "".
     */
    public String getSaltFromPassword(String password)
    {
        String[] components = password.split(SEPARATOR);
        if (components.length == 4) {
            return components[2];
        } else {
            return "";
        }
    }

    /**
     * Transforms a plain text password so that it has the same encryption as a password stored in the database. The
     * current configuration for this password XProperty cannot be used, as the user might have a different encryption
     * mechanism (for example, if the user was imported, or the password was not yet upgraded).
     *
     * @param storedPassword The stored password, which gives the storage type and algorithm.
     * @param plainPassword The plain text password to be encrypted.
     * @return The input password, encrypted with the same mechanism as the stored password.
     */
    public String getEquivalentPassword(String storedPassword, String plainPassword)
    {
        String result = plainPassword;
        if (storedPassword != null && plainPassword != null) {
            if (storedPassword.startsWith(HASH_IDENTIFIER + SEPARATOR)) {
                result =
                        getPasswordHash(result, getAlgorithmFromPassword(storedPassword), getSaltFromPassword(storedPassword));
            } else if (storedPassword.startsWith(CRYPT_IDENTIFIER + SEPARATOR)) {
                result = getPasswordCrypt(result, getAlgorithmFromPassword(storedPassword));
            }
        }
        return result;
    }

    public String getProcessedPassword(String password)
    {
        String storageType = getStorageType();
        String result = password;
        if (storageType.equals(PasswordMetaClass.HASH)) {
            result = getPasswordHash(result);
        } else if (storageType.equals(PasswordMetaClass.ENCRYPTED)) {
            result = getPasswordCrypt(result);
        }
        return result;
    }

    public String getPasswordCrypt(String password)
    {
        return getPasswordCrypt(password, getCryptAlgorithm());
    }

    public String getPasswordCrypt(String password, String algorithmName)
    {
        // TODO Write me!
        return password;
    }

    /**
     * @param password the password to hash.
     * @return a string of the form {@code hash:<algorithmName>:<salt>:<hexStrignHash>}, where {@code <algorithmName>} is
     *         the default hashing algorithm (see {@link #DEFAULT_HASH_ALGORITHM}), {@code <salt>} is a random 64 character
     *         salt and {@code <hexStrignHash>} is the salted hash of the given password, using the given hashing algorithm.
     */
    public String getPasswordHash(String password)
    {
        return getPasswordHash(password, getHashAlgorithm(), null);
    }

    /**
     * @param password the password to hash.
     * @param algorithmName the name of the hashing algorithm to use. See {@link MessageDigest#getInstance(String)}.
     * @return a string of the form {@code hash:<algorithmName>:<salt>:<hexStrignHash>}, where {@code <salt>} is a random
     *         64 character salt and {@code <hexStrignHash>} is the salted hash of the given password, using the given
     *         hashing algorithm.
     */
    public String getPasswordHash(String password, String algorithmName)
    {
        return getPasswordHash(password, algorithmName, null);
    }

    /**
     * @param password the password to hash.
     * @param algorithmName the name of the hashing algorithm to use. See {@link MessageDigest#getInstance(String)}.
     * @param salt the string to pad the password with before hashing. If {@code null}, a random 64 character salt will
     *            be used. To disable salting, use an empty ({@code ""}) salt string.
     * @return a string of the form {@code hash:<algorithmName>:<salt>:<hexStrignHash>}, where {@code <hexStrignHash>} is
     *         the salted hash of the given password, using the given hashing algorithm.
     * @since 6.3M2
     */
    public String getPasswordHash(String password, String algorithmName, String salt)
    {
        // If no salt given, let's generate one.
        if (salt == null) {
            salt = randomSalt();
        }

        try {
            LOGGER.debug("Hashing password");

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
            if (!salt.equals("")) {
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
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.error("Wrong hash algorithm [{}] specified in property [{}] of class [{}]", algorithmName,
                getName(), getXClassReference(), ex);
        } catch (NullPointerException ex) {
            LOGGER.error("Error hashing password", ex);
        }
        return password;
    }

    public static String randomSalt()
    {
        StringBuilder salt = new StringBuilder();
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[32];
        random.nextBytes(bytes);
        for (byte temp : bytes) {
            String s = Integer.toHexString(Byte.valueOf(temp));
            while (s.length() < 2) {
                s = "0" + s;
            }
            s = s.substring(s.length() - 2);
            salt.append(s);
        }
        return salt.toString();
    }
}

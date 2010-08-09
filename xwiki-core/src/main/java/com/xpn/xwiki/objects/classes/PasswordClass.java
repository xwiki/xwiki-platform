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
 *
 */

package com.xpn.xwiki.objects.classes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.xhtml.input;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.meta.PasswordMetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public class PasswordClass extends StringClass
{
    protected static Log log = LogFactory.getLog(PasswordClass.class);

    protected static final String DEFAULT_STORAGE = PasswordMetaClass.HASH;

    protected static final String DEFAULT_HASH_ALGORITHM = "SHA-512";

    protected static final String DEFAULT_CRYPT_ALGORITHM = "AES";

    protected static final String HASH_IDENTIFIER = "hash";

    protected static final String CRYPT_IDENTIFIER = "crypt";

    protected static final String SEPARATOR = ":";

    protected static final String FORM_PASSWORD_PLACEHODLER = "********";

    public PasswordClass(PropertyMetaClass wclass)
    {
        super("password", "Password", wclass);
        setxWikiClass(wclass);
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
        if (value.startsWith(HASH_IDENTIFIER + SEPARATOR) || value.startsWith(CRYPT_IDENTIFIER + SEPARATOR)) {
            property.setValue(value);
        } else {
            property.setValue(getProcessedPassword(value));
        }
        return property;
    }

    @Override
    public void displayHidden(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
        // Passwords cannot go through the preview interface, so we don't do something here..
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
        ElementInterface prop = object.safeget(name);
        if (prop != null) {
            buffer.append(FORM_PASSWORD_PLACEHODLER);
        }
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
        input input = new input();
        ElementInterface prop = object.safeget(name);
        if (prop != null) {
            input.setValue(FORM_PASSWORD_PLACEHODLER);
        }

        input.setType("password");
        input.setName(prefix + name);
        input.setSize(getSize());
        input.setDisabled(isDisabled());
        buffer.append(input.toString());
    }

    /**
     * @return One of 'Clear', 'Hash' or 'Encrypt'.
     */
    public String getStorageType()
    {
        BaseProperty st = (BaseProperty) this.getField("storageType");
        if (st != null) {
            String type = st.getValue().toString().trim();
            if (!type.equals("")) {
                return type;
            }
        }
        return DEFAULT_STORAGE;
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
        if (storedPassword.startsWith(HASH_IDENTIFIER + SEPARATOR)) {
            result = getPasswordHash(result, getAlgorithmFromPassword(storedPassword));
        } else if (storedPassword.startsWith(CRYPT_IDENTIFIER + SEPARATOR)) {
            result = getPasswordCrypt(result, getAlgorithmFromPassword(storedPassword));
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

    public String getPasswordHash(String password)
    {
        return getPasswordHash(password, getHashAlgorithm());
    }

    public String getPasswordHash(String password, String algorithmName)
    {
        try {
            log.debug("Hashing password");
            MessageDigest hashAlgorithm = MessageDigest.getInstance(algorithmName);
            hashAlgorithm.update(password.getBytes());
            byte[] digest = hashAlgorithm.digest();
            StringBuffer sb =
                new StringBuffer(HASH_IDENTIFIER + SEPARATOR + algorithmName + SEPARATOR);
            for (int j = 0; j < digest.length; ++j) {
                int b = digest[j] & 0xFF;
                if (b < 0x10) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            log.error("Wrong hash algorithm [" + algorithmName + "] in [" + getXClassReference() + "]",
                ex);
        } catch (NullPointerException ex) {
            log.error("Error hashing password", ex);
        }
        return password;
    }
}

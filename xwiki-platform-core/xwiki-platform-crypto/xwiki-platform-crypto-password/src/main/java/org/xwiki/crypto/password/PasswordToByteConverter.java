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
package org.xwiki.crypto.password;

import org.bouncycastle.crypto.PBEParametersGenerator;
import org.xwiki.stability.Unstable;

/**
 * Helper class to convert password/passphrase to bytes arrays.
 *
 * @version $Id$
 * @since 5.4M1
 */
@Unstable
public final class PasswordToByteConverter
{
    private PasswordToByteConverter() { }

    /**
     * Conversion mode of password from String to byte array. Defaults to is to use PKCS5_UTF8.
     */
    public enum ToBytesMode
    {
        /** converts according to the scheme in PKCS5 (UTF-8, no padding). */
        PKCS5_UTF8,
        /** converts according to the scheme in PKCS5 (ascii, no padding). */
        PKCS5,
        /** converts according to the scheme in PKCS12 (unicode, big endian, 2 zero pad bytes at the end). */
        PKCS12
    }

    /**
     * Convert password to bytes.
     *
     * @param password password to convert.
     * @return a bytes array representing the password.
     */
    public static byte[] convert(String password)
    {
        return convert(password.toCharArray());
    }

    /**
     * Convert password to bytes.
     *
     * @param password password to convert.
     * @return a bytes array representing the password.
     */
    public static byte[] convert(char[] password)
    {
        return convert(password, ToBytesMode.PKCS5_UTF8);
    }

    /**
     * Convert password to bytes.
     *
     * @param password password to convert.
     * @param mode mode of conversion.
     * @return a bytes array representing the password.
     */
    public static byte[] convert(String password, ToBytesMode mode)
    {
        return convert(password.toCharArray(), mode);
    }

    /**
     * Convert password to bytes.
     *
     * @param password password to convert.
     * @param mode mode of conversion.
     * @return a bytes array representing the password.
     */
    public static byte[] convert(char[] password, ToBytesMode mode)
    {
        byte[] passwd;

        switch (mode) {
            case PKCS12:
                passwd = PBEParametersGenerator.PKCS12PasswordToBytes(password);
                break;
            case PKCS5:
                passwd = PBEParametersGenerator.PKCS5PasswordToBytes(password);
                break;
            default:
                passwd = PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password);
                break;
        }

        return passwd;
    }
}

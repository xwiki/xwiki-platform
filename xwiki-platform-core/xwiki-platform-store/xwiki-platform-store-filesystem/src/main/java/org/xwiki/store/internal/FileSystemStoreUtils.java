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
package org.xwiki.store.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.CharUtils;

/**
 * Various file system module utils.
 * 
 * @version $Id$
 * @since 10.1RC1
 */
public final class FileSystemStoreUtils
{
    /**
     * The standard role hint used by most filesystem store implementations.
     */
    public static final String HINT = "file";

    private FileSystemStoreUtils()
    {
        // Utility class
    }

    /**
     * Return a safe version of the passed name for any filesystem.
     * <p>
     * In practice it means the following:
     * <ul>
     * <li>any forbidden character is encoded with URL escaping format</li>
     * <li>in case of case sensitivity upper case characters are encoded with URL escaping format</li>
     * </ul>
     * 
     * @param name the name to escape
     * @param caseInsensitive true if case insensitive filesystems should be supported
     * @return a safe version of the name
     */
    public static String encode(String name, boolean caseInsensitive)
    {
        StringBuilder builder = new StringBuilder(name.length() * 3);

        for (int i = 0; i < name.length(); ++i) {
            char c = name.charAt(i);

            boolean encode = false;

            switch (c) {
                // % is used for encoding
                // + is used for encoding
                // Characters reserved on Windows and Unix
                // (https://msdn.microsoft.com/en-us/library/windows/desktop/aa365247.aspx#naming_conventions)
                case '%':
                case '+':
                case '<':
                case '>':
                case ':':
                case '"':
                case '/':
                case '\\':
                case '|':
                case '?':
                case '*':
                    encode = true;

                    break;

                case ' ':
                    // White space at the beginning and the end of a file is forbidden on Windows
                    if (i == 0 || i == name.length() - 1) {
                        encode = true;
                    }

                    break;

                case '.':
                    // Dot at the beginning of a file means hidden file on Unix systems
                    // Dot at the end of a file is forbidden on Windows
                    if (i == 0 || i == name.length() - 1) {
                        encode = true;
                    }

                    break;

                default:
                    // Encode any non ASCII character to avoid surprises
                    // For case insensitive filesystem encode upper case characters
                    if (!CharUtils.isAscii(c) || (caseInsensitive && Character.isUpperCase(c))) {
                        encode = true;
                    }

                    break;
            }

            if (encode) {
                encode(c, builder);
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    private static void encode(char c, StringBuilder builder)
    {
        if (c == ' ') {
            builder.append("+");
        } else {
            byte[] ba = String.valueOf(c).getBytes(StandardCharsets.UTF_8);

            for (int j = 0; j < ba.length; j++) {
                builder.append('%');

                char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                // Make it upper case
                ch = Character.toUpperCase(ch);
                builder.append(ch);

                ch = Character.forDigit(ba[j] & 0xF, 16);
                // Make it upper case
                ch = Character.toUpperCase(ch);
                builder.append(ch);
            }
        }
    }

    /**
     * Decode name encoded with {@link #encode(String, boolean)}.
     * 
     * @param name the name to decode
     * @return the decoded name
     */
    public static String decode(String name)
    {
        try {
            return URLDecoder.decode(name, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 encoding is not present on the system!", e);
        }
    }
}

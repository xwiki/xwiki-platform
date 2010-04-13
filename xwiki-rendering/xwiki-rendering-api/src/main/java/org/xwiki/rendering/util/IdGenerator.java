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
package org.xwiki.rendering.util;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Stateful generator of id attributes. It's stateful since it remembers the generated ids. Thus a new instance of it
 * should be used for each document.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class IdGenerator
{
    /**
     * Id allowed characters {@link BitSet}.
     */
    private static final BitSet ALLOWED = new BitSet(256);
    static {
        // digits
        for (int i = '0'; i <= '9'; i++) {
            ALLOWED.set(i);
        }

        // alpha
        for (int i = 'a'; i <= 'z'; i++) {
            ALLOWED.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            ALLOWED.set(i);
        }

        ALLOWED.set(':');
        ALLOWED.set('_');
        ALLOWED.set('.');
        ALLOWED.set('-');
    }

    /**
     * A table of hex digits.
     */
    private static final char[] HEXDIGIT =
    {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Contains the already generated ids.
     */
    private Set<String> generatedIds = new HashSet<String>();

    /**
     * Same as {@link #generateUniqueId(String, String)} but with a fixed prefix of "I".
     * 
     * @param text the text used to generate the unique id
     * @return the unique id. For example "Hello world" will generate "IHelloworld".
     */
    public String generateUniqueId(String text)
    {
        // Note: We always use a prefix (and a prefix with alpha characters) so that the generated id is a valid HTML id
        // (since HTML id must start with an alpha prefix).
        return generateUniqueId("I", text);
    }

    /**
     * Generate a unique id attribute using the passed text as the seed value. The generated id complies with the XHTML
     * specification. Extract from <a href="http://www.w3.org/TR/xhtml1/#C_8">XHTML RFC</a>:
     * <p>
     * <code> When defining fragment identifiers to be backward-compatible, only strings matching the pattern
     * [A-Za-z][A-Za-z0-9:_.-]* should be used.</code>
     * </p>
     * 
     * @param prefix the prefix of the identifier. Has to match [a-zA-Z].
     * @param text the text used to generate the unique id
     * @return the unique id. For example "Hello world" will generate prefix + "Helloworld".
     */
    public String generateUniqueId(String prefix, String text)
    {
        // Verify that the passed prefix contains only alpha characters since the generated id must be a valid HTML id.
        if (StringUtils.isEmpty(prefix) || !StringUtils.isAlpha(prefix)) {
            throw new IllegalArgumentException("The prefix [" + prefix
                + "] should only contain alphanumerical characters and not be empty.");
        }

        String idPrefix = (prefix != null ? prefix : "") + normalizeId(text);

        int occurence = 0;
        String id = idPrefix;
        while (this.generatedIds.contains(id)) {
            occurence++;
            id = idPrefix + "-" + occurence;
        }

        // Save the generated id so that the next call to this method will not generate the same id.
        this.generatedIds.add(id);

        return id;
    }

    /**
     * Normalize passed string into valid string.
     * <ul>
     * <li>Remote white spaces: Clean white space since otherwise they'll get transformed into %20 by the below and thus
     * for "Hello world" we would get "Hello20world" for the id. It's nicer to get "Helloworld".</li>
     * <li>Convert all non allowed characters. See {@link #ALLOWED} for allowed characters.</li>
     * </ul>
     * 
     * @param stringToNormalize the string to normalize
     * @return the normalized string
     */
    private String normalizeId(String stringToNormalize)
    {
        int len = stringToNormalize.length();
        int bufLen = len * 2;
        if (bufLen < 0) {
            bufLen = Integer.MAX_VALUE;
        }
        StringBuffer outBuffer = new StringBuffer(bufLen);

        for (int x = 0; x < len; x++) {
            char c = stringToNormalize.charAt(x);

            if (ALLOWED.get(c)) {
                outBuffer.append(c);
            } else if (!Character.isWhitespace(c)) {
                int nibble;
                boolean skip = true;

                nibble = (c >> 12) & 0xF;
                if (nibble != 0) {
                    skip = false;
                    outBuffer.append(toHex(nibble));
                }

                nibble = (c >> 8) & 0xF;
                if (!skip || nibble != 0) {
                    skip = false;
                    outBuffer.append(toHex(nibble));
                }

                nibble = (c >> 4) & 0xF;
                if (!skip || nibble != 0) {
                    outBuffer.append(toHex(nibble));
                }

                outBuffer.append(toHex(c & 0xF));
            }
        }

        return outBuffer.toString();
    }

    /**
     * Convert a nibble to a hex character.
     * 
     * @param nibble the nibble to convert.
     * @return hex character
     */
    private char toHex(int nibble)
    {
        return HEXDIGIT[(nibble & 0xF)];
    }

    /**
     * Remove the saved previously generated id to make it available again.
     * 
     * @param id the id to remove from the generated ids.
     */
    public void remove(String id)
    {
        this.generatedIds.remove(id);
    }

    /**
     * Reset the known generated ids.
     */
    public void reset()
    {
        this.generatedIds.clear();
    }
}

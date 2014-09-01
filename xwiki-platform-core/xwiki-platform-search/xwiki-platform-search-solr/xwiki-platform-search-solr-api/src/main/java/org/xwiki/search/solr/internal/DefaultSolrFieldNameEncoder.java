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
package org.xwiki.search.solr.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.internal.api.SolrFieldNameEncoder;

/**
 * Default {@link SolrFieldNameEncoder} implementation. It uses an encoding scheme similar to the URL encoding, with '$'
 * (dollar) as the escape character instead of '%'. We couldn't use directly an URL encoding because some characters
 * that are URL valid are not allowed in a field name and the other way around. Also the '%' (percent) character is not
 * allowed in the name of a Solr field. We chose to use '$' instead because it was the least used from the few
 * non-alphanumeric characters that are allowed in the field name. Other options would have been '.' (but it is used to
 * separate the space and page name in a class/property reference), '_' (often used in field names), '-' (which doesn't
 * look natural as an escape character and it's also used more often).
 * 
 * @version $Id$
 * @since 5.3RC1
 */
@Component
@Singleton
public class DefaultSolrFieldNameEncoder implements SolrFieldNameEncoder
{
    /**
     * The character used to escape/encode special characters.
     */
    private static final char ESCAPE = '$';

    /**
     * The code point difference between upper case and lower case.
     */
    private static final int CASE_DIFF = ('a' - 'A');

    /**
     * The UTF-8 character set.
     */
    private static final String UTF8 = "UTF-8";

    @Override
    public String encode(final String fieldName)
    {
        if (fieldName == null) {
            return null;
        }

        int offset = 0;
        final int length = fieldName.length();
        final StringBuilder output = new StringBuilder(length);
        boolean dirty = false;
        while (offset < length) {
            final int codePoint = fieldName.codePointAt(offset);
            final char[] chars = Character.toChars(codePoint);
            if (needsEncoding(codePoint)) {
                encode(chars, output);
                dirty = true;
            } else {
                output.append(chars);
            }
            offset += chars.length;
        }
        return dirty ? output.toString() : fieldName;
    }

    @Override
    public String decode(String fieldName)
    {
        try {
            if (fieldName != null) {
                return URLDecoder.decode(fieldName.replace('$', '%'), UTF8);
            }
        } catch (UnsupportedEncodingException e) {
            // Should never happen.
        }
        return null;
    }

    /**
     * @param codePoint a code point, as returned by {@link String#codePointAt(int)}
     * @return {@code true} if the specified code point needs to be encoded, {@code false} otherwise
     */
    protected boolean needsEncoding(int codePoint)
    {
        return codePoint == ESCAPE
            || !(Character.isJavaIdentifierPart(codePoint) || codePoint == '-' || codePoint == '.');
    }

    /**
     * Encodes the given characters.
     * 
     * @param chars the characters to encode
     * @param output where to write the encoded characters
     */
    protected void encode(char[] chars, StringBuilder output)
    {
        try {
            byte[] bytes = new String(chars).getBytes(UTF8);
            for (int i = 0, length = bytes.length; i < length; i++) {
                output.append(ESCAPE);
                char ch = Character.forDigit((bytes[i] >> 4) & 0xF, 16);
                // Use upper case letters in the hex value.
                if (Character.isLetter(ch)) {
                    ch -= CASE_DIFF;
                }
                output.append(ch);
                ch = Character.forDigit(bytes[i] & 0xF, 16);
                if (Character.isLetter(ch)) {
                    ch -= CASE_DIFF;
                }
                output.append(ch);
            }
        } catch (UnsupportedEncodingException e) {
            // Shouldn't never happen.
        }
    }
}

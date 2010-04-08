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

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.util.EncodingUtil;

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
     * Contains the already generated ids.
     */
    private Set<String> generatedIds = new HashSet<String>();

    /**
     * Generate a unique id attribute using the passed text as the seed value. The generated id complies with the XHTML
     * specification. Extract from <a href="http://www.w3.org/TR/xhtml1/#C_8">XHTML RFC</a>:
     * <p>
     * <quote>When defining fragment identifiers to be backward-compatible, only strings matching the pattern
     * [A-Za-z][A-Za-z0-9:_.-]* should be used. </quote>
     * </p>
     * 
     * @param text the text used to generate the unique id. For example "Hello world" will generate "Helloworld".
     * @return the unique id
     */
    public String generateUniqueId(String text)
    {
        return generateUniqueId("I", text);
    }

    /**
     * Generate a unique id attribute using the passed text as the seed value. The generated id complies with the XHTML
     * specification. Extract from <a href="http://www.w3.org/TR/xhtml1/#C_8">XHTML RFC</a>:
     * <p>
     * <quote> When defining fragment identifiers to be backward-compatible, only strings matching the pattern
     * [A-Za-z][A-Za-z0-9:_.-]* should be used.</quote>
     * </p>
     * 
     * @param prefix the prefix of the identifier. Has to match [a-zA-Z].
     * @param text the text used to generate the unique id. For example "Hello world" will generate "Helloworld".
     * @return the unique id
     */
    public String generateUniqueId(String prefix, String text)
    {
        String idPrefix = text;

        // Clean white space since otherwise they'll get transformed into %20 by the below and thus for
        // "Hello world" we would get "Hello20world" for the id. It's nicer to get "Helloworld".
        idPrefix = idPrefix.replaceAll("\\s", "");

        // convert non alpha-numeric characters
        idPrefix = EncodingUtil.getAsciiString(URLCodec.encodeUrl(ALLOWED, EncodingUtil.getBytes(idPrefix, "UTF-8")));

        // Remove all non alpha numeric characters to make a nice compact id which respect the XHTML specification.
        idPrefix = (prefix != null ? prefix : "") + idPrefix.replace("%", "");

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

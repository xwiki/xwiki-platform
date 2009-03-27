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

import java.util.HashSet;
import java.util.Set;

/**
 * Stateful generator of id attributes. It's stateful since it remembers the generated ids. Thus a new instance of it
 * should be used for each document.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class IdGenerator
{
    /**
     * Contains the already generated ids.
     */
    private Set<String> generatedIds = new HashSet<String>();

    /**
     * Generate a unique id attribute using the passed text as the seed value. The generated id complies with the XHTML
     * specification. Extract from <a
     * href="http://www.devguru.com/technologies/xhtml/QuickRef/xhtml_attribute_id.html">DevGuru</a>:
     * <p>
     * <quote> "The id attribute is used to assign a identifier value to a tag. Each id must be unique within the
     * document and each element can only have one id. In XHTML, the id attribute has essentially replaced the use of
     * the name attribute. The value of the id must start with an alphabetic letter or an underscore. The rest of the
     * value can contain any alpha/numeric character." </quote>
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
     * specification. Extract from <a
     * href="http://www.devguru.com/technologies/xhtml/QuickRef/xhtml_attribute_id.html">DevGuru</a>:
     * <p>
     * <quote> "The id attribute is used to assign a identifier value to a tag. Each id must be unique within the
     * document and each element can only have one id. In XHTML, the id attribute has essentially replaced the use of
     * the name attribute. The value of the id must start with an alphabetic letter or an underscore. The rest of the
     * value can contain any alpha/numeric character." </quote>
     * </p>
     * 
     * @param prefix the prefix of the identifier. Has to match [a-zA-Z].
     * @param text the text used to generate the unique id. For example "Hello world" will generate "Helloworld".
     * @return the unique id
     */
    public String generateUniqueId(String prefix, String text)
    {
        // Remove all non alpha numeric characters to make a nice compact id which respect the XHTML specification.
        String idPrefix;
        if (prefix != null) {
            idPrefix = prefix;
        }
        idPrefix = text.replaceAll("[^a-zA-Z0-9]", "");

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
        generatedIds.remove(id);
    }

    /**
     * Reset the known generated ids.
     */
    public void reset()
    {
        this.generatedIds.clear();
    }
}

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
package org.xwiki.model.reference;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link RegexEntityReference} class.
 * 
 * @version $Id$
 */
public class RegexEntityReferenceTest
{
    private static final DocumentReference REFERENCETOMATCH = new DocumentReference("wiki", "space", "page");

    @Test
    public void equalsWhenExact()
    {
        EntityReference wikiReference =
            new RegexEntityReference(Pattern.compile(REFERENCETOMATCH.getWikiReference().getName(), Pattern.LITERAL),
                EntityType.WIKI);
        EntityReference spaceReference =
            new RegexEntityReference(Pattern.compile(REFERENCETOMATCH.getLastSpaceReference().getName(),
                Pattern.LITERAL), EntityType.SPACE, wikiReference);
        EntityReference reference =
            new RegexEntityReference(Pattern.compile(REFERENCETOMATCH.getName(), Pattern.LITERAL), EntityType.DOCUMENT,
                spaceReference);

        assertTrue(reference.equals(REFERENCETOMATCH));
    }

    @Test
    public void equalsWithOnlyPage()
    {
        EntityReference reference =
            new RegexEntityReference(Pattern.compile(REFERENCETOMATCH.getName(), Pattern.LITERAL), EntityType.DOCUMENT);

        assertTrue(reference.equals(REFERENCETOMATCH));
    }

    @Test
    public void equalsWithOnlyWiki()
    {
        EntityReference reference =
            new RegexEntityReference(Pattern.compile(REFERENCETOMATCH.getWikiReference().getName(), Pattern.LITERAL),
                EntityType.WIKI);

        assertTrue(reference.equals(REFERENCETOMATCH));
    }

    @Test
    public void equalsWithPattern()
    {
        EntityReference reference = new RegexEntityReference(Pattern.compile("p.*"), EntityType.DOCUMENT);

        assertTrue(reference.equals(REFERENCETOMATCH));
    }

    @Test
    public void equalsWhenPatternNotMatching()
    {
        EntityReference reference = new RegexEntityReference(Pattern.compile("space"), EntityType.DOCUMENT);

        assertFalse(reference.equals(REFERENCETOMATCH));
    }

    @Test
    public void equalsWhenNonRegexParent()
    {
        EntityReference reference =
            new RegexEntityReference(Pattern.compile("space"), EntityType.SPACE, new EntityReference("wiki",
                EntityType.WIKI));

        assertTrue(reference.equals(REFERENCETOMATCH));
    }
}

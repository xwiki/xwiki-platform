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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Validate {@link RegexEntityReference} class.
 * 
 * @version $Id$
 */
// RegexEntityReference.equals() is an asymmetric matcher: it runs the regex only when the regex
// reference is the receiver. JUnit evaluates assertEquals(expected, actual) as
// expected.equals(actual), so the regex reference must stay the first argument. Swapping to
// expected-first order (as SonarQube's S3415 suggests) would call the concrete reference's
// equals() and skip regex matching, breaking the tests.
@SuppressWarnings("java:S3415")
class RegexEntityReferenceTest
{
    private static final DocumentReference REFERENCETOMATCH = new DocumentReference("wiki", "space", "page");

    @Test
    void equalsWhenExact()
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

        assertEquals(reference, REFERENCETOMATCH);
    }

    @Test
    void equalsWithOnlyPage()
    {
        EntityReference reference =
            new RegexEntityReference(Pattern.compile(REFERENCETOMATCH.getName(), Pattern.LITERAL), EntityType.DOCUMENT);

        assertEquals(reference, REFERENCETOMATCH);
    }

    @Test
    void equalsWithOnlyWiki()
    {
        EntityReference reference =
            new RegexEntityReference(Pattern.compile(REFERENCETOMATCH.getWikiReference().getName(), Pattern.LITERAL),
                EntityType.WIKI);

        assertEquals(reference, REFERENCETOMATCH);
    }

    @Test
    void equalsWithPattern()
    {
        EntityReference reference = new RegexEntityReference(Pattern.compile("p.*"), EntityType.DOCUMENT);

        assertEquals(reference, REFERENCETOMATCH);
    }

    @Test
    void equalsWhenPatternNotMatching()
    {
        EntityReference reference = new RegexEntityReference(Pattern.compile("space"), EntityType.DOCUMENT);

        assertNotEquals(reference, REFERENCETOMATCH);
    }

    @Test
    void equalsWhenNonRegexParent()
    {
        EntityReference reference =
            new RegexEntityReference(Pattern.compile("space"), EntityType.SPACE, new EntityReference("wiki",
                EntityType.WIKI));

        assertEquals(reference, REFERENCETOMATCH);
    }
}

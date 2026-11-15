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
package org.xwiki.tag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link Tag}.
 *
 * @version $Id$
 */
class TagTest
{
    @Test
    void equalsAndHashCode()
    {
        Tag tag = new Tag("foo");
        assertEquals(tag, tag);
        assertEquals(new Tag("foo"), tag);
        assertEquals(new Tag("foo").hashCode(), tag.hashCode());
        assertNotEquals(new Tag("bar"), tag);
        assertNotEquals("foo", tag);
        assertEquals(new Tag(null), new Tag(null));
        assertNotEquals(new Tag(null), tag);
        assertNotEquals(tag, new Tag(null));
    }

    @Test
    void equalsAndHashCodeIgnoreCase()
    {
        Tag tag = new Tag("Foo");
        assertEquals(new Tag("foo"), tag);
        assertEquals(new Tag("FOO").hashCode(), tag.hashCode());
        // The name is kept as given.
        assertEquals("Foo", tag.name());
    }

    @Test
    void nameAndToString()
    {
        Tag tag = new Tag("foo");
        assertEquals("foo", tag.name());
        assertEquals("foo", tag.toString());
    }
}

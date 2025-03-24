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
package org.xwiki.netflux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;

/**
 * Unit tests for {@link EntityChannel}.
 * 
 * @version $Id$
 */
class EntityChannelTest
{
    @Test
    void hashCodeEquals()
    {
        EntityChannel alice =
            new EntityChannel(new DocumentReference("foo", "Some", "Page"), List.of("en", "content", "wiki"), "123456");
        EntityChannel bob = new EntityChannel(new DocumentReference("foo", "Other", "Page"),
            List.of("en", "content", "wysiwyg"), "qwerty");

        assertEquals(alice, alice);
        assertNotEquals(alice, bob);

        Set<EntityChannel> channels = new HashSet<>();
        channels.add(alice);
        channels.add(bob);
        channels.add(alice);
        channels.add(bob);
        assertEquals(2, channels.size());
    }

    @Test
    void toStringTest()
    {
        EntityChannel alice =
            new EntityChannel(new DocumentReference("foo", "Some", "Page"), List.of("en", "content", "wiki"), "123456");
        assertEquals("entity = [foo:Some.Page], path = [[en], [content], [wiki]], key = [123456]", alice.toString());
    }
}

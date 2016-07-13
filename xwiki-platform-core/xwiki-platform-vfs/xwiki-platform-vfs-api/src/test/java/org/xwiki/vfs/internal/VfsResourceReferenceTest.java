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
package org.xwiki.vfs.internal;

import java.net.URI;

import org.junit.Test;
import org.xwiki.vfs.VfsResourceReference;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link VfsResourceReference}.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class VfsResourceReferenceTest
{
    @Test
    public void constructor()
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:Sandbox.WebHome@my.zip/path/to/file"));
        assertEquals("attach:Sandbox.WebHome@my.zip", reference.getURI().toString());
        assertEquals("path/to/file", reference.getPath());
    }

    @Test
    public void equality()
    {
        VfsResourceReference reference1 =
            new VfsResourceReference(URI.create("scheme:specific"), "a/b");
        reference1.addParameter("key", "value");
        VfsResourceReference reference2 =
            new VfsResourceReference(URI.create("scheme:specific"), "a/b");

        assertNotEquals(reference1, reference2);

        reference2.addParameter("key", "value");
        assertEquals(reference1, reference2);
        assertEquals(reference1.hashCode(), reference2.hashCode());
    }

    @Test
    public void toURI()
    {
        VfsResourceReference reference =
            new VfsResourceReference(URI.create("scheme:specific"), "a/b");

        URI expected = URI.create("scheme:specific/a/b");
        assertEquals(expected, reference.toURI());
    }

    @Test
    public void stringValue()
    {
        VfsResourceReference reference =
            new VfsResourceReference(URI.create("scheme:specific"), "a/b");
        reference.addParameter("key", "value");
        assertEquals("uri = [scheme:specific], path = [a/b], parameters = [[key] = [[value]]]", reference.toString());
    }
}

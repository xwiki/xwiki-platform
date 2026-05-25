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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsResourceReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultVfsManager}.
 *
 * @version $Id$
 * @since 7.4M2
 */
@ComponentTest
class DefaultVfsManagerTest
{
    @InjectMockComponents
    private DefaultVfsManager manager;

    @MockComponent
    private ResourceReferenceSerializer<VfsResourceReference, ExtendedURL> serializer;

    @Test
    void getURL() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:xwiki:space.page@attachment"), "path1/path2/test.txt");

        when(this.serializer.serialize(reference)).thenReturn(new ExtendedURL(List.of("generated", "url")));

        assertEquals("/generated/url", this.manager.getURL(reference));
    }

    @Test
    void getURLError() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:xwiki:space.page@attachment"), "path1/path2/test.txt");

        when(this.serializer.serialize(reference)).thenThrow(new SerializeResourceReferenceException("error"));

        VfsException exception = assertThrows(VfsException.class, () -> this.manager.getURL(reference));
        assertEquals("Failed to compute URL for [scheme = [attach], reference = [xwiki:space.page@attachment], "
            + "path = [path1/path2/test.txt], parameters = []]", exception.getMessage());
    }
}

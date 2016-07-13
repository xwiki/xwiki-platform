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
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;
import org.xwiki.vfs.VfsException;
import org.xwiki.vfs.VfsResourceReference;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultVfsManager}.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class DefaultVfsManagerTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultVfsManager> mocker =
        new MockitoComponentMockingRule<>(DefaultVfsManager.class);

    @Test
    public void getURL() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:xwiki:space.page@attachment"), "path1/path2/test.txt");

        ResourceReferenceSerializer<VfsResourceReference, ExtendedURL> serializer = this.mocker.getInstance(
            new DefaultParameterizedType(null, ResourceReferenceSerializer.class, VfsResourceReference.class,
                ExtendedURL.class));
        when(serializer.serialize(reference)).thenReturn(new ExtendedURL(Arrays.asList("generated", "url")));

        assertEquals("/generated/url", this.mocker.getComponentUnderTest().getURL(reference));
    }

    @Test
    public void getURLerror() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:xwiki:space.page@attachment"), "path1/path2/test.txt");

        ResourceReferenceSerializer<VfsResourceReference, ExtendedURL> serializer = this.mocker.getInstance(
            new DefaultParameterizedType(null, ResourceReferenceSerializer.class, VfsResourceReference.class,
                ExtendedURL.class));
        when(serializer.serialize(reference)).thenThrow(new SerializeResourceReferenceException("error"));

        try {
            this.mocker.getComponentUnderTest().getURL(reference);
            fail("Should have thrown an exception");
        } catch (VfsException expected) {
            assertEquals("Failed to compute URL for [uri = [attach:xwiki:space.page@attachment], "
                + "path = [path1/path2/test.txt], parameters = []]", expected.getMessage());
        }
    }
}

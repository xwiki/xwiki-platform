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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;
import org.xwiki.vfs.VfsResourceReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VfsResourceReferenceSerializer}.
 *
 * @version $Id$
 * @since 7.4M2
 */
@ComponentTest
public class VfsResourceReferenceSerializerTest
{
    @InjectMockComponents
    private VfsResourceReferenceSerializer vfsResourceReferenceSerializer;

    @MockComponent
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> urlNormalizer;

    @MockComponent
    @Named("attach")
    private ResourceReferenceSerializer<VfsResourceReference, ExtendedURL> serializer;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @BeforeComponent
    public void setup(MockitoComponentManager componentManager)
    {
        when(this.componentManagerProvider.get()).thenReturn(componentManager);
    }

    @Test
    public void serializeWhenNoSpecificSchemeSerializer() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("somescheme:wiki:space.page@attachment"), "path1/path2/test.txt");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList(
            "vfs", "somescheme:wiki:space.page@attachment", "path1", "path2", "test.txt"));

        when(this.urlNormalizer.normalize(extendedURL)).thenReturn(extendedURL);

        assertEquals("/vfs/somescheme%3Awiki%3Aspace.page%40attachment/path1/path2/test.txt",
            this.vfsResourceReferenceSerializer.serialize(reference).toString());
    }

    @Test
    public void serializeWhenSpecificSchemeSerializer() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:attachment"), "path1/path2/test.txt");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList(
            "vfs", "attach:wiki:space.page@attachment", "path1", "path2", "test.txt"));
        when(serializer.serialize(reference)).thenReturn(extendedURL);

        assertEquals("/vfs/attach%3Awiki%3Aspace.page%40attachment/path1/path2/test.txt",
            this.vfsResourceReferenceSerializer.serialize(reference).toString());
    }
}

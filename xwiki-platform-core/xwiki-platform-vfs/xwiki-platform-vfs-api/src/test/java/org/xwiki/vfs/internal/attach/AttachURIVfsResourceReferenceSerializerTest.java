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
package org.xwiki.vfs.internal.attach;

import java.net.URI;
import java.util.Arrays;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.vfs.VfsResourceReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AttachURIVfsResourceReferenceSerializer}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@ComponentTest
class AttachURIVfsResourceReferenceSerializerTest
{
    @InjectMockComponents
    private AttachURIVfsResourceReferenceSerializer vfsResourceReferenceSerializer;

    @MockComponent
    @Named("current")
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Test
    void serialize() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:xwiki:Toto.WebHome@testvfs.zip"), "path1/path2/test.txt");

        AttachmentReference attachmentReference = new AttachmentReference("testvfs.zip",
            new DocumentReference("xwiki", Arrays.asList("Toto"), "WebHome"));
        when(attachmentReferenceResolver.resolve("xwiki:Toto.WebHome@testvfs.zip")).thenReturn(attachmentReference);

        when(entityReferenceSerializer.serialize(attachmentReference.getDocumentReference()))
            .thenReturn("xwiki:Toto.WebHome");

        assertEquals("attach://xwiki:Toto.WebHome/testvfs.zip/path1/path2/test.txt",
            vfsResourceReferenceSerializer.serialize(reference).toString());
    }

    @Test
    void serializeWithSpace() throws Exception
    {
        VfsResourceReference reference = new VfsResourceReference(
            URI.create("attach:xwiki:Toto.WebHome@testvfs.zip"), "path1/path2/xwiki logo.png");

        AttachmentReference attachmentReference = new AttachmentReference("testvfs.zip",
            new DocumentReference("xwiki", Arrays.asList("Toto"), "WebHome"));
        when(attachmentReferenceResolver.resolve("xwiki:Toto.WebHome@testvfs.zip")).thenReturn(attachmentReference);

        when(entityReferenceSerializer.serialize(attachmentReference.getDocumentReference()))
            .thenReturn("xwiki:Toto.WebHome");

        assertEquals("attach://xwiki:Toto.WebHome/testvfs.zip/path1/path2/xwiki%20logo.png",
            vfsResourceReferenceSerializer.serialize(reference).toString());
    }
}

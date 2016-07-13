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
package org.xwiki.wysiwyg.server.wiki;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EntityReferenceConverter}.
 * 
 * @version $Id$
 */
public class EntityReferenceConverterTest
{
    @Rule
    public MockitoComponentMockingRule<EntityReferenceConverter> mocker = new MockitoComponentMockingRule<>(
        EntityReferenceConverter.class);

    @Test
    public void convert() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("dev", Arrays.asList("One", "Two"), "Product");
        AttachmentReference serverAttachmentReference = new AttachmentReference("logo.png", documentReference);

        WikiPageReference wikiPageReference = new WikiPageReference("dev", "One.Two", "Product");
        org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference clientAttachmentReference =
            new org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference("logo.png", wikiPageReference);

        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localEntityReferenceSerializer.serialize(documentReference.getLastSpaceReference())).thenReturn("One.Two");

        SpaceReferenceResolver<String> spaceResolver = this.mocker.getInstance(SpaceReferenceResolver.TYPE_STRING);
        when(spaceResolver.resolve("One.Two", documentReference.getWikiReference())).thenReturn(
            documentReference.getLastSpaceReference());

        assertEquals(clientAttachmentReference, this.mocker.getComponentUnderTest().convert(serverAttachmentReference));
        assertEquals(serverAttachmentReference, this.mocker.getComponentUnderTest().convert(clientAttachmentReference));

        assertEquals(serverAttachmentReference,
            this.mocker.getComponentUnderTest().convert(clientAttachmentReference.getEntityReference()));
        assertEquals(clientAttachmentReference.getEntityReference(),
            this.mocker.getComponentUnderTest().convert((EntityReference) serverAttachmentReference));

        assertNull(this.mocker.getComponentUnderTest().convert((EntityReference) null));
        assertNull(this.mocker.getComponentUnderTest()
            .convert((org.xwiki.gwt.wysiwyg.client.wiki.EntityReference) null));
    }
}

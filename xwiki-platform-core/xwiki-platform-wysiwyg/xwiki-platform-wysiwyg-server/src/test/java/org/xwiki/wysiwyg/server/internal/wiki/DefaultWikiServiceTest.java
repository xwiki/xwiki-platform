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
package org.xwiki.wysiwyg.server.internal.wiki;

import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wysiwyg.server.wiki.EntityReferenceConverter;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultWikiService}.
 */
public class DefaultWikiServiceTest
{
    @Rule
    public MockitoComponentMockingRule<WikiService> mocker = new MockitoComponentMockingRule<WikiService>(
        DefaultWikiService.class);

    @Test
    public void getUploadURL() throws Exception
    {
        CSRFToken csrf = mocker.getInstance(CSRFToken.class);
        when(csrf.getToken()).thenReturn("123");

        WikiPageReference wikiPageReference = new WikiPageReference("wiki", "Space", "Page");
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");

        DocumentAccessBridge documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        String uploadURL = "/upload/to/page";
        when(documentAccessBridge.getDocumentURL(documentReference, "upload", "form_token=123", null)).thenReturn(
            uploadURL);

        EntityReferenceConverter converter = this.mocker.getInstance(EntityReferenceConverter.class);
        when(converter.convert(wikiPageReference)).thenReturn(documentReference);

        assertEquals(uploadURL, mocker.getComponentUnderTest().getUploadURL(wikiPageReference));
    }

    @Test
    public void getVirtualWikiNames() throws Exception
    {
        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getAllIds()).thenReturn(Arrays.asList("foo", "bar"));

        assertEquals(Arrays.asList("bar", "foo"), this.mocker.getComponentUnderTest().getVirtualWikiNames());
    }
}

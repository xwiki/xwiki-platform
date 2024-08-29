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
package org.xwiki.icon.internal;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconType;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.document.DefaultDocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.DefaultIconSetLoader}.
 *
 * @version $Id$
 * @since 6.2M1
 */
@ComponentTest
class DefaultIconSetLoaderTest
{
    @InjectMockComponents
    private DefaultIconSetLoader iconSetLoader;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private DocumentAuthorizationManager documentAuthorizationManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentUserSerializer;

    @BeforeEach
    void setUp()
    {
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
    }

    private void verifies(IconSet result) throws Exception
    {
        assertNotNull(result);
        assertEquals("http://url_to_css", result.getCss());
        assertEquals("IconThemes.Default", result.getSsx());
        assertEquals("IconThemes.JS", result.getJsx());
        assertEquals("{{html clean=\"false\"}}<span class=\"fa fa-$icon\"></span>{{/html}}", result.getRenderWiki());
        assertEquals("<span class=\"fa fa-$icon\"></span>", result.getRenderHTML());
        assertEquals("http://url_to_image/$icon.png", result.getIconUrl());
        assertEquals("fa fa-$icon", result.getIconCssClass());
        assertEquals("anchor", result.getIcon("transmit").getValue());
        assertEquals("globe", result.getIcon("earth").getValue());
        assertEquals(IconType.FONT, result.getType());
    }

    @Test
    void loadIconSet() throws Exception
    {
        Reader content = new InputStreamReader(getClass().getResourceAsStream("/test.iconset"));

        // Test
        IconSet result = this.iconSetLoader.loadIconSet(content, "FontAwesome");

        // Verify
        verifies(result);
        assertEquals("FontAwesome", result.getName());
    }

    @Test
    void loadIconSetFromWikiDocument() throws Exception
    {
        DocumentReference iconSetRef = new DocumentReference("xwiki", "IconThemes", "Default");
        DocumentReference iconClassRef = new DocumentReference("wikiId", "IconThemesCode", "IconThemeClass");
        when(this.documentAccessBridge.getProperty(iconSetRef, iconClassRef, "name")).thenReturn("MyIconTheme");
        DocumentModelBridge doc = mock(DocumentModelBridge.class);
        when(this.documentAccessBridge.getDocumentInstance(iconSetRef)).thenReturn(doc);

        StringWriter content = new StringWriter();
        IOUtils.copyLarge(new InputStreamReader(getClass().getResourceAsStream("/test.iconset")), content);
        when(doc.getContent()).thenReturn(content.toString());

        DefaultDocumentAuthors authors = new DefaultDocumentAuthors(new XWikiDocument(iconSetRef));
        when(doc.getAuthors()).thenReturn(authors);

        UserReference contentAuthor = mock();
        UserReference metadataAuthor = mock();
        authors.setContentAuthor(contentAuthor);
        authors.setEffectiveMetadataAuthor(metadataAuthor);

        DocumentReference contentAuthorDocumentReference = mock();
        DocumentReference metadataAuthorDocumentReference = mock();
        when(this.documentUserSerializer.serialize(contentAuthor)).thenReturn(contentAuthorDocumentReference);
        when(this.documentUserSerializer.serialize(metadataAuthor)).thenReturn(metadataAuthorDocumentReference);

        // Test
        IconSet result = this.iconSetLoader.loadIconSet(iconSetRef);

        // Verify
        verifies(result);
        assertEquals("MyIconTheme", result.getName());
        verify(this.documentAuthorizationManager).checkAccess(Right.SCRIPT, EntityType.DOCUMENT,
            contentAuthorDocumentReference, iconSetRef);
        verify(this.documentAuthorizationManager).checkAccess(Right.SCRIPT, EntityType.DOCUMENT,
            metadataAuthorDocumentReference, iconSetRef);
    }

    @Test
    void loadIconSetWithException() throws Exception
    {
        Reader content = mock(Reader.class);
        IOException exception = new IOException("test");
        when(content.read(any(char[].class))).thenThrow(exception);

        // Test
        Exception caughtException = assertThrows(IconException.class, () ->
            this.iconSetLoader.loadIconSet(content, "FontAwesome"));

        assertEquals(exception, caughtException.getCause());
        assertEquals("Failed to load the IconSet [FontAwesome].", caughtException.getMessage());
    }

    @Test
    void loadIconSetFromWikiDocumentWithException() throws Exception
    {
        Exception exception = new Exception("test");
        when(this.documentAccessBridge.getDocumentInstance(any(DocumentReference.class))).thenThrow(exception);

        IconException caughtException = assertThrows(IconException.class, () ->
            this.iconSetLoader.loadIconSet(new DocumentReference("a", "b", "c")));

        assertEquals(exception, caughtException.getCause());
        assertEquals("Failed to load the IconSet [a:b.c].", caughtException.getMessage());
    }
}

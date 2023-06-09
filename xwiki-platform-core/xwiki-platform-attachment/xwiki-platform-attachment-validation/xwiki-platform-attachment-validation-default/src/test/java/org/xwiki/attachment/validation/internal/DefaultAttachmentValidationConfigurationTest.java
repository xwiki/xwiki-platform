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
package org.xwiki.attachment.validation.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE;
import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.attachment.validation.internal.AttachmentMimetypeRestrictionClassDocumentInitializer.ALLOWED_MIMETYPES_FIELD;
import static org.xwiki.attachment.validation.internal.AttachmentMimetypeRestrictionClassDocumentInitializer.BLOCKED_MIMETYPES_FIELD;

/**
 * Test of {@link DefaultAttachmentValidationConfiguration}.
 *
 * @version $Id$
 * @since 14.10
 */
@ComponentTest
class DefaultAttachmentValidationConfigurationTest
{
    @InjectMockComponents
    private DefaultAttachmentValidationConfiguration configuration;

    @MockComponent
    @Named(DefaultAttachmentMimetypeRestrictionSpacesConfigurationSource.HINT)
    private ConfigurationSource attachmentConfigurationSource;

    @MockComponent
    @Named(DefaultAttachmentMimetypeRestrictionWikiConfigurationSource.HINT)
    private ConfigurationSource wikiConfigurationSource;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource xWikiPropertiesConfigurationSource;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext xwikiContext;

    @Mock
    private XWiki wiki;

    @BeforeEach
    void setUp()
    {
        when(this.contextProvider.get()).thenReturn(this.xwikiContext);
        when(this.xwikiContext.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void getAllowedMimetypesInConfigurationSource()
    {
        when(this.attachmentConfigurationSource.containsKey(ALLOWED_MIMETYPES_FIELD)).thenReturn(true);
        List<String> expectedMimetypes = List.of("image/*");
        when(this.attachmentConfigurationSource.getProperty(ALLOWED_MIMETYPES_FIELD)).thenReturn(expectedMimetypes);
        assertEquals(expectedMimetypes, this.configuration.getAllowedMimetypes());
        verifyNoInteractions(this.wikiConfigurationSource);
        verifyNoInteractions(this.xWikiPropertiesConfigurationSource);
        verify(this.attachmentConfigurationSource).getProperty(ALLOWED_MIMETYPES_FIELD);
    }

    @Test
    void getAllowedMimetypesInWikiConfigurationSource()
    {
        when(this.attachmentConfigurationSource.containsKey(ALLOWED_MIMETYPES_FIELD)).thenReturn(false);
        when(this.wikiConfigurationSource.containsKey(ALLOWED_MIMETYPES_FIELD)).thenReturn(true);
        List<String> expectedMimetypes = List.of("image/*");
        when(this.wikiConfigurationSource.getProperty(ALLOWED_MIMETYPES_FIELD)).thenReturn(expectedMimetypes);
        assertEquals(expectedMimetypes, this.configuration.getAllowedMimetypes());

        verifyNoInteractions(this.xWikiPropertiesConfigurationSource);
        verify(this.attachmentConfigurationSource, never()).getProperty(ALLOWED_MIMETYPES_FIELD);
    }

    @Test
    void getAllowedMimetypesInXWikiPropertiesConfigurationSource()
    {
        when(this.attachmentConfigurationSource.containsKey(ALLOWED_MIMETYPES_FIELD)).thenReturn(false);
        when(this.wikiConfigurationSource.containsKey(ALLOWED_MIMETYPES_FIELD)).thenReturn(false);
        List<String> expectedMimetypes = List.of("image/*");
        when(this.xWikiPropertiesConfigurationSource.containsKey("attachment.upload.allowList")).thenReturn(true);
        when(this.xWikiPropertiesConfigurationSource.<List<String>>getProperty(
            "attachment.upload.allowList")).thenReturn(expectedMimetypes);
        assertEquals(expectedMimetypes, this.configuration.getAllowedMimetypes());

        verify(this.xWikiPropertiesConfigurationSource).getProperty("attachment.upload.allowList");
        verify(this.attachmentConfigurationSource, never()).getProperty(ALLOWED_MIMETYPES_FIELD);
        verify(this.wikiConfigurationSource, never()).getProperty(ALLOWED_MIMETYPES_FIELD);
    }

    @Test
    void getBlockerMimetypesInConfigurationSource()
    {
        when(this.attachmentConfigurationSource.containsKey(BLOCKED_MIMETYPES_FIELD)).thenReturn(true);
        List<String> expectedMimetypes = List.of("image/*");
        when(this.attachmentConfigurationSource.getProperty(BLOCKED_MIMETYPES_FIELD)).thenReturn(expectedMimetypes);
        assertEquals(expectedMimetypes, this.configuration.getBlockerMimetypes());
        verifyNoInteractions(this.wikiConfigurationSource);
        verifyNoInteractions(this.xWikiPropertiesConfigurationSource);
        verify(this.attachmentConfigurationSource).getProperty(BLOCKED_MIMETYPES_FIELD);
    }

    @Test
    void getBlockerMimetypesInWikiConfigurationSource()
    {
        when(this.attachmentConfigurationSource.containsKey(BLOCKED_MIMETYPES_FIELD)).thenReturn(false);
        when(this.wikiConfigurationSource.containsKey(BLOCKED_MIMETYPES_FIELD)).thenReturn(true);
        List<String> expectedMimetypes = List.of("image/*");
        when(this.wikiConfigurationSource.getProperty(BLOCKED_MIMETYPES_FIELD)).thenReturn(expectedMimetypes);
        assertEquals(expectedMimetypes, this.configuration.getBlockerMimetypes());

        verifyNoInteractions(this.xWikiPropertiesConfigurationSource);
        verify(this.attachmentConfigurationSource, never()).getProperty(BLOCKED_MIMETYPES_FIELD);
    }

    @Test
    void getBlockerMimetypesInXWikiPropertiesConfigurationSource()
    {
        when(this.attachmentConfigurationSource.containsKey(BLOCKED_MIMETYPES_FIELD)).thenReturn(false);
        when(this.wikiConfigurationSource.containsKey(BLOCKED_MIMETYPES_FIELD)).thenReturn(false);
        List<String> expectedMimetypes = List.of("image/*");
        when(this.xWikiPropertiesConfigurationSource.containsKey("attachment.upload.blockList")).thenReturn(true);
        when(this.xWikiPropertiesConfigurationSource.<List<String>>getProperty("attachment.upload.blockList"))
            .thenReturn(expectedMimetypes);
        assertEquals(expectedMimetypes, this.configuration.getBlockerMimetypes());

        verify(this.xWikiPropertiesConfigurationSource).getProperty("attachment.upload.blockList");
        verify(this.attachmentConfigurationSource, never()).getProperty(BLOCKED_MIMETYPES_FIELD);
        verify(this.wikiConfigurationSource, never()).getProperty(BLOCKED_MIMETYPES_FIELD);
    }

    @Test
    void getAllowedMimeTypesEmptyLists()
    {
        when(this.attachmentConfigurationSource.containsKey(ALLOWED_MIMETYPES_FIELD)).thenReturn(true);
        when(this.attachmentConfigurationSource.getProperty(ALLOWED_MIMETYPES_FIELD)).thenReturn(List.of());
        when(this.wikiConfigurationSource.containsKey(ALLOWED_MIMETYPES_FIELD)).thenReturn(true);
        when(this.wikiConfigurationSource.getProperty(ALLOWED_MIMETYPES_FIELD)).thenReturn(List.of("image/*"));
        assertEquals(List.of("image/*"), this.configuration.getAllowedMimetypes());
        verify(this.attachmentConfigurationSource).containsKey(ALLOWED_MIMETYPES_FIELD);
        verify(this.attachmentConfigurationSource).getProperty(ALLOWED_MIMETYPES_FIELD);
        verify(this.wikiConfigurationSource).getProperty(ALLOWED_MIMETYPES_FIELD);
        verify(this.wikiConfigurationSource).getProperty(ALLOWED_MIMETYPES_FIELD);
        verifyNoInteractions(this.xWikiPropertiesConfigurationSource);
    }

    @Test
    void getMaxUploadSize()
    {
        XWikiDocument previous = mock(XWikiDocument.class);
        when(this.xwikiContext.getDoc()).thenReturn(previous);
        this.configuration.getMaxUploadSize(null);
        verify(this.wiki).getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, this.xwikiContext);
        verify(this.xwikiContext).setDoc(previous);
    }

    @Test
    void getMaxUploadSizeWithReference() throws Exception
    {
        DocumentReference entityReference = new DocumentReference("xwiki", "Space", "Page");
        XWikiDocument previous = mock(XWikiDocument.class);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(this.xwikiContext.getDoc()).thenReturn(previous);
        when(this.wiki.getDocument(any(EntityReference.class), any())).thenReturn(doc);
        this.configuration.getMaxUploadSize(entityReference);
        verify(this.wiki).getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, this.xwikiContext);
        InOrder inOrder = inOrder(this.xwikiContext);
        inOrder.verify(this.xwikiContext).setDoc(doc);
        inOrder.verify(this.xwikiContext).setDoc(previous);
    }
}

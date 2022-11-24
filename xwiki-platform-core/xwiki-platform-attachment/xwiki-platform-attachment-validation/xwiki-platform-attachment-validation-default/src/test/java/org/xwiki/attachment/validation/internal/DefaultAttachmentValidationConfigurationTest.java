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

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void getAllowedMimetypesInConfigurationSource()
    {
        when(this.attachmentConfigurationSource.containsKey(ALLOWED_MIMETYPES_FIELD)).thenReturn(true);
        List<String> expectedMimetypes = List.of("image/.*");
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
        List<String> expectedMimetypes = List.of("image/.*");
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
        List<String> expectedMimetypes = List.of("image/.*");
        when(this.xWikiPropertiesConfigurationSource.<List<String>>getProperty("attachment.upload.allowList",
            List.of())).thenReturn(expectedMimetypes);
        assertEquals(expectedMimetypes, this.configuration.getAllowedMimetypes());

        verify(this.xWikiPropertiesConfigurationSource).getProperty("attachment.upload.allowList", List.of());
        verify(this.attachmentConfigurationSource, never()).getProperty(ALLOWED_MIMETYPES_FIELD);
        verify(this.wikiConfigurationSource, never()).getProperty(ALLOWED_MIMETYPES_FIELD);
    }

    @Test
    void getBlockerMimetypesInConfigurationSource()
    {
        when(this.attachmentConfigurationSource.containsKey(BLOCKED_MIMETYPES_FIELD)).thenReturn(true);
        List<String> expectedMimetypes = List.of("image/.*");
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
        List<String> expectedMimetypes = List.of("image/.*");
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
        List<String> expectedMimetypes = List.of("image/.*");
        when(this.xWikiPropertiesConfigurationSource.<List<String>>getProperty("attachment.upload.blockList",
            List.of()))
            .thenReturn(expectedMimetypes);
        assertEquals(expectedMimetypes, this.configuration.getBlockerMimetypes());

        verify(this.xWikiPropertiesConfigurationSource).getProperty("attachment.upload.blockList", List.of());
        verify(this.attachmentConfigurationSource, never()).getProperty(BLOCKED_MIMETYPES_FIELD);
        verify(this.wikiConfigurationSource, never()).getProperty(BLOCKED_MIMETYPES_FIELD);
    }
}

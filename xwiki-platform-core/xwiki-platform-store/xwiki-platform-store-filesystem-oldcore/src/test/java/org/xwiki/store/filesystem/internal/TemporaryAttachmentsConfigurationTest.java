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
package org.xwiki.store.filesystem.internal;

import java.io.File;
import java.time.Duration;

import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE;
import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TemporaryAttachmentsConfiguration}.
 *
 * @version $Id$
 */
@ComponentTest
class TemporaryAttachmentsConfigurationTest
{
    @InjectMockComponents
    private TemporaryAttachmentsConfiguration configuration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Test
    void getMaxAge()
    {
        when(this.configurationSource.getProperty("store.temporaryAttachments.maxAge", 24)).thenReturn(24);
        assertEquals(Duration.ofHours(24), this.configuration.getMaxAge());

        when(this.configurationSource.getProperty("store.temporaryAttachments.maxAge", 24)).thenReturn(2);
        assertEquals(Duration.ofHours(2), this.configuration.getMaxAge());

        when(this.configurationSource.getProperty("store.temporaryAttachments.maxAge", 24)).thenReturn(0);
        assertEquals(Duration.ofHours(24), this.configuration.getMaxAge());
    }

    @Test
    void getDirectory()
    {
        assertNull(this.configuration.getDirectory());

        when(this.configurationSource.getProperty("store.temporaryAttachments.directory")).thenReturn(" ");
        assertNull(this.configuration.getDirectory());

        when(this.configurationSource.getProperty("store.temporaryAttachments.directory")).thenReturn("/shared/tmp");
        assertEquals(new File("/shared/tmp"), this.configuration.getDirectory());
    }

    @Test
    void getMaxCount()
    {
        when(this.configurationSource.getProperty("store.temporaryAttachments.maxCount", 100)).thenReturn(7);
        assertEquals(7, this.configuration.getMaxCount());
    }

    @Test
    void getMaxTotalSize()
    {
        when(this.configurationSource.getProperty("store.temporaryAttachments.maxTotalSize", Long.class))
            .thenReturn(1000L);
        assertEquals(1000L, this.configuration.getMaxTotalSize());
    }

    @Test
    void getMaxTotalSizeDefault()
    {
        // No context.
        assertEquals(10 * UPLOAD_DEFAULT_MAXSIZE, this.configuration.getMaxTotalSize());

        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);
        // No XWiki instance yet.
        assertEquals(10 * UPLOAD_DEFAULT_MAXSIZE, this.configuration.getMaxTotalSize());

        XWiki xwiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xwiki);
        when(xwiki.getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, context))
            .thenReturn(1000L);
        assertEquals(10000L, this.configuration.getMaxTotalSize());
    }
}

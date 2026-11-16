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
package org.xwiki.localization.legacy.internal.xwikipreferences;

import java.util.Locale;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link XWikiPreferencesTranslationBundle}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiPreferencesTranslationBundleTest
{
    @InjectMockComponents
    private XWikiPreferencesTranslationBundle bundle;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private XWikiContext xcontext;

    @BeforeEach
    void setUp()
    {
        when(this.contextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(mock(XWiki.class));
    }

    @Test
    void getTranslationOnExistingWiki() throws Exception
    {
        when(this.xcontext.getWikiId()).thenReturn("wiki");
        when(this.wikiDescriptorManager.exists("wiki")).thenReturn(true);

        assertNull(this.bundle.getTranslation("key", Locale.ROOT));
        assertNull(this.bundle.getTranslation("key", Locale.ROOT));

        // The wiki bundle is created once and then reused.
        verify(this.observationManager).addListener(any(XWikiPreferencesWikiTranslationBundle.class));
    }

    @Test
    void getTranslationOnUnknownWiki() throws Exception
    {
        when(this.xcontext.getWikiId()).thenReturn("unknownwiki");

        assertNull(this.bundle.getTranslation("key", Locale.ROOT));

        verify(this.observationManager, never()).addListener(any());
    }

    @Test
    void getTranslationWhenWikiExistenceCheckFails() throws Exception
    {
        when(this.xcontext.getWikiId()).thenReturn("brokenwiki");
        when(this.wikiDescriptorManager.exists("brokenwiki")).thenThrow(new WikiManagerException("Store failure"));

        assertNull(this.bundle.getTranslation("key", Locale.ROOT));

        verify(this.observationManager, never()).addListener(any());
        assertEquals("Failed to check if wiki [brokenwiki] exists. Root cause: [WikiManagerException: Store failure]",
            this.logCapture.getMessage(0));
    }
}

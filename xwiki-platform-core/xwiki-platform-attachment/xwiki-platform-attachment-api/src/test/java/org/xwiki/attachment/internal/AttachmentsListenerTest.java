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
package org.xwiki.attachment.internal;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.attachment.internal.listener.AttachmentsListener;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.AttachmentAddedEvent;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link AttachmentsListener}.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@ComponentTest
class AttachmentsListenerTest
{
    @InjectMockComponents
    private AttachmentsListener attachmentsListener;

    @MockComponent
    private AttachmentsManager attachmentsManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    @Mock
    private XWikiDocument xWikiDocument;

    @Mock
    private XWikiContext xWikiContext;

    @Mock
    private XWiki wiki;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xWikiContext);
        when(this.xWikiContext.getWiki()).thenReturn(this.wiki);
    }

    @Test
    void onEventNoExistingRedirection()
    {
        AttachmentAddedEvent attachmentAddedEvent = new AttachmentAddedEvent("Space.Doc", "filename.txt");
        this.attachmentsListener.onEvent(attachmentAddedEvent, this.xWikiDocument, null);
        when(this.attachmentsManager.removeExistingRedirection("filename.txt", this.xWikiDocument)).thenReturn(false);
        verifyNoInteractions(this.xcontextProvider);
        verifyNoInteractions(this.contextualLocalizationManager);
    }

    @Test
    void onEvent() throws Exception
    {
        when(this.contextualLocalizationManager.getTranslationPlain(
            "attachment.listener.attachmentAdded.removeRedirection", "filename.txt"))
            .thenReturn("message");
        AttachmentAddedEvent attachmentAddedEvent = new AttachmentAddedEvent("Space.Doc", "filename.txt");
        when(this.attachmentsManager.removeExistingRedirection("filename.txt", this.xWikiDocument)).thenReturn(true);
        this.attachmentsListener.onEvent(attachmentAddedEvent, this.xWikiDocument, null);
        verify(this.wiki).saveDocument(this.xWikiDocument, "message", true, this.xWikiContext);
    }

    @Test
    void onEventFailOnSave() throws Exception
    {
        when(this.contextualLocalizationManager.getTranslationPlain(
            "attachment.listener.attachmentAdded.removeRedirection", "filename.txt"))
            .thenReturn("message");
        AttachmentAddedEvent attachmentAddedEvent = new AttachmentAddedEvent("Space.Doc", "filename.txt");
        when(this.attachmentsManager.removeExistingRedirection("filename.txt", this.xWikiDocument)).thenReturn(true);
        doThrow(new XWikiException()).when(this.wiki)
            .saveDocument(this.xWikiDocument, "message", true, this.xWikiContext);
        this.attachmentsListener.onEvent(attachmentAddedEvent, this.xWikiDocument, null);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Unable to remove deprecated attachment redirection object from [xWikiDocument] "
                + "for attachment [filename.txt]. Cause: [XWikiException: Error number 0 in 0].",
            this.logCapture.getMessage(0));
    }
}

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
package org.xwiki.mail.internal;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.Session;

import org.junit.jupiter.api.Test;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.internal.thread.MailQueueManager;
import org.xwiki.mail.internal.thread.PrepareMailQueueItem;
import org.xwiki.mail.internal.thread.context.Copier;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultMailSender}.
 *
 * @version $Id$
 */
@ComponentTest
class MailSenderTest
{
    @InjectMockComponents
    private DefaultMailSender mailSender;

    @MockComponent
    private Copier<ExecutionContext> executionContextCloner;

    @MockComponent
    private MailQueueManager<PrepareMailQueueItem> prepareMailQueueManager;

    @MockComponent
    private Execution execution;

    @Test
    void sendAsynchronouslyWhenAddIsInterrupted() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWikiId()).thenReturn("wiki");
        ExecutionContext copiedContext = mock(ExecutionContext.class);
        when(copiedContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).thenReturn(xcontext);
        when(this.executionContextCloner.copy(null)).thenReturn(copiedContext);
        Session session = Session.getInstance(new Properties());
        doThrow(new InterruptedException("error")).when(this.prepareMailQueueManager).addMessageToQueue(
            any(PrepareMailQueueItem.class), anyLong(), any(TimeUnit.class));

        Throwable exception = assertThrows(RuntimeException.class, () ->
            this.mailSender.sendAsynchronously(mock(Iterable.class), session, mock(MailListener.class)));
        assertLinesMatch(List.of("Mail items couldn't be added to the prepare queue as it was interrupted. "
            + "The following messages will be lost: \\[.*\\]..."), List.of(exception.getMessage()));
    }

    @Test
    void sendAsynchronouslyWhenAddTimesOut() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getWikiId()).thenReturn("wiki");
        ExecutionContext copiedContext = mock(ExecutionContext.class);
        when(copiedContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).thenReturn(xcontext);
        when(this.executionContextCloner.copy(null)).thenReturn(copiedContext);
        Session session = Session.getInstance(new Properties());
        when(this.prepareMailQueueManager.addMessageToQueue(any(PrepareMailQueueItem.class), anyLong(),
            any(TimeUnit.class))).thenReturn(false);

        Throwable exception = assertThrows(RuntimeException.class, () ->
            this.mailSender.sendAsynchronously(mock(Iterable.class), session, mock(MailListener.class)));
        assertLinesMatch(List.of("The mail prepare queue is still full after waiting \\[60\\] \\[SECONDS\\]. The "
            + "following messages will be lost: \\[.*\\]..."), List.of(exception.getMessage()));
    }

    @Test
    void sendAsynchronouslyWhenNoWikiIdInCopiedContext()
    {
        XWikiContext xcontext2 = mock(XWikiContext.class);
        when(xcontext2.getWikiId()).thenReturn("wiki");
        ExecutionContext originalContext = mock(ExecutionContext.class);
        when(originalContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).thenReturn(xcontext2);
        when(this.execution.getContext()).thenReturn(originalContext);

        XWikiContext xcontext1 = mock(XWikiContext.class);
        ExecutionContext copiedContext = mock(ExecutionContext.class);
        when(copiedContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).thenReturn(xcontext1);
        when(this.executionContextCloner.copy(originalContext)).thenReturn(copiedContext);

        Session session = Session.getInstance(new Properties());

        Throwable exception = assertThrows(RuntimeException.class, () ->
            this.mailSender.sendAsynchronously(mock(Iterable.class), session, mock(MailListener.class)));
        assertEquals("Aborting Mail Sending: the Wiki Id must not be null in the XWiki Context. Got [wiki] in the "
            + "original context.", exception.getMessage());
    }
}

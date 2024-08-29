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

import java.util.Collections;
import java.util.UUID;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatabaseMailListener}.
 *
 * @version $Id$
 * @since 6.4M3
 */
@ComponentTest
class DatabaseMailListenerTest
{
    @InjectMockComponents
    private DatabaseMailListener listener;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("database")
    private MailStatusStore mailStatusStore;

    @MockComponent
    @Named("filesystem")
    private MailContentStore mailContentStore;

    private ExtendedMimeMessage message;

    private String batchId = UUID.randomUUID().toString();

    private String mimeMessageId = "<1128820400.0.1419205781342.JavaMail.contact@xwiki.org>";

    private String messageId;

    @BeforeEach
    void setUp() throws Exception
    {
        this.message = new ExtendedMimeMessage();
        this.message.setType("type");
        this.message.saveChanges();
        this.message.setHeader("Message-ID", this.mimeMessageId);
        this.messageId = this.message.getUniqueMessageId();

        ExecutionContext executionContext = Mockito.mock(ExecutionContext.class);
        XWikiContext xcontext = Mockito.mock(XWikiContext.class);
        when(xcontext.getWikiId()).thenReturn("mywiki");
        when(executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).thenReturn(xcontext);
        when(this.execution.getContext()).thenReturn(executionContext);
    }

    @Test
    void onPrepareSuccess() throws Exception
    {
        this.listener.onPrepareBegin(this.batchId, Collections.emptyMap());
        this.listener.onPrepareMessageSuccess(this.message, Collections.emptyMap());

        verify(this.mailStatusStore).save(argThat(new isSameMailStatus(MailState.PREPARE_SUCCESS, "mywiki")), anyMap());
    }

    @Test
    void onPrepareError() throws Exception
    {
        this.listener.onPrepareBegin(this.batchId, Collections.emptyMap());
        this.listener.onPrepareMessageError(this.message, new Exception("Error"), Collections.emptyMap());

        verify(this.mailStatusStore).save(argThat(new isSameMailStatus(MailState.PREPARE_ERROR, "mywiki")), anyMap());
    }

    @Test
    void onPrepareWhenSaveFails() throws Exception
    {
        doThrow(new MailStoreException("error")).when(this.mailStatusStore).save(any(MailStatus.class), anyMap());

        this.listener.onPrepareBegin(this.batchId, Collections.emptyMap());
        this.listener.onPrepareMessageSuccess(this.message, Collections.emptyMap());

        ArgumentCaptor<MailStatus> statusCapture = ArgumentCaptor.forClass(MailStatus.class);
        verify(this.mailStatusStore).save(statusCapture.capture(), anyMap());

        assertEquals("Failed to save mail status [messageId = [" + this.messageId + "], batchId = ["
            + this.batchId + "], state = [prepare_success], date = [" + statusCapture.getValue().getDate() + "], "
            + "recipients = [<null>], type = [type], wiki = [mywiki]] to the database", logCapture.getMessage(0));
    }

    @Test
    void onSendMessageSuccess() throws Exception
    {
        MailStatus status = new MailStatus(this.batchId, this.message, MailState.PREPARE_SUCCESS);
        status.setWiki("otherwiki");
        when(this.mailStatusStore.load(this.messageId)).thenReturn(status);

        this.listener.onPrepareBegin(this.batchId, Collections.emptyMap());
        this.listener.onSendMessageSuccess(this.message, Collections.emptyMap());

        verify(this.mailStatusStore).load(this.messageId);
        verify(this.mailStatusStore).save(argThat(new isSameMailStatus(MailState.SEND_SUCCESS, "otherwiki")), anyMap());

        verify(this.mailContentStore).delete(this.batchId, this.messageId);
    }

    @Test
    void onSuccessWhenStatusLoadFails() throws Exception
    {
        when(this.mailStatusStore.load(this.messageId)).thenThrow(new MailStoreException("error"));

        this.listener.onPrepareBegin(this.batchId, Collections.emptyMap());
        this.listener.onSendMessageSuccess(this.message, Collections.emptyMap());

        assertEquals("Error when looking for a previous mail status for message [" + this.messageId + "] of batch ["
            + this.batchId + "] and state [send_success].", logCapture.getMessage(0));
        assertEquals("Forcing a new mail status for message [" + this.messageId + "] of batch [" + this.batchId
            + "] to send_success state.", logCapture.getMessage(1));

        // Verify that save and delete happened
        verify(this.mailStatusStore).save(any(MailStatus.class), anyMap());
        verify(this.mailContentStore).delete(any(), any());
    }

    @Test
    void onSuccessWhenMailContentDeletionFails() throws Exception
    {
        MailStatus status = new MailStatus(this.batchId, this.message, MailState.PREPARE_SUCCESS);
        status.setWiki("otherwiki");
        when(this.mailStatusStore.load(this.messageId)).thenReturn(status);

        doThrow(new MailStoreException("error")).when(this.mailContentStore).delete(this.batchId, this.messageId);

        this.listener.onPrepareBegin(this.batchId, Collections.emptyMap());
        this.listener.onSendMessageSuccess(this.message, Collections.emptyMap());

        assertEquals("Failed to remove previously failing message [" + this.messageId + "] (batch id ["
            + this.batchId + "]) from the file system. Reason [MailStoreException: error].",
            logCapture.getMessage(0));
    }

    @Test
    void onSendMessageError() throws Exception
    {
        MailStatus status = new MailStatus(this.batchId, this.message, MailState.PREPARE_SUCCESS);
        status.setWiki("otherwiki");
        when(this.mailStatusStore.load(this.messageId)).thenReturn(status);

        this.listener.onPrepareBegin(this.batchId, Collections.emptyMap());
        this.listener.onSendMessageError(this.message, new Exception("Error"), Collections.emptyMap());

        verify(this.mailStatusStore).load(this.messageId);
        verify(this.mailStatusStore).save(argThat(new isSameMailStatus(MailState.SEND_ERROR, "otherwiki")), anyMap());
    }

    @Test
    void onSendMessageFatalError() throws Exception
    {
        MailStatus status = new MailStatus(this.batchId, this.message, MailState.PREPARE_SUCCESS);
        status.setWiki("otherwiki");
        when(this.mailStatusStore.load(this.messageId)).thenReturn(status);

        this.listener.onPrepareBegin(this.batchId, Collections.emptyMap());
        this.listener.onSendMessageFatalError(this.messageId, new Exception("Error"), Collections.emptyMap());

        verify(this.mailStatusStore).load(this.messageId);
        verify(this.mailStatusStore).save(argThat(new isSameMailStatus(MailState.SEND_FATAL_ERROR, "otherwiki")),
            anyMap());
    }

    /**
     * Custom Mokito Argument Matchers.
     */
    class isSameMailStatus implements ArgumentMatcher<MailStatus>
    {
        private String state;

        private String wikiId;

        public isSameMailStatus(MailState state, String wikiId)
        {
            this.state = state.toString();
            this.wikiId = wikiId;
        }

        @Override
        public boolean matches(MailStatus argument)
        {
            return argument.getBatchId().equals(batchId) &&
                argument.getMessageId().equals(messageId) &&
                argument.getType().equals("type") &&
                argument.getState().equals(state) &&
                argument.getWiki().equals(wikiId);
        }
    }
}

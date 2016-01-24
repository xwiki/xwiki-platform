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

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatabaseMailListener}.
 *
 * @version $Id$
 * @since 6.4M3
 */
public class DatabaseMailListenerTest
{
    @Rule
    public AllLogRule logRule = new AllLogRule();

    @Rule
    public MockitoComponentMockingRule<DatabaseMailListener> mocker =
        new MockitoComponentMockingRule<>(DatabaseMailListener.class, Arrays.asList(Logger.class));

    private ExtendedMimeMessage message;

    private String batchId = UUID.randomUUID().toString();

    private String mimeMessageId = "<1128820400.0.1419205781342.JavaMail.contact@xwiki.org>";

    private String messageId;

    @Before
    public void setUp() throws Exception
    {
        this.message = new ExtendedMimeMessage();
        this.message.setType("type");
        this.message.saveChanges();
        this.message.setHeader("Message-ID", mimeMessageId);
        this.messageId = message.getUniqueMessageId();

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = Mockito.mock(ExecutionContext.class);
        XWikiContext xcontext = Mockito.mock(XWikiContext.class);
        when(xcontext.getWikiId()).thenReturn("mywiki");
        when(executionContext.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY)).thenReturn(xcontext);
        when(execution.getContext()).thenReturn(executionContext);
    }

    @Test
    public void onPrepareSuccess() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");

        MailListener listener = this.mocker.getComponentUnderTest();
        listener.onPrepareBegin(batchId, Collections.<String, Object>emptyMap());
        listener.onPrepareMessageSuccess(this.message, Collections.<String, Object>emptyMap());

        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.PREPARE_SUCCESS, "mywiki")), anyMap());
    }

    @Test
    public void onPrepareError() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");

        MailListener listener = this.mocker.getComponentUnderTest();
        listener.onPrepareBegin(batchId, Collections.<String, Object>emptyMap());
        listener.onPrepareMessageError(this.message, new Exception("Error"), Collections.<String, Object>emptyMap());

        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.PREPARE_ERROR, "mywiki")), anyMap());
    }

    @Test
    public void onPrepareWhenSaveFails() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        doThrow(new MailStoreException("error")).when(mailStatusStore).save(any(MailStatus.class), anyMap());

        MailListener listener = this.mocker.getComponentUnderTest();
        listener.onPrepareBegin(batchId, Collections.<String, Object>emptyMap());
        listener.onPrepareMessageSuccess(this.message, Collections.<String, Object>emptyMap());

        ArgumentCaptor<MailStatus> statusCapture = ArgumentCaptor.forClass(MailStatus.class);
        verify(mailStatusStore).save(statusCapture.capture(), anyMap());

        assertEquals("Failed to save mail status [messageId = [" + this.messageId + "], batchId = ["
            + this.batchId + "], state = [prepare_success], date = [" + statusCapture.getValue().getDate() + "], "
            + "recipients = [<null>], type = [type], wiki = [mywiki]] to the database", this.logRule.getMessage(0));
    }

    @Test
    public void onSendMessageSuccess() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        MailStatus status = new MailStatus(this.batchId, this.message, MailState.PREPARE_SUCCESS);
        status.setWiki("otherwiki");
        when(mailStatusStore.load(this.messageId)).thenReturn(status);

        MailListener listener = this.mocker.getComponentUnderTest();
        listener.onPrepareBegin(batchId, Collections.<String, Object>emptyMap());
        listener.onSendMessageSuccess(this.message, Collections.<String, Object>emptyMap());

        verify(mailStatusStore).load(this.messageId);
        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.SEND_SUCCESS, "otherwiki")), anyMap());

        MailContentStore mailContentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        verify(mailContentStore).delete(this.batchId, this.messageId);
    }

    @Test
    public void onSuccessWhenStatusLoadFails() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        when(mailStatusStore.load(this.messageId)).thenThrow(new MailStoreException("error"));

        MailListener listener = this.mocker.getComponentUnderTest();
        listener.onPrepareBegin(batchId, Collections.<String, Object>emptyMap());
        listener.onSendMessageSuccess(this.message, Collections.<String, Object>emptyMap());

        assertEquals("Error when looking for a previous mail status for message [" + this.messageId + "] of batch ["
            + batchId + "].", this.logRule.getMessage(0));
        assertEquals("Forcing a new mail status for message [" + this.messageId + "] of batch [" + batchId
            + "] to send_success state.", this.logRule.getMessage(1));

        // Verify that save and delete happened
        verify(mailStatusStore).save(any(MailStatus.class), anyMap());
        MailContentStore mailContentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        verify(mailContentStore).delete(anyString(), anyString());
    }

    @Test
    public void onSuccessWhenMailContentDeletionFails() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        MailStatus status = new MailStatus(this.batchId, this.message, MailState.PREPARE_SUCCESS);
        status.setWiki("otherwiki");
        when(mailStatusStore.load(this.messageId)).thenReturn(status);

        MailContentStore mailContentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        doThrow(new MailStoreException("error")).when(mailContentStore).delete(this.batchId, this.messageId);

        MailListener listener = this.mocker.getComponentUnderTest();
        listener.onPrepareBegin(batchId, Collections.<String, Object>emptyMap());
        listener.onSendMessageSuccess(this.message, Collections.<String, Object>emptyMap());

        assertEquals("Failed to remove previously failing message [" + this.messageId + "] (batch id ["
                + this.batchId + "]) from the file system. Reason [MailStoreException: error].",
            this.logRule.getMessage(0));
    }

    @Test
    public void onSendMessageError() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        MailStatus status = new MailStatus(this.batchId, this.message, MailState.PREPARE_SUCCESS);
        status.setWiki("otherwiki");
        when(mailStatusStore.load(this.messageId)).thenReturn(status);

        MailListener listener = this.mocker.getComponentUnderTest();
        listener.onPrepareBegin(batchId, Collections.<String, Object>emptyMap());
        listener.onSendMessageError(this.message, new Exception("Error"), Collections.<String, Object>emptyMap());

        verify(mailStatusStore).load(this.messageId);
        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.SEND_ERROR, "otherwiki")), anyMap());
    }

    @Test
    public void onSendMessageFatalError() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        MailStatus status = new MailStatus(this.batchId, this.message, MailState.PREPARE_SUCCESS);
        status.setWiki("otherwiki");
        when(mailStatusStore.load(this.messageId)).thenReturn(status);

        MailListener listener = this.mocker.getComponentUnderTest();
        listener.onPrepareBegin(batchId, Collections.<String, Object>emptyMap());
        listener.onSendMessageFatalError(this.messageId, new Exception("Error"), Collections.<String, Object>emptyMap());

        verify(mailStatusStore).load(this.messageId);
        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.SEND_FATAL_ERROR, "otherwiki")), anyMap());
    }

    /**
     * Custom Mokito Argument Matchers.
     */
    class isSameMailStatus extends ArgumentMatcher<MailStatus>
    {
        private String state;

        private String wikiId;

        public isSameMailStatus(MailState state, String wikiId)
        {
            this.state = state.toString();
            this.wikiId = wikiId;
        }

        @Override
        public boolean matches(Object argument)
        {
            MailStatus statusArgument = (MailStatus) argument;
            return statusArgument.getBatchId().equals(batchId) &&
                statusArgument.getMessageId().equals(messageId) &&
                statusArgument.getType().equals("type") &&
                statusArgument.getState().equals(state) &&
                statusArgument.getWiki().equals(wikiId);
        }
    }
}

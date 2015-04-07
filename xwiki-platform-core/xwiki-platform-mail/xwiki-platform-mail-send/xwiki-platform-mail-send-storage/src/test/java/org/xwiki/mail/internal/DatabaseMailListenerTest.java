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
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.slf4j.Logger;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

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

    private MimeMessage message;

    private String batchId = UUID.randomUUID().toString();

    private UUID mailId = UUID.randomUUID();

    @Before
    public void setUp() throws Exception
    {
        Session session = Session.getInstance(new Properties());
        this.message = new MimeMessage(session);
        this.message.setHeader("X-MailID", this.mailId.toString());
        this.message.setHeader("X-BatchID", this.batchId);
        this.message.setHeader("X-MailType", "type");
    }

    @Test
    public void onPrepare() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        Map<String, Object> parameters = Collections.singletonMap("wikiId", (Object) "mywiki");

        this.mocker.getComponentUnderTest().onPrepare(this.message, parameters);

        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.READY, "mywiki")), eq(parameters));
    }

    @Test
    public void onPrepareWhenSaveFails() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        doThrow(new MailStoreException("error")).when(mailStatusStore).save(any(MailStatus.class), anyMap());

        Map < String, Object > parameters = Collections.singletonMap("wikiId", (Object) "mywiki");

        this.mocker.getComponentUnderTest().onPrepare(this.message, parameters);

        ArgumentCaptor<MailStatus> statusCapture = ArgumentCaptor.forClass(MailStatus.class);
        verify(mailStatusStore).save(statusCapture.capture(), anyMap());

        assertEquals("Failed to save mail status [messageId = [" + this.mailId.toString() + "], batchId = ["
            + this.batchId + "], state = [ready], date = [" + statusCapture.getValue().getDate() + "], "
            + "recipients = [<null>], type = [type], wiki = [mywiki]] to the database", this.logRule.getMessage(0));
    }

    @Test
    public void onSuccess() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        Map<String, Object> parameters = Collections.singletonMap("wikiId", (Object) "mywiki");
        MailStatus status = new MailStatus(this.message, MailState.READY);
        status.setWiki("otherwiki");
        when(mailStatusStore.load(Collections.<String, Object>singletonMap("id",
            this.mailId.toString()), 0, 0)).thenReturn(Arrays.asList(status));

        this.mocker.getComponentUnderTest().onSuccess(this.message, parameters);

        verify(mailStatusStore).load(Collections.<String, Object>singletonMap("id", this.mailId.toString()), 0, 0);
        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.SENT, "otherwiki")), eq(parameters));

        MailContentStore mailContentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        verify(mailContentStore).delete(this.batchId, this.mailId.toString());
    }

    @Test
    public void onSuccessWhenStatusLoadFails() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        when(mailStatusStore.load(Collections.<String, Object>singletonMap("id",
            this.mailId.toString()), 0, 0)).thenThrow(new MailStoreException("error"));

        Map<String, Object> parameters = Collections.singletonMap("wikiId", (Object) "mywiki");

        this.mocker.getComponentUnderTest().onSuccess(this.message, parameters);

        assertEquals("Error when looking for a previous mail status for message id [" + this.mailId + "]. However the "
            + "mail was sent successfully. Forcing status to [sent]", this.logRule.getMessage(0));

        // Verify that save and delete happened
        verify(mailStatusStore).save(any(MailStatus.class), anyMap());
        MailContentStore mailContentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        verify(mailContentStore).delete(anyString(), anyString());
    }

    @Test
    public void onSuccessWhenMailContentDeletionFails() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        Map<String, Object> parameters = Collections.singletonMap("wikiId", (Object) "mywiki");
        MailStatus status = new MailStatus(this.message, MailState.READY);
        status.setWiki("otherwiki");
        when(mailStatusStore.load(Collections.<String, Object>singletonMap("id",
            this.mailId.toString()), 0, 0)).thenReturn(Arrays.asList(status));

        MailContentStore mailContentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");
        doThrow(new MailStoreException("error")).when(mailContentStore).delete(this.batchId, this.mailId.toString());

        this.mocker.getComponentUnderTest().onSuccess(this.message, parameters);

        assertEquals("Failed to remove previously failing message [" + this.mailId.toString() + "] (batch id ["
            + this.batchId + "]) from the file system. Reason [MailStoreException: error].",
            this.logRule.getMessage(0));
    }

    @Test
    public void onError() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        Map<String, Object> parameters = Collections.singletonMap("wikiId", (Object) "mywiki");
        MailStatus status = new MailStatus(this.message, MailState.READY);
        status.setWiki("otherwiki");
        when(mailStatusStore.load(Collections.<String, Object>singletonMap("id",
            this.mailId.toString()), 0, 0)).thenReturn(Arrays.asList(status));

        this.mocker.getComponentUnderTest().onError(this.message, new Exception("Error"), parameters);

        verify(mailStatusStore).load(Collections.<String, Object>singletonMap("id", this.mailId.toString()), 0, 0);
        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.FAILED, "otherwiki")), eq(parameters));
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
                statusArgument.getMessageId().equals(mailId.toString()) &&
                statusArgument.getType().equals("type") &&
                statusArgument.getState().equals(state) &&
                statusArgument.getWiki().equals(wikiId);
        }
    }
}
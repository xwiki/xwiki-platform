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

import java.util.UUID;

import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
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
    public MockitoComponentMockingRule<DatabaseMailListener> mocker =
        new MockitoComponentMockingRule<>(DatabaseMailListener.class);

    private MimeMessage message;

    private UUID batchId = UUID.randomUUID();

    private UUID mailId = UUID.randomUUID();

    @Before
    public void setUp() throws Exception
    {
        this.message = mock(MimeMessage.class);
        when(this.message.getHeader("X-MailID", null)).thenReturn(this.mailId.toString());
        when(this.message.getHeader("X-BatchID", null)).thenReturn(this.batchId.toString());
        when(this.message.getHeader("X-MailType", null)).thenReturn("type");
    }

    @Test
    public void prepareAndSaveStatus() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");

        this.mocker.getComponentUnderTest().onPrepare(this.message);

        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.READY)));
    }

    @Test
    public void successAndSaveStatus() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        MailStatus status = new MailStatus(this.mailId.toString());
        status.setState(MailState.READY);
        status.setType("type");
        status.setBatchId(this.batchId.toString());
        when(mailStatusStore.loadFromMessageId(this.mailId.toString())).thenReturn(status);

        this.mocker.getComponentUnderTest().onSuccess(this.message);

        verify(mailStatusStore).loadFromMessageId(this.mailId.toString());
        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.SENT)));
    }

    @Test
    public void successAndSaveStatusWithPreviouslyFailedMessage() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        MailStatus status = new MailStatus(this.mailId.toString());
        status.setState(MailState.FAILED);
        status.setType("type");
        status.setBatchId(this.batchId.toString());
        when(mailStatusStore.loadFromMessageId(this.mailId.toString())).thenReturn(status);

        MailContentStore mailContentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");

        this.mocker.getComponentUnderTest().onSuccess(this.message);

        verify(mailStatusStore).loadFromMessageId(this.mailId.toString());

        verify(mailContentStore).delete(this.batchId.toString(), this.mailId.toString());

        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.SENT)));
    }

    @Test
    public void errorAndSaveStatusAndMessage() throws Exception
    {
        MailStatusStore mailStatusStore = this.mocker.getInstance(MailStatusStore.class, "database");
        MailStatus status = new MailStatus(this.mailId.toString());
        status.setState(MailState.READY);
        status.setType("type");
        status.setBatchId(this.batchId.toString());
        when(mailStatusStore.loadFromMessageId(this.mailId.toString())).thenReturn(status);

        MailContentStore mailContentStore = this.mocker.getInstance(MailContentStore.class, "filesystem");

        this.mocker.getComponentUnderTest().onError(this.message, new Exception("Error"));

        verify(mailStatusStore).loadFromMessageId(this.mailId.toString());

        verify(mailContentStore).save(this.message);

        verify(mailStatusStore).save(argThat(new isSameMailStatus(MailState.FAILED)));
    }

    /**
     * Custom Mokito Argument Matchers.
     */
    class isSameMailStatus extends ArgumentMatcher<MailStatus>
    {
        private String state;

        public isSameMailStatus(MailState state)
        {
            this.state = state.toString();
        }

        @Override
        public boolean matches(Object argument)
        {
            MailStatus statusArgument = (MailStatus) argument;
            return statusArgument.getBatchId().equals(batchId.toString()) &&
                statusArgument.getMessageId().equals(mailId.toString()) &&
                statusArgument.getType().equals("type") &&
                statusArgument.getState().equals(state);
        }
    }
}
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
package org.xwiki.mail.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MailResender;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStoreException;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeprecatedMailStorageScriptService}.
 *
 * @version $Id$
 * @since 6.4
 */
@ComponentList({
    MemoryMailListener.class
})
public class DeprecatedMailStorageScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<DeprecatedMailStorageScriptService> mocker =
        new MockitoComponentMockingRule<>(DeprecatedMailStorageScriptService.class);

    @Before
    public void setUp() throws Exception
    {
        Provider<ComponentManager> componentManagerProvider = this.mocker.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.mocker);

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);
    }

    @Test
    public void resendWhenMailResendingFailed() throws Exception
    {
        MailResender resender = this.mocker.getInstance(MailResender.class, "database");
        when(resender.resendAsynchronously("batchId", "messageId")).thenThrow(new MailStoreException("error"));

        ScriptMailResult result = this.mocker.getComponentUnderTest().resend("batchId", "messageId");

        assertNull(result);
        assertEquals("error", this.mocker.getComponentUnderTest().getLastError().getMessage());
    }

    @Test
    public void resend() throws Exception
    {
        MailResender resender = this.mocker.getInstance(MailResender.class, "database");
        MailStatusResult statusResult = mock(MailStatusResult.class);
        when(resender.resendAsynchronously("batchId", "messageId")).thenReturn(statusResult);

        ScriptMailResult result = this.mocker.getComponentUnderTest().resend("batchId", "messageId");

        assertEquals("batchId", result.getBatchId());
        assertNotNull(result.getStatusResult());
    }

    @Test
    public void resendAsynchronouslySeveralMessages() throws Exception
    {
        Map filterMap = Collections.singletonMap("state", "prepare_%");

        MailStatus status1 = new MailStatus();
        status1.setBatchId("batch1");
        status1.setMessageId("message1");

        MailStatus status2 = new MailStatus();
        status2.setBatchId("batch2");
        status2.setMessageId("message2");

        MailStatusResult statusResult1 = mock(MailStatusResult.class, "status1");
        when(statusResult1.getTotalMailCount()).thenReturn(1L);

        MailStatusResult statusResult2 = mock(MailStatusResult.class, "status2");
        when(statusResult2.getTotalMailCount()).thenReturn(2L);

        List<Pair<MailStatus, MailStatusResult>> results = new ArrayList<>();
        results.add(new ImmutablePair<>(status1, statusResult1));
        results.add(new ImmutablePair<>(status2, statusResult2));

        MailResender resender = this.mocker.getInstance(MailResender.class, "database");
        when(resender.resendAsynchronously(filterMap, 5, 10)).thenReturn(results);

        List<ScriptMailResult> scriptResults =
            this.mocker.getComponentUnderTest().resendAsynchronously(filterMap, 5, 10);

        assertEquals(2, scriptResults.size());
        assertEquals("batch1", scriptResults.get(0).getBatchId());
        assertEquals(1L, scriptResults.get(0).getStatusResult().getTotalMailCount());
        assertEquals("batch2", scriptResults.get(1).getBatchId());
        assertEquals(2L, scriptResults.get(1).getStatusResult().getTotalMailCount());
    }

    @Test
    public void resendAsynchronouslySeveralMessagesWhenMailResendingFailed() throws Exception
    {
        Map filterMap = Collections.singletonMap("state", "prepare_%");

        MailResender resender = this.mocker.getInstance(MailResender.class, "database");
        when(resender.resendAsynchronously(filterMap, 5, 10)).thenThrow(new MailStoreException("error"));

        List<ScriptMailResult> scriptResults =
            this.mocker.getComponentUnderTest().resendAsynchronously(filterMap, 5, 10);

        assertNull(scriptResults);
        assertEquals("error", this.mocker.getComponentUnderTest().getLastError().getMessage());
    }

    @Test
    public void loadWhenNotAuthorized() throws Exception
    {
        ContextualAuthorizationManager authorizationManager =
            this.mocker.getInstance(ContextualAuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);

        List<MailStatus> result = this.mocker.getComponentUnderTest().load(
            Collections.<String, Object>emptyMap(), 0, 0, null, false);

        assertNull(result);
        assertEquals("You need Admin rights to load mail statuses",
            this.mocker.getComponentUnderTest().getLastError().getMessage());
    }
}

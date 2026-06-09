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

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.mail.MailResender;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStoreException;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeprecatedMailStorageScriptService}.
 *
 * @version $Id$
 * @since 6.4
 */
@ComponentTest
@ComponentList({
    MemoryMailListener.class
})
class DeprecatedMailStorageScriptServiceTest
{
    @InjectMockComponents
    private DeprecatedMailStorageScriptService service;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("database")
    private MailResender resender;

    @MockComponent
    private ContextualAuthorizationManager authorizationManager;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager cm)
    {
        when(this.componentManagerProvider.get()).thenReturn(cm);
    }

    @BeforeEach
    void setUp()
    {
        ExecutionContext executionContext = new ExecutionContext();
        when(this.execution.getContext()).thenReturn(executionContext);
    }

    @Test
    void resendWhenMailResendingFailed() throws Exception
    {
        when(this.resender.resendAsynchronously("batchId", "messageId")).thenThrow(new MailStoreException("error"));

        ScriptMailResult result = this.service.resend("batchId", "messageId");

        assertNull(result);
        assertEquals("error", this.service.getLastError().getMessage());
    }

    @Test
    void resend() throws Exception
    {
        MailStatusResult statusResult = mock(MailStatusResult.class);
        when(this.resender.resendAsynchronously("batchId", "messageId")).thenReturn(statusResult);

        ScriptMailResult result = this.service.resend("batchId", "messageId");

        assertEquals("batchId", result.getBatchId());
        assertNotNull(result.getStatusResult());
    }

    @Test
    void resendAsynchronouslySeveralMessages() throws Exception
    {
        Map filterMap = Map.of("state", "prepare_%");

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

        List<Pair<MailStatus, MailStatusResult>> results = List.of(
            new ImmutablePair<>(status1, statusResult1),
            new ImmutablePair<>(status2, statusResult2)
        );

        when(this.resender.resendAsynchronously(filterMap, 5, 10)).thenReturn(results);

        List<ScriptMailResult> scriptResults = this.service.resendAsynchronously(filterMap, 5, 10);

        assertEquals(2, scriptResults.size());
        assertEquals("batch1", scriptResults.getFirst().getBatchId());
        assertEquals(1L, scriptResults.getFirst().getStatusResult().getTotalMailCount());
        assertEquals("batch2", scriptResults.getLast().getBatchId());
        assertEquals(2L, scriptResults.getLast().getStatusResult().getTotalMailCount());
    }

    @Test
    void resendAsynchronouslySeveralMessagesWhenMailResendingFailed() throws Exception
    {
        Map filterMap = Map.of("state", "prepare_%");

        when(this.resender.resendAsynchronously(filterMap, 5, 10)).thenThrow(new MailStoreException("error"));

        List<ScriptMailResult> scriptResults = this.service.resendAsynchronously(filterMap, 5, 10);

        assertNull(scriptResults);
        assertEquals("error", this.service.getLastError().getMessage());
    }

    @Test
    void loadWhenNotAuthorized() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);

        List<MailStatus> result = this.service.load(Map.of(), 0, 0, null, false);

        assertNull(result);
        assertEquals("You need Admin rights to load mail statuses",
            this.service.getLastError().getMessage());
    }
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MailStorageScriptService}.
 *
 * @version $Id$
 * @since 6.4
 */
@ComponentTest
@ComponentList({
    MemoryMailListener.class
})
class MailStorageScriptServiceTest
{
    @InjectMockComponents
    private MailStorageScriptService mailStorageScriptService;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @Mock
    private XWikiContext xcontext;

    @BeforeEach
    void setUp() throws Exception
    {
        Provider<ComponentManager> componentManagerProvider = this.componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        when(componentManagerProvider.get()).thenReturn(this.componentManager);

        Execution execution = this.componentManager.getInstance(Execution.class);
        ExecutionContext executionContext = new ExecutionContext();
        when(execution.getContext()).thenReturn(executionContext);

        when(this.xcontext.isMainWiki()).thenReturn(true);
        when(this.xwikiContextProvider.get()).thenReturn(this.xcontext);
    }

    @Test
    void resendWhenMailResendingFailed() throws Exception
    {
        MailResender resender = this.componentManager.getInstance(MailResender.class, "database");
        when(resender.resendAsynchronously("batchId", "messageId")).thenThrow(new MailStoreException("error"));

        ScriptMailResult result = this.mailStorageScriptService.resend("batchId", "messageId");

        assertNull(result);
        assertEquals("error", this.mailStorageScriptService.getLastError().getMessage());
    }

    @Test
    void resend() throws Exception
    {
        MailResender resender = this.componentManager.getInstance(MailResender.class, "database");
        MailStatusResult statusResult = mock(MailStatusResult.class);
        when(resender.resendAsynchronously("batchId", "messageId")).thenReturn(statusResult);

        ScriptMailResult result = this.mailStorageScriptService.resend("batchId", "messageId");

        assertEquals("batchId", result.getBatchId());
        assertNotNull(result.getStatusResult());
    }

    @Test
    void resendWhenInSubWiki() throws Exception
    {
        when(this.xcontext.isMainWiki()).thenReturn(false);
        when(this.xcontext.getWikiId()).thenReturn("wiki");

        Map filterMap = Collections.singletonMap("state", "prepare_%");

        MailResender resender = this.componentManager.getInstance(MailResender.class, "database");

        this.mailStorageScriptService.resend(filterMap, 5, 10);

        // The test is here by checking that the filter map has been augmented of the wiki
        Map expectedFilterMap = new HashMap(filterMap);
        expectedFilterMap.put("wiki", "wiki");
        verify(resender).resend(expectedFilterMap, 5, 10);
    }


    @Test
    void resendAsynchronouslySeveralMessages() throws Exception
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

        MailResender resender = this.componentManager.getInstance(MailResender.class, "database");
        when(resender.resendAsynchronously(filterMap, 5, 10)).thenReturn(results);

        List<ScriptMailResult> scriptResults = this.mailStorageScriptService.resendAsynchronously(filterMap, 5, 10);

        assertEquals(2, scriptResults.size());
        assertEquals("batch1", scriptResults.get(0).getBatchId());
        assertEquals(1L, scriptResults.get(0).getStatusResult().getTotalMailCount());
        assertEquals("batch2", scriptResults.get(1).getBatchId());
        assertEquals(2L, scriptResults.get(1).getStatusResult().getTotalMailCount());
    }

    @Test
    void resendSynchronouslySeveralMessages() throws Exception
    {
        Map<String, Object> filterMap = Collections.singletonMap("state", "prepare_%");

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

        MailResender resender = this.componentManager.getInstance(MailResender.class, "database");
        when(resender.resend(filterMap, 5, 10)).thenReturn(results);

        List<ScriptMailResult> scriptResults = this.mailStorageScriptService.resend(filterMap, 5, 10);

        assertEquals(2, scriptResults.size());
        assertEquals("batch1", scriptResults.get(0).getBatchId());
        assertEquals(1L, scriptResults.get(0).getStatusResult().getTotalMailCount());
        assertEquals("batch2", scriptResults.get(1).getBatchId());
        assertEquals(2L, scriptResults.get(1).getStatusResult().getTotalMailCount());
    }

    @Test
    void resendAsynchronouslySeveralMessagesWhenMailResendingFailed() throws Exception
    {
        Map filterMap = Collections.singletonMap("state", "prepare_%");

        MailResender resender = this.componentManager.getInstance(MailResender.class, "database");
        when(resender.resendAsynchronously(filterMap, 5, 10)).thenThrow(new MailStoreException("error"));

        List<ScriptMailResult> scriptResults = this.mailStorageScriptService.resendAsynchronously(filterMap, 5, 10);

        assertNull(scriptResults);
        assertEquals("error", this.mailStorageScriptService.getLastError().getMessage());
    }

    @Test
    void loadWhenNotAuthorized() throws Exception
    {
        ContextualAuthorizationManager authorizationManager =
            this.componentManager.getInstance(ContextualAuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.ADMIN)).thenReturn(false);

        List<MailStatus> result = this.mailStorageScriptService.load(Collections.emptyMap(), 0, 0, null, false);

        assertNull(result);
        assertEquals("You need Admin rights to load mail statuses",
            this.mailStorageScriptService.getLastError().getMessage());
    }
}

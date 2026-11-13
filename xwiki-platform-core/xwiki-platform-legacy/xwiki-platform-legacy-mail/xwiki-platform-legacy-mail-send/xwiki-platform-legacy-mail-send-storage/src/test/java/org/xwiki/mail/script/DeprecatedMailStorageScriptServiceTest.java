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
import org.xwiki.mail.MailContentStore;
import org.xwiki.mail.MailResender;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusResult;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;
import org.xwiki.mail.internal.MemoryMailListener;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Author");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");
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
    private AuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @MockComponent
    @Named("database")
    private MailStatusStore statusStore;

    @MockComponent
    @Named("filesystem")
    private MailContentStore contentStore;

    private XWikiContext xcontext;

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

        this.xcontext = mock(XWikiContext.class);
        when(this.xcontext.isMainWiki()).thenReturn(true);
        when(this.xcontext.getWikiId()).thenReturn("xwiki");
        when(this.xcontext.getAuthorReference()).thenReturn(AUTHOR_REFERENCE);
        when(this.xcontext.getUserReference()).thenReturn(USER_REFERENCE);
        when(this.xwikiContextProvider.get()).thenReturn(this.xcontext);
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(true);
    }

    private void setSubWiki()
    {
        when(this.xcontext.isMainWiki()).thenReturn(false);
        when(this.xcontext.getWikiId()).thenReturn("wiki");
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("wiki")))
            .thenReturn(true);
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
    void loadWhenNotAuthorized()
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);

        List<MailStatus> result = this.service.load(Map.of(), 0, 0, null, false);

        assertNull(result);
        assertEquals("The author of the script needs Admin rights on the wiki to load mail statuses",
            this.service.getLastError().getMessage());
    }

    @Test
    void resendWhenNotAuthorized()
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);
        // A script written by a non-admin and viewed by an admin is not allowed: only the script author is checked.
        when(this.authorizationManager.hasAccess(Right.ADMIN, USER_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(true);

        assertNull(this.service.resend("batchId", "messageId"));
        assertNull(this.service.resendAsynchronously(Map.of(), 0, 0));
        assertEquals("The author of the script needs Admin rights on the wiki to resend mails",
            this.service.getLastError().getMessage());
        verifyNoInteractions(this.resender);
    }

    @Test
    void deleteWhenNotAuthorized()
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);

        this.service.delete("batchId", "messageId");

        assertEquals("The author of the script needs Admin rights on the wiki to delete mails",

            this.service.getLastError().getMessage());
        verifyNoInteractions(this.statusStore);
        verifyNoInteractions(this.contentStore);
    }

    @Test
    void loadByIdWhenNotAuthorized()
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);

        assertNull(this.service.load("messageId"));
        assertEquals("The author of the script needs Admin rights on the wiki to load mail statuses",
            this.service.getLastError().getMessage());
        verifyNoInteractions(this.statusStore);
    }

    @Test
    void deleteWhenInSubWikiAndMailFromOtherWiki() throws Exception
    {
        setSubWiki();
        MailStatus status = new MailStatus();
        status.setBatchId("batchId");
        status.setWiki("otherwiki");
        when(this.statusStore.load("messageId")).thenReturn(status);

        this.service.delete("batchId", "messageId");

        assertEquals("The mail [messageId] of the batch [batchId] was not sent from the current wiki",
            this.service.getLastError().getMessage());
        verify(this.statusStore, never()).delete(anyString(), anyMap());
        verifyNoInteractions(this.contentStore);
    }

    @Test
    void resendWhenInSubWikiAndMailFromOtherBatch() throws Exception
    {
        setSubWiki();
        MailStatus status = new MailStatus();
        status.setBatchId("batchId");
        status.setWiki("wiki");
        when(this.statusStore.load("messageId")).thenReturn(status);

        assertNull(this.service.resend("otherBatchId", "messageId"));
        assertEquals("The mail [messageId] of the batch [otherBatchId] was not sent from the current wiki",
            this.service.getLastError().getMessage());
        verifyNoInteractions(this.resender);
    }

    @Test
    void resendAsynchronouslySeveralMessagesWhenInSubWiki() throws Exception
    {
        setSubWiki();

        this.service.resendAsynchronously(Map.of("state", "prepare_%"), 5, 10);

        // The mails of the other wikis are filtered out.
        verify(this.resender).resendAsynchronously(Map.of("state", "prepare_%", "wiki", "wiki"), 5, 10);
    }
}

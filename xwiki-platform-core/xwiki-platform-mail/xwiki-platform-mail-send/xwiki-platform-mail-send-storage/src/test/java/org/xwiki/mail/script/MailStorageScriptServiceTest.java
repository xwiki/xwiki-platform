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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Author");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");
    @InjectMockComponents
    private MailStorageScriptService mailStorageScriptService;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @Mock
    private XWikiContext xcontext;

    private AuthorizationManager authorizationManager;

    private MailResender resender;

    private MailStatusStore statusStore;

    private MailContentStore contentStore;

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
        when(this.xcontext.getWikiId()).thenReturn("xwiki");
        when(this.xcontext.getAuthorReference()).thenReturn(AUTHOR_REFERENCE);
        when(this.xcontext.getUserReference()).thenReturn(USER_REFERENCE);
        when(this.xwikiContextProvider.get()).thenReturn(this.xcontext);

        this.authorizationManager = this.componentManager.getInstance(AuthorizationManager.class);
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(true);

        this.resender = this.componentManager.getInstance(MailResender.class, "database");
        this.statusStore = this.componentManager.getInstance(MailStatusStore.class, "database");
        this.contentStore = this.componentManager.getInstance(MailContentStore.class, "filesystem");
    }

    private void setSubWiki()
    {
        when(this.xcontext.isMainWiki()).thenReturn(false);
        when(this.xcontext.getWikiId()).thenReturn("wiki");
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("wiki")))
            .thenReturn(true);
    }

    private MailStatus createMailStatus(String batchId, String messageId, String wiki)
    {
        MailStatus status = new MailStatus();
        status.setBatchId(batchId);
        status.setMessageId(messageId);
        status.setWiki(wiki);
        return status;
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
        setSubWiki();

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
    void loadWhenNotAuthorized()
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);

        List<MailStatus> result = this.mailStorageScriptService.load(Collections.emptyMap(), 0, 0, null, false);

        assertNull(result);
        assertEquals("The author of the script needs Admin rights on the wiki to load mail statuses",
            this.mailStorageScriptService.getLastError().getMessage());
    }

    @Test
    void loadWhenOnlyTheCurrentUserIsAdmin()
    {
        // A script written by a non-admin and viewed by an admin is not allowed: only the script author is checked.
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.ADMIN, USER_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(true);

        assertNull(this.mailStorageScriptService.load(Collections.emptyMap(), 0, 0, null, false));
        assertEquals(0, this.mailStorageScriptService.count(Collections.emptyMap()));
        assertNull(this.mailStorageScriptService.resend("batchId", "messageId"));
        this.mailStorageScriptService.delete("batchId", "messageId");
        assertEquals("The author of the script needs Admin rights on the wiki to delete mails",
            this.mailStorageScriptService.getLastError().getMessage());
        verifyNoInteractions(this.statusStore);
        verifyNoInteractions(this.contentStore);
        verifyNoInteractions(this.resender);
    }

    @Test
    void loadWhenOnlyTheAuthorIsAdmin() throws Exception
    {
        // The current user is not checked: the page calling the script service is responsible for checking its viewer.
        when(this.xcontext.getUserReference()).thenReturn(null);

        this.mailStorageScriptService.load(Collections.emptyMap(), 0, 0, null, false);

        verify(this.statusStore).load(Collections.emptyMap(), 0, 0, null, false);
        assertNull(this.mailStorageScriptService.getLastError());
    }

    @Test
    void loadWhenPermissionsAreDropped()
    {
        // The author of the script cannot be trusted when the permissions have been dropped.
        when(this.xcontext.hasDroppedPermissions()).thenReturn(true);

        assertNull(this.mailStorageScriptService.load(Collections.emptyMap(), 0, 0, null, false));
        assertNull(this.mailStorageScriptService.resend(Collections.emptyMap(), 0, 0));
        assertEquals("The author of the script needs Admin rights on the wiki to resend mails",
            this.mailStorageScriptService.getLastError().getMessage());
        verifyNoInteractions(this.statusStore);
        verifyNoInteractions(this.resender);
    }

    @Test
    void countWhenNotAuthorized()
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);

        assertEquals(0, this.mailStorageScriptService.count(Collections.emptyMap()));
        assertEquals("The author of the script needs Admin rights on the wiki to count mail statuses",
            this.mailStorageScriptService.getLastError().getMessage());
        verifyNoInteractions(this.statusStore);
    }

    @Test
    void resendWhenNotAuthorized() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);

        assertNull(this.mailStorageScriptService.resend("batchId", "messageId"));
        assertEquals("The author of the script needs Admin rights on the wiki to resend mails",
            this.mailStorageScriptService.getLastError().getMessage());

        assertNull(this.mailStorageScriptService.resend(Collections.emptyMap(), 0, 0));
        assertNull(this.mailStorageScriptService.resendAsynchronously(Collections.emptyMap(), 0, 0));
        assertEquals("The author of the script needs Admin rights on the wiki to resend mails",
            this.mailStorageScriptService.getLastError().getMessage());

        verifyNoInteractions(this.resender);
    }

    @Test
    void deleteWhenNotAuthorized() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);

        this.mailStorageScriptService.delete("batchId", "messageId");

        assertEquals("The author of the script needs Admin rights on the wiki to delete mails",
            this.mailStorageScriptService.getLastError().getMessage());
        verifyNoInteractions(this.statusStore);
        verifyNoInteractions(this.contentStore);
    }

    @Test
    void loadByIdWhenNotAuthorized()
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);

        assertNull(this.mailStorageScriptService.load("messageId"));
        assertEquals("The author of the script needs Admin rights on the wiki to load mail statuses",
            this.mailStorageScriptService.getLastError().getMessage());
        verifyNoInteractions(this.statusStore);
    }

    @Test
    void delete() throws Exception
    {
        this.mailStorageScriptService.delete("batchId", "messageId");

        verify(this.statusStore).delete("messageId", Collections.emptyMap());
        verify(this.contentStore).delete("batchId", "messageId");
        assertNull(this.mailStorageScriptService.getLastError());
    }

    @Test
    void deleteWhenInSubWikiAndMailFromCurrentWiki() throws Exception
    {
        setSubWiki();
        when(this.statusStore.load("messageId")).thenReturn(createMailStatus("batchId", "messageId", "wiki"));

        this.mailStorageScriptService.delete("batchId", "messageId");

        verify(this.statusStore).delete("messageId", Collections.emptyMap());
        verify(this.contentStore).delete("batchId", "messageId");
    }

    @Test
    void deleteWhenInSubWikiAndMailFromOtherWiki() throws Exception
    {
        setSubWiki();
        when(this.statusStore.load("messageId")).thenReturn(createMailStatus("batchId", "messageId", "otherwiki"));

        this.mailStorageScriptService.delete("batchId", "messageId");

        assertEquals("The mail [messageId] of the batch [batchId] was not sent from the current wiki",
            this.mailStorageScriptService.getLastError().getMessage());
        verify(this.statusStore, never()).delete(anyString(), anyMap());
        verifyNoInteractions(this.contentStore);
    }

    @Test
    void resendWhenInSubWikiAndMailFromOtherWiki() throws Exception
    {
        setSubWiki();
        when(this.statusStore.load("messageId")).thenReturn(createMailStatus("batchId", "messageId", "otherwiki"));

        assertNull(this.mailStorageScriptService.resend("batchId", "messageId"));
        assertEquals("The mail [messageId] of the batch [batchId] was not sent from the current wiki",
            this.mailStorageScriptService.getLastError().getMessage());
        verifyNoInteractions(this.resender);
    }

    @Test
    void resendWhenInSubWikiAndMailNotFound() throws Exception
    {
        setSubWiki();

        assertNull(this.mailStorageScriptService.resend("batchId", "messageId"));
        assertEquals("The mail [messageId] of the batch [batchId] was not sent from the current wiki",
            this.mailStorageScriptService.getLastError().getMessage());
        verifyNoInteractions(this.resender);
    }

    @Test
    void deleteWhenInSubWikiAndMailFromOtherBatch() throws Exception
    {
        setSubWiki();
        when(this.statusStore.load("messageId")).thenReturn(createMailStatus("batchId", "messageId", "wiki"));

        this.mailStorageScriptService.delete("otherBatchId", "messageId");

        assertEquals("The mail [messageId] of the batch [otherBatchId] was not sent from the current wiki",
            this.mailStorageScriptService.getLastError().getMessage());
        verify(this.statusStore, never()).delete(anyString(), anyMap());
        verifyNoInteractions(this.contentStore);
    }

    @Test
    void resendWhenInSubWikiAndMailFromOtherBatch() throws Exception
    {
        setSubWiki();
        when(this.statusStore.load("messageId")).thenReturn(createMailStatus("batchId", "messageId", "wiki"));

        assertNull(this.mailStorageScriptService.resend("otherBatchId", "messageId"));
        assertEquals("The mail [messageId] of the batch [otherBatchId] was not sent from the current wiki",
            this.mailStorageScriptService.getLastError().getMessage());
        verifyNoInteractions(this.resender);
    }

    @Test
    void loadById() throws Exception
    {
        MailStatus status = createMailStatus("batchId", "messageId", "otherwiki");
        when(this.statusStore.load("messageId")).thenReturn(status);

        // On the main wiki, the mails of all the wikis are accessible.
        assertEquals(status, this.mailStorageScriptService.load("messageId"));
    }

    @Test
    void loadByIdWhenInSubWikiAndMailFromOtherWiki() throws Exception
    {
        setSubWiki();
        when(this.statusStore.load("messageId")).thenReturn(createMailStatus("batchId", "messageId", "otherwiki"));

        assertNull(this.mailStorageScriptService.load("messageId"));
        assertEquals("The mail [messageId] was not sent from the current wiki",
            this.mailStorageScriptService.getLastError().getMessage());
    }

    @Test
    void deleteAll() throws Exception
    {
        setSubWiki();
        when(this.statusStore.load(Map.of("wiki", "wiki"), 0, 0, null, false)).thenReturn(List.of(
            createMailStatus("batch1", "message1", "wiki"),
            createMailStatus("batch2", "message2", "wiki")));

        this.mailStorageScriptService.deleteAll();

        verify(this.statusStore).delete("message1", Collections.emptyMap());
        verify(this.contentStore).delete("batch1", "message1");
        verify(this.statusStore).delete("message2", Collections.emptyMap());
        verify(this.contentStore).delete("batch2", "message2");
        // The statuses returned by the filtered load are not loaded again one by one.
        verify(this.statusStore, never()).load(anyString());
    }

    @Test
    void deleteAllWhenNotAuthorized() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.ADMIN, AUTHOR_REFERENCE, new WikiReference("xwiki")))
            .thenReturn(false);

        this.mailStorageScriptService.deleteAll();
        this.mailStorageScriptService.delete("batchId");

        verify(this.statusStore, never()).load(anyMap(), anyInt(), anyInt(), any(), anyBoolean());
        verify(this.statusStore, never()).delete(anyString(), anyMap());
        verifyNoInteractions(this.contentStore);
    }
}

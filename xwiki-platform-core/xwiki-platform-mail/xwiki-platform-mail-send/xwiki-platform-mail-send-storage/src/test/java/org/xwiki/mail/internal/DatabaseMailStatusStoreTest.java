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
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStoreException;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatabaseMailStatusStore}.
 *
 * @version $Id$
 * @since 6.4RC1
 */
@ComponentTest
class DatabaseMailStatusStoreTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @InjectMockComponents
    private DatabaseMailStatusStore store;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Test
    void computeSelectQueryString() throws Exception
    {
        Map<String, Object> filterMap = new LinkedHashMap<>();
        filterMap.put("state", "failed");
        filterMap.put("wiki", "mywiki");

        assertEquals(
            "from org.xwiki.mail.MailStatus as mailStatus where mailStatus.state like :state and "
                + "mailStatus.wiki like :wiki order by mailStatus.date desc",
            this.store.computeSelectQueryString(filterMap, "date", false));
    }

    @Test
    void computeCountQueryString() throws Exception
    {
        Map<String, Object> filterMap = new LinkedHashMap<>();
        filterMap.put("state", "failed");
        filterMap.put("wiki", "mywiki");

        assertEquals("select count(*) from org.xwiki.mail.MailStatus as mailStatus where mailStatus.state like "
            + ":state and mailStatus.wiki like :wiki", this.store.computeCountQueryString(filterMap));
    }

    @Test
    void computeSelectQueryStringAcceptsStatusAlias() throws Exception
    {
        assertEquals(String.format("from %s as mailStatus where mailStatus.state like :state",
            MailStatus.class.getName()),
            this.store.computeSelectQueryString(Collections.singletonMap("status", "failed"), null, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"messageId", "batchId", "date", "recipients", "type", "state",
        "errorSummary", "errorDescription", "wiki"})
    void computeSelectQueryStringAllowsMailStatusSortFields(String sortableField) throws Exception
    {
        assertEquals(String.format("from %s as mailStatus order by mailStatus.%s", MailStatus.class.getName(),
            sortableField),
            this.store.computeSelectQueryString(Collections.emptyMap(), sortableField, true));
    }

    @Test
    void computeSelectQueryStringAllowsMailStatusFilterFields() throws Exception
    {
        Map<String, Object> filterMap = new LinkedHashMap<>();
        filterMap.put("messageId", "messageId");
        filterMap.put("batchId", "batchId");
        filterMap.put("date", "date");
        filterMap.put("recipients", "recipients");
        filterMap.put("type", "type");
        filterMap.put("state", "state");
        filterMap.put("errorSummary", "errorSummary");
        filterMap.put("errorDescription", "errorDescription");
        filterMap.put("wiki", "wiki");

        assertEquals("from org.xwiki.mail.MailStatus as mailStatus where mailStatus.messageId like :messageId "
            + "and mailStatus.batchId like :batchId and mailStatus.date like :date and mailStatus.recipients "
            + "like :recipients and mailStatus.type like :type and mailStatus.state like :state and "
            + "mailStatus.errorSummary like :errorSummary and mailStatus.errorDescription like :errorDescription "
            + "and mailStatus.wiki like :wiki",
            this.store.computeSelectQueryString(filterMap, null, true));
    }

    @Test
    void computeSelectQueryStringIgnoresUnsupportedSortField() throws Exception
    {
        assertEquals(String.format("from %s as mailStatus", MailStatus.class.getName()),
            this.store.computeSelectQueryString(Collections.emptyMap(), "date desc, (select 1)", true));
    }

    @Test
    void computeSelectQueryStringRejectsUnsupportedFilterField()
    {
        Map<String, Object> filterMap = Map.of("state or 1=1", "failed");
        MailStoreException exception = assertThrows(MailStoreException.class, () ->
            this.store.computeSelectQueryString(filterMap, "date", true));

        assertEquals("Unsupported mail status filter field [state or 1=1]", exception.getMessage());
    }

    @Test
    void logsForloadMailStatus() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWikiId()).thenReturn("wiki");
        when(xcontext.getMainXWiki()).thenReturn("mainwiki");

        XWikiHibernateStore hibernateStore = mock(XWikiHibernateStore.class);
        ReflectionUtils.setFieldValue(this.store, "hibernateStore", hibernateStore);
        MailStatus status = new MailStatus();
        status.setBatchId("batchid");
        status.setMessageId("messageid");
        status.setState(MailState.PREPARE_SUCCESS);
        status.setRecipients("recipients");
        when(hibernateStore.executeRead(eq(xcontext), any())).thenReturn(Collections.singletonList(status));

        Map<String, Object> filterMap = new LinkedHashMap<>();
        filterMap.put("status", "failed");
        filterMap.put("wiki", "mywiki");

        this.store.load(filterMap, 0, 0, null, false);

        // The test is here, we verify that debug logs are correct
        assertEquals(2, this.logCapture.size());
        assertEquals("Find mail statuses for query [from org.xwiki.mail.MailStatus as mailStatus where "
            + "mailStatus.state like :state and mailStatus.wiki like :wiki] and parameters [[state] = [failed], "
            + "[wiki] = [mywiki]]",
            this.logCapture.getMessage(0));
        assertEquals("Loaded mail status [messageId = [messageid], batchId = [batchid], state = [prepare_success], "
            + "date = [<null>], recipients = [recipients]]", this.logCapture.getMessage(1));
    }
}

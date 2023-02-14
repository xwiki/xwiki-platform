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
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void computeSelectQueryString()
    {
        Map<String, Object> filterMap = new LinkedHashMap<>();
        filterMap.put("status", "failed");
        filterMap.put("wiki", "mywiki");

        assertEquals(
            "from org.xwiki.mail.MailStatus where mail_status like :status and mail_wiki like :wiki order by date desc",
            this.store.computeSelectQueryString(filterMap, "date", false));
    }

    @Test
    void computeCountQueryString()
    {
        Map<String, Object> filterMap = new LinkedHashMap<>();
        filterMap.put("status", "failed");
        filterMap.put("wiki", "mywiki");

        assertEquals("select count(*) from org.xwiki.mail.MailStatus where mail_status like :status and "
            + "mail_wiki like :wiki", this.store.computeCountQueryString(filterMap));
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
        assertEquals("Find mail statuses for query [from org.xwiki.mail.MailStatus where mail_status like :status "
            + "and mail_wiki like :wiki] and parameters [[status] = [failed], [wiki] = [mywiki]]",
            this.logCapture.getMessage(0));
        assertEquals("Loaded mail status [messageId = [messageid], batchId = [batchid], state = [prepare_success], "
            + "date = [<null>], recipients = [recipients]]", this.logCapture.getMessage(1));
    }
}

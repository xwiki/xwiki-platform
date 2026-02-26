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
package org.xwiki.job.store.internal;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.job.store.internal.entity.JobStatusLogEntryEntity;
import org.xwiki.job.store.internal.entity.JobStatusSummaryEntity;
import org.xwiki.job.store.internal.hibernate.JobStatusHibernateExecutor;
import org.xwiki.job.store.internal.hibernate.JobStatusHibernateExecutor.HibernateCallback;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Component integration tests for {@link org.xwiki.job.store.internal.hibernate.JobStatusHibernateStore}.
 * <p>
 * Verifies that the job-status-specific Hibernate session factory only maps the job status entities
 * ({@link JobStatusSummaryEntity} and {@link JobStatusLogEntryEntity}) and that the standard XWiki entities
 * (e.g. {@code XWikiDocument}) are neither mapped nor have their tables created.
 *
 * @version $Id$
 */
@ComponentTest
class JobStatusHibernateStoreTest extends AbstractJobStatusHibernateTest
{
    private static final String NODE_ID = "hibernate-store-test-node";

    private static final String STATUS_KEY = "hibernate-store-test-key";

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private JobStatusHibernateExecutor executor;

    @Test
    void summaryEntityCanBeStoredAndRetrieved() throws Exception
    {
        JobStatusSummaryEntity summary = new JobStatusSummaryEntity();
        summary.setNodeId(NODE_ID);
        summary.setStatusKey(STATUS_KEY);
        summary.setFullJobId("/test/job");
        summary.setJobType("test-job-type");
        summary.setState("FINISHED");
        summary.setStartDate(new Date(1000L));
        summary.setEndDate(new Date(2000L));
        summary.setBlobLocator("/blob/locator");

        this.executor.executeWrite(session -> {
            session.save(summary);
            return null;
        });

        JobStatusSummaryEntity loaded = this.executor.executeRead(session ->
            session.createQuery(
                    "from org.xwiki.job.store.internal.entity.JobStatusSummaryEntity "
                        + "where nodeId = :nodeId and statusKey = :statusKey",
                    JobStatusSummaryEntity.class)
                .setParameter("nodeId", NODE_ID)
                .setParameter("statusKey", STATUS_KEY)
                .uniqueResult());

        assertNotNull(loaded);
        assertEquals("/test/job", loaded.getFullJobId());
        assertEquals("test-job-type", loaded.getJobType());
        assertEquals("FINISHED", loaded.getState());
        assertEquals(new Date(1000L), loaded.getStartDate());
        assertEquals(new Date(2000L), loaded.getEndDate());
        assertEquals("/blob/locator", loaded.getBlobLocator());
    }

    @Test
    void logEntryEntityCanBeStoredAndRetrieved() throws Exception
    {
        JobStatusLogEntryEntity entry = new JobStatusLogEntryEntity();
        entry.setNodeId(NODE_ID);
        entry.setStatusKey(STATUS_KEY);
        entry.setLineIndex(7);
        entry.setLevel(2);
        entry.setTimeStamp(123456789L);
        entry.setMessage("raw {0}");
        entry.setFormattedMessage("raw message");
        entry.setThrowableType("java.lang.RuntimeException");
        entry.setThrowableMessage("something went wrong");
        entry.setLogSerialized("<log/>");

        this.executor.executeWrite(session -> {
            session.save(entry);
            return null;
        });

        JobStatusLogEntryEntity loaded = this.executor.executeRead(session ->
            session.createQuery(
                    "from org.xwiki.job.store.internal.entity.JobStatusLogEntryEntity "
                        + "where nodeId = :nodeId and statusKey = :statusKey and lineIndex = :lineIndex",
                    JobStatusLogEntryEntity.class)
                .setParameter("nodeId", NODE_ID)
                .setParameter("statusKey", STATUS_KEY)
                .setParameter("lineIndex", 7L)
                .uniqueResult());

        assertNotNull(loaded);
        assertEquals(2, loaded.getLevel());
        assertEquals(123456789L, loaded.getTimeStamp());
        assertEquals("raw {0}", loaded.getMessage());
        assertEquals("raw message", loaded.getFormattedMessage());
        assertEquals("java.lang.RuntimeException", loaded.getThrowableType());
        assertEquals("something went wrong", loaded.getThrowableMessage());
        assertEquals("<log/>", loaded.getLogSerialized());
    }

    @Test
    void xwikiDocumentEntityIsNotMapped()
    {
        // XWikiDocument is listed in xwiki.hbm.xml which is referenced in the Hibernate configuration, but the
        // job-status session factory only registers jobstatus.hbm.xml and must not map it.
        assertThrows(JobStatusStoreException.class, () -> this.executor.executeRead(session ->
            session.createQuery("from com.xpn.xwiki.doc.XWikiDocument", Object.class).list()));
        // Hibernate logs a warning that no persistent classes are found for the query class.
        assertEquals(1, this.logCapture.size());
        assertThat(this.logCapture.getMessage(0), containsString("no persistent classes found for query class"));
    }

    @Test
    void xwikiDocumentTableDoesNotExist()
    {
        // The schema-update step in JobStatusHibernateStore must not create the XWikiDocument table (xwikidoc).
        HibernateCallback<Object> query = session -> {
            session.createNativeQuery("SELECT count(*) FROM xwikidoc").getSingleResult();
            return null;
        };

        assertThrows(JobStatusStoreException.class, () -> this.executor.executeRead(query));
        // SqlExceptionHelper logs a WARN with the SQL error code and an ERROR with the details.
        assertEquals(2, this.logCapture.size());
        assertThat(this.logCapture.getMessage(0), containsString("SQL Error"));
        assertThat(this.logCapture.getMessage(1), containsString("object not found: XWIKIDOC"));
    }
}

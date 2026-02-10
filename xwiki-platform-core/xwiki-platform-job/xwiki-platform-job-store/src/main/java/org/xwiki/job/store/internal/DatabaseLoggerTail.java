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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.store.internal.entity.JobStatusLogEntryEntity;
import org.xwiki.job.store.internal.hibernate.JobStatusHibernateExecutor;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.internal.tail.AbstractLoggerTail;
import org.xwiki.logging.tail.LogTailResult;
import org.xwiki.xstream.internal.SafeXStream;

/**
 * Database-backed {@link org.xwiki.logging.tail.LoggerTail} implementation. It only loads the requested log entries
 * from the database when needed and appends new entries incrementally for running jobs.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Component(roles = DatabaseLoggerTail.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DatabaseLoggerTail extends AbstractLoggerTail
{
    private static final int MESSAGE_COLUMN_LENGTH = 2000;

    private static final int THROWABLE_TYPE_LENGTH = 512;

    private static final int LOG_PAGE_SIZE = 200;

    private static final String NODE_ID = "nodeId";

    private static final String NODE_AND_STATUS_WHERE =
        "where nodeId = :nodeId and statusKey = :statusKey ";

    private static final String SELECT_LOGS_BASE_HQL =
        "from org.xwiki.job.store.internal.entity.JobStatusLogEntryEntity "
            + NODE_AND_STATUS_WHERE;

    private static final String DELETE_LOGS_HQL = "delete " + SELECT_LOGS_BASE_HQL;

    private static final String SORT_BY_LINE_INDEX_ASC = "order by lineIndex asc";

    private static final String SELECT_LOGS_HQL = SELECT_LOGS_BASE_HQL + SORT_BY_LINE_INDEX_ASC;

    private static final String SELECT_LOGS_REVERSE_HQL = SELECT_LOGS_BASE_HQL + "order by lineIndex desc";

    private static final String SELECT_LOGS_BY_LEVEL_HQL =
        SELECT_LOGS_BASE_HQL + "and level >= :minLevel " + SORT_BY_LINE_INDEX_ASC;

    private static final String SELECT_LAST_LOG_BY_LEVEL_HQL =
        SELECT_LOGS_BASE_HQL + "and level >= :minLevel order by lineIndex desc";

    private static final String SELECT_LOG_BY_LINE_INDEX_HQL =
        SELECT_LOGS_BASE_HQL + "and lineIndex = :lineIndex";

    private static final String SELECT_LOGS_AFTER_LINE_INDEX_BEFORE_LINE_INDEX_HQL =
        SELECT_LOGS_BASE_HQL + "and lineIndex > :lineIndex and lineIndex < :toLineIndex " + SORT_BY_LINE_INDEX_ASC;

    private static final String SELECT_LOGS_BY_LEVEL_AFTER_LINE_INDEX_BEFORE_LINE_INDEX_HQL =
        SELECT_LOGS_BASE_HQL + "and level >= :minLevel and lineIndex > :lineIndex and lineIndex < :toLineIndex "
            + SORT_BY_LINE_INDEX_ASC;

    private static final String COUNT_LOGS_HQL =
        "select count(*) from org.xwiki.job.store.internal.entity.JobStatusLogEntryEntity " + NODE_AND_STATUS_WHERE;

    private static final String COUNT_LOGS_BY_LEVEL_HQL = COUNT_LOGS_HQL + "and level >= :minLevel";

    private static final String STATUS_KEY = "statusKey";

    private static final String MIN_LEVEL = "minLevel";

    private static final String LINE_INDEX = "lineIndex";

    private static final String TO_LINE_INDEX = "toLineIndex";

    private String nodeId;

    private String statusKey;

    private boolean readOnly;

    @Inject
    private Logger logger;

    @Inject
    private SafeXStream xstream;

    @Inject
    private JobStatusHibernateExecutor hibernateExecutor;

    private final AtomicInteger nextLineIndex = new AtomicInteger();

    /**
     * Initialize the logger tail for the given nodeId and statusKey. If not read-only, it will delete any existing log
     * entries for the nodeId/statusKey to start with a clean slate.
     *
     * @param nodeId the cluster node ID of the log entries to manage
     * @param statusKey the job status key of the log entries to manage
     * @param readOnly whether the tail will be used in read-only mode (e.g., for completed jobs) or in read-write
     * mode (e.g., for running jobs)
     * @return this tail instance for chaining
     */
    DatabaseLoggerTail initialize(String nodeId, String statusKey, boolean readOnly)
    {
        this.nodeId = nodeId;
        this.statusKey = statusKey;
        this.readOnly = readOnly;

        if (!readOnly) {
            // Delete existing log entries for the nodeId/statusKey.
            this.hibernateExecutor.executeWrite(session -> {
                deleteLogs(session);
                return null;
            });
        }

        return this;
    }

    /**
     * Initialize the logger tail for the given nodeId and statusKey in read-write mode, deleting any existing log
     * entries for the nodeId/statusKey to start with a clean slate.
     *
     * @param nodeId the cluster node ID of the log entries to manage
     * @param statusKey the job status key of the log entries to manage
     * @param session the Hibernate session to use for deleting existing log entries
     * @return this tail instance for chaining
     */
    DatabaseLoggerTail initialize(String nodeId, String statusKey, Session session)
    {
        this.nodeId = nodeId;
        this.statusKey = statusKey;
        this.readOnly = false;

        deleteLogs(session);

        return this;
    }

    private void deleteLogs(Session session)
    {
        // Delete logs only if there are existing entries to avoid warning logs from Hibernate about "no data"
        // with some databases.
        if (countLogEntries(session) > 0) {
            session.createQuery(DELETE_LOGS_HQL)
                .setParameter(NODE_ID, this.nodeId)
                .setParameter(STATUS_KEY, this.statusKey)
                .executeUpdate();
        }
    }

    @Override
    public Iterator<LogEvent> iterator()
    {
        return new DatabaseLogIterator(this, null, 0, -1);
    }

    @Override
    public LogEvent getLogEvent(int index)
    {
        if (index < 0) {
            return null;
        }

        JobStatusLogEntryEntity entry = this.hibernateExecutor.executeRead(session -> session
            .createQuery(SELECT_LOG_BY_LINE_INDEX_HQL, JobStatusLogEntryEntity.class)
            .setParameter(NODE_ID, this.nodeId)
            .setParameter(STATUS_KEY, this.statusKey)
            .setParameter(LINE_INDEX, (long) index)
            .uniqueResult());
        return entry != null ? deserialize(entry) : null;
    }

    @Override
    public LogTailResult getLogEvents(LogLevel from, int offset, int limit)
    {
        return new DatabaseLogTailResult(this, from, offset, limit);
    }

    @Override
    public boolean hasLogLevel(LogLevel from)
    {
        if (from == null) {
            return countLogEntries() > 0;
        }

        Long count = this.hibernateExecutor
            .executeRead(session -> session.createQuery(COUNT_LOGS_BY_LEVEL_HQL, Long.class)
                .setParameter(NODE_ID, this.nodeId)
                .setParameter(STATUS_KEY, this.statusKey)
                .setParameter(MIN_LEVEL, from.ordinal())
                .uniqueResult());
        return count != null && count > 0;
    }

    @Override
    public int size()
    {
        long count = countLogEntries();
        return count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
    }

    @Override
    public void log(LogEvent logEvent)
    {
        if (this.readOnly) {
            throw new UnsupportedOperationException("Database logger tail is read-only");
        }

        if (logEvent == null) {
            return;
        }

        // Allocate index and insert log entry in a single transaction for consistency
        this.hibernateExecutor.executeWrite(session -> {
            int lineIndex = this.nextLineIndex.getAndIncrement();
            appendLogEntry(lineIndex, logEvent, session);
            return null;
        });
    }

    @Override
    public void close()
    {
        // Nothing to release
    }

    @Override
    public void flush() throws IOException
    {
        // Nothing buffered locally
    }

    @Override
    public LogEvent getFirstLogEvent(LogLevel from)
    {
        return getLogEvent(from, false);
    }

    @Override
    public LogEvent getLastLogEvent(LogLevel from)
    {
        return getLogEvent(from, true);
    }

    @Nullable
    private LogEvent getLogEvent(LogLevel from, boolean reverse)
    {
        List<JobStatusLogEntryEntity> entries = fetchLogEntries(from, 0, 1, reverse);

        return entries.isEmpty() ? null : deserialize(entries.getFirst());
    }

    private long countLogEntries()
    {
        return this.hibernateExecutor.executeRead(this::countLogEntries);
    }

    private long countLogEntries(Session session)
    {
        Long count = session.createQuery(COUNT_LOGS_HQL, Long.class)
            .setParameter(NODE_ID, this.nodeId)
            .setParameter(STATUS_KEY, this.statusKey)
            .uniqueResult();

        return count != null ? count : 0;
    }

    private List<JobStatusLogEntryEntity> fetchLogEntries(LogLevel minLevel, int limit, int offset, boolean reverse)
    {
        int actualLimit = Math.max(1, limit);
        return this.hibernateExecutor.executeRead(session -> {
            Query<JobStatusLogEntryEntity> query;
            if (minLevel == null) {
                query = session.createQuery(reverse ? SELECT_LOGS_REVERSE_HQL : SELECT_LOGS_HQL,
                    JobStatusLogEntryEntity.class);
            } else {
                query = session.createQuery(reverse ? SELECT_LAST_LOG_BY_LEVEL_HQL : SELECT_LOGS_BY_LEVEL_HQL,
                    JobStatusLogEntryEntity.class);
                query.setParameter(MIN_LEVEL, minLevel.ordinal());
            }
            query.setParameter(NODE_ID, this.nodeId);
            query.setParameter(STATUS_KEY, this.statusKey);
            if (offset > 0) {
                query.setFirstResult(offset);
            }
            query.setMaxResults(actualLimit);
            return query.list();
        });
    }

    private List<JobStatusLogEntryEntity> fetchLogEntriesAfterLineIndex(LogLevel minLevel, int limit, long lineIndex,
        long toLineIndexExclusive)
    {
        int actualLimit = Math.max(1, limit);
        return this.hibernateExecutor.executeRead(session -> {
            Query<JobStatusLogEntryEntity> query;
            if (minLevel == null) {
                query = session.createQuery(SELECT_LOGS_AFTER_LINE_INDEX_BEFORE_LINE_INDEX_HQL,
                    JobStatusLogEntryEntity.class);
            } else {
                query = session.createQuery(SELECT_LOGS_BY_LEVEL_AFTER_LINE_INDEX_BEFORE_LINE_INDEX_HQL,
                    JobStatusLogEntryEntity.class);
                query.setParameter(MIN_LEVEL, minLevel.ordinal());
            }
            query.setParameter(NODE_ID, this.nodeId);
            query.setParameter(STATUS_KEY, this.statusKey);
            query.setParameter(LINE_INDEX, lineIndex);
            query.setParameter(TO_LINE_INDEX, toLineIndexExclusive);
            query.setMaxResults(actualLimit);
            return query.list();
        });
    }

    void appendLogEntry(long lineIndex, LogEvent event, Session session)
    {
        session.save(toLogEntry(lineIndex, event));
    }

    private JobStatusLogEntryEntity toLogEntry(long lineIndex, LogEvent event)
    {
        JobStatusLogEntryEntity entity = new JobStatusLogEntryEntity();
        entity.setNodeId(this.nodeId);
        entity.setStatusKey(this.statusKey);
        entity.setLineIndex(lineIndex);
        entity.setLevel(event.getLevel() != null ? event.getLevel().ordinal() : LogLevel.INFO.ordinal());
        entity.setTimeStamp(event.getTimeStamp());
        entity.setMessage(truncate(event.getMessage(), MESSAGE_COLUMN_LENGTH));
        entity.setFormattedMessage(truncate(event.getFormattedMessage(), MESSAGE_COLUMN_LENGTH));
        if (event.getThrowable() != null) {
            entity.setThrowableType(truncate(event.getThrowable().getClass().getName(), THROWABLE_TYPE_LENGTH));
            entity.setThrowableMessage(truncate(event.getThrowable().getMessage(), MESSAGE_COLUMN_LENGTH));
        }
        entity.setLogSerialized(serialize(event));
        return entity;
    }

    @NonNull
    private LogEvent deserialize(JobStatusLogEntryEntity entity)
    {
        try {
            return (LogEvent) this.xstream.fromXML(entity.getLogSerialized());
        } catch (Exception e) {
            this.logger.debug("Failed to deserialize log entry [{}], falling back to metadata.", entity.getLineIndex(),
                e);
            return buildFallbackEvent(entity);
        }
    }

    @NonNull
    private LogEvent buildFallbackEvent(JobStatusLogEntryEntity entity)
    {
        LogLevel level = parseLogLevel(entity.getLevel());
        String message = StringUtils.defaultIfBlank(entity.getFormattedMessage(), entity.getMessage());
        return new LogEvent(null, level, message, null, null, entity.getTimeStamp());
    }

    private LogLevel parseLogLevel(int level)
    {
        if (level < 0 || level >= LogLevel.values().length) {
            return LogLevel.INFO;
        }
        return LogLevel.values()[level];
    }

    private String serialize(Object value)
    {
        if (value == null) {
            return null;
        }

        StringWriter writer = new StringWriter();
        this.xstream.toXML(value, writer);
        return writer.toString();
    }

    private String truncate(String value, int length)
    {
        if (value == null || value.length() <= length) {
            return value;
        }

        return StringUtils.abbreviate(value, length);
    }

    private static final class DatabaseLogTailResult implements LogTailResult
    {
        private final DatabaseLoggerTail tail;

        private final LogLevel minLevel;

        private final int offset;

        private final int limit;

        DatabaseLogTailResult(DatabaseLoggerTail tail, LogLevel minLevel, int offset, int limit)
        {
            this.tail = tail;
            this.minLevel = minLevel;
            this.offset = Math.max(0, offset);
            this.limit = limit;
        }

        @Override
        @NonNull
        public Iterator<LogEvent> iterator()
        {
            return new DatabaseLogIterator(this.tail, this.minLevel, this.offset, this.limit);
        }

        @Override
        public Stream<LogEvent> stream()
        {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator(), 0), false);
        }
    }

    private static final class DatabaseLogIterator implements Iterator<LogEvent>
    {
        private final DatabaseLoggerTail tail;

        private final LogLevel minLevel;

        private final long endLineIndexExclusive;

        private long nextLineIndex;

        private List<JobStatusLogEntryEntity> buffer = List.of();

        private int bufferIndex;

        private LogEvent nextEvent;

        DatabaseLogIterator(DatabaseLoggerTail tail, LogLevel minLevel, int offset, int limit)
        {
            this.tail = tail;
            this.minLevel = minLevel;
            int safeOffset = Math.max(0, offset);
            this.nextLineIndex = safeOffset - 1L;
            this.endLineIndexExclusive = limit > 0 ? (long) safeOffset + limit : Long.MAX_VALUE;
        }

        @Override
        public boolean hasNext()
        {
            if (this.nextEvent == null) {
                prepareNext();
            }

            return this.nextEvent != null;
        }

        @Override
        public LogEvent next()
        {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            LogEvent result = this.nextEvent;
            this.nextEvent = null;
            return result;
        }

        private void prepareNext()
        {
            if (this.nextLineIndex + 1 >= this.endLineIndexExclusive) {
                this.nextEvent = null;
                return;
            }

            if (this.bufferIndex >= this.buffer.size()) {
                this.buffer = this.tail.fetchLogEntriesAfterLineIndex(this.minLevel, LOG_PAGE_SIZE,
                    this.nextLineIndex, this.endLineIndexExclusive);
                this.bufferIndex = 0;
                if (this.buffer.isEmpty()) {
                    this.nextEvent = null;
                    return;
                }

                this.nextLineIndex = this.buffer.getLast().getLineIndex();
            }

            JobStatusLogEntryEntity entry = this.buffer.get(this.bufferIndex++);
            this.nextEvent = this.tail.deserialize(entry);
        }
    }
}

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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.xwiki.job.store.internal.entity.JobStatusLogEntryEntity;
import org.xwiki.job.store.internal.hibernate.JobStatusHibernateExecutor;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Component integration tests for {@link DatabaseLoggerTail}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(JobStatusHibernateExecutor.class)
class DatabaseLoggerTailTest extends AbstractJobStatusHibernateTest
{
    private static final String NODE_ID = "node-logger-tail";

    private static final String STATUS_KEY = "status-logger-tail";

    @InjectMockComponents
    private DatabaseLoggerTail loggerTail;

    @InjectMockComponents
    private JobStatusHibernateExecutor executor;

    @BeforeEach
    void setUp()
    {
        this.loggerTail.initialize(NODE_ID, STATUS_KEY, false);
    }

    @Test
    void iteratorPaginatesOverTwoHundredEntries()
    {
        for (int i = 0; i < 260; ++i) {
            this.loggerTail.log(new LogEvent(LogLevel.INFO,
                "message-" + i, null, null));
        }

        List<String> messages = new ArrayList<>();
        this.loggerTail.iterator().forEachRemaining(event -> messages.add(event.getFormattedMessage()));

        assertEquals(260, messages.size());
        assertEquals("message-0", messages.getFirst());
        assertEquals("message-259", messages.getLast());

        List<LogEvent> streamedEvents = this.loggerTail.getLogEvents(LogLevel.INFO).stream().toList();
        assertEquals(260, streamedEvents.size());
        assertEquals("message-0", streamedEvents.getFirst().getFormattedMessage());
        assertEquals("message-259", streamedEvents.getLast().getFormattedMessage());

        assertEquals(260, this.loggerTail.size());
    }

    @Test
    void getLogEventsAppliesOffsetAndLimitAcrossPages()
    {
        for (int i = 0; i < 250; ++i) {
            this.loggerTail.log(new LogEvent(
                i % 2 == 0 ? LogLevel.INFO : LogLevel.WARN,
                "event-" + i, null, null));
        }

        List<LogEvent> messages =
            this.loggerTail.getLogEvents(LogLevel.WARN, 10, 220).stream().toList();

        // The messages should be messages 11 to 229 in increments of two. Yes, the logic is a bit weird, but offset
        // and limit are calculated without the level filter as this matches the existing filesystem-based
        // implementation.
        assertEquals(110, messages.size());
        for (int i = 0; i < 110; ++i) {
            assertEquals("event-" + (11 + i * 2), messages.get(i).getFormattedMessage());
        }

        // Ensure that filtering for INFO includes WARNING messages.
        messages = this.loggerTail.getLogEvents(LogLevel.INFO, 10, 220).stream().toList();
        assertEquals(220, messages.size());
        for (int i = 0; i < 220; ++i) {
            assertEquals("event-" + (10 + i), messages.get(i).getFormattedMessage());
        }
    }

    @Test
    void getLogEventsAppliesOffsetAndLimitAcrossPagesWithNoEntries()
    {
        List<LogEvent> messages = this.loggerTail.getLogEvents(LogLevel.INFO, 10, 220).stream().toList();
        assertEquals(0, messages.size());
    }

    @Test
    void getFirstAndLastLogEvent()
    {
        // Create log events of all levels to test the skipping of events with lower levels.
        // Create events first-TRACE, first-DEBUG, first-INFO, first-WARN, first-ERROR, last-ERROR, last-WARN,
        // last-INFO, last-DEBUG, last-TRACE.
        // Values are from error to trace, so reverse the order.
        LogLevel[] increasingLevels = LogLevel.values();
        ArrayUtils.reverse(increasingLevels);
        for (LogLevel level : increasingLevels) {
            this.loggerTail.log(new LogEvent(level, "first-" + level.name(), null, null));
        }
        for (LogLevel level : LogLevel.values()) {
            this.loggerTail.log(new LogEvent(level, "last-" + level.name(), null, null));
        }

        for (LogLevel level : LogLevel.values()) {
            LogEvent first = this.loggerTail.getFirstLogEvent(level);
            assertEquals("first-" + level.name(), first.getFormattedMessage());
            LogEvent last = this.loggerTail.getLastLogEvent(level);
            assertEquals("last-" + level.name(), last.getFormattedMessage());
        }

        LogEvent first = this.loggerTail.getFirstLogEvent(null);
        assertEquals("first-TRACE", first.getFormattedMessage());
        LogEvent last = this.loggerTail.getLastLogEvent(null);
        assertEquals("last-TRACE", last.getFormattedMessage());
    }

    @Test
    void hasLogLevelEmpty()
    {
        assertFalse(this.loggerTail.hasLogLevel(null));
        for (LogLevel level : LogLevel.values()) {
            assertFalse(this.loggerTail.hasLogLevel(level));
        }
    }

    @ParameterizedTest
    @EnumSource(LogLevel.class)
    void hasLogLevel(LogLevel level)
    {
        this.loggerTail.initialize(NODE_ID, STATUS_KEY, false);
        this.loggerTail.log(new LogEvent(level, "message", null, null));
        assertTrue(this.loggerTail.hasLogLevel(null));
        assertTrue(this.loggerTail.hasLogLevel(level));

        // It should also be true for all higher levels.
        for (LogLevel otherLevel : LogLevel.values()) {
            if (otherLevel.ordinal() > level.ordinal()) {
                assertTrue(this.loggerTail.hasLogLevel(otherLevel));
            }
        }
    }

    @Test
    void getLogEventFallsBackOnMalformedSerializedXML() throws Exception
    {
        long timeStamp = 1234567890L;

        // Directly insert a log entry with malformed XML that will cause the XML parser to fail before
        // SafeTreeUnmarshaller.convert() can catch the exception.
        this.executor.executeWrite(session -> {
            JobStatusLogEntryEntity entity = new JobStatusLogEntryEntity();
            entity.setNodeId(NODE_ID);
            entity.setStatusKey(STATUS_KEY);
            entity.setLineIndex(0);
            entity.setLevel(LogLevel.WARN.ordinal());
            entity.setTimeStamp(timeStamp);
            entity.setMessage("raw message");
            entity.setFormattedMessage("formatted message");
            entity.setLogSerialized("not valid xml at all");
            session.save(entity);
            return null;
        });

        LogEvent result = this.loggerTail.getLogEvent(0);
        assertEquals(LogLevel.WARN, result.getLevel());
        assertEquals("formatted message", result.getMessage());
        assertEquals(timeStamp, result.getTimeStamp());
        assertNull(result.getArgumentArray());
    }

    @Test
    void getLogEventFallsBackOnNullDeserializedEntry() throws Exception
    {
        long timeStamp = 9876543210L;

        // <null/> is valid XML that XStream deserializes to null. This simulates the case where
        // SafeTreeUnmarshaller.convert() swallows a top-level exception and returns null.
        this.executor.executeWrite(session -> {
            JobStatusLogEntryEntity entity = new JobStatusLogEntryEntity();
            entity.setNodeId(NODE_ID);
            entity.setStatusKey(STATUS_KEY);
            entity.setLineIndex(0);
            entity.setLevel(LogLevel.ERROR.ordinal());
            entity.setTimeStamp(timeStamp);
            entity.setMessage("raw message");
            entity.setFormattedMessage("formatted message");
            entity.setLogSerialized("<null/>");
            session.save(entity);
            return null;
        });

        LogEvent result = this.loggerTail.getLogEvent(0);
        assertEquals(LogLevel.ERROR, result.getLevel());
        assertEquals("formatted message", result.getMessage());
        assertEquals(timeStamp, result.getTimeStamp());
        assertNull(result.getArgumentArray());
    }
}

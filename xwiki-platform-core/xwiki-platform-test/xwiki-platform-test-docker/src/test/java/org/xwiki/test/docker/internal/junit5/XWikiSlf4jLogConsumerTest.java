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
package org.xwiki.test.docker.internal.junit5;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link XWikiSlf4jLogConsumer}.
 *
 * @version $Id$
 */
class XWikiSlf4jLogConsumerTest
{
    /**
     * Captures the logs propagated by the consumer.
     */
    @RegisterExtension
    LogCaptureExtension logCapture =
        new LogCaptureExtension(LogLevel.INFO, XWikiSlf4jLogConsumerTest.class.getName());

    /**
     * Feeds each passed line (as a separate STDOUT frame, as the containers do) to the consumer and returns the log
     * lines that were actually propagated (i.e. logged), with the {@code STDOUT: } prefix that the parent consumer adds
     * stripped away.
     */
    private List<String> consume(boolean verbose, String... lines)
    {
        XWikiSlf4jLogConsumer consumer =
            new XWikiSlf4jLogConsumer(LoggerFactory.getLogger(XWikiSlf4jLogConsumerTest.class), verbose);
        for (String line : lines) {
            consumer.accept(new OutputFrame(OutputFrame.OutputType.STDOUT,
                (line + "\n").getBytes(StandardCharsets.UTF_8)));
        }
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < this.logCapture.size(); i++) {
            messages.add(this.logCapture.getMessage(i).substring("STDOUT: ".length()));
        }
        return messages;
    }

    @Test
    void acceptWithVerboseLogsEverything()
    {
        List<String> lines = List.of(
            "2024-01-15 10:23:45,001 [main] INFO  o.x.Foo - Starting up",
            "2024-01-15 10:23:45,002 [main] DEBUG o.x.Foo - Some detail",
            "2024-01-15 10:23:45,003 [main] WARN  o.x.Foo - A warning");
        assertEquals(lines, consume(true, lines.toArray(new String[0])));
    }

    @Test
    void acceptWithoutVerboseSkipsInfoAndDebug()
    {
        assertEquals(List.of(), consume(false,
            "2024-01-15 10:23:45,001 [main] INFO  o.x.Foo - Starting up",
            "2024-01-15 10:23:45,002 [main] DEBUG o.x.Foo - Some detail"));
    }

    @Test
    void acceptWithoutVerboseLogsWarnAndError()
    {
        assertEquals(
            List.of(
                "2024-01-15 10:23:45,002 [main] WARN  o.x.Foo - A warning",
                "2024-01-15 10:23:45,004 [main] ERROR o.x.Foo - An error"),
            consume(false,
                "2024-01-15 10:23:45,001 [main] INFO  o.x.Foo - Starting up",
                "2024-01-15 10:23:45,002 [main] WARN  o.x.Foo - A warning",
                "2024-01-15 10:23:45,003 [main] INFO  o.x.Foo - More info",
                "2024-01-15 10:23:45,004 [main] ERROR o.x.Foo - An error",
                "2024-01-15 10:23:45,005 [main] INFO  o.x.Foo - Even more info"));
    }

    @Test
    void acceptWithoutVerboseIncludesMultiLineStackTrace()
    {
        assertEquals(
            List.of(
                "2024-01-15 10:23:45,002 [main] ERROR o.x.Foo - Failed to do something",
                "java.lang.RuntimeException: Something went wrong",
                "\tat org.xwiki.Foo.doSomething(Foo.java:42)",
                "\tat org.xwiki.Bar.call(Bar.java:13)",
                "Caused by: java.lang.IllegalStateException: bad state",
                "\tat org.xwiki.Baz.check(Baz.java:99)",
                "\t... 25 common frames omitted"),
            consume(false,
                "2024-01-15 10:23:45,001 [main] INFO  o.x.Foo - Starting up",
                "2024-01-15 10:23:45,002 [main] ERROR o.x.Foo - Failed to do something",
                "java.lang.RuntimeException: Something went wrong",
                "\tat org.xwiki.Foo.doSomething(Foo.java:42)",
                "\tat org.xwiki.Bar.call(Bar.java:13)",
                "Caused by: java.lang.IllegalStateException: bad state",
                "\tat org.xwiki.Baz.check(Baz.java:99)",
                "\t... 25 common frames omitted",
                "2024-01-15 10:23:45,010 [main] INFO  o.x.Foo - Recovered"));
    }

    @Test
    void acceptWithoutVerboseStopsAtNextTomcatFormatLogMessage()
    {
        // The next INFO message uses the Tomcat/java.util.logging format, which must be recognized as a new log message
        // that ends the propagation. The non-timestamped continuation line is kept as part of the warning.
        assertEquals(
            List.of("2024-01-15 10:23:45,002 [main] WARN  o.x.Foo - A warning with details:",
                "some non-timestamped continuation line"),
            consume(false,
                "2024-01-15 10:23:45,002 [main] WARN  o.x.Foo - A warning with details:",
                "some non-timestamped continuation line",
                "15-Jan-2024 10:23:45.003 INFO [main] o.a.catalina.startup.Catalina.start Server startup"));
    }

    @Test
    void acceptWithoutVerboseStopsAtNextMysqlFormatLogMessage()
    {
        // The next line uses the MySQL ISO-8601 format, which must be recognized as a new log message that ends the
        // propagation.
        assertEquals(
            List.of("2024-01-15 10:23:45,002 [main] WARN  o.x.Foo - A warning"),
            consume(false,
                "2024-01-15 10:23:45,002 [main] WARN  o.x.Foo - A warning",
                "2024-01-15T10:23:45.003456Z 0 [Note] mysqld: ready for connections"));
    }

    @Test
    void acceptWithoutVerboseHandlesJettyNativeFormat()
    {
        // Jetty logs with its own "yyyy-MM-dd HH:mm:ss.SSS:LEVEL :" format.
        assertEquals(
            List.of("2026-07-20 22:22:13.419:WARN :oejd.DeploymentScanner:main: Does not exist: /var/lib/jetty"),
            consume(false,
                "2026-07-20 22:22:13.387:INFO :oejs.Server:main: jetty-12.1.11; built: ...",
                "2026-07-20 22:22:13.419:WARN :oejd.DeploymentScanner:main: Does not exist: /var/lib/jetty",
                "2026-07-20 22:22:13.733:INFO :oejsh.ContextHandler:main: Started XWiki"));
    }

    @Test
    void acceptWithoutVerboseHandlesMariaDBFormat()
    {
        // MariaDB uses a space-padded single-digit hour and a mixed-case [Warning] marker. Both warnings and the
        // following non-timestamped continuation line must be propagated, then the next [Note] line ends propagation.
        assertEquals(
            List.of(
                "2026-07-21  1:25:40 0 [Warning] option 'table_open_cache': unsigned value 4 adjusted to 10",
                "2026-07-21  1:25:41 0 [Warning] mariadbd: io_uring_queue_init() failed with EPERM",
                "create_uring failed: falling back to libaio"),
            consume(false,
                "2026-07-21  1:25:40 0 [Note] Starting MariaDB",
                "2026-07-21  1:25:40 0 [Warning] option 'table_open_cache': unsigned value 4 adjusted to 10",
                "2026-07-21  1:25:41 0 [Warning] mariadbd: io_uring_queue_init() failed with EPERM",
                "create_uring failed: falling back to libaio",
                "2026-07-21  1:25:41 0 [Note] mariadbd: ready for connections"));
    }

    @Test
    void acceptWithoutVerboseStopsAtLogbackStatusLine()
    {
        // Logback's own status printer emits time-only "HH:mm:ss,SSS |-LEVEL in ..." lines with no date. The trailing
        // INFO status line must be recognized as a new log message and thus end the propagation of the warning.
        assertEquals(
            List.of("22:22:15,626 |-WARN in ch.qos.logback.core...ImplicitModelHandler - Ignoring unknown property"),
            consume(false,
                "22:22:15,618 |-INFO in ch.qos.logback.classic...ConfigurationModelHandlerFull - Scan attribute not set",
                "22:22:15,626 |-WARN in ch.qos.logback.core...ImplicitModelHandler - Ignoring unknown property",
                "22:22:15,634 |-INFO in ch.qos.logback.core...AppenderModelHandler - Processing appender named [stdout]"));
    }
}

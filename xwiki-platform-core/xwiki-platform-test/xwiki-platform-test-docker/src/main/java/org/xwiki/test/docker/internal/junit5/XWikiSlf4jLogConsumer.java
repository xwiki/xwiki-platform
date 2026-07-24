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

import java.util.regex.Pattern;

import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;

/**
 * Custom extension of {@link Slf4jLogConsumer} to output warnings and errors when container start and when verbose
 * is off.
 * <p>
 * When verbose is off, a plain warning/error line is not enough: a single log message often spans several lines (a
 * stack trace, a wrapped message, {@code Caused by:} chains, etc.) and only the first line contains the {@code WARN} or
 * {@code ERROR} marker. To avoid losing that context we propagate everything starting from a warning/error line and up
 * to (but excluding) the next line that can be clearly identified as the start of another, non-warning/error log
 * message (i.e. a line starting with a log timestamp). Continuation lines (stack trace frames, {@code Caused by:},
 * etc.) don't start with a timestamp and are therefore kept as part of the current warning/error message.
 * <p>
 * The {@code warn}/{@code error} markers are matched case-insensitively so that the various casings used across the
 * containers are all caught, e.g. {@code WARN}/{@code ERROR} (Logback, Jetty), {@code WARNING} (java.util.logging) and
 * {@code [Warning]}/{@code [Error]} (MariaDB, MySQL).
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class XWikiSlf4jLogConsumer extends Slf4jLogConsumer
{
    /**
     * Matches the beginning of a line that can be clearly identified as the start of a new log message, based on the
     * various timestamp formats emitted inside the containers, e.g.:
     * <ul>
     *   <li>{@code 2024-01-15 10:23:45,123 ...} (Logback, the format used by XWiki)</li>
     *   <li>{@code 2024-01-15 10:23:45.123:WARN :oejd... } (Jetty's own logging format)</li>
     *   <li>{@code 2024-01-15T10:23:45.123456Z ...} / {@code 2024-01-15 10:23:45.123 UTC ...} (MySQL, PostgreSQL)</li>
     *   <li>{@code 2026-07-21  1:25:40 0 [Warning] ...} (MariaDB / MySQL, note the space-padded single-digit hour)</li>
     *   <li>{@code 15-Jan-2024 10:23:45.123 ...} (Tomcat / java.util.logging default format)</li>
     *   <li>{@code 22:22:15,378 |-INFO in ch.qos.logback... } (Logback's own status printer, time only)</li>
     * </ul>
     * Continuation lines of a multi-line log message (stack trace frames, {@code Caused by:}, wrapped text, ...) don't
     * start with such a timestamp, which is what lets us tell them apart from a new log message. This is matched with
     * {@link java.util.regex.Matcher#lookingAt()}, so it only needs to match the start of the line, not the whole line.
     * The hour is allowed to be a single digit and to be preceded by extra padding spaces, as MariaDB/MySQL pad it (for
     * example {@code 2026-07-21  1:25:40}).
     * <p>
     * Note that formats which don't carry the log level on the same line as the timestamp (e.g. the
     * {@code java.util.logging} {@code SimpleFormatter} used by Lucene, which prints the timestamp/class on one line
     * and {@code WARNING: ...} on the next) can't be fully handled by this forward-looking heuristic: the level-less
     * first line is treated as a continuation, which is acceptable as it keeps the message together.
     */
    private static final Pattern NEW_LOG_MESSAGE_PATTERN = Pattern.compile(
        // ISO-like date and time: XWiki/Logback, Jetty, MySQL, MariaDB, PostgreSQL, ...
        "\\d{4}-\\d{2}-\\d{2}[ T] *\\d{1,2}:\\d{2}:\\d{2}"
        // Tomcat / java.util.logging default: dd-MMM-yyyy HH:mm:ss
        + "|\\d{1,2}-[A-Za-z]{3}-\\d{4} \\d{2}:\\d{2}:\\d{2}"
        // Time only with milliseconds, e.g. Logback's own status printer: HH:mm:ss,SSS or HH:mm:ss.SSS
        + "|\\d{2}:\\d{2}:\\d{2}[,.]\\d{3}");

    private final boolean isVerbose;

    /**
     * Whether the lines currently being consumed belong to a warning/error log message and should thus be propagated
     * (only relevant when verbose is off).
     */
    private boolean propagating;

    /**
     * @param logger the SLF4J logger to proxy to
     * @param isVerbose if true then log everything, otherwise only log warnings and errors (including their multi-line
     *            continuations such as stack traces)
     */
    public XWikiSlf4jLogConsumer(Logger logger, boolean isVerbose)
    {
        super(logger);
        this.isVerbose = isVerbose;
    }

    @Override
    public void accept(OutputFrame outputFrame)
    {
        if (this.isVerbose) {
            super.accept(outputFrame);
            return;
        }

        String utf8String = outputFrame.getUtf8String();
        utf8String = utf8String.replaceAll("((\\r?\\n)|(\\r))$", "");

        if (Strings.CI.containsAny(utf8String, "WARN", "ERROR")) {
            // Start (or continue) propagating a warning/error message.
            this.propagating = true;
        } else if (NEW_LOG_MESSAGE_PATTERN.matcher(utf8String).lookingAt()) {
            // A new, non-warning/error log message starts here: stop propagating the previous warning/error message.
            this.propagating = false;
        }
        // Otherwise this is a continuation line (stack trace frame, Caused by:, wrapped message, ...): keep the current
        // propagating state so that it's included if and only if it belongs to a warning/error message.

        if (this.propagating) {
            super.accept(outputFrame);
        }
    }
}

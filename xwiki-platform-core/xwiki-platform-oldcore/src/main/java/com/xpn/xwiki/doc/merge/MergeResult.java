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
package com.xpn.xwiki.doc.merge;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.event.Level;
import org.xwiki.logging.LoggingEventMessage;
import org.xwiki.logging.util.LoggingEventMessageQueue;

/**
 * Report of what happen during merge.
 *
 * @version $Id$
 * @since 3.2M1
 */
public class MergeResult
{
    /**
     * @see #isModified()
     */
    private boolean modified;

    /**
     * @see #getLog()
     */
    private LoggingEventMessageQueue log = new LoggingEventMessageQueue();

    /**
     * @param modified indicate that something has been modified during the merge
     */
    public void setModified(boolean modified)
    {
        this.modified = modified;
    }

    /**
     * @return true if something has been modified during the merge
     */
    public boolean isModified()
    {
        return this.modified;
    }

    /**
     * @return the log associated to the merge
     * @since 4.1RC1
     */
    public LogQueue getLog()
    {
        return this.log;
    }

    /**
     * @return the log associated to the merge
     * @since 8.0M2
     */
    public LoggingEventMessageQueue getLogs()
    {
        return this.log;
    }

    // Deprecated

    /**
     * @param logLevel the level of the logs to return
     * @return the exceptions associated to the provided log level
     */
    private List<Exception> getExceptions(Level logLevel)
    {
        List<Exception> exceptions = new LinkedList<Exception>();

        for (LoggingEventMessage logEvent : getLogs()) {
            if (logEvent.getLevel() == logLevel) {
                if (logEvent.getThrowable() != null && logEvent.getThrowable() instanceof Exception) {
                    exceptions.add(new MergeException(logEvent.getFormattedMessage(), logEvent.getThrowable()));
                } else {
                    exceptions.add(new MergeException(logEvent.getFormattedMessage()));
                }
            }
        }

        return exceptions;
    }

    /**
     * Error raised during the merge.
     * <p>
     * Generally collision for which we don't know what do to at all.
     *
     * @return the merge errors
     * @deprecated since 4.1RC1 use {@link #getLog()} instead
     */
    @Deprecated
    public List<Exception> getErrors()
    {
        return getExceptions(Level.ERROR);
    }

    /**
     * Warning raised during the merge.
     * <p>
     * The difference with error is that in that case a decision which should be good (or at least safe enough) for most
     * of the case has been made.
     *
     * @return the merge warning
     * @deprecated since 4.1RC1 use {@link #getLog()} instead
     */
    @Deprecated
    public List<Exception> getWarnings()
    {
        return getExceptions(Level.WARN);
    }

    /**
     * Add error.
     *
     * @param e the error
     * @deprecated since 4.1RC1 use {@link #getLog()} instead
     */
    @Deprecated
    public void error(Exception e)
    {
        getLogs().error("", e);
    }

    /**
     * Add warning.
     *
     * @param e the warning
     * @deprecated since 4.1RC1 use {@link #getLog()} instead
     */
    @Deprecated
    public void warn(Exception e)
    {
        getLogs().warn("", e);
    }
}

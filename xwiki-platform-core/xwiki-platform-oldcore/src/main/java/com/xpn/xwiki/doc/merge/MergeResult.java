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

import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;

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
    private LogQueue log = new LogQueue();

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

    // Deprecated

    /**
     * @param logLevel the level of the logs to return
     * @return the exceptions associated to the provided log level
     */
    private List<Exception> getExceptions(LogLevel logLevel)
    {
        List<Exception> exceptions = new LinkedList<Exception>();

        for (LogEvent logEvent : getLog()) {
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
        return getExceptions(LogLevel.ERROR);
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
        return getExceptions(LogLevel.WARN);
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
        getLog().error("", e);
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
        getLog().warn("", e);
    }
}

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
 *
 */
package org.xwiki.component.logging;

/**
 * Logger component, used for logging messages. Classes that want to log should <em>not</em> declare a dependency on
 * this component, but implement the {@link org.xwiki.component.phase.LogEnabled} interface. A quick way to enable
 * logging is to extend {@link AbstractLogEnabled}.
 * 
 * @version $Id$
 */
public interface Logger
{
    /**
     * Log a message with the <code>debug</code> priority.
     * 
     * @param message The message to be logged. No further processing of the message is performed.
     */
    void debug(String message);

    /**
     * Log a message with the <code>debug</code> priority, also printing the exception that caused this message, along
     * with its stack trace.
     * 
     * @param message The message to be logged. No further processing of the message is performed.
     * @param throwable The exception associated with the message, for stack trace output.
     */
    void debug(String message, Throwable throwable);

    /**
     * <p>
     * Logs the {@code message} to the logger with <code>debug</code> priority while expanding the message with the
     * objects passed. The message uses the {@link java.text.MessageFormat} syntax.
     * </p>
     * <p>
     * Example:<br/> <code>
     * log.debug("Doc was created on {0} and was changed {1} times", date, 5);
     * log.debug("There {0,choice,0#are|1#is|1<are} {0,choice,0#no|1#one|1<{0}} file{0,choice,0#s|1#|1<s}", nrFiles);
     * </code>
     * </p>
     * <p>
     * The logged message will only be formatted if the message is actually logged.
     * </p>
     * 
     * @param message The message to be formatted via {@link java.text.MessageFormat#format()}
     * @param objects The objects to be filled into the message
     * @see java.text.MessageFormat
     */
    void debug(String message, Object... objects);

    /**
     * <p>
     * Logs the {@code message} to the logger with <code>debug</code> priority while expanding the message with the
     * objects passed, also printing the exception that caused this message, along with its stack trace. The message
     * uses the {@link java.text.MessageFormat} syntax.
     * </p>
     * <p>
     * Example:<br/> <code>
     * log.debug("Unhandled exception while executing the query [{0}]", exception, query);
     * </code>
     * </p>
     * <p>
     * The logged message will only be assembled if the message is actually logged.
     * </p>
     * 
     * @param message The message to be formatted via {@link java.text.MessageFormat#format()}
     * @param throwable The exception associated with the message, for stack trace output.
     * @param objects The objects to be filled into the message
     * @see java.text.MessageFormat
     */
    void debug(String message, Throwable throwable, Object... objects);

    /**
     * Check if the <code>debug</code> priority is enabled.
     * 
     * @return <code>true</code> if messages with <code>debug</code> priority will be logged, <code>false</code>
     *         otherwise
     */
    boolean isDebugEnabled();

    /**
     * Log a message with the <code>info</code> priority.
     * 
     * @param message The message to be logged. No further processing of the message is performed.
     */
    void info(String message);

    /**
     * Log a message with the <code>info</code> priority, also printing the exception that caused this message, along
     * with its stack trace.
     * 
     * @param message The message to be logged. No further processing of the message is performed.
     * @param throwable The exception associated with the message, for stack trace output.
     */
    void info(String message, Throwable throwable);

    /**
     * <p>
     * Logs the {@code message} to the logger with <code>info</code> priority while expanding the message with the
     * objects passed. The message uses the {@link java.text.MessageFormat} syntax. <br>
     * </p>
     * <p>
     * Example:<br/> <code>
     * log.info("Doc was created on {0} and was changed {1} times", date, 5);
     * log.info("There {0,choice,0#are|1#is|1<are} {0,choice,0#no|1#one|1<{0}} file{0,choice,0#s|1#|1<s}", nrFiles);
     * </code>
     * </p>
     * <p>
     * The logged message will only be assembled if the message is actually logged.
     * </p>
     * 
     * @param message The message to be formatted via {@link java.text.MessageFormat#format()}
     * @param objects The objects to be filled into the message
     * @see java.text.MessageFormat
     */
    void info(String message, Object... objects);

    /**
     * <p>
     * Logs the {@code message} to the logger with <code>info</code> priority while expanding the message with the
     * objects passed, also printing the exception that caused this message, along with its stack trace. The message
     * uses the {@link java.text.MessageFormat} syntax.
     * </p>
     * <p>
     * Example:<br/> <code>
     * log.info("Unhandled exception while executing the query [{0}]", exception, query);
     * </code>
     * </p>
     * <p>
     * The logged message will only be assembled if the message is actually logged.
     * </p>
     * 
     * @param message The message to be formatted via {@link java.text.MessageFormat#format()}
     * @param throwable The exception associated with the message, for stack trace output.
     * @param objects The objects to be filled into the message
     * @see java.text.MessageFormat
     */
    void info(String message, Throwable throwable, Object... objects);

    /**
     * Check if the <code>info</code> priority is enabled.
     * 
     * @return <code>true</code> if messages with <code>info</code> priority will be logged, <code>false</code>
     *         otherwise
     */
    boolean isInfoEnabled();

    /**
     * Log a message with the <code>warn</code> priority.
     * 
     * @param message The message to be logged. No further processing of the message is performed.
     */
    void warn(String message);

    /**
     * Log a message with the <code>warn</code> priority, also printing the exception that caused this message, along
     * with its stack trace.
     * 
     * @param message The message to be logged. No further processing of the message is performed.
     * @param throwable The exception associated with the message, for stack trace output.
     */
    void warn(String message, Throwable throwable);

    /**
     * <p>
     * Logs the {@code message} to the logger with <code>warn</code> priority while expanding the message with the
     * objects passed. The message uses the {@link java.text.MessageFormat} syntax.
     * </p>
     * <p>
     * Example:<br/> <code>
     * log.warn("Doc was created on {0} and was changed {1} times", date, 5);
     * log.warn("There {0,choice,0#are|1#is|1<are} {0,choice,0#no|1#one|1<{0}} file{0,choice,0#s|1#|1<s}", nrFiles);
     * </code>
     * </p>
     * <p>
     * The logged message will only be assembled if the message is actually logged.
     * </p>
     * 
     * @param message The message to be formatted via {@link java.text.MessageFormat#format()}
     * @param objects The objects to be filled into the message
     * @see java.text.MessageFormat
     */
    void warn(String message, Object... objects);

    /**
     * <p>
     * Logs the {@code message} to the logger with <code>warn</code> priority while expanding the message with the
     * objects passed, also printing the exception that caused this message, along with its stack trace. The message
     * uses the {@link java.text.MessageFormat} syntax.
     * </p>
     * <p>
     * Example:<br/> <code>
     * log.warn("Unhandled exception while executing the query [{0}]", exception, query);
     * </code>
     * </p>
     * <p>
     * The logged message will only be assembled if the message is actually logged.
     * </p>
     * 
     * @param message The message to be formatted via {@link java.text.MessageFormat#format()}
     * @param throwable The exception associated with the message, for stack trace output.
     * @param objects The objects to be filled into the message
     * @see java.text.MessageFormat
     */
    void warn(String message, Throwable throwable, Object... objects);

    /**
     * Check if the <code>warn</code> priority is enabled.
     * 
     * @return <code>true</code> if messages with <code>warn</code> priority will be logged, <code>false</code>
     *         otherwise
     */
    boolean isWarnEnabled();

    /**
     * Log a message with the <code>error</code> priority.
     * 
     * @param message The message to be logged. No further processing of the message is performed.
     */
    void error(String message);

    /**
     * Log a message with the <code>error</code> priority, also printing the exception that caused this message, along
     * with its stack trace.
     * 
     * @param message The message to be logged. No further processing of the message is performed.
     * @param throwable The exception associated with the message, for stack trace output.
     */
    void error(String message, Throwable throwable);

    /**
     * <p>
     * Logs the {@code message} to the logger with <code>error</code> priority while expanding the message with the
     * objects passed. The message uses the {@link java.text.MessageFormat} syntax.
     * </p>
     * <p>
     * Example:<br/> <code>
     * log.error("Doc was created on {0} and was changed {1} times", date, 5);
     * log.error("There {0,choice,0#are|1#is|1<are} {0,choice,0#no|1#one|1<{0}} file{0,choice,0#s|1#|1<s}", nrFiles);
     * </code>
     * </p>
     * <p>
     * The logged message will only be assembled if the message is actually logged.
     * </p>
     * 
     * @param message The message to be formatted via {@link java.text.MessageFormat#format()}
     * @param objects The objects to be filled into the message
     * @see java.text.MessageFormat
     */
    void error(String message, Object... objects);

    /**
     * <p>
     * Logs the {@code message} to the logger with <code>error</code> priority while expanding the message with the
     * objects passed, also printing the exception that caused this message, along with its stack trace. The message
     * uses the {@link java.text.MessageFormat} syntax.
     * </p>
     * <p>
     * Example:<br/> <code>
     * log.error("Unhandled exception while executing the query [{0}]", exception, query);
     * </code>
     * </p>
     * <p>
     * The logged message will only be assembled if the message is actually logged.
     * </p>
     * 
     * @param message The message to be formatted via {@link java.text.MessageFormat#format()}
     * @param throwable The exception associated with the message, for stack trace output.
     * @param objects The objects to be filled into the message
     * @see java.text.MessageFormat
     */
    void error(String message, Throwable throwable, Object... objects);

    /**
     * Check if the <code>error</code> priority is enabled.
     * 
     * @return <code>true</code> if messages with <code>error</code> priority will be logged, <code>false</code>
     *         otherwise
     */
    boolean isErrorEnabled();
}

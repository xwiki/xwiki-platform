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
package org.xwiki.component.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Bridge between XWiki Logging and Commons Logging.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class CommonsLoggingLogger extends AbstractLogger
{
    /** Wrapped Commons Logging logger object. This communicates with the underlying logging framework. */
    private Log logger;
    
    public CommonsLoggingLogger(Class< ? > clazz)
    {
        this.logger = LogFactory.getLog(clazz);
    }

    /**
     * {@inheritDoc}
     * @see Logger#debug(String, Object...)
     */
    public void debug(String message, Object... objects)
    {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug(formatMessage(message, objects));
        }
    }

    /**
     * {@inheritDoc}
     * @see Logger#debug(String, Throwable, Object...)
     */
    public void debug(String message, Throwable throwable, Object... objects)
    {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug(formatMessage(message, objects), throwable);
        }
    }

    /**
     * {@inheritDoc}
     * @see Logger#debug(String, Throwable)
     */
    public void debug(String message, Throwable throwable)
    {
        this.logger.debug(message, throwable);
    }

    /**
     * {@inheritDoc}
     * @see Logger#debug(String)
     */
    public void debug(String message)
    {
        this.logger.debug(message);
    }

    public void error(String message, Object... objects)
    {
        if (this.logger.isErrorEnabled()) {
            this.logger.error(formatMessage(message, objects));
        }
    }

    public void error(String message, Throwable throwable, Object... objects)
    {
        if (this.logger.isErrorEnabled()) {
            this.logger.error(formatMessage(message, objects), throwable);
        }
    }

    public void error(String message, Throwable throwable)
    {
        this.logger.error(message, throwable);
    }

    public void error(String message)
    {
        this.logger.error(message);
    }

    public void info(String message, Object... objects)
    {
        if (this.logger.isInfoEnabled()) {
            this.logger.info(formatMessage(message, objects));
        }
    }

    public void info(String message, Throwable throwable, Object... objects)
    {
        if (this.logger.isInfoEnabled()) {
            this.logger.info(formatMessage(message, objects), throwable);
        }
    }

    public void info(String message, Throwable throwable)
    {
        this.logger.info(message, throwable);
    }

    public void info(String message)
    {
        this.logger.info(message);
    }

    public boolean isDebugEnabled()
    {
        return this.logger.isDebugEnabled();
    }

    public boolean isErrorEnabled()
    {
        return this.logger.isErrorEnabled();
    }

    public boolean isInfoEnabled()
    {
        return this.logger.isInfoEnabled();
    }

    public boolean isWarnEnabled()
    {
        return this.logger.isWarnEnabled();
    }

    public void warn(String message, Object... objects)
    {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn(formatMessage(message, objects));
        }
    }

    public void warn(String message, Throwable throwable, Object... objects)
    {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn(formatMessage(message, objects), throwable);
        }
    }

    public void warn(String message, Throwable throwable)
    {
        this.logger.warn(message, throwable);
    }

    public void warn(String message)
    {
        this.logger.warn(message);
    }
}

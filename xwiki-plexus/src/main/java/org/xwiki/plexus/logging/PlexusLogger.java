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
package org.xwiki.plexus.logging;

import org.xwiki.component.logging.AbstractLogger;
import org.xwiki.component.logging.Logger;

/**
 * Logger implementation that should be used when the component manager used is Plexus. Note that this is a wrapper
 * over the Plexus logging facade itself, which can be configured to decide what actual underlying logging system 
 * to use.
 * 
 * @see Logger
 * @version $Id$
 */
public class PlexusLogger extends AbstractLogger
{
    /** Wrapped Plexus logger object. This communicates with the underlying logging framework. */
    private org.codehaus.plexus.logging.Logger logger;

    /**
     * Default constructor, wrapping the actual logger created by Plexus.
     * 
     * @param logger The internal Plexus logger.
     */
    public PlexusLogger(org.codehaus.plexus.logging.Logger logger)
    {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#debug(String)
     */
    public void debug(String message)
    {
        this.logger.debug(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#debug(String, Throwable)
     */
    public void debug(String message, Throwable throwable)
    {
        this.logger.debug(message, throwable);
    }

    /**
     * {@inheritDoc}
     * 
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
     * 
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
     * 
     * @see Logger#isDebugEnabled()
     */
    public boolean isDebugEnabled()
    {
        return this.logger.isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#info(String)
     */
    public void info(String message)
    {
        this.logger.info(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#info(String, Throwable)
     */
    public void info(String message, Throwable throwable)
    {
        this.logger.info(message, throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#info(String, Object...)
     */
    public void info(String message, Object... objects)
    {
        if (this.logger.isInfoEnabled()) {
            this.logger.info(formatMessage(message, objects));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#info(String, Throwable, Object...)
     */
    public void info(String message, Throwable throwable, Object... objects)
    {
        if (this.logger.isInfoEnabled()) {
            this.logger.info(formatMessage(message, objects), throwable);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#isInfoEnabled()
     */
    public boolean isInfoEnabled()
    {
        return this.logger.isInfoEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#warn(String)
     */
    public void warn(String message)
    {
        this.logger.warn(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#warn(String, Throwable)
     */
    public void warn(String message, Throwable throwable)
    {
        this.logger.warn(message, throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#warn(String, Object...)
     */
    public void warn(String message, Object... objects)
    {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn(formatMessage(message, objects));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#warn(String, Throwable, Object...)
     */
    public void warn(String message, Throwable throwable, Object... objects)
    {
        if (this.logger.isWarnEnabled()) {
            this.logger.warn(formatMessage(message, objects), throwable);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#isWarnEnabled()
     */
    public boolean isWarnEnabled()
    {
        return this.logger.isWarnEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#error(String)
     */
    public void error(String message)
    {
        this.logger.error(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#error(String, Throwable)
     */
    public void error(String message, Throwable throwable)
    {
        this.logger.error(message, throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#error(String, Object...)
     */
    public void error(String message, Object... objects)
    {
        if (this.logger.isErrorEnabled()) {
            this.logger.error(formatMessage(message, objects));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#error(String, Throwable, Object...)
     */
    public void error(String message, Throwable throwable, Object... objects)
    {
        if (this.logger.isErrorEnabled()) {
            this.logger.error(formatMessage(message, objects), throwable);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Logger#isErrorEnabled()
     */
    public boolean isErrorEnabled()
    {
        return this.logger.isErrorEnabled();
    }
}

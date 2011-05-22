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
package org.xwiki.extension.repository.aether.internal;

import org.slf4j.Logger;

class XWikiLogger implements org.codehaus.plexus.logging.Logger
{
    private Logger logger;

    public XWikiLogger(Logger logger)
    {
        this.logger = logger;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#debug(java.lang.String)
     */
    public void debug(String message)
    {
        this.logger.debug(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#debug(java.lang.String, java.lang.Throwable)
     */
    public void debug(String message, Throwable throwable)
    {
        this.logger.debug(message, throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#info(java.lang.String)
     */
    public void info(String message)
    {
        this.logger.info(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#info(java.lang.String, java.lang.Throwable)
     */
    public void info(String message, Throwable throwable)
    {
        this.logger.info(message, throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#warn(java.lang.String)
     */
    public void warn(String message)
    {
        this.logger.warn(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#warn(java.lang.String, java.lang.Throwable)
     */
    public void warn(String message, Throwable throwable)
    {
        this.logger.warn(message, throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#fatalError(java.lang.String)
     */
    public void fatalError(String message)
    {
        this.logger.error(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#fatalError(java.lang.String, java.lang.Throwable)
     */
    public void fatalError(String message, Throwable throwable)
    {
        this.logger.error(message, throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#error(java.lang.String)
     */
    public void error(String message)
    {
        this.logger.error(message);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#error(java.lang.String, java.lang.Throwable)
     */
    public void error(String message, Throwable throwable)
    {
        this.logger.error(message, throwable);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#isDebugEnabled()
     */
    public boolean isDebugEnabled()
    {
        return this.logger.isDebugEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#isInfoEnabled()
     */
    public boolean isInfoEnabled()
    {
        return this.logger.isInfoEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#isWarnEnabled()
     */
    public boolean isWarnEnabled()
    {
        return this.logger.isWarnEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#isErrorEnabled()
     */
    public boolean isErrorEnabled()
    {
        return this.logger.isErrorEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#isFatalErrorEnabled()
     */
    public boolean isFatalErrorEnabled()
    {
        return this.logger.isErrorEnabled();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#setThreshold(int)
     */
    public void setThreshold(int treshold)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#getThreshold()
     */
    public int getThreshold()
    {
        return LEVEL_DEBUG;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#getChildLogger(java.lang.String)
     */
    public org.codehaus.plexus.logging.Logger getChildLogger(String name)
    {
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.Logger#getName()
     */
    public String getName()
    {
        return "xwiki logger";
    }

}

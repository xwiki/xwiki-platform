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

    @Override
    public void debug(String message)
    {
        this.logger.debug(message);
    }

    @Override
    public void debug(String message, Throwable throwable)
    {
        this.logger.debug(message, throwable);
    }

    @Override
    public void info(String message)
    {
        this.logger.info(message);
    }

    @Override
    public void info(String message, Throwable throwable)
    {
        this.logger.info(message, throwable);
    }

    @Override
    public void warn(String message)
    {
        this.logger.warn(message);
    }

    @Override
    public void warn(String message, Throwable throwable)
    {
        this.logger.warn(message, throwable);
    }

    @Override
    public void fatalError(String message)
    {
        this.logger.error(message);
    }

    @Override
    public void fatalError(String message, Throwable throwable)
    {
        this.logger.error(message, throwable);
    }

    @Override
    public void error(String message)
    {
        this.logger.error(message);
    }

    @Override
    public void error(String message, Throwable throwable)
    {
        this.logger.error(message, throwable);
    }

    @Override
    public boolean isDebugEnabled()
    {
        return this.logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled()
    {
        return this.logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled()
    {
        return this.logger.isWarnEnabled();
    }

    @Override
    public boolean isErrorEnabled()
    {
        return this.logger.isErrorEnabled();
    }

    @Override
    public boolean isFatalErrorEnabled()
    {
        return this.logger.isErrorEnabled();
    }

    @Override
    public void setThreshold(int treshold)
    {
    }

    @Override
    public int getThreshold()
    {
        return LEVEL_DEBUG;
    }

    @Override
    public org.codehaus.plexus.logging.Logger getChildLogger(String name)
    {
        return this;
    }

    @Override
    public String getName()
    {
        return "xwiki logger";
    }

}

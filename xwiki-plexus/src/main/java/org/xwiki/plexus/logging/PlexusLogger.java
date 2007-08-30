/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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

import org.xwiki.component.logging.Logger;

public class PlexusLogger implements Logger
{
    private org.codehaus.plexus.logging.Logger logger;

    public PlexusLogger(org.codehaus.plexus.logging.Logger logger)
    {
        this.logger = logger;
    }

    public void debug(String message)
    {
        this.logger.debug(message);
    }

    public void debug(String message, Throwable throwable)
    {
        this.logger.debug(message, throwable);
    }

    public boolean isDebugEnabled()
    {
        return this.logger.isDebugEnabled();
    }

    public void info(String message)
    {
        this.logger.info(message);
    }

    public void info(String message, Throwable throwable)
    {
        this.logger.info(message, throwable);
    }

    public boolean isInfoEnabled()
    {
        return this.logger.isInfoEnabled();
    }

    public void warn(String message)
    {
        this.logger.warn(message);
    }

    public void warn(String message, Throwable throwable)
    {
        this.logger.warn(message, throwable);
    }

    public boolean isWarnEnabled()
    {
        return this.logger.isWarnEnabled();
    }

    public void error(String message)
    {
        this.logger.error(message);
    }

    public void error(String message, Throwable throwable)
    {
        this.logger.error(message, throwable);
    }

    public boolean isErrorEnabled()
    {
        return this.logger.isErrorEnabled();
    }

    public Logger getChildLogger(String name)
    {
        return new PlexusLogger(this.logger.getChildLogger(name));
    }

    public int getThreshold()
    {
        return this.logger.getThreshold();
    }

    public void setThreshold(int threshold)
    {
        this.logger.setThreshold(threshold);
    }

    public String getName()
    {
        return this.logger.getName();
    }
}
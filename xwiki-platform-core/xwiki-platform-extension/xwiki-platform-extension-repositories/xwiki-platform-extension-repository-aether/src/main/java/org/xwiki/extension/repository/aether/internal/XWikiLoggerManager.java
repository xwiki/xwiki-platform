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

import org.codehaus.plexus.logging.AbstractLoggerManager;
import org.slf4j.Logger;

public class XWikiLoggerManager extends AbstractLoggerManager
{
    private XWikiLogger logger;

    public XWikiLoggerManager(Logger logger)
    {
        this.logger = new XWikiLogger(logger);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.LoggerManager#getActiveLoggerCount()
     */
    public int getActiveLoggerCount()
    {
        return 1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.LoggerManager#getLoggerForComponent(java.lang.String, java.lang.String)
     */
    public org.codehaus.plexus.logging.Logger getLoggerForComponent(String arg0, String arg1)
    {
        return this.logger;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.LoggerManager#getThreshold()
     */
    public int getThreshold()
    {
        return this.logger.getThreshold();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.LoggerManager#getThreshold(java.lang.String, java.lang.String)
     */
    public int getThreshold(String arg0, String arg1)
    {
        return this.logger.getThreshold();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.LoggerManager#returnComponentLogger(java.lang.String, java.lang.String)
     */
    public void returnComponentLogger(String arg0, String arg1)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.LoggerManager#setThreshold(int)
     */
    public void setThreshold(int arg0)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.LoggerManager#setThreshold(java.lang.String, java.lang.String, int)
     */
    public void setThreshold(String arg0, String arg1, int arg2)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.LoggerManager#setThresholds(int)
     */
    /**
     * {@inheritDoc}
     * 
     * @see org.codehaus.plexus.logging.LoggerManager#setThresholds(int)
     */
    public void setThresholds(int arg0)
    {
    }
}

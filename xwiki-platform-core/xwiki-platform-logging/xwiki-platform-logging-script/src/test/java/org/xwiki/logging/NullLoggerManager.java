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
package org.xwiki.logging;

import java.util.Collection;

import org.slf4j.Logger;
import org.xwiki.observation.EventListener;

/**
 * Null logger manager, to be overridden if need be.
 * 
 * @version $Id$
 * @since 13.10.11
 * @since 14.4.7
 * @since 14.10
 */
public class NullLoggerManager implements LoggerManager
{
    @Override
    public void pushLogListener(EventListener listener)
    {
        
    }

    @Override
    public EventListener popLogListener()
    {
        return null;
    }

    @Override
    public void setLoggerLevel(String loggerName, LogLevel level)
    {

    }

    @Override
    public LogLevel getLoggerLevel(String loggerName)
    {
        return null;
    }

    @Override
    public Collection<Logger> getLoggers()
    {
        return null;
    }
}

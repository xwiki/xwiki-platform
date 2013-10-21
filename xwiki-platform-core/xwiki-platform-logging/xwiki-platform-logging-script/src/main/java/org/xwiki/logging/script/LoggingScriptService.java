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
package org.xwiki.logging.script;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LoggerManager;
import org.xwiki.script.service.ScriptService;

/**
 * Provide logging related script oriented APIs.
 * 
 * @since 4.2M3
 * @version $Id$
 */
@Component
@Named("logging")
@Singleton
public class LoggingScriptService implements ScriptService
{
    /**
     * Used to manipulate loggers.
     */
    @Inject
    private LoggerManager loggerManager;

    /**
     * Used to check rights.
     */
    @Inject
    private DocumentAccessBridge bridge;

    // Get/Set log levels

    /**
     * @return all the loggers (usually packages) with corresponding levels.
     */
    public Map<String, LogLevel> getLevels()
    {
        Collection<Logger> loggers = this.loggerManager.getLoggers();

        Map<String, LogLevel> levels = new HashMap<String, LogLevel>(loggers.size());

        for (Logger logger : loggers) {
            levels.put(logger.getName(), getLevel(logger.getName()));
        }

        return levels;
    }

    /**
     * @param loggerName the logger name (usually packages)
     * @return the level associated to the logger
     */
    public LogLevel getLevel(String loggerName)
    {
        return this.loggerManager.getLoggerLevel(loggerName);
    }

    /**
     * @param logger the logger name (usually package)
     * @param level the level associated to the logger
     */
    public void setLevel(String logger, LogLevel level)
    {
        // Not allow anyone to set log level or it could be a window to produce a real mess even if not exactly a
        // security issue
        if (!this.bridge.hasProgrammingRights()) {
            return;
        }

        this.loggerManager.setLoggerLevel(logger, level);
    }
}

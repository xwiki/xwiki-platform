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

package liquibase.ext.logger4xwiki;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.store.migration.DataMigrationManager;

import liquibase.logging.LogLevel;

/**
 * Implementation of the liquibase logger for the migration component.
 *
 * Note: the package name is imposed by liquibase.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class XWikiLiquibaseLogger implements liquibase.logging.Logger
{
    /** Get logger for com.xpn.xwiki.store.migration.liquibase. */
    private static final Logger LOGGER = LoggerFactory.getLogger(
        DataMigrationManager.class.getPackage().getName() + '.'
            + liquibase.Liquibase.class.getPackage().getName());

    @Override
    public int getPriority()
    {
        // Liquibase use value between 1 and 5, so 6 will be prioritized
        return 6;
    }
    
    @Override
    public void setName(String s)
    {
        // do not care
    }

    @Override
    public void setLogLevel(String s)
    {
        // ignored, use logging backend level
    }

    @Override
    public void setLogLevel(LogLevel logLevel)
    {
        // ignored, use logging backend level
    }

    @Override
    public void setLogLevel(String s, String s1)
    {
        // ignored, use logging backend level
    }

    @Override
    public LogLevel getLogLevel()
    {
        if (LOGGER.isDebugEnabled()) {
            return LogLevel.DEBUG;
        }
        if (LOGGER.isInfoEnabled()) {
            return LogLevel.INFO;
        }
        if (LOGGER.isWarnEnabled()) {
            return LogLevel.WARNING;
        }
        if (LOGGER.isErrorEnabled()) {
            return LogLevel.SEVERE;
        }
        return LogLevel.OFF;
    }

    @Override
    public void severe(String s)
    {
        LOGGER.error(s);
    }

    @Override
    public void severe(String s, Throwable throwable)
    {
        LOGGER.error(s, throwable);
    }

    @Override
    public void warning(String s)
    {
        // Do not log "if this is the case" silly messages, this is not the case !!!
        if (s.endsWith("if this is the case")) {
            return;
        }
        LOGGER.warn(s);
    }

    @Override
    public void warning(String s, Throwable throwable)
    {
        LOGGER.warn(s, throwable);
    }

    @Override
    public void info(String s)
    {
        LOGGER.info(s);
    }

    @Override
    public void info(String s, Throwable throwable)
    {
        LOGGER.info(s, throwable);
    }

    @Override
    public void debug(String s)
    {
        LOGGER.debug(s);
    }

    @Override
    public void debug(String s, Throwable throwable)
    {
        LOGGER.debug(s, throwable);
    }
}

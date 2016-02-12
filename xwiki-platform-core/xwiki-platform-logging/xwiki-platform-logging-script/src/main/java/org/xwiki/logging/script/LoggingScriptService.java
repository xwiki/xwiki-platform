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
import org.slf4j.LoggerFactory;
import org.slf4j.event.LoggingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LoggerManager;
import org.xwiki.logging.LoggingEventMessage;
import org.xwiki.logging.util.LoggingEventTree;
import org.xwiki.logging.util.LoggingUtils;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

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
    private ContextualAuthorizationManager authorization;

    @Inject
    private ContextualLocalizationManager localization;

    /**
     * Return a logger named according to the name parameter.
     * 
     * @param name The name of the logger.
     * @return logger
     * @since 6.1RC1
     */
    public Logger getLogger(String name)
    {
        return LoggerFactory.getLogger(name);
    }

    // Get/Set log levels

    /**
     * @return all the loggers (usually packages) with corresponding levels.
     */
    public Map<String, LogLevel> getLevels()
    {
        Collection<Logger> registeredLoggers = this.loggerManager.getLoggers();

        Map<String, LogLevel> levels = new HashMap<String, LogLevel>(registeredLoggers.size());

        for (Logger registeredLogger : registeredLoggers) {
            levels.put(registeredLogger.getName(), getLevel(registeredLogger.getName()));
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
        if (!this.authorization.hasAccess(Right.PROGRAM)) {
            return;
        }

        this.loggerManager.setLoggerLevel(logger, level);
    }

    /**
     * Create a tree representation of a series of logs.
     * 
     * @param logs the logs
     * @return the logs as a {@link LogTree}
     * @since 5.4RC1
     * @deprecated since 8.0M2, use {@link #toLoggingEventMessageTree(Iterable)} instead
     */
    @Deprecated
    public LogTree toLogTree(Iterable<LogEvent> logs)
    {
        LogTree logTree = new LogTree();

        for (LogEvent logEvent : logs) {
            logTree.log(logEvent);
        }

        return logTree;
    }

    public LoggingEventTree toLoggingEventMessageTree(Iterable<? extends LoggingEvent> logs)
    {
        LoggingEventTree logTree = new LoggingEventTree();

        for (LoggingEvent logEvent : logs) {
            logTree.log(logEvent);
        }

        return logTree;
    }

    /**
     * Translate the passed {@link LogEvent} based on the translation message corresponding to the translation key
     * stored in the {@link LogEvent}.
     * <p>
     * The translation message pattern use the same syntax than standard message pattern except that it's optionally
     * possible to provide a custom index as in <code>Some {1} translation {0} message</code> in order to modify the
     * order of the argument which can be required depending on the language.
     * 
     * @param logEvent the {@link LogEvent} to translate
     * @return the translated version of the passed {@link LogEvent}
     * @deprecated since 8.0M2, use {@link #translate(LoggingEvent)} instead
     */
    @Deprecated
    public LogEvent translate(LogEvent logEvent)
    {
        if (logEvent.getTranslationKey() != null) {
            Translation translation = this.localization.getTranslation(logEvent.getTranslationKey());

            if (translation != null) {
                return LogUtils.translate(logEvent, (String) translation.getRawSource());
            }
        }

        return logEvent;
    }

    /**
     * Translate the passed {@link LogEvent} based on the translation message corresponding to the translation key
     * stored in the {@link LogEvent}.
     * <p>
     * The translation message pattern use the same syntax than standard message pattern except that it's optionally
     * possible to provide a custom index as in <code>Some {1} translation {0} message</code> in order to modify the
     * order of the argument which can be required depending on the language.
     * 
     * @param logEvent the {@link LogEvent} to translate
     * @return the translated version of the passed {@link LogEvent}
     */
    public LoggingEvent translate(LoggingEvent logEvent)
    {
        String translationKey = LoggingEventMessage.getTranslationKey(logEvent);
        if (translationKey != null) {
            Translation translation = this.localization.getTranslation(translationKey);

            if (translation != null) {
                return LoggingUtils.translate(logEvent, (String) translation.getRawSource());
            }
        }

        return logEvent;
    }
}

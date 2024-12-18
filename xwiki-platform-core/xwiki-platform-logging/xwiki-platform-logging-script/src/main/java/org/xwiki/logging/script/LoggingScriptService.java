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
import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogTree;
import org.xwiki.logging.LogUtils;
import org.xwiki.logging.LoggerConfiguration;
import org.xwiki.logging.LoggerManager;
import org.xwiki.logging.Message;
import org.xwiki.logging.event.LogEvent;
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

    @Inject
    private LoggerConfiguration loggerConfiguration;

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
     */
    public LogTree toLogTree(Iterable<LogEvent> logs)
    {
        LogTree logTree = new LogTree();

        for (LogEvent logEvent : logs) {
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
     */
    public LogEvent translate(LogEvent logEvent)
    {
        return (LogEvent) translate((Message) logEvent);
    }

    /**
     * Translate the passed {@link Message} based on the translation message corresponding to the translation key stored
     * in the {@link Message}.
     * <p>
     * The translation message pattern use the same syntax than standard message pattern except that it's optionally
     * possible to provide a custom index as in <code>Some {1} translation {0} message</code> in order to modify the
     * order of the argument which can be required depending on the language.
     * 
     * @param message the {@link Message} to translate
     * @return the translated version of the passed {@link Message}
     * @since 15.0RC1
     * @since 14.10.1
     */
    public Message translate(Message message)
    {
        if (message != null) {
            String translationKey = message.getTranslationKey();

            if (translationKey != null) {
                Translation translation = this.localization.getTranslation(message.getTranslationKey());

                if (translation != null) {
                    return LogUtils.translate(message, (String) translation.getRawSource());
                }
            }
        }

        return message;
    }

    /**
     * Log a deprecated message, only if the {@link LoggerConfiguration#isDeprecatedLogEnabled()} is {@code true}. Note
     * that the deprecated message is displayed as a warning with the {@code [DEPRECATED]} suffix.
     *
     * @param loggerName the name of the logger to be used for the deprecated message.
     * @param message the message to be displayed for explaining the deprecation.
     * @since 13.1RC1
     */
    public void deprecate(String loggerName, String message)
    {
        if (this.loggerConfiguration.isDeprecatedLogEnabled()) {
            this.getLogger(loggerName).warn("[DEPRECATED] " + message);
        }
    }
}

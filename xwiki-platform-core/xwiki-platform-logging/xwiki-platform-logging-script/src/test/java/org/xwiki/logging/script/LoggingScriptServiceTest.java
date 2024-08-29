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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LoggerConfiguration;
import org.xwiki.logging.LoggerManager;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LoggingScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class LoggingScriptServiceTest
{
    @InjectMockComponents
    private LoggingScriptService loggingScriptService;

    @MockComponent
    private LoggerManager loggerManager;

    @MockComponent
    private LoggerConfiguration loggerConfiguration;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private ContextualLocalizationManager localization;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(org.xwiki.test.LogLevel.WARN);

    @Test
    void getLogger()
    {
        assertEquals(LoggerFactory.getLogger("something"), this.loggingScriptService.getLogger("something"));
    }

    @Test
    void getLevel()
    {
        when(this.loggerManager.getLoggerLevel("foo")).thenReturn(LogLevel.INFO);
        assertEquals(LogLevel.INFO, this.loggingScriptService.getLevel("foo"));
        verify(this.loggerManager).getLoggerLevel("foo");
    }

    @Test
    void getLevels()
    {
        Logger logger1 = mock(Logger.class);
        when(logger1.getName()).thenReturn("logger1");
        Logger logger2 = mock(Logger.class);
        when(logger2.getName()).thenReturn("logger2");
        Logger logger3 = mock(Logger.class);
        when(logger3.getName()).thenReturn("logger3");
        when(loggerManager.getLoggers()).thenReturn(Arrays.asList(logger1, logger2, logger3));
        when(loggerManager.getLoggerLevel("logger1")).thenReturn(LogLevel.INFO);
        when(loggerManager.getLoggerLevel("logger2")).thenReturn(LogLevel.WARN);
        when(loggerManager.getLoggerLevel("logger3")).thenReturn(LogLevel.WARN);

        Map<String, LogLevel> expectedMap = new HashMap<>();
        expectedMap.put("logger1", LogLevel.INFO);
        expectedMap.put("logger2", LogLevel.WARN);
        expectedMap.put("logger3", LogLevel.WARN);

        assertEquals(expectedMap, this.loggingScriptService.getLevels());
    }

    @Test
    void setLevel()
    {
        when(this.authorization.hasAccess(Right.PROGRAM)).thenReturn(false);
        this.loggingScriptService.setLevel("foo", LogLevel.WARN);
        verify(this.loggerManager, never()).setLoggerLevel(any(), any());

        when(this.authorization.hasAccess(Right.PROGRAM)).thenReturn(true);
        this.loggingScriptService.setLevel("foo", LogLevel.INFO);
        verify(this.loggerManager).setLoggerLevel("foo", LogLevel.INFO);
        verify(this.authorization, times(2)).hasAccess(Right.PROGRAM);
    }

    @Test
    void translate()
    {
        LogEvent logEvent = mock(LogEvent.class);
        assertSame(logEvent, this.loggingScriptService.translate(logEvent));

        when(logEvent.getTranslationKey()).thenReturn("translationKey");
        assertSame(logEvent, this.loggingScriptService.translate(logEvent));

        Translation translation = mock(Translation.class);
        when(this.localization.getTranslation("translationKey")).thenReturn(translation);
        when(translation.getRawSource()).thenReturn("The full translation");

        when(logEvent.getLevel()).thenReturn(LogLevel.TRACE);
        when(logEvent.getArgumentArray()).thenReturn(new Object[0]);
        LogEvent expectedLogEvent = new LogEvent(null, LogLevel.TRACE, "The full translation", new Object[0], null);
        assertEquals(expectedLogEvent, this.loggingScriptService.translate(logEvent));
    }

    @Test
    void deprecate()
    {
        when(this.loggerConfiguration.isDeprecatedLogEnabled()).thenReturn(false);
        this.loggingScriptService.deprecate("foo", "Something is deprecated");
        assertEquals(0, this.logCapture.size());

        when(this.loggerConfiguration.isDeprecatedLogEnabled()).thenReturn(true);
        this.loggingScriptService.deprecate("foo", "Something is deprecated");

        assertEquals(1, this.logCapture.size());
        ILoggingEvent logEvent = this.logCapture.getLogEvent(0);
        assertEquals("[DEPRECATED] Something is deprecated", logEvent.getMessage());
        assertEquals("foo", logEvent.getLoggerName());
        assertEquals(Level.WARN, logEvent.getLevel());
    }
}

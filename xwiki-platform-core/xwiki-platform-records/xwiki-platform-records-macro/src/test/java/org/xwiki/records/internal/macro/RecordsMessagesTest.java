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
package org.xwiki.records.internal.macro;

import java.io.Reader;

import jakarta.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RecordsMessages}.
 * <p>
 * How a message is escaped is covered through {@link RecordsMacroTest}, with the real parser and renderer. This test
 * covers the failure the real plain text parser never produces.
 *
 * @version $Id$
 */
@ComponentTest
class RecordsMessagesTest
{
    @InjectMockComponents
    private RecordsMessages messages;

    @MockComponent
    private ContextualLocalizationManager localization;

    @MockComponent
    @Named("plain/1.0")
    private Parser plainParser;

    @Test
    void warningFailsWhenTheMessageCannotBeEscaped() throws Exception
    {
        ParseException cause = new ParseException("parse failure");
        when(this.plainParser.parse(any(Reader.class))).thenThrow(cause);
        when(this.localization.getTranslationPlain("rendering.macro.records.warning.columnSkipped", "first_name"))
            .thenReturn("The field first_name no longer exists.");
        when(this.localization.getTranslationPlain("rendering.macro.records.error.renderFailed"))
            .thenReturn("The table cannot be displayed.");

        MacroExecutionException exception = assertThrows(MacroExecutionException.class,
            () -> this.messages.warning("warning.columnSkipped", "first_name"));

        assertEquals("The table cannot be displayed.", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}

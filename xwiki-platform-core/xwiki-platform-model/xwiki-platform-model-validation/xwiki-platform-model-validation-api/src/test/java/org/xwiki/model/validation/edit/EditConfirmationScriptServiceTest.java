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
package org.xwiki.model.validation.edit;

import java.util.List;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static ch.qos.logback.classic.Level.WARN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test of {@link EditConfirmationScriptService}.
 *
 * @version $Id$
 */
@ComponentTest
class EditConfirmationScriptServiceTest
{
    private static final EditConfirmationCheckerResult WARNING_MESSAGE_1 =
        new EditConfirmationCheckerResult(new WordBlock("warning"), false);

    private static final EditConfirmationCheckerResult WARNING_MESSAGE_2 =
        new EditConfirmationCheckerResult(new WordBlock("warning 2"), false);

    private static final EditConfirmationCheckerResult ERROR_MESSAGE_1 =
        new EditConfirmationCheckerResult(new WordBlock("error"), true);

    @InjectMockComponents
    private EditConfirmationScriptService target;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private EditConfirmationChecker editConfirmationCheckerWarn;

    @Mock
    private EditConfirmationChecker editConfirmationCheckerWarn2;

    @Mock
    private EditConfirmationChecker editConfirmationCheckerError;

    @BeforeEach
    void setUp()
    {
        when(this.editConfirmationCheckerWarn.check()).thenReturn(Optional.of(WARNING_MESSAGE_1));
        when(this.editConfirmationCheckerWarn2.check()).thenReturn(Optional.of(WARNING_MESSAGE_2));
        when(this.editConfirmationCheckerError.check()).thenReturn(Optional.of(ERROR_MESSAGE_1));
    }

    @Test
    void checkWithComponentManagerIssue() throws Exception
    {
        when(this.componentManager.<EditConfirmationChecker>getInstanceList(EditConfirmationChecker.class))
            .thenThrow(ComponentLookupException.class);
        assertEquals(new EditConfirmationCheckerResults(), this.target.check());
        assertEquals("Failed to resolve the list of "
            + "[interface org.xwiki.model.validation.edit.EditConfirmationChecker]. "
            + "Cause: [ComponentLookupException: ]", this.logCapture.getMessage(0));
        assertEquals(WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void check() throws Exception
    {
        when(this.componentManager.<EditConfirmationChecker>getInstanceList(EditConfirmationChecker.class))
            .thenReturn(List.of(this.editConfirmationCheckerWarn, this.editConfirmationCheckerWarn2));
        EditConfirmationCheckerResults expected = new EditConfirmationCheckerResults();
        expected.append(WARNING_MESSAGE_1);
        expected.append(WARNING_MESSAGE_2);
        assertEquals(expected, this.target.check());
    }

    @Test
    void checkResultsEmpty()
    {
        EditConfirmationCheckerResults editConfirmationCheckerResults = new EditConfirmationCheckerResults();
        assertFalse(editConfirmationCheckerResults.isError());
        assertEquals(List.of(), editConfirmationCheckerResults.getErrorMessages());
        assertEquals(List.of(), editConfirmationCheckerResults.getWarningMessages());
    }
}

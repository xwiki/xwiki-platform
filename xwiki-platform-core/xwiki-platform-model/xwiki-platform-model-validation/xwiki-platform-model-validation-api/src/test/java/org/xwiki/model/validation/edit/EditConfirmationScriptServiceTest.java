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
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.GroupBlock;
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
import static org.xwiki.model.validation.edit.EditConfirmationScriptService.CheckResults;

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
    private EditConfirmationChecker editConfirmationCheckerError;

    @BeforeEach
    void setUp()
    {
        // Only return a warning when edit is not forced 
        when(this.editConfirmationCheckerWarn.check(false)).thenReturn(Optional.of(WARNING_MESSAGE_1));
        when(this.editConfirmationCheckerWarn.check(true)).thenReturn(Optional.empty());
        // Return a warning when edit is not forced, and an error otherwise.
        when(this.editConfirmationCheckerError.check(false)).thenReturn(Optional.of(WARNING_MESSAGE_2));
        when(this.editConfirmationCheckerError.check(true)).thenReturn(Optional.of(ERROR_MESSAGE_1));
    }

    @Test
    void checkWithComponentManagerIssue() throws Exception
    {
        when(this.componentManager.<EditConfirmationChecker>getInstanceList(EditConfirmationChecker.class))
            .thenThrow(ComponentLookupException.class);
        assertEquals(new CheckResults(), this.target.check(null));
        assertEquals("Failed to resolve the list of "
            + "[interface org.xwiki.model.validation.edit.EditConfirmationChecker]. "
            + "Cause: [ComponentLookupException: ]", this.logCapture.getMessage(0));
        assertEquals(WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @ParameterizedTest
    @ValueSource(booleans = false)
    @NullSource
    void check(Boolean editForced) throws Exception
    {
        when(this.componentManager.<EditConfirmationChecker>getInstanceList(EditConfirmationChecker.class))
            .thenReturn(List.of(this.editConfirmationCheckerWarn, this.editConfirmationCheckerError));
        CheckResults expected = new CheckResults();
        expected.append(WARNING_MESSAGE_1);
        expected.append(WARNING_MESSAGE_2);
        assertEquals(expected, this.target.check(editForced));
    }

    @Test
    void checkEditForced() throws Exception
    {
        when(this.componentManager.<EditConfirmationChecker>getInstanceList(EditConfirmationChecker.class))
            .thenReturn(List.of(this.editConfirmationCheckerWarn, this.editConfirmationCheckerError));
        CheckResults expected = new CheckResults();
        expected.append(ERROR_MESSAGE_1);
        assertEquals(expected, this.target.check(true));
    }

    @Test
    void checkResultsEmpty()
    {
        CheckResults checkResults = new CheckResults();
        assertFalse(checkResults.isError());
        assertEquals(List.of(), checkResults.getMessages());
    }

    @Test
    void checkResultsOneWarning()
    {
        CheckResults checkResults = new CheckResults();
        checkResults.append(WARNING_MESSAGE_1);
        assertFalse(checkResults.isError());
        assertEquals(List.of(new GroupBlock(List.of(new WordBlock("warning")), Map.of("class", "box warningmessage"))),
            checkResults.getMessages());
    }

    @Test
    void checkResultsOneError()
    {
        CheckResults checkResults = new CheckResults();
        checkResults.append(ERROR_MESSAGE_1);
        assertTrue(checkResults.isError());
        assertEquals(List.of(
            new GroupBlock(List.of(new WordBlock("error")), Map.of("class", "box errormessage"))
        ), checkResults.getMessages());
    }


    @Test
    void checkResultsTwoWarning()
    {
        CheckResults checkResults = new CheckResults();
        checkResults.append(WARNING_MESSAGE_1);
        checkResults.append(WARNING_MESSAGE_2);
        assertFalse(checkResults.isError());
        assertEquals(List.of(
            new GroupBlock(List.of(new WordBlock("warning")), Map.of("class", "box warningmessage")),
            new GroupBlock(List.of(new WordBlock("warning 2")), Map.of("class", "box warningmessage"))
        ), checkResults.getMessages());
    }

    @Test
    void checkResultsOneWarningOneError()
    {
        CheckResults checkResults = new CheckResults();
        checkResults.append(WARNING_MESSAGE_1);
        checkResults.append(ERROR_MESSAGE_1);
        assertTrue(checkResults.isError());
        assertEquals(List.of(
            new GroupBlock(List.of(new WordBlock("warning")), Map.of("class", "box warningmessage")),
            new GroupBlock(List.of(new WordBlock("error")), Map.of("class", "box errormessage"))
        ), checkResults.getMessages());
    }
}

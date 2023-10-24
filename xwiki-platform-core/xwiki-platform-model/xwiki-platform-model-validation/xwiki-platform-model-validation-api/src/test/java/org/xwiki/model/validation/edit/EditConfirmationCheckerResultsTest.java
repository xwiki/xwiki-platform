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

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.WordBlock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link EditConfirmationCheckerResults}.
 *
 * @version $Id$
 */
class EditConfirmationCheckerResultsTest
{
    private static final EditConfirmationCheckerResult WARNING_MESSAGE_1 =
        new EditConfirmationCheckerResult(new WordBlock("warning"), false);

    private static final EditConfirmationCheckerResult WARNING_MESSAGE_2 =
        new EditConfirmationCheckerResult(new WordBlock("warning 2"), false);

    private static final EditConfirmationCheckerResult ERROR_MESSAGE_1 =
        new EditConfirmationCheckerResult(new WordBlock("error"), true);

    @Test
    void checkResultsOneWarning()
    {
        EditConfirmationCheckerResults editConfirmationCheckerResults = new EditConfirmationCheckerResults();
        editConfirmationCheckerResults.append(WARNING_MESSAGE_1);
        assertFalse(editConfirmationCheckerResults.isError());
        assertEquals(List.of(new WordBlock("warning")), editConfirmationCheckerResults.getWarningMessages());
        assertEquals(List.of(), editConfirmationCheckerResults.getErrorMessages());
    }

    @Test
    void checkResultsOneError()
    {
        EditConfirmationCheckerResults editConfirmationCheckerResults = new EditConfirmationCheckerResults();
        editConfirmationCheckerResults.append(ERROR_MESSAGE_1);
        assertTrue(editConfirmationCheckerResults.isError());
        assertEquals(List.of(), editConfirmationCheckerResults.getWarningMessages());
        assertEquals(List.of(new WordBlock("error")), editConfirmationCheckerResults.getErrorMessages());
    }

    @Test
    void checkResultsTwoWarning()
    {
        EditConfirmationCheckerResults editConfirmationCheckerResults = new EditConfirmationCheckerResults();
        editConfirmationCheckerResults.append(WARNING_MESSAGE_1);
        editConfirmationCheckerResults.append(WARNING_MESSAGE_2);
        assertFalse(editConfirmationCheckerResults.isError());
        assertEquals(List.of(new WordBlock("warning"), new WordBlock("warning 2")), editConfirmationCheckerResults.getWarningMessages());
        assertEquals(List.of(), editConfirmationCheckerResults.getErrorMessages());
    }

    @Test
    void checkResultsOneWarningOneError()
    {
        EditConfirmationCheckerResults editConfirmationCheckerResults = new EditConfirmationCheckerResults();
        editConfirmationCheckerResults.append(WARNING_MESSAGE_1);
        editConfirmationCheckerResults.append(ERROR_MESSAGE_1);
        assertTrue(editConfirmationCheckerResults.isError());
        assertEquals(List.of(new WordBlock("warning")), editConfirmationCheckerResults.getWarningMessages());
        assertEquals(List.of(new WordBlock("error")), editConfirmationCheckerResults.getErrorMessages());
    }
}

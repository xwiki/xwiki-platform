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
package org.xwiki.test.ui.po;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.JavascriptException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BasePage}.
 *
 * @version $Id$
 */
class BasePageTest
{
    @ParameterizedTest
    @ValueSource(strings = {
        // Chrome, when the axe object is gone: the analysis reads the entry point off it.
        "javascript error: Cannot read properties of undefined (reading 'runPartial')",
        // Firefox, when the axe binding itself is gone.
        "ReferenceError: axe is not defined",
        "TypeError: can't access property \"runPartial\", window.axe is undefined"
    })
    void isAxeNotReadyErrorWithAxeMissing(String message)
    {
        assertTrue(BasePage.isAxeNotReadyError(new JavascriptException(message)));
    }

    @Test
    void isAxeNotReadyErrorWithUnrelatedError()
    {
        assertFalse(BasePage.isAxeNotReadyError(
            new JavascriptException("javascript error: Cannot read properties of undefined (reading 'toLowerCase')")));
    }
}

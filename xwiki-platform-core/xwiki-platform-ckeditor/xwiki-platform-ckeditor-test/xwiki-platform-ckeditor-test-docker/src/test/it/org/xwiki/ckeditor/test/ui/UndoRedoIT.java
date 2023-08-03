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
package org.xwiki.ckeditor.test.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

/**
 * Integration tests for the Undo / Redo operations.
 * 
 * @version $Id$
 * @since 15.5.1
 * @since 15.6RC1
 */
@UITest
public class UndoRedoIT extends AbstractCKEditorIT
{
    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        createAndLoginStandardUser(setup);
    }

    @BeforeEach
    void beforeEach(TestUtils setup, TestReference testReference)
    {
        edit(setup, testReference);
    }

    @AfterEach
    void afterEach(TestUtils setup, TestReference testReference)
    {
        maybeLeaveEditMode(setup, testReference);
    }

    @Test
    @Order(1)
    void undoRedoWithPlaceholder(TestUtils setup, TestReference testReference) throws Exception
    {
        textArea.sendKeys("one", Keys.ENTER, "two", Keys.ENTER, "three", Keys.ENTER);

        // Undo 4 steps.
        textArea.sendKeys(Keys.chord(Keys.CONTROL, "zzzz"));

        // Redo 2 steps.
        textArea.sendKeys(Keys.chord(Keys.CONTROL, Keys.SHIFT, "zz"));

        assertSourceEquals("one\n\ntwo\n\n ");
    }
}

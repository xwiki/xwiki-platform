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
package org.xwiki.blocknote.test.ui;

import java.util.concurrent.Callable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.xwiki.test.ui.TestUtils;

/**
 * Base class for all BlockNote integration tests.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
abstract class AbstractBlockNoteIT
{
    @BeforeAll
    static void beforeAll(TestUtils setup)
    {
        // Make Blocknote the default WYSIWYG editor.
        setup.loginAsSuperAdmin();
        setup.addObject("XWiki", "XWikiPreferences", "XWiki.EditorBindingClass", "dataType",
            "org.xwiki.rendering.syntax.SyntaxContent#wysiwyg", "roleHint", "blocknote");

        // Create a user without administration and script rights, then make it advanced in order to have access to the
        // edit dropdown and also to be able to edit in-place.
        setup.createUserAndLogin("John", "pass", "editor", "Wysiwyg", "usertype", "Advanced");
    }

    @AfterEach
    void afterEach(TestUtils setup)
    {
        setup.maybeLeaveEditMode();
    }

    protected void loginAsJohn(TestUtils setup)
    {
        setup.login("John", "pass");
    }

    protected <T> T disableWCAG(TestUtils setup, Callable<T> testCode) throws Exception
    {
        boolean wcagEnabled = setup.getWCAGUtils().getWCAGContext().isWCAGEnabled();
        setup.getWCAGUtils().getWCAGContext().setWCAGEnabled(false);
        try {
            return testCode.call();
        } finally {
            setup.getWCAGUtils().getWCAGContext().setWCAGEnabled(wcagEnabled);
        }
    }
}

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
package org.xwiki.wiki.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.wiki.test.po.CreateWikiPage;
import org.xwiki.wiki.test.po.WikiIndexPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests the validation of the first step of the wiki creation wizard.
 *
 * @version $Id$
 * @since 18.8.0RC1
 */
@UITest
class CreateWikiIT
{
    private static final String WIKI_PRETTY_NAME = "My new wiki";

    private static final String WIKI_ID = "mynewwiki";

    private static final String MAIN_WIKI_ID = "xwiki";

    @Test
    @Order(1)
    void validateFirstStepWhateverTheFieldOrder(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        CreateWikiPage createWikiPage = WikiIndexPage.gotoPage().createWiki();

        // The wizard cannot be continued as long as the form is empty, and no error is reported before the user
        // has filled anything. The button is disabled by the form script, so wait for it rather than asserting.
        createWikiPage.waitUntilNextStepIsDisabled();
        assertEquals("", createWikiPage.getPrettyNameValidationMessage());

        // Fill the identifier before the pretty name. Setting the identifier by hand stops it from being computed
        // from the pretty name, so this is the order that used to leave the button disabled forever.
        createWikiPage.setWikiName(WIKI_ID);
        assertFalse(createWikiPage.isNextStepEnabled());
        createWikiPage.setPrettyName(WIKI_PRETTY_NAME);
        createWikiPage.waitUntilNextStepIsEnabled();

        // Emptying either field disables the button again, and filling it back enables it without touching the
        // other one.
        createWikiPage.clearPrettyName();
        createWikiPage.waitUntilNextStepIsDisabled();
        assertEquals("Pretty name must not be empty", createWikiPage.getPrettyNameValidationMessage());
        createWikiPage.setPrettyName(WIKI_PRETTY_NAME);
        createWikiPage.waitUntilNextStepIsEnabled();

        // An identifier that is already taken keeps the button disabled.
        createWikiPage.setWikiName(MAIN_WIKI_ID);
        createWikiPage.waitForWikiNameValidationMessage("This identifier is already used");
        assertFalse(createWikiPage.isNextStepEnabled());

        // So does an empty identifier.
        createWikiPage.clearWikiName();
        createWikiPage.waitForWikiNameValidationMessage("Identifier can't be empty");
        assertFalse(createWikiPage.isNextStepEnabled());
    }
}

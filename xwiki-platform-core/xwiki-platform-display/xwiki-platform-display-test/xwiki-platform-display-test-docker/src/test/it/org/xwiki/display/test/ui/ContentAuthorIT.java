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
package org.xwiki.display.test.ui;

import java.util.Locale;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Make sure displayed content is executed with the right author in various use cases.
 *
 * @version $Id$
 */
@UITest
class ContentAuthorIT
{
    private static final String EXECUTED = "EXECUTED";

    private static final String SCRIPT = "{{velocity}}#set ($x = 'EXEC')${x}UTED{{/velocity}}";

    private static final String ASYNC_CLASS = "XWiki.DocumentAsyncClass";

    private static final String LANGUAGE_FR = "language=fr";

    private static final DocumentReference HOME_REFERENCE = new DocumentReference("xwiki", "Main", "WebHome");

    @Test
    @Order(1)
    void displayTranslatedContent(TestUtils setup, TestReference reference) throws Exception
    {
        displayTranslatedContent(setup, reference, "displayTranslatedContent", false);
    }

    @Test
    @Order(2)
    void displayTranslatedAsynchronousContent(TestUtils setup, TestReference reference) throws Exception
    {
        displayTranslatedContent(setup, reference, "displayTranslatedAsynchronousContent", true);
    }

    @Test
    @Order(3)
    void renderAsynchronousContent(TestUtils setup, TestReference reference) throws Exception
    {
        String userName = "renderAsynchronousContent";
        setup.loginAsSuperAdmin();
        setup.createUser(userName, userName, null);

        boolean homeCreated = createHomePage(setup);

        try {
            // A page the user is not allowed to view.
            DocumentReference hiddenReference = new DocumentReference("Hidden", reference.getLastSpaceReference());
            setup.rest().savePage(hiddenReference, "Hidden content", "");
            setup.setRights(hiddenReference, null, "XWiki." + userName, "view", false);

            // Content with asynchronous rendering written by a user without script right.
            setup.login(userName, userName);
            DocumentReference userReference = new DocumentReference("UserContent", reference.getLastSpaceReference());
            setup.rest().savePage(userReference, String.format("User content\n\n%s\n\n{{display reference=\"%s\"/}}",
                SCRIPT, setup.serializeReference(hiddenReference)), "");
            setup.rest().addObject(userReference, ASYNC_CLASS, "async_enabled", 1, "async_context", "wiki");

            // Content with asynchronous rendering written by a user with script right.
            setup.loginAsSuperAdmin();
            DocumentReference adminReference =
                new DocumentReference("AdminContent", reference.getLastSpaceReference());
            setup.rest().savePage(adminReference, "Admin content\n\n" + SCRIPT, "");
            setup.rest().addObject(adminReference, ASYNC_CLASS, "async_enabled", 1, "async_context", "wiki");

            String content = setup.gotoPage(userReference).getContent();
            assertTrue(content.contains("User content"), content);
            assertFalse(content.contains(EXECUTED), content);
            assertFalse(content.contains("Hidden content"), content);

            content = setup.gotoPage(adminReference).getContent();
            assertTrue(content.contains("Admin content"), content);
            assertTrue(content.contains(EXECUTED), content);
        } finally {
            setup.loginAsSuperAdmin();
            deleteHomePage(setup, homeCreated);
        }
    }

    @Test
    @Order(4)
    void displayModifiedAsynchronousContent(TestUtils setup, TestReference reference) throws Exception
    {
        // A page written by a user with programming right.
        setup.loginAsSuperAdmin();
        setup.rest().savePage(reference, "Saved content", "");

        // A script without programming right which modifies that page and makes it asynchronous, without saving it.
        String userName = "displayModifiedAsynchronousContent";
        setup.createUser(userName, userName, null);
        setup.setGlobalRights(null, userName, "script", true);
        setup.login(userName, userName);
        DocumentReference scriptReference = new DocumentReference("Script", reference.getLastSpaceReference());
        StringBuilder source = new StringBuilder();
        source.append("{{velocity}}\n");
        source.append("#set ($modified = $xwiki.getDocument('" + setup.serializeReference(reference) + "'))\n");
        source.append("$modified.setContent('Modified content {{groovy}}print(\"EXEC\" + \"UTED\"){{/groovy}}')\n");
        source.append("#set ($asyncObject = $modified.newObject('" + ASYNC_CLASS + "'))\n");
        source.append("$asyncObject.set('async_enabled', 1)\n");
        source.append("{{html clean=\"false\"}}$services.display.content($modified){{/html}}\n");
        source.append("{{/velocity}}");
        setup.rest().savePage(scriptReference, source.toString(), "");

        ViewPage viewPage = setup.gotoPage(scriptReference);
        viewPage.waitUntilContent("Modified content");
        String content = viewPage.getContent();
        assertFalse(content.contains(EXECUTED), content);
    }

    private void displayTranslatedContent(TestUtils setup, TestReference reference, String userName, boolean async)
        throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.createUser(userName, userName, null);
        setup.setWikiPreference("multilingual", "true");
        setup.setWikiPreference("languages", "en,fr");
        boolean homeCreated = createHomePage(setup);

        try {
            // A page written by a user with script right.
            DocumentReference displayedReference =
                new DocumentReference("Displayed", reference.getLastSpaceReference());
            setup.rest().savePage(displayedReference, "Default content\n\n" + SCRIPT, "");
            if (async) {
                setup.rest().addObject(displayedReference, ASYNC_CLASS, "async_enabled", 1, "async_context", "wiki");
            }
            // A page displaying the content of the previous page.
            DocumentReference displayReference = new DocumentReference("Display", reference.getLastSpaceReference());
            setup.rest().savePage(displayReference,
                String.format("{{display reference=\"%s\"/}}", setup.serializeReference(displayedReference)), "");

            // A translation written by a user without script right.
            setup.login(userName, userName);
            setup.rest().savePage(new DocumentReference(displayedReference, Locale.FRENCH),
                "Translated content\n\n" + SCRIPT, "");

            setup.loginAsSuperAdmin();

            setup.gotoPage(displayReference, "view", LANGUAGE_FR);
            String content = new ViewPage().getContent();
            assertTrue(content.contains("Translated content"), content);
            assertFalse(content.contains(EXECUTED), content);

            setup.gotoPage(displayReference, "view", "language=en");
            content = new ViewPage().getContent();
            assertTrue(content.contains("Default content"), content);
            assertTrue(content.contains(EXECUTED), content);
        } finally {
            setup.loginAsSuperAdmin();
            setup.setWikiPreference("multilingual", "false");
            setup.setWikiPreference("default_language", "en");
            deleteHomePage(setup, homeCreated);
        }
    }

    /**
     * Make sure the wiki home page is written by a user with script right, as in a standard distribution.
     *
     * @return {@code true} if the home page was created
     */
    private boolean createHomePage(TestUtils setup) throws Exception
    {
        boolean created = !setup.rest().exists(HOME_REFERENCE);
        if (created) {
            setup.rest().savePage(HOME_REFERENCE, "Home", "");
        }
        return created;
    }

    private void deleteHomePage(TestUtils setup, boolean created) throws Exception
    {
        if (created) {
            setup.rest().delete(HOME_REFERENCE);
        }
    }
}

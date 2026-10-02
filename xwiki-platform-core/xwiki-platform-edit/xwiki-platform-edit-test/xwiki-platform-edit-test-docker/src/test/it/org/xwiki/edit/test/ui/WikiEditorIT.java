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
package org.xwiki.edit.test.ui;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the editors defined in wiki pages through {@code XWiki.EditorClass} objects.
 *
 * @version $Id$
 */
@UITest
class WikiEditorIT
{
    private static final String EDITOR_CLASS = "XWiki.EditorClass";

    private static final String USER_NAME = "WikiEditorUser";

    // The password of a test user that only exists in the test instance.
    @SuppressWarnings("java:S2068")
    private static final String USER_PASSWORD = "pa$$word";

    private static final String EDITOR_MISSING = "EDITOR_MISSING";

    /**
     * Editor code that outputs {@code EXECUTED} only when its author is allowed to execute scripts.
     */
    private static final String SCRIPT_CODE = "EDITOR_START {{velocity}}#set ($marker = 'EXEC')${marker}UTED"
        + "{{/velocity}}";

    private static final String EXECUTED = "EXECUTED";

    private static final String EDITOR_START = "EDITOR_START";

    private static final String WIKI = "wiki";

    private static final DocumentReference RENDERER_REFERENCE =
        new DocumentReference("xwiki", "WikiEditorIT", "Renderer");

    @BeforeAll
    void beforeAll(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        testUtils.createUser(USER_NAME, USER_PASSWORD, null);

        // A page that renders the syntax content editor with the hint passed in the request, if it exists.
        testUtils.rest().delete(RENDERER_REFERENCE);
        testUtils.rest().savePage(RENDERER_REFERENCE, """
            {{velocity}}
            #set ($editor = $services.edit.syntaxContent.getEditor($request.hint))
            #if ($editor)
            {{html clean="false"}}$editor.render($NULL, {}){{/html}}
            #else
            EDITOR_MISSING
            #end
            {{/velocity}}""", "");
    }

    private String renderEditor(TestUtils testUtils, String hint)
    {
        testUtils.gotoPage(RENDERER_REFERENCE, "view", "hint=" + hint);
        return new ViewPage().getContent();
    }

    private Object[] editorProperties(String hint, String scope, String code)
    {
        return new Object[] { "roleHint", hint, "scope", scope, "dataType", "org.xwiki.rendering.syntax.SyntaxContent",
            "category", "text", "code", code };
    }

    /**
     * Adds an editor object with the user logged in the browser, which doesn't change the content author.
     */
    private void addEditorObject(TestUtils testUtils, DocumentReference reference, String hint, String scope,
        String code)
    {
        testUtils.addObject(reference, EDITOR_CLASS, editorProperties(hint, scope, code));
    }

    @Test
    @Order(1)
    void registerWikiEditor(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String hint = "wikiEditorITWiki";
        testUtils.loginAsSuperAdmin();
        testUtils.rest().delete(testReference);
        testUtils.rest().savePage(testReference, "Editor", "");

        // A user without wiki admin right adds a wiki-scoped editor: it's not registered.
        testUtils.login(USER_NAME, USER_PASSWORD);
        addEditorObject(testUtils, testReference, hint, WIKI, SCRIPT_CODE);
        assertEquals(EDITOR_MISSING, renderEditor(testUtils, hint));

        // Once saved by an administrator, the editor is registered and executed with the rights of the administrator.
        testUtils.loginAsSuperAdmin();
        testUtils.rest().savePage(testReference, "Editor saved by an administrator", "");
        String content = renderEditor(testUtils, hint);
        assertTrue(content.contains(EDITOR_START + " " + EXECUTED), content);
    }

    @Test
    @Order(2)
    void executeEditorWithTheRightsOfItsAuthor(TestUtils testUtils, TestReference testReference) throws Exception
    {
        String hint = "wikiEditorITUser";
        // The content of the page is written by an administrator.
        testUtils.loginAsSuperAdmin();
        testUtils.rest().delete(testReference);
        testUtils.rest().savePage(testReference, "Content written by an administrator", "");

        // A user without script right adds an editor for themselves, which doesn't change the content author.
        testUtils.login(USER_NAME, USER_PASSWORD);
        addEditorObject(testUtils, testReference, hint, "user", SCRIPT_CODE);

        // The editor is registered for the user but its code is executed with the rights of the user.
        String content = renderEditor(testUtils, hint);
        assertTrue(content.contains(EDITOR_START), content);
        assertFalse(content.contains(EXECUTED), content);
    }
}

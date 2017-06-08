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
package org.xwiki.test.wysiwyg;

import org.junit.Test;
import org.xwiki.test.wysiwyg.framework.AbstractWysiwygTestCase;

import static org.junit.Assert.*;

/**
 * Functional tests for the native JavaScript API exposed by the WYSIWYG editor.
 * 
 * @version $Id$
 */
public class NativeJavaScriptApiTest extends AbstractWysiwygTestCase
{
    /**
     * Functional tests for:
     * <ul>
     * <li>WysiwygEditor#getPlainTextArea()</li>
     * <li>WysiwygEditor#getRichTextArea()</li>
     * </ul>
     * .
     */
    @Test
    public void testTextAreaElementsGetters()
    {
        insertEditor("editor", "displayTabs: true");

        // Test plain text editor.
        assertEquals("xPlainTextEditor", getSelenium().getEval("window.editor.getPlainTextArea().className"));
        assertEquals("textarea", getSelenium().getEval("window.editor.getPlainTextArea().nodeName").toLowerCase());

        // Test rich text editor.
        assertEquals("gwt-RichTextArea", getSelenium().getEval("window.editor.getRichTextArea().className"));
        assertEquals("iframe", getSelenium().getEval("window.editor.getRichTextArea().nodeName").toLowerCase());
    }

    /**
     * Functional test for {@code WysiwygEditor#getSourceText()} when the rich text area is enabled. The rich text area
     * is disable when the source tab is active.
     */
    @Test
    public void testGetSourceTextWhenRichTextAreaIsEnabled()
    {
        insertEditor("editor", "syntax: 'xwiki/2.0'");
        setContent("<em>xwiki</em> is the <strong>best</strong>!<br/>");
        assertEquals("//xwiki// is the **best**!", getSourceText("editor"));
    }

    /**
     * Functional test for {@code WysiwygEditor#getSourceText()} when the rich text area is disabled. The rich text area
     * is disable when the source tab is active.
     */
    @Test
    public void testGetSourceTextWhenRichTextAreaIsDisabled()
    {
        insertEditor("editor", "syntax: 'xwiki/2.0',\ndisplayTabs: true,\ndefaultEditor: 'wysiwyg'");
        setContent("<h1>Veni, <em>vidi</em>, vici<br/></h1>");
        switchToSource();
        assertEquals("= Veni, //vidi//, vici =", getSourceText("editor"));

        // Type something in the plain text area and see if we get it.
        getSourceTextArea().sendKeys("x");
        assertEquals("x= Veni, //vidi//, vici =", getSourceText("editor"));
    }

    /**
     * Functional test for {@code WysiwygEditor#release()}.
     */
    @Test
    public void testRelease()
    {
        switchToSource();
        StringBuffer content = new StringBuffer();
        content.append("{{velocity}}\n");
        content.append("{{html}}\n");
        content.append("#wysiwyg_import(true)\n");
        content.append("<div id=\"wrapper\"></div>\n");
        content.append("<div><button onclick=\"loadEditor();\">Load Editor</button></div>\n");
        content.append("<script type=\"text/javascript\">\n");
        content.append("function loadEditor() {\n");
        content.append("    if (window.editor) {\n");
        content.append("        editor.release();\n");
        content.append("        delete window.editor;\n");
        content.append("    }\n");
        content.append("    Wysiwyg.onModuleLoad(function() {\n");
        content.append("        document.getElementById('wrapper').innerHTML = '<textarea id=\"test\"></textarea>';\n");
        content.append("        editor = new WysiwygEditor({hookId: 'test', syntax: 'xwiki/2.0'});\n");
        content.append("    });\n");
        content.append("}\n");
        content.append("</script>\n");
        content.append("{{/html}}\n");
        content.append("{{/velocity}}");
        setSourceText(content.toString());
        clickEditSaveAndView();

        clickButtonWithText("Load Editor");
        waitForCondition("typeof window.editor == 'object'");
        waitForEditorToLoad();
        focusRichTextArea();

        typeText("x");
        assertEquals("x", getSourceText("editor"));

        // "y" (lower case only) is misinterpreted.
        // See http://jira.openqa.org/browse/SIDE-309
        // See http://jira.openqa.org/browse/SRC-385
        typeText("Y");
        clickButtonWithText("Load Editor");
        waitForCondition("typeof window.editor == 'object'");
        waitForEditorToLoad();
        focusRichTextArea();

        typeText("z");
        applyStyleTitle1();
        assertEquals("= z =", getSourceText("editor"));
    }

    /**
     * @see XWIKI-4067: Trying to edit a missing object property with the new WYSIWYG editor can lead to infinite
     *      include recursion.
     */
    @Test
    public void testEditMissingProperty()
    {
        // Save the current location to be able to get back.
        String location = getSelenium().getLocation();

        // Create a new page and set its content. This page should have a XWiki.TagClass object attached.
        open("Test", "Alice", "edit", "editor=wiki");
        // We have to set the content of the page in order to detect if the value of the missing property is empty.
        setFieldValue("content", "I want ice cream");
        clickEditSaveAndView();

        // Go back to the previous location.
        open(location);
        waitForEditorToLoad();
        switchToSource();
        // Load the WYSIWYG editor for a property that doesn't exist.
        StringBuffer content = new StringBuffer();
        content.append("{{velocity}}\n");
        content.append("{{html}}\n");
        // xyz is not a property of the XWiki.TagClass.
        content.append("<textarea id=\"XWiki.TagClass_0_xyz\"></textarea>\n");
        content.append("#wysiwyg_editProperty($xwiki.getDocument('Test.Alice') 'XWiki.TagClass_0_xyz' false)\n");
        content.append("<script type=\"text/javascript\">\n");
        content.append("document.observe('xwiki:wysiwyg:created', function(event) {\n");
        content.append("    window.editor = event.memo.instance;\n");
        content.append("});\n");
        content.append("</script>\n");
        content.append("{{/html}}\n");
        content.append("{{/velocity}}");
        setSourceText(content.toString());
        clickEditSaveAndView();

        waitForCondition("typeof window.editor == 'object'");
        waitForEditorToLoad();
        // Check the WYSIWYG editor input value.
        assertEquals("", getSourceText("editor"));
    }

    /**
     * @see XWIKI-4519: Add the ability to execute commands on the rich text area from JavaScript.
     */
    @Test
    public void testCommandManagerApi()
    {
        insertEditor("editor", "syntax: 'xwiki/2.0'");
        focusRichTextArea();
        typeText("x");
        selectNodeContents("document.body.firstChild");
        assertTrue(Boolean.valueOf(getSelenium().getEval("window.editor.getCommandManager().isSupported('bold')")));
        assertTrue(Boolean.valueOf(getSelenium().getEval("window.editor.getCommandManager().isEnabled('bold')")));
        assertFalse(Boolean.valueOf(getSelenium().getEval("window.editor.getCommandManager().isExecuted('bold')")));
        assertTrue(Boolean.valueOf(getSelenium().getEval("window.editor.getCommandManager().execute('bold')")));
        assertTrue(Boolean.valueOf(getSelenium().getEval("window.editor.getCommandManager().isExecuted('bold')")));
        assertEquals("**x**", getSourceText("editor"));
    }

    /**
     * Tests the WYSIWYG events are fired properly.
     */
    @Test
    public void testEvents()
    {
        // Insert the code that creates the editor.
        switchToSource();
        StringBuffer content = new StringBuffer();
        content.append("{{velocity}}\n");
        content.append("{{html}}\n");
        content.append("#wysiwyg_import(false)\n");
        content.append("<div id=\"log\"></div>\n");
        content.append("<textarea id=\"source\"></textarea>\n");
        content.append("<script type=\"text/javascript\">\n");
        content.append("['created', 'loaded', 'showingSource', 'showSource',"
            + " 'showingWysiwyg', 'showWysiwyg'].each(function(actionName) {\n");
        content.append("  document.observe('xwiki:wysiwyg:' + actionName, function(event) {\n");
        content.append("    if (!window.editor || event.memo.instance == window.editor) {\n");
        content.append("      $('log').appendChild(document.createTextNode(event.eventName + ' '));\n");
        content.append("    }\n");
        content.append("  });\n");
        content.append("});\n");
        content.append("document.observe('xwiki:dom:loaded', function() {\n");
        content.append("  Wysiwyg.onModuleLoad(function() {\n");
        content.append("    editor = new WysiwygEditor({\n");
        content.append("      hookId: 'source',\n");
        content.append("      syntax: 'xwiki/2.0',\n");
        String inputURL =
            getUrl(this.getClass().getSimpleName(), "_" + getTestMethodName(), "edit", "xpage=wysiwyginput");
        content.append("      inputURL: '" + inputURL + "',\n");
        content.append("      displayTabs: true,\n");
        content.append("      defaultEditor: 'wysiwyg'\n");
        content.append("    });\n");
        content.append("  });\n");
        content.append("});\n");
        content.append("</script>\n");
        content.append("{{/html}}\n");
        content.append("{{/velocity}}");
        setSourceText(content.toString());
        clickEditSaveAndView();

        // Wait for the editor to be created.
        waitForCondition("typeof window.editor == 'object'");
        waitForEditorToLoad();
        focusRichTextArea();

        // Switch to source tab and back.
        typeText("1");
        switchToSource();
        setSourceText("2");
        switchToWysiwyg();

        // Check the log.
        assertEquals("xwiki:wysiwyg:created xwiki:wysiwyg:loaded xwiki:wysiwyg:showWysiwyg xwiki:wysiwyg:showingSource"
            + " xwiki:wysiwyg:showSource xwiki:wysiwyg:showingWysiwyg xwiki:wysiwyg:showWysiwyg ", getSelenium()
            .getEval("window.document.getElementById('log').innerHTML"));
    }

    /**
     * Tests if the configuration parameters for a WYSIWYG editor instance are accessible through the JavaScript API.
     */
    @Test
    public void testAccessConfigurationParameters()
    {
        String hookId = "XWiki.ArticleClass_0_description";
        insertEditor(hookId, "syntax: 'xwiki/2.0',\nxyz: 'abc'");
        assertEquals("abc", getSelenium().getEval("window['" + hookId + "'].getParameter('xyz')"));
        assertEquals("3", getSelenium().getEval("window['" + hookId + "'].getParameterNames().length"));
    }

    /**
     * Tests that we can get a reference to a WYSIWYG editor instance by knowing its hookId.
     */
    @Test
    public void testAccessWysiwygEditorInstanceByHookId()
    {
        String hookId = "XWiki.ArticleClass_0_description";
        insertEditor(hookId, "syntax: 'xwiki/2.0'");
        assertEquals(hookId,
            getSelenium().getEval("window.Wysiwyg.getInstance('" + hookId + "').getParameter('hookId')"));
    }

    /**
     * Inserts a WYSIWYG editor into the current page.
     * 
     * @param name the name of JavaScript variable to be used for accessing the editor
     * @param config additional configuration parameters to give to the created editor
     */
    protected void insertEditor(String name, String config)
    {
        // Insert the code that creates the editor.
        switchToSource();
        StringBuilder content = new StringBuilder();
        content.append("{{velocity}}\n");
        content.append("{{html}}\n");
        content.append("#wysiwyg_import(false)\n");
        content.append("<textarea id=\"" + name + "\"></textarea>\n");
        content.append("<script type=\"text/javascript\">\n");
        content.append("document.observe('xwiki:dom:loaded', function() {\n");
        content.append("  Wysiwyg.onModuleLoad(function() {\n");
        content.append("    window['" + name + "'] = new WysiwygEditor({\n");
        content.append("      hookId: '" + name + "',\n");
        content.append("      " + config + "\n");
        content.append("    });\n");
        content.append("  });\n");
        content.append("});\n");
        content.append("</script>\n");
        content.append("{{/html}}\n");
        content.append("{{/velocity}}");
        setSourceText(content.toString());
        clickEditSaveAndView();

        // Wait for the editor to be created.
        waitForCondition("typeof window['" + name + "'] == 'object'");
        waitForEditorToLoad();
    }

    /**
     * @param editorName the name of a JavaScript variable holding a reference to a WysiwygEditor instance
     * @return the source text of the specified editor
     */
    protected String getSourceText(String editorName)
    {
        getSelenium().getEval(
            "delete window.sourceText;\nwindow['" + editorName
                + "'].getSourceText(function(result){window.sourceText = result;})");
        waitForCondition("typeof window.sourceText == 'string'");
        return getSelenium().getEval("window.sourceText");
    }
}

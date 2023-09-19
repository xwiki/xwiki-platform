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
package org.xwiki.web;

import java.util.Map;
import java.util.Optional;

import javax.script.ScriptContext;
import javax.servlet.http.HttpSession;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.validation.edit.EditConfirmationChecker;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.model.validation.edit.EditConfirmationScriptService;
import org.xwiki.model.validation.internal.ReplaceCharacterEntityNameValidationConfiguration;
import org.xwiki.model.validation.script.ModelValidationScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test the {@code edit_macros.vm} template.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@ComponentList({
    TemplateMacro.class,
    EditConfirmationScriptService.class,
    ModelValidationScriptService.class,
    // Start - Required in addition of RenderingScriptServiceComponentList
    DefaultExtendedRenderingConfiguration.class,
    RenderingConfigClassDocumentConfigurationSource.class,
    // End - Required in additional of RenderingScriptServiceComponentList
})
class EditMacrosPageTest extends PageTest
{
    @MockComponent
    private ReplaceCharacterEntityNameValidationConfiguration replaceCharacterEntityNameValidationConfiguration;

    @Mock
    private HttpSession httpSession;

    @BeforeEach
    void setUp()
    {
        this.request.setSession(this.httpSession);
    }

    @Test
    void allChecksPassing() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        document.setContent("{{template name=\"edit_macros.vm\" output='false'/}}\n"
            + "{{velocity}}#getEditConfirmation(){{/velocity}}"
        );
        document.setSyntax(Syntax.XWIKI_2_1);
        this.context.setDoc(document);
        this.componentManager.registerComponent(EditConfirmationChecker.class, "testChecker",
            (EditConfirmationChecker) Optional::empty);

        document.getRenderedContent(this.context);
        ScriptContext scriptContext =
            this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class).getScriptContext();
        assertNull(scriptContext.getAttribute("editConfirmation"));
    }

    @Test
    void oneWarningCheck() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        document.setContent("{{template name=\"edit_macros.vm\" output='false'/}}\n"
            + "{{velocity}}#getEditConfirmation(){{/velocity}}"
        );
        document.setSyntax(Syntax.XWIKI_2_1);
        this.context.setDoc(document);
        this.componentManager.registerComponent(EditConfirmationChecker.class, "testChecker",
            (EditConfirmationChecker) () -> Optional.of(
                new EditConfirmationCheckerResult(new WordBlock("Warning"), false)));
        document.getRenderedContent(this.context);
        ScriptContext scriptContext =
            this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class).getScriptContext();
        Map<String, String> editConfirmation = (Map<String, String>) scriptContext.getAttribute("editConfirmation");
        assertEquals("warning", editConfirmation.get("title"));
        Document message = Jsoup.parse(editConfirmation.get("message"));
        assertEquals("platform.core.editConfirmation.warnings", message.selectFirst("p").text());
        assertEquals("Warning", message.selectFirst(".box").text());
        assertTrue(message.selectFirst(".box").hasClass("warningmessage"));
        assertEquals("cancel", editConfirmation.get("reject"));
        assertEquals("forcelock", editConfirmation.get("confirm"));
    }

    @Test
    void twoWarningsTwoErrorsCheck() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        document.setContent("{{template name=\"edit_macros.vm\" output='false'/}}\n"
            + "{{velocity}}#getEditConfirmation(){{/velocity}}"
        );
        document.setSyntax(Syntax.XWIKI_2_1);
        this.context.setDoc(document);
        this.componentManager.registerComponent(EditConfirmationChecker.class, "warningChecker1",
            (EditConfirmationChecker) () -> Optional.of(
                new EditConfirmationCheckerResult(new WordBlock("Warning 1"), false)));
        this.componentManager.registerComponent(EditConfirmationChecker.class, "warningChecker2",
            (EditConfirmationChecker) () -> Optional.of(
                new EditConfirmationCheckerResult(new WordBlock("Warning 2"), false)));
        this.componentManager.registerComponent(EditConfirmationChecker.class, "errorChecker1",
            (EditConfirmationChecker) () -> Optional.of(
                new EditConfirmationCheckerResult(new WordBlock("Error 1"), true)));
        this.componentManager.registerComponent(EditConfirmationChecker.class, "errorChecker2",
            (EditConfirmationChecker) () -> Optional.of(
                new EditConfirmationCheckerResult(new WordBlock("Error 2"), true)));
        document.getRenderedContent(this.context);
        ScriptContext scriptContext =
            this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class).getScriptContext();
        Map<String, String> editConfirmation = (Map<String, String>) scriptContext.getAttribute("editConfirmation");
        assertEquals("error", editConfirmation.get("title"));
        Document message = Jsoup.parse(editConfirmation.get("message"));
        assertEquals("platform.core.editConfirmation.errors", message.selectFirst("p").text());
        assertEquals("Error 1 Error 2", message.selectFirst(".box.errormessage").text());
        assertEquals("Warning 1 Warning 2", message.selectFirst(".box.warningmessage").text());
        assertTrue(message.selectFirst(".box").hasClass("errormessage"));
        assertEquals("cancel", editConfirmation.get("reject"));
        assertNull(editConfirmation.get("confirm"));
    }

    @Test
    void oneWarningEditForced() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        document.setContent("{{template name=\"edit_macros.vm\" output='false'/}}\n"
            + "{{velocity}}#getEditConfirmation(){{/velocity}}"
        );
        document.setSyntax(Syntax.XWIKI_2_1);
        this.context.setDoc(document);
        this.componentManager.registerComponent(EditConfirmationChecker.class, "warningChecker1",
            (EditConfirmationChecker) () -> Optional.of(
                new EditConfirmationCheckerResult(new WordBlock("Warning 1"), false)));
        this.request.put("force", "true");

        document.getRenderedContent(this.context);
        ScriptContext scriptContext =
            this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class).getScriptContext();
        assertNull(scriptContext.getAttribute("editConfirmation"));
    }

    @Test
    void oneErrorEditForced() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Page"), this.context);
        document.setContent("{{template name=\"edit_macros.vm\" output='false'/}}\n"
            + "{{velocity}}#getEditConfirmation(){{/velocity}}"
        );
        document.setSyntax(Syntax.XWIKI_2_1);
        this.context.setDoc(document);
        this.componentManager.registerComponent(EditConfirmationChecker.class, "errorChecker",
            (EditConfirmationChecker) () -> Optional.of(
                new EditConfirmationCheckerResult(new WordBlock("Error"), true)));
        this.request.put("force", "true");

        document.getRenderedContent(this.context);
        ScriptContext scriptContext =
            this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class).getScriptContext();
        Map<String, String> editConfirmation = (Map<String, String>) scriptContext.getAttribute("editConfirmation");
        assertEquals("error", editConfirmation.get("title"));
        Document message = Jsoup.parse(editConfirmation.get("message"));
        assertEquals("platform.core.editConfirmation.errors", message.selectFirst("p").text());
        assertEquals("Error", message.selectFirst(".box").text());
        assertTrue(message.selectFirst(".box").hasClass("errormessage"));
        assertEquals("cancel", editConfirmation.get("reject"));
        assertNull(editConfirmation.get("confirm"));
    }
}

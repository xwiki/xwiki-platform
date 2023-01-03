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
package org.xwiki.xclass;

import java.util.HashMap;

import javax.script.ScriptContext;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.internal.macro.LiveDataMacroComponentList;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.rendering.internal.macro.message.InfoMessageMacro;
import org.xwiki.rendering.internal.macro.message.WarningMessageMacro;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.internal.macro.TemplateMacro;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax20ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.tools.EscapeTool;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;
import com.xpn.xwiki.script.sheet.SheetScriptService;

import static javax.script.ScriptContext.GLOBAL_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page Test of {@code XWiki.ClassSheet}.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.3
 * @since 14.4.8
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@XWikiSyntax20ComponentList
@LiveDataMacroComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@ComponentList({
    TemplateMacro.class,
    TranslationMacro.class,
    WarningMessageMacro.class,
    InfoMessageMacro.class,
    TemplateScriptService.class,
    TestNoScriptMacro.class,
    ModelScriptService.class,
    SheetScriptService.class
})
class ClassSheetPageTest extends PageTest
{
    private static final DocumentReference CLASS_SHEET_BINDING_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "ClassSheetBinding");

    private static final DocumentReference XWIKI_CLASS_SHEET = new DocumentReference("xwiki", "XWiki", "ClassSheet");

    private ScriptContext scriptContext;

    @BeforeEach
    void setUp() throws Exception
    {
        // Spy the jsfx plugin used during the macro rendering to return a mock of its API when required. 
        when(this.oldcore.getSpyXWiki().getPluginApi("jsfx", this.context))
            .thenReturn(mock(SkinExtensionPluginApi.class));

        // Return minimal icons metadata since this is not what we want to test in this test suite.
        IconManager iconManager = this.componentManager.registerMockComponent(IconManager.class);
        doReturn(new HashMap<>()).when(iconManager).getMetaData(anyString());

        initializeClassSheetBinding();

        this.scriptContext = this.oldcore.getMocker().<ScriptContextManager>getInstance(ScriptContextManager.class)
            .getCurrentScriptContext();
    }

    @Test
    void notASheetNoProperties() throws Exception
    {
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "/}}{{/html}}{{noscript/}}"), this.context);
        XWikiDocument doc = loadPage(XWIKI_CLASS_SHEET);

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);

        Document document = render(doc);

        Elements forms = document.select("form");

        Element warningMessage = document.selectFirst(".warningmessage");
        assertEquals("platform.xclass.defaultClassSheet.properties.empty [, ]", warningMessage.text());
        assertEquals("/xwiki/bin/edit/Space/%2F%7D%7D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D?editor=class",
            warningMessage.selectFirst("a").attr("href"));
        Element classSheetForm = forms.get(0);
        assertEquals("Space./}}{{/html}}{{noscript/}}", classSheetForm.selectFirst("[name='parent']").val());
        assertEquals("/xwiki/bin/view/Space/%2F%7D%7D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D",
            classSheetForm.selectFirst("[name='xredirect']").val());
        assertEquals("#if($doc.documentReference.name == '/}}{{/html}}{{noscript/}}Sheet')/}}{{/html}}"
            + "{{noscript/}} Sheet#{else}$services.display.title($doc, {'displayerHint': 'default', "
            + "'outputSyntaxId': 'plain/1.0'})#end", classSheetForm.selectFirst("[name='title']").val());
        Element classTemplateForm = forms.get(1);
        assertEquals("Space./}}{{/html}}{{noscript/}}", classTemplateForm.selectFirst("[name='parent']").val());
        assertEquals("/xwiki/bin/view/Space/%2F%7D%7D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D",
            classTemplateForm.selectFirst("[name='xredirect']").val());
        assertEquals("/}}{{/html}}{{noscript/}} Template", classTemplateForm.selectFirst("[name='title']").val());
        Elements infomessages = document.select(".infomessage");
        Element infomessage1 = infomessages.get(0);
        assertEquals("platform.xclass.defaultClassSheet.sheets.description [, ]", infomessage1.text());
        assertNotNull(infomessage1.selectFirst("em"));
        Element infomessage2 = infomessages.get(1);
        assertEquals("platform.xclass.defaultClassSheet.template.description [, ]", infomessage2.text());
        assertNotNull(infomessage2.selectFirst("em"));
    }

    @Test
    void notASheetHasProperties() throws Exception
    {
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "/}}{{/html}}{{noscript/}}"), this.context);
        xwikiDocument.getXClass().addTextField("testField", "Test Field", 10);
        XWikiDocument doc = loadPage(XWIKI_CLASS_SHEET);

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);

        Document document = render(doc);
        Elements forms = document.select("form");

        Element ul = document.selectFirst("ul");
        Elements lis = ul.select("li");
        assertEquals("Test Field (testField: String)", lis.get(0).text());
        assertEquals("platform.xclass.defaultClassSheet.properties.edit [, ]", lis.get(1).text());
        assertEquals("/xwiki/bin/edit/Space/%2F%7D%7D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D?editor=class",
            lis.get(1).selectFirst("a").attr("href"));
        Element classSheetForm = forms.get(0);
        assertEquals("Space./}}{{/html}}{{noscript/}}", classSheetForm.selectFirst("[name='parent']").val());
        assertEquals("/xwiki/bin/view/Space/%2F%7D%7D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D",
            classSheetForm.selectFirst("[name='xredirect']").val());
        assertEquals("#if($doc.documentReference.name == '/}}{{/html}}{{noscript/}}Sheet')/}}{{/html}}"
            + "{{noscript/}} Sheet#{else}$services.display.title($doc, {'displayerHint': 'default', "
            + "'outputSyntaxId': 'plain/1.0'})#end", classSheetForm.selectFirst("[name='title']").val());
        Element classTemplateForm = forms.get(1);
        assertEquals("Space./}}{{/html}}{{noscript/}}", classTemplateForm.selectFirst("[name='parent']").val());
        assertEquals("/xwiki/bin/view/Space/%2F%7D%7D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D",
            classTemplateForm.selectFirst("[name='xredirect']").val());
        assertEquals("/}}{{/html}}{{noscript/}} Template", classTemplateForm.selectFirst("[name='title']").val());
    }

    @Test
    void notASheetHasSheetAlreadyExists() throws Exception
    {
        String alreadyExistsPageName = "AlreadyExists/}}{{noscript/}}";
        XWikiDocument alreadyExistsDoc =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", alreadyExistsPageName), this.context);
        this.xwiki.saveDocument(alreadyExistsDoc, this.context);

        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "/}}{{/html}}{{noscript/}}"), this.context);
        xwikiDocument.getXClass().addTextField("testField", "Test Field", 10);
        BaseObject baseObject = xwikiDocument.newXObject(CLASS_SHEET_BINDING_DOCUMENT_REFERENCE, this.context);
        baseObject.set("sheet", alreadyExistsPageName, this.context);
        this.xwiki.saveDocument(xwikiDocument, this.context);
        XWikiDocument doc = loadPage(XWIKI_CLASS_SHEET);

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);

        Document document = render(doc);

        Elements h1s = document.select("h1");
        assertEquals("platform.xclass.defaultClassSheet.properties.heading", h1s.get(0).text());
        assertEquals("platform.xclass.defaultClassSheet.pages.heading", h1s.get(1).text());

        Element ul = document.selectFirst("ul");
        Elements lis = ul.select("li");
        assertEquals("Test Field (testField: String)", lis.get(0).text());
        assertEquals("platform.xclass.defaultClassSheet.properties.edit [, ]", lis.get(1).text());
        assertEquals("/xwiki/bin/edit/Space/%2F%7D%7D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D?editor=class",
            lis.get(1).selectFirst("a").attr("href"));
        Element classSheetForm = document.selectFirst("form");
        assertEquals("Space./}}{{/html}}{{noscript/}}", classSheetForm.selectFirst("[name='parent']").val());
        assertEquals("/xwiki/bin/view/Space/%2F%7D%7D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D",
            classSheetForm.selectFirst("[name='xredirect']").val());
        assertEquals("/}}{{/html}}{{noscript/}} Template", classSheetForm.selectFirst("[name='title']").val());

        assertEquals(String.format("platform.xclass.defaultClassSheet.sheets.view [Space / %s] »",
                new EscapeTool().xml(alreadyExistsPageName)),
            document.selectFirst(String.format("[href='Space.%s']", alreadyExistsPageName)).text());
    }

    @Test
    void notASheetHasSheetAlreadyExistsClassTemplateExists() throws Exception
    {
        String alreadyExistsPageName = "AlreadyExists/}}{{noscript/}}";
        String pageName = "/}}{{/html}}{{noscript/}}";
        XWikiDocument alreadyExistsDoc =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", alreadyExistsPageName), this.context);
        this.xwiki.saveDocument(alreadyExistsDoc, this.context);

        XWikiDocument alreadyExistsDocTemplate =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", pageName + "Template"), this.context);
        this.xwiki.saveDocument(alreadyExistsDocTemplate, this.context);

        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", pageName), this.context);
        xwikiDocument.getXClass().addTextField("testField", "Test Field", 10);
        BaseObject baseObject = xwikiDocument.newXObject(CLASS_SHEET_BINDING_DOCUMENT_REFERENCE, this.context);
        baseObject.set("sheet", alreadyExistsPageName, this.context);
        this.xwiki.saveDocument(xwikiDocument, this.context);
        XWikiDocument doc = loadPage(XWIKI_CLASS_SHEET);

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);

        Document document = render(doc);

        Elements h1s = document.select("h1");
        assertEquals("platform.xclass.defaultClassSheet.properties.heading", h1s.get(0).text());
        assertEquals("platform.xclass.defaultClassSheet.createPage.heading", h1s.get(1).text());

        Element ul = document.selectFirst("ul");
        Elements lis = ul.select("li");
        assertEquals("Test Field (testField: String)", lis.get(0).text());
        assertEquals(String.format("%s [, ]", "platform.xclass.defaultClassSheet.properties.edit"), lis.get(1).text());
        assertEquals("/xwiki/bin/edit/Space/%2F%7D%7D%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D?editor=class",
            lis.get(1).selectFirst("a").attr("href"));
        Element classSheetForm = document.selectFirst("form");
        assertEquals("Space./}}{{/html}}{{noscript/}}", classSheetForm.selectFirst("[name='parent']").val());
        assertEquals("Space./}}{{/html}}{{noscript/}}Template", classSheetForm.selectFirst("[name='template']").val());
        assertEquals("1", classSheetForm.selectFirst("[name='sheet']").val());

        assertEquals(String.format("platform.xclass.defaultClassSheet.sheets.view [Space / %s] »",
                new EscapeTool().xml(alreadyExistsPageName)),
            document.selectFirst(String.format("[href='Space.%s']", alreadyExistsPageName)).text());
    }

    @Test
    void hasClassSheetsAndHasClassTemplate() throws Exception
    {
        String pageName = "Page";
        String sheetPage = "MySheet";
        DocumentReference mainPageReference = new DocumentReference("xwiki", "Space", pageName);
        XWikiDocument xwikiDocument = this.xwiki.getDocument(mainPageReference, this.context);
        xwikiDocument.getXClass().addTextField("testField", "Test Field", 10);
        BaseObject classSheetBindingObject =
            xwikiDocument.newXObject(CLASS_SHEET_BINDING_DOCUMENT_REFERENCE, this.context);
        classSheetBindingObject.set("sheet", sheetPage, this.context);

        this.xwiki.saveDocument(xwikiDocument, this.context);
        XWikiDocument doc = loadPage(XWIKI_CLASS_SHEET);

        // Class Template Creation.
        XWikiDocument templateProvider = this.xwiki.getDocument(
            new DocumentReference(pageName + "Template", mainPageReference.getLastSpaceReference()),
            this.context);
        this.xwiki.saveDocument(templateProvider, this.context);

        // Class docRef
        XWikiDocument docRef = this.xwiki.getDocument(
            new DocumentReference("DOC_NAME", mainPageReference.getLastSpaceReference()),
            this.context);
        this.xwiki.saveDocument(docRef, this.context);

        this.request.put("docName", "DOC_NAME");

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);

        Document document = render(doc);

        Elements warnings = document.select(".warningmessage");
        Element warning1 = warnings.get(0);
        assertEquals("platform.xclass.defaultClassSheet.createPage.pageAlreadyExists [, ]", warning1.text());
        assertEquals("/xwiki/bin/view/Space/DOC_NAME", warning1.selectFirst("a").attr("href"));
        Element warning2 = warnings.get(1);
        assertEquals("platform.xclass.defaultClassSheet.template.missingObject [Page] "
            + "platform.xclass.defaultClassSheet.template.addObject [Page] ».", warning2.text());
        assertEquals("/xwiki/bin/objectadd/Space/PageTemplate"
                + "?classname=Space.Page&xredirect=%2Fxwiki%2Fbin%2FMain%2FWebHome&form_token=",
            warning2.selectFirst("a").attr("href"));
    }

    @Test
    void hasEditAndDefaultClassSheetReference() throws Exception
    {
        this.scriptContext.setAttribute("hasEdit", true, GLOBAL_SCOPE);

        String pageName = "Page";
        DocumentReference mainPageReference = new DocumentReference("xwiki", "Space", pageName);
        XWikiDocument xwikiDocument = this.xwiki.getDocument(mainPageReference, this.context);
        xwikiDocument.getXClass().addTextField("testField", "Test Field", 10);
        this.xwiki.saveDocument(xwikiDocument, this.context);

        // Create defaultClassSheetReference
        XWikiDocument defaultClassSheet =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "PageSheet"), this.context);
        this.xwiki.saveDocument(defaultClassSheet, this.context);

        XWikiDocument doc = loadPage(XWIKI_CLASS_SHEET);

        this.request.put("docName", "DOC_NAME");

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);

        Document document = render(doc);

        Element warning = document.selectFirst(".warningmessage");
        assertEquals("platform.xclass.defaultClassSheet.sheets.notBound "
            + "platform.xclass.defaultClassSheet.sheets.bind ».", warning.text());
        assertEquals("/xwiki/bin/view/Space/Page?bindSheet=xwiki%3ASpace.PageSheet"
            + "&xredirect=%2Fxwiki%2Fbin%2FMain%2FWebHome&form_token=", warning.selectFirst("a").attr("href"));
    }

    @Test
    void hasEditAndDefaultClassSheetReferenceTemplateHasDoc() throws Exception
    {
        this.scriptContext.setAttribute("hasEdit", true, GLOBAL_SCOPE);

        String pageName = "Page";
        DocumentReference mainPageReference = new DocumentReference("xwiki", "Space", pageName);
        XWikiDocument xwikiDocument = this.xwiki.getDocument(mainPageReference, this.context);
        xwikiDocument.getXClass().addTextField("testField", "Test Field", 10);
        this.xwiki.saveDocument(xwikiDocument, this.context);

        // Create defaultClassSheetReference
        XWikiDocument defaultClassSheet =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "PageSheet"), this.context);
        this.xwiki.saveDocument(defaultClassSheet, this.context);

        XWikiDocument pageTemplate =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "PageTemplate"), this.context);
        pageTemplate.newXObject(mainPageReference, this.context);
        this.xwiki.saveDocument(pageTemplate, this.context);

        XWikiDocument doc = loadPage(XWIKI_CLASS_SHEET);

        this.request.put("docName", "DOC_NAME");

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);

        Document document = render(doc);

        Elements infomessages = document.select(".infomessage");
        assertEquals("platform.xclass.defaultClassSheet.sheets.description [, ]", infomessages.get(0).text());
        assertEquals("platform.xclass.defaultClassSheet.template.description [, ]", infomessages.get(1).text());
        assertEquals("platform.xclass.defaultClassSheet.templateProvider.description []", infomessages.get(2).text());
        assertNotNull(infomessages.get(2).selectFirst("em"));
    }

    @Test
    void hasEditAndDefaultClassSheetReferenceTemplateHasDocHasClassTemplateProvider() throws Exception
    {
        this.scriptContext.setAttribute("hasEdit", true, GLOBAL_SCOPE);

        DocumentReference mainPageReference = new DocumentReference("xwiki", "Space", "Page");
        XWikiDocument xwikiDocument = this.xwiki.getDocument(mainPageReference, this.context);
        xwikiDocument.getXClass().addTextField("testField", "Test Field", 10);
        this.xwiki.saveDocument(xwikiDocument, this.context);

        // Create defaultClassSheetReference
        XWikiDocument defaultClassSheet =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "PageSheet"), this.context);
        this.xwiki.saveDocument(defaultClassSheet, this.context);

        XWikiDocument pageTemplate =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "PageTemplate"), this.context);
        pageTemplate.newXObject(mainPageReference, this.context);
        this.xwiki.saveDocument(pageTemplate, this.context);

        XWikiDocument pageTemplateProvider =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "PageTemplateProvider"), this.context);
        this.xwiki.saveDocument(pageTemplateProvider, this.context);

        XWikiDocument doc = loadPage(XWIKI_CLASS_SHEET);

        this.request.put("docName", "DOC_NAME");

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);

        Document document = render(doc);

        Elements infomessages = document.select(".infomessage");
        assertEquals("platform.xclass.defaultClassSheet.sheets.description [, ]", infomessages.get(0).text());
        assertEquals("platform.xclass.defaultClassSheet.template.description [, ]", infomessages.get(1).text());
        assertEquals("platform.xclass.defaultClassSheet.templateProvider.description []", infomessages.get(2).text());
        assertNotNull(infomessages.get(2).selectFirst("em"));
        Elements links = document.select("a");
        Element lastLink = links.get(links.size() - 1);
        assertEquals("platform.xclass.defaultClassSheet.templateProvider.view [Space / PageTemplateProvider] »",
            lastLink.text());
        assertEquals("Space.PageTemplateProvider", lastLink.attr("href"));
    }

    private Document render(XWikiDocument doc) throws XWikiException
    {
        return Jsoup.parse(doc.getRenderedContent(this.context));
    }

    /**
     * TODO: Create the class because it is required but we currently do not support loading documents from other
     * modules. An option would be to move ClassSheetBinding to a mandatory document initializer.
     */
    private void initializeClassSheetBinding() throws Exception
    {
        XWikiDocument document = this.xwiki.getDocument(CLASS_SHEET_BINDING_DOCUMENT_REFERENCE, this.context);

        if (!document.isNew()) {
            return;
        }

        BaseClass xClass = document.getXClass();
        xClass.addPageField("sheet", "Sheet", 30, false, false, "");

        this.xwiki.saveDocument(document, this.context);
    }
}

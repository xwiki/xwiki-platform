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
package org.xwiki.skin;

import java.util.Collections;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.edit.EditConfiguration;
import org.xwiki.edit.internal.DefaultEditorDescriptorBuilder;
import org.xwiki.edit.internal.DefaultEditorManager;
import org.xwiki.edit.internal.PureTextSyntaxContentEditor;
import org.xwiki.icon.IconManagerScriptService;
import org.xwiki.localization.macro.internal.TranslationMacro;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultExtendedRenderingConfiguration;
import org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource;
import org.xwiki.rendering.internal.macro.message.WarningMessageMacro;
import org.xwiki.rendering.internal.parser.reference.type.PathResourceReferenceTypeParser;
import org.xwiki.rendering.internal.renderer.xhtml.link.DocumentXHTMLLinkTypeRenderer;
import org.xwiki.rendering.internal.renderer.xhtml.link.PathXHTMLLinkTypeRenderer;
import org.xwiki.rendering.internal.resolver.DefaultResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.internal.resolver.DocumentResourceReferenceEntityReferenceResolver;
import org.xwiki.rendering.internal.wiki.XWikiWikiModel;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.script.service.ScriptService;
import org.xwiki.template.script.TemplateScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.DefaultSkinAccessBridge;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiSkinFileOverrideClassDocumentInitializer;
import com.xpn.xwiki.internal.mandatory.XWikiSkinsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Page test for SkinsCode.XWikiSkinsSheet.
 *
 * @version $Id$
 */
@HTML50ComponentList
@XWikiSyntax21ComponentList
@RenderingScriptServiceComponentList
@ReferenceComponentList
@ComponentList({
    // Class initializers
    XWikiSkinsDocumentInitializer.class,
    XWikiSkinFileOverrideClassDocumentInitializer.class,
    // Test macro
    TestNoScriptMacro.class,
    // Various classes to prevent errors in the displayed document
    ModelScriptService.class,
    TranslationMacro.class,
    WarningMessageMacro.class,
    DefaultExtendedRenderingConfiguration.class,
    RenderingConfigClassDocumentConfigurationSource.class,
    PathXHTMLLinkTypeRenderer.class,
    DocumentXHTMLLinkTypeRenderer.class,
    PathResourceReferenceTypeParser.class,
    // Provide a WikiModel implementation to get document links to work.
    XWikiWikiModel.class,
    ContextComponentManagerProvider.class,
    DefaultSkinAccessBridge.class,
    DefaultResourceReferenceEntityReferenceResolver.class,
    DocumentResourceReferenceEntityReferenceResolver.class,
    // For displaying text area editors
    PureTextSyntaxContentEditor.class,
    DefaultEditorDescriptorBuilder.class,
    DefaultEditorManager.class,
    TemplateScriptService.class
})
class XWikiSkinsSheetPageTest extends PageTest
{
    private static final DocumentReference XWIKI_SKINS_SHEET = new DocumentReference("xwiki", "SkinsCode",
        "XWikiSkinsSheet");

    private static final DocumentReference XWIKI_SKINS = new DocumentReference("xwiki", "XWiki", "XWikiSkins");

    private static final DocumentReference XWIKI_SKIN_FILE_OVERRIDE = new DocumentReference("xwiki", "XWiki",
        "XWikiSkinFileOverrideClass");

    private static final String NASTY_INPUT = "\"]]<img onerror=\"alert(1)\"/>{{/html}}{{noscript/}}";

    /**
     * Mock the editor configuration.
     */
    @MockComponent
    private EditConfiguration editConfiguration;

    /**
     * Mock attachment selector macro to avoid rendering errors.
     */
    @Mock
    private Macro<Object> attachmentSelectorMacro;

    @BeforeEach
    void setup() throws Exception
    {
        this.xwiki.initializeMandatoryDocuments(this.context);

        // Register fake attachment selector.
        when(this.attachmentSelectorMacro.supportsInlineMode()).thenReturn(true);
        when(this.attachmentSelectorMacro.execute(any(), any(), any())).thenReturn(Collections.emptyList());
        when(this.attachmentSelectorMacro.getDescriptor()).thenReturn(
            new DefaultMacroDescriptor(new MacroId("attachmentSelector"), "Attachment Selector Macro"));
        this.oldcore.getMocker().registerComponent(Macro.class, "attachmentSelector", this.attachmentSelectorMacro);

        // Make sure icons can be rendered.
        IconManagerScriptService iconManagerScriptService = this.oldcore.getMocker()
            .registerMockComponent(ScriptService.class, "icon", IconManagerScriptService.class, true);
        when(iconManagerScriptService.renderHTML(anyString()))
            .then(call -> "HTML_ICON:" + call.getArgument(0, String.class));
        when(iconManagerScriptService.render(anyString()))
            .then(call -> "ICON:" + call.getArgument(0, String.class));
    }

    @Test
    void escapesDocumentReference() throws Exception
    {
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", NASTY_INPUT), this.context);
        String encodedName =
            "%22%5D%5D%3Cimg%20onerror%3D%22alert%281%29%22%2F%3E%7B%7B%2Fhtml%7D%7D%7B%7Bnoscript%2F%7D%7D";
        XWikiDocument doc = loadPage(XWIKI_SKINS_SHEET);

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);

        Document document = render(doc);
        Elements links = document.getElementsByTag("a");
        assertEquals(2, links.size());

        assertEquals("xe.admin.skin.editskin", links.get(0).text());
        // Verify that the link has exactly one attribute, which is the expected URL (and not also a part of the URL as
        // a separate attribute).
        assertEquals(1, links.get(0).attributes().size());
        assertEquals(xwikiDocument.getURL(xwikiDocument.getDefaultEditMode(this.context), this.context),
            links.get(0).attr("href"));

        assertEquals("xe.admin.skin.testskin", links.get(1).text());
        // Verify that the link has exactly one attribute, which is the expected URL (and not also the queryString or
        // a part of the URL as a separate attribute).
        assertEquals(1, links.get(1).attributes().size());
        XWikiDocument mainDoc = this.xwiki.getDocument(new DocumentReference("xwiki", "Main", "WebHome"), this.context);
        assertEquals(mainDoc.getURL("create", "skin=Space." + encodedName, this.context), links.get(1).attr("href"));
    }

    @Test
    void editMode() throws Exception
    {
        // Add the object to test.
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", NASTY_INPUT), this.context);
        BaseObject baseObject = new BaseObject();
        baseObject.setXClassReference(XWIKI_SKINS);
        xwikiDocument.addXObject(baseObject);

        // Add properties with nasty names to test escaping.
        XWikiDocument classDocument = this.xwiki.getDocument(XWIKI_SKINS, this.context);
        BaseClass xClass = classDocument.getXClass();
        String textName = NASTY_INPUT + "_text";
        xClass.addTextField(textName, NASTY_INPUT, 20);
        String templateName = NASTY_INPUT + "_template";
        xClass.addTemplateField(templateName, NASTY_INPUT);
        this.xwiki.saveDocument(classDocument, this.context);

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);
        this.context.setAction("edit");
        this.context.put("display", "edit");

        XWikiDocument doc = loadPage(XWIKI_SKINS_SHEET);
        Document document = render(doc);
        verifyNoErrors(document);

        Elements nastyLabels = document.getElementsContainingOwnText(NASTY_INPUT);
        assertEquals(2, nastyLabels.size());
        assertEquals(NASTY_INPUT, nastyLabels.get(0).text());
        assertEquals("ICON:file-white " + NASTY_INPUT, nastyLabels.get(1).text());

        for (String suffix : List.of(textName, templateName)) {
            String id = "XWiki.XWikiSkins_0_" + suffix;
            Element input = document.getElementById(id);
            assertNotNull(input);
            assertEquals(id, input.attr("name"));
        }
    }

    @Test
    void displayObject() throws Exception
    {
        XWikiDocument xwikiDocument =
            this.xwiki.getDocument(new DocumentReference("xwiki", "Space", NASTY_INPUT), this.context);
        BaseObject baseObject = new BaseObject();
        baseObject.setXClassReference(XWIKI_SKIN_FILE_OVERRIDE);
        xwikiDocument.addXObject(baseObject);

        // Add a property with nasty name to test escaping.
        XWikiDocument classDocument = this.xwiki.getDocument(XWIKI_SKIN_FILE_OVERRIDE, this.context);
        BaseClass xClass = classDocument.getXClass();
        xClass.addTextField(NASTY_INPUT + "_text", NASTY_INPUT, 20);
        this.xwiki.saveDocument(classDocument, this.context);

        // Set up the current doc in the context so that $doc is bound in scripts
        this.context.setDoc(xwikiDocument);
        // Trigger displaying the object.
        this.context.setAction("get");
        this.request.put("action", "displayObj");
        this.request.put("objNumber", "0");

        XWikiDocument doc = loadPage(XWIKI_SKINS_SHEET);
        Document document = render(doc);
        verifyNoErrors(document);
        Elements propertyLabel = document.getElementsContainingOwnText(NASTY_INPUT);
        assertEquals(1, propertyLabel.size());
        assertEquals("dt", propertyLabel.get(0).tagName());
        assertEquals(NASTY_INPUT, propertyLabel.text());
    }

    private void verifyNoErrors(Document document)
    {
        assertEquals(0, document.getElementsByClass("xwikirenderingerror").size());
        assertEquals(0, document.getElementsContainingText("{{html").size());
        assertEquals(0, document.getElementsContainingText("$").size());
        for (Element img : document.getElementsByTag("img")) {
            assertFalse(img.hasAttr("onerror"));
        }
    }

    private Document render(XWikiDocument doc) throws XWikiException
    {
        return Jsoup.parse(doc.getRenderedContent(this.context));
    }
}

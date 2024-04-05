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
package org.xwiki.rendering.wikimacro.internal;

import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.script.ScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.converter.ConversionException;
import org.xwiki.rendering.converter.Converter;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultWikiMacro}.
 * 
 * @version $Id$
 */
@OldcoreTest
@AllComponents
class DefaultWikiMacroTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    /**
     * Dummy document reference of the document which contains the wiki macro.
     */
    private DocumentReference wikiMacroDocumentReference;

    private DocumentReference wikiMacroAuthorReference;

    private XWikiDocument wikiMacroDocument;

    private BaseObject wikiMacroObject;

    private WikiMacroManager wikiMacroManager;

    private XWikiDocument user;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.wikiMacroDocumentReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "space", "macroPage");
        this.wikiMacroManager = this.componentManager.getInstance(WikiMacroManager.class);

        this.wikiMacroDocument = new XWikiDocument(wikiMacroDocumentReference);

        this.user =
            new XWikiDocument(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "user"));
        this.user.setNew(false);
        BaseObject userObject = new BaseObject();
        userObject.setXClassReference(
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "XWiki", "XWikiusers"));
        this.user.addXObject(userObject);

        this.oldcore.getSpyXWiki().saveDocument(this.user, this.oldcore.getXWikiContext());

        this.wikiMacroAuthorReference = new DocumentReference("mawiki", "maspace", "mauser");
        this.wikiMacroDocument.setCreatorReference(this.wikiMacroAuthorReference);
        this.wikiMacroDocument.setAuthorReference(this.wikiMacroAuthorReference);
        this.wikiMacroDocument.setContentAuthorReference(this.wikiMacroAuthorReference);

        this.wikiMacroObject = new BaseObject();
        this.wikiMacroObject.setXClassReference(new DocumentReference(this.oldcore.getXWikiContext().getWikiId(),
            WikiMacroConstants.WIKI_MACRO_PARAMETER_CLASS_SPACE, WikiMacroConstants.WIKI_MACRO_PARAMETER_CLASS_PAGE));
        this.wikiMacroDocument.addXObject(this.wikiMacroObject);

        this.oldcore.getSpyXWiki().saveDocument(this.wikiMacroDocument, this.oldcore.getXWikiContext());

        when(this.oldcore.getMockAuthorizationManager().hasAccess(same(Right.PROGRAM), any(), any())).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(same(Right.PROGRAM))).thenReturn(true);
        when(this.oldcore.getMockContextualAuthorizationManager().hasAccess(same(Right.SCRIPT))).thenReturn(true);
        when(this.oldcore.getMockRightService().hasProgrammingRights(any())).thenReturn(true);

        this.oldcore.getXWikiContext().setUserReference(this.user.getDocumentReference());

        XWikiDocument sDocument = new XWikiDocument(new DocumentReference("swiki", "sspace", "sdoc"));
        sDocument.setContentAuthorReference(new DocumentReference("swiki", "sspace", "suser"));
        this.oldcore.getXWikiContext().put("sdoc", sDocument);
    }

    private void registerWikiMacro(String macroId, String macroContent, Syntax syntax) throws Exception
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors =
            Arrays.asList(new WikiMacroParameterDescriptor("param1", "This is param1", true),
                new WikiMacroParameterDescriptor("param2", "This is param2", true));
        registerWikiMacro(macroId, macroContent, syntax, parameterDescriptors);
    }

    private void registerWikiMacro(String macroId, String macroContent, Syntax syntax,
        List<WikiMacroParameterDescriptor> parameterDescriptors) throws Exception
    {
        registerWikiMacro(macroId, macroContent, syntax, new DefaultContentDescriptor(false), parameterDescriptors);
    }

    private void registerWikiMacro(String macroId, String macroContent, Syntax syntax,
        DefaultContentDescriptor contentDescriptor, List<WikiMacroParameterDescriptor> parameterDescriptors)
        throws Exception
    {
        // TOOD: add a test with defaultCategory and move this test to legacy
        WikiMacroDescriptor descriptor =
            new WikiMacroDescriptor.Builder().id(new MacroId(macroId)).name("Wiki Macro").description("Description")
                .defaultCategories(Set.of("Test")).visibility(WikiMacroVisibility.GLOBAL).supportsInlineMode(true)
                .contentDescriptor(contentDescriptor).parameterDescriptors(parameterDescriptors).build();

        this.wikiMacroDocument.setSyntax(syntax);
        this.wikiMacroObject.setLargeStringValue(WikiMacroConstants.MACRO_CODE_PROPERTY, macroContent);

        DefaultWikiMacro wikiMacro = this.componentManager.getInstance(DefaultWikiMacro.class);
        wikiMacro.initialize(this.wikiMacroObject, descriptor);

        this.wikiMacroManager.registerWikiMacro(wikiMacroDocumentReference, wikiMacro);
    }

    private void registerWikiMacro(String macroId, String macroContent) throws Exception
    {
        registerWikiMacro(macroId, macroContent, Syntax.XWIKI_2_0);
    }

    private void assertXDOM(String expectEvents, String inputXWiki) throws ComponentLookupException, ConversionException
    {
        assertMacro(expectEvents, Syntax.XDOMXML_CURRENT, inputXWiki);
    }

    private void assertXHTML(String expectXHTML, String inputXWiki) throws ComponentLookupException, ConversionException
    {
        assertMacro(expectXHTML, Syntax.XHTML_1_0, inputXWiki);
    }

    private void assertMacro(String expect, Syntax expectSyntax, String inputXWiki)
        throws ComponentLookupException, ConversionException
    {
        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader(inputXWiki), Syntax.XWIKI_2_0, expectSyntax, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        assertEquals(expect, printer.toString());

        assertFalse(this.oldcore.getXWikiContext().containsKey("macro"));
    }

    // Tests

    /**
     * Test normal wiki macro execution.
     */
    @Test
    void execute() throws Exception
    {
        registerWikiMacro("wikimacro1", "This is **bold**", Syntax.XWIKI_2_0);

        assertXHTML("<p>This is <strong>bold</strong></p>", "{{wikimacro1 param1=\"value1\" param2=\"value2\"/}}");
        assertXDOM("<document><p><metadata><metadata><entry><string>syntax</string><org.xwiki.rendering.syntax.Syntax>"
            + "<type><name>XWiki</name><id>xwiki</id><variants class=\"empty-list\"></variants></type>"
            + "<version>2.0</version></org.xwiki.rendering.syntax.Syntax></entry></metadata></metadata></p>"
            + "<macroMarker name=\"wikimacro1\"><p><parameters><entry><string>param1</string><string>value1</string>"
            + "</entry><entry><string>param2</string><string>value2</string></entry></parameters></p><paragraph>"
            + "<word>This</word><space></space><word>is</word><space></space><format format=\"BOLD\"><word>bold</word>"
            + "</format></paragraph></macroMarker></document>", "{{wikimacro1 param1=\"value1\" param2=\"value2\"/}}");
    }

    /**
     * When a wiki macro is used in inline mode and its code starts with a macro, that nested macro is made inline. In
     * other words, the nested macro should not generate extra paragraph elements.
     */
    @Test
    void executeWhenInlineAndWithMacro() throws Exception
    {
        registerWikiMacro("wikimacro1", "This is **bold**", Syntax.XWIKI_2_0);
        registerWikiMacro("wikimacro2", "{{wikimacro1 param1=\"v1\" param2=\"v2\"/}}", Syntax.XWIKI_2_0);

        // Note: We're putting the macro after the "Hello" text to force it as an inline macro.
        assertXHTML("<p>Hello This is <strong>bold</strong></p>",
            "Hello {{wikimacro2 param1=\"value1\" param2=\"value2\"/}}");
    }

    /**
     * Check that macro used inside wiki macro are executed as part of the document.
     */
    @Test
    void executeWhenInnerMacro() throws Exception
    {
        WikiModel wikiModel = this.componentManager.registerMockComponent(WikiModel.class);
        registerWikiMacro("wikimacro1", "{{toc/}}", Syntax.XWIKI_2_0);

        DocumentResourceReference reference = new DocumentResourceReference(null);
        reference.setAnchor("Hheading");
        when(wikiModel.getDocumentViewURL(reference)).thenReturn("url");

        assertXHTML(
            "<h2 id=\"Hheading\" class=\"wikigeneratedid\"><span>heading</span></h2>"
                + "<ul class=\"wikitoc\"><li class=\"nodirectchild\"><ul>"
                + "<li><span class=\"wikilink\"><a href=\"#Hheading\">heading</a></span></li>"
                + "</ul></li></ul>",
            "== heading\n\n{{wikimacro1 param1=\"value1\" param2=\"value2\"/}}");
    }

    /**
     * Check that macro used inside wiki macro are executed with the right syntax.
     */
    @Test
    void executeWhenWithDifferentMacroSyntax() throws Exception
    {
        registerWikiMacro("wikimacro", "{{groovy}}println \"[[path:/some/path]]\"{{/groovy}}", Syntax.XWIKI_2_1);

        assertXHTML(
            "<p><span class=\"wikiinternallink\"><a href=\"/some/path\">"
                + "<span class=\"wikigeneratedlinkcontent\">/some/path</span></a></span></p>",
            "{{wikimacro param1=\"value1\" param2=\"value2\"/}}");
    }

    /**
     * A wiki macro can directly provide the list of blocks instead of having to render them to let
     * {@link DefaultWikiMacro} re-parse it.
     */
    @Test
    void executeWhenWikiMacroDirectlyProvideTheResult() throws Exception
    {
        registerWikiMacro("wikimacrowithresult",
            "{{groovy}}" + "xcontext.macro.result = java.util.Collections.singletonList("
                + "new org.xwiki.rendering.block.WordBlock(xcontext.macro.params.param1));" + "{{/groovy}}",
            Syntax.XWIKI_2_0);

        assertXHTML("<p>Hello World</p>", "Hello {{wikimacrowithresult param1=\"World\" param2=\"param2\"/}}");
    }

    /**
     * Test default parameter value injection.
     */
    @Test
    void defaultParameterValues() throws Exception
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors =
            Arrays.asList(new WikiMacroParameterDescriptor("param1", "This is param1", false, "default_value"));

        registerWikiMacro("wikimacro1",
            "{{velocity}}$xcontext.macro.params.param1 $xcontext.macro.params.paraM1{{/velocity}}", Syntax.XWIKI_2_0,
            parameterDescriptors);

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro1/}}"), Syntax.XWIKI_2_0, Syntax.PLAIN_1_0, printer);

        assertEquals("default_value default_value", printer.toString());
    }

    /**
     * Test result value injection.
     */
    @Test
    void resultValueFromContext() throws Exception
    {
        this.scriptContextManager.getCurrentScriptContext().setAttribute("wordBlock", new WordBlock("foo"),
            ScriptContext.GLOBAL_SCOPE);

        registerWikiMacro("wikimacro1", "{{velocity}}#set ($xcontext.macro.result = $wordBlock)" + "{{/velocity}}",
            Syntax.XWIKI_2_0, Collections.emptyList());

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro1/}}"), Syntax.XWIKI_2_0, Syntax.PLAIN_1_0, printer);

        assertEquals("foo", printer.toString());
    }

    /**
     * Test result value injection from new binding.
     */
    @Test
    void resultValueFromNewBinding() throws Exception
    {
        this.scriptContextManager.getCurrentScriptContext().setAttribute("wordBlock", new WordBlock("foo"),
            ScriptContext.GLOBAL_SCOPE);

        registerWikiMacro("wikimacro1", "{{velocity}}#set ($wikimacro.result = $wordBlock){{/velocity}}",
            Syntax.XWIKI_2_0, Collections.emptyList());

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro1/}}"), Syntax.XWIKI_2_0, Syntax.PLAIN_1_0, printer);

        assertEquals("foo", printer.toString());
    }

    /**
     * Test parameter types are converted to appropriate value
     */
    @Test
    void parameterType() throws Exception
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors =
            Arrays.asList(new WikiMacroParameterDescriptor("param1", "This is param1", true, null, Integer.class));

        //@formatter:off
        registerWikiMacro("wikimacro1",
            "{{velocity}}"
            + "$xcontext.macro.params.param1 $xcontext.macro.params.param1.class.name "
            + "$xcontext.macro.params.paraM1 $xcontext.macro.params.paraM1.class.name "
            + "$wikimacro.parameters.param1 $wikimacro.parameters.param1.class.name "
            + "$wikimacro.parameters.paraM1 $wikimacro.parameters.paraM1.class.name"
            + "{{/velocity}}",
            Syntax.XWIKI_2_0, parameterDescriptors);
        //@formatter:on

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro1 param1=\"43\"/}}"), Syntax.XWIKI_2_0, Syntax.PLAIN_1_0,
            printer);

        assertEquals("43 java.lang.String 43 java.lang.String 43 java.lang.Integer 43 java.lang.Integer",
            printer.toString());
    }

    @Test
    void context() throws Exception
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors =
            Arrays.asList(new WikiMacroParameterDescriptor("param1", "This is param1", false, "default_value"));

        registerWikiMacro("wikimacro1", "{{velocity}}$xcontext.sdoc{{/velocity}}", Syntax.XWIKI_2_0,
            parameterDescriptors);

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro1/}}"), Syntax.XWIKI_2_0, Syntax.PLAIN_1_0, printer);

        assertEquals("space.macroPage", printer.toString());
    }

    /**
     * Makes sure getParameterNames return parameters with source case.
     */
    @Test
    void defaultParameterNames() throws Exception
    {
        registerWikiMacro("wikimacro1", "{{velocity}}$xcontext.macro.params.parameterNames{{/velocity}}",
            Syntax.XWIKI_2_0);

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro1 paRam1=\"value1\" paraM2=\"value2\"/}}"), Syntax.XWIKI_2_0,
            Syntax.PLAIN_1_0, printer);

        assertEquals("[paRam1, paraM2]", printer.toString());
    }

    @Test
    void getCurrentMacroBlock() throws Exception
    {
        //@formatter:off
        registerWikiMacro("wikimacro",
            "{{groovy}}"
            + "println xcontext.macro.context.getCurrentMacroBlock().id\n"
            + "println xcontext.macro.context.getCurrentMacroBlock().parent.class\n"
            + "println xcontext.macro.context.getCurrentMacroBlock().nextSibling.children[0].word\n"
            + "println xcontext.macro.context.getCurrentMacroBlock().previousSibling.children[0].word\n"
            + "{{/groovy}}",
            Syntax.XWIKI_2_0, Collections.emptyList());
        //@formatter:on

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("before\n\n{{wikimacro/}}\n\nafter"), Syntax.XWIKI_2_0, Syntax.PLAIN_1_0,
            printer);

        assertEquals("before" + "\n\n" + "wikimacro\n" + "class org.xwiki.rendering.block.XDOM\n" + "after\n" + "before"
            + "\n\n" + "after", printer.toString());
    }

    @Test
    void executeWhenWikiMacroBinding() throws Exception
    {
        //@formatter:off
        registerWikiMacro("wikimacrobindings",
            "{{groovy}}"
            + "println xcontext.macro.doc\n"
            + "println xcontext.macro.doc.class\n"
            + "println wikimacro.doc\n"
            + "println wikimacro.doc.class\n"
            + "{{/groovy}}");
        //@formatter:on

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacrobindings param1=\"value2\" param2=\"value2\"/}}"),
            Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        //@formatter:off
        String expectedOutput = "<p>"
            + this.wikiMacroDocument.toString() + "<br/>"
            + "class com.xpn.xwiki.api.Document<br/>"
            + this.wikiMacroDocument.toString() + "<br/>"
            + "class com.xpn.xwiki.api.Document"
            + "</p>";
        //@formatter:on

        assertEquals(expectedOutput, printer.toString());
    }

    @Test
    void executeWhenWikiRequiringPRAfterDropPermission() throws Exception
    {
        registerWikiMacro("wikimacrobindings", "{{groovy}}print xcontext.macro.doc{{/groovy}}");

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();

        this.oldcore.getXWikiContext().dropPermissions();
        this.wikiMacroDocument.newDocument(this.oldcore.getXWikiContext()).dropPermissions();

        converter.convert(new StringReader("{{wikimacrobindings param1=\"value2\" param2=\"value2\"/}}"),
            Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        assertEquals("<p>" + this.wikiMacroDocument.toString() + "</p>", printer.toString());
        assertTrue(this.oldcore.getXWikiContext().hasDroppedPermissions(),
            "Wiki macro did not properly restord persmission dropping");
    }

    @Test
    void wikimacrocontentWhenOptionalContentWithNoContent() throws Exception
    {
        WikiMacroDescriptor descriptor = new WikiMacroDescriptor.Builder()
            .id(new MacroId("wikimacro"))
            .name("Wiki Macro")
            .visibility(WikiMacroVisibility.GLOBAL)
            .contentDescriptor(new DefaultContentDescriptor(false))
            .parameterDescriptors(Collections.emptyList())
            .build();
        this.wikiMacroDocument.setSyntax(Syntax.XWIKI_2_0);
        this.wikiMacroObject.setLargeStringValue(WikiMacroConstants.MACRO_CODE_PROPERTY, "{{wikimacrocontent/}}");
        DefaultWikiMacro wikiMacro = this.componentManager.getInstance(DefaultWikiMacro.class);
        wikiMacro.initialize(this.wikiMacroObject, descriptor);
        this.wikiMacroManager.registerWikiMacro(wikiMacroDocumentReference, wikiMacro);

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro/}}"), Syntax.XWIKI_2_1, Syntax.EVENT_1_0, printer);

        String expect = "beginDocument [[syntax]=[XWiki 2.1]]\n"
            + "beginMacroMarkerStandalone [wikimacro] []\n"
            + "beginMacroMarkerStandalone [wikimacrocontent] []\n"
            + "beginGroup [[data-wikimacro-id]=[wikimacrocontent]]\n"
            + "endGroup [[data-wikimacro-id]=[wikimacrocontent]]\n"
            + "endMacroMarkerStandalone [wikimacrocontent] []\n"
            + "endMacroMarkerStandalone [wikimacro] []\n"
            + "endDocument [[syntax]=[XWiki 2.1]]";

        assertEquals(expect, printer.toString());
    }

    @Test
    void wikimacrocontentWhenTypeWiki() throws Exception
    {
        registerWikiMacro("wikimacro",
            "{{velocity}}$xcontext.context.authorReference{{/velocity}}\n{{wikimacrocontent/}}", Syntax.XWIKI_2_0,
            new DefaultContentDescriptor("", false, Block.LIST_BLOCK_TYPE),
            Collections.emptyList());

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader(
            "{{wikimacro}}" + "{{velocity}}$xcontext.context.authorReference{{/velocity}}\n[[label>>path:reference]]"
                + "{{/wikimacro}}"),
            Syntax.XWIKI_2_1, Syntax.EVENT_1_0, printer);

        //@formatter:off
        String expect =
            "beginDocument [[syntax]=[XWiki 2.1]]\n" + 
            "beginMacroMarkerStandalone [wikimacro] [] [{{velocity}}$xcontext.context.authorReference{{/velocity}}\n" + 
            "[[label>>path:reference]]]\n" + 
            "beginParagraph\n" + 
            "onWord [mawiki]\n" + 
            "onSpecialSymbol [:]\n" + 
            "onWord [maspace]\n" + 
            "onSpecialSymbol [.]\n" + 
            "onWord [mauser]\n" + 
            "onNewLine\n" + 
            "beginMetaData [[non-generated-content]=[java.util.List<org.xwiki.rendering.block.Block>]"
                + "[wikimacrocontent]=[true]]\n" +
            "beginMacroMarkerInline [velocity] [] [$xcontext.context.authorReference]\n" + 
            "onWord [swiki]\n" + 
            "onSpecialSymbol [:]\n" + 
            "onWord [sspace]\n" + 
            "onSpecialSymbol [.]\n" + 
            "onWord [suser]\n" + 
            "endMacroMarkerInline [velocity] [] [$xcontext.context.authorReference]\n" + 
            "onNewLine\n" + 
            "beginLink [Typed = [true] Type = [path] Reference = [reference]] [false]\n" + 
            "onWord [label]\n" + 
            "endLink [Typed = [true] Type = [path] Reference = [reference]] [false]\n" + 
            "endMetaData [[non-generated-content]=[java.util.List<org.xwiki.rendering.block.Block>]"
                + "[wikimacrocontent]=[true]]\n" +
            "endParagraph\n" + 
            "endMacroMarkerStandalone [wikimacro] [] [{{velocity}}$xcontext.context.authorReference{{/velocity}}\n" + 
            "[[label>>path:reference]]]\n" + 
            "endDocument [[syntax]=[XWiki 2.1]]";
        //@formatter:on

        assertEquals(expect, printer.toString());
    }

    @Test
    void wikimacroparameterWhenTypeWiki() throws Exception
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors = Arrays
            .asList(new WikiMacroParameterDescriptor("param1", "This is param1", false, null, Block.LIST_BLOCK_TYPE));

        registerWikiMacro("wikimacro",
            "{{velocity}}$xcontext.context.authorReference{{/velocity}}\n{{wikimacroparameter name=\"param1\"/}}",
            Syntax.XWIKI_2_0, parameterDescriptors);

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(
            new StringReader("{{wikimacro param1=\""
                + "{{velocity}}$xcontext.context.authorReference{{/velocity}}\n[[label>>path:reference]]" + "\"/}}"),
            Syntax.XWIKI_2_1, Syntax.EVENT_1_0, printer);

        //@formatter:off
        String expect =
            "beginDocument [[syntax]=[XWiki 2.1]]\n" + 
            "beginMacroMarkerStandalone [wikimacro] "
                + "[param1={{velocity}}$xcontext.context.authorReference{{/velocity}}\n" +
            "[[label>>path:reference]]]\n" + 
            "beginParagraph\n" + 
            "onWord [mawiki]\n" + 
            "onSpecialSymbol [:]\n" + 
            "onWord [maspace]\n" + 
            "onSpecialSymbol [.]\n" + 
            "onWord [mauser]\n" + 
            "onNewLine\n" + 
            "beginMetaData [[non-generated-content]=[java.util.List<org.xwiki.rendering.block.Block>]"
                + "[parameter-name]=[param1][wikimacrocontent]=[true]]\n" +
            "beginMacroMarkerInline [velocity] [] [$xcontext.context.authorReference]\n" + 
            "onWord [swiki]\n" + 
            "onSpecialSymbol [:]\n" + 
            "onWord [sspace]\n" + 
            "onSpecialSymbol [.]\n" + 
            "onWord [suser]\n" + 
            "endMacroMarkerInline [velocity] [] [$xcontext.context.authorReference]\n" + 
            "onNewLine\n" + 
            "beginLink [Typed = [true] Type = [path] Reference = [reference]] [false]\n" + 
            "onWord [label]\n" + 
            "endLink [Typed = [true] Type = [path] Reference = [reference]] [false]\n" + 
            "endMetaData [[non-generated-content]=[java.util.List<org.xwiki.rendering.block.Block>]"
                + "[parameter-name]=[param1][wikimacrocontent]=[true]]\n" +
            "endParagraph\n" + 
            "endMacroMarkerStandalone [wikimacro] "
                + "[param1={{velocity}}$xcontext.context.authorReference{{/velocity}}\n" +
            "[[label>>path:reference]]]\n" + 
            "endDocument [[syntax]=[XWiki 2.1]]";
        //@formatter:on

        assertEquals(expect, printer.toString());
    }

    @Test
    void wikimacrocontentWhenInRawBlock() throws Exception
    {
        registerWikiMacro("wikimacro",
            "{{html wiki='true' clean='false'}}<span class='class'>{{wikimacrocontent/}}</span>{{/html}}",
            Syntax.XWIKI_2_0, new DefaultContentDescriptor("", false, Block.LIST_BLOCK_TYPE),
            Collections.emptyList());

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro}}[[label>>path:reference]]{{/wikimacro}}"), Syntax.XWIKI_2_1,
            Syntax.EVENT_1_0, printer);

        //@formatter:off
        String expect =
            "beginDocument [[syntax]=[XWiki 2.1]]\n"
            + "beginMacroMarkerStandalone [wikimacro] [] [[[label>>path:reference]]]\n"
            + "onRawText [<span class='class'>] [html/5.0]\n"
            + "beginMetaData [[non-generated-content]="
                + "[java.util.List<org.xwiki.rendering.block.Block>][wikimacrocontent]=[true]]\n"
            + "beginLink [Typed = [true] Type = [path] Reference = [reference]] [false]\n"
            + "onWord [label]\n"
            + "endLink [Typed = [true] Type = [path] Reference = [reference]] [false]\n"
            + "endMetaData [[non-generated-content]="
                + "[java.util.List<org.xwiki.rendering.block.Block>][wikimacrocontent]=[true]]\n"
            + "onRawText [</span>] [html/5.0]\n"
            + "endMacroMarkerStandalone [wikimacro] [] [[[label>>path:reference]]]\n"
            + "endDocument [[syntax]=[XWiki 2.1]]";
        //@formatter:on

        assertEquals(expect, printer.toString());
    }

    @Test
    void wikimacroparameterWhenInRawBlock() throws Exception
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors = Arrays
            .asList(new WikiMacroParameterDescriptor("param1", "This is param1", false, null, Block.LIST_BLOCK_TYPE));

        registerWikiMacro("wikimacro",
        //@formatter:off
            "{{html wiki='true' clean='false'}}"
              + "<span class='class'>{{wikimacroparameter name=\"param1\"/}}</span>" +
            "{{/html}}",
        //@formatter:on
            Syntax.XWIKI_2_0, new DefaultContentDescriptor("", false, Block.LIST_BLOCK_TYPE), parameterDescriptors);

        Converter converter = this.componentManager.getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacro param1=\"[[label>>path:reference]]\"/}}"),
            Syntax.XWIKI_2_1, Syntax.EVENT_1_0, printer);

        //@formatter:off
        String expect =
            "beginDocument [[syntax]=[XWiki 2.1]]\n"
            + "beginMacroMarkerStandalone [wikimacro] [param1=[[label>>path:reference]]]\n"
            + "onRawText [<span class='class'>] [html/5.0]\n"
            + "beginMetaData [[non-generated-content]=[java.util.List<org.xwiki.rendering.block.Block>]"
                + "[parameter-name]=[param1][wikimacrocontent]=[true]]\n"
            + "beginLink [Typed = [true] Type = [path] Reference = [reference]] [false]\n"
            + "onWord [label]\n"
            + "endLink [Typed = [true] Type = [path] Reference = [reference]] [false]\n"
            + "endMetaData [[non-generated-content]=[java.util.List<org.xwiki.rendering.block.Block>]"
                + "[parameter-name]=[param1][wikimacrocontent]=[true]]\n"
            + "onRawText [</span>] [html/5.0]\n"
            + "endMacroMarkerStandalone [wikimacro] [param1=[[label>>path:reference]]]\n" +
            "endDocument [[syntax]=[XWiki 2.1]]";
        //@formatter:on

        assertEquals(expect, printer.toString());
    }
}

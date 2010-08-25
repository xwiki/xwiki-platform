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
package com.xpn.xwiki.internal;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.converter.Converter;
import org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.script.ScriptMockSetup;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;

/**
 * Unit tests for {@link DefaultWikiMacro} in the context of XWiki core.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class DefaultWikiMacroTest extends AbstractBridgedComponentTestCase
{
    /**
     * Dummy document reference of the document which contains the wiki macro.
     */
    private DocumentReference wikiMacroDocumentReference;

    /**
     * The {@link org.xwiki.rendering.macro.wikibridge.WikiMacroManager} component.
     */
    private WikiMacroManager wikiMacroManager;

    private Parser xwiki20Parser;
    
    private DocumentAccessBridge mockDocBridge;
    
    private XWikiDocument wikiMacroDocument;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        
        // Script setup.
        ScriptMockSetup scriptMockSetup = new ScriptMockSetup(getMockery(), getComponentManager());
        this.mockDocBridge = scriptMockSetup.bridge;
    }
    
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        
        final XWiki xwiki = getMockery().mock(XWiki.class);
        
        getContext().setWiki(xwiki);

        this.xwiki20Parser = getComponentManager().lookup(Parser.class, "xwiki/2.0");

        this.wikiMacroDocumentReference = new DocumentReference("wiki", "space", "macroPage");
        this.wikiMacroManager = getComponentManager().lookup(WikiMacroManager.class);

        this.wikiMacroDocument = new XWikiDocument(wikiMacroDocumentReference);
        
        getMockery().checking(new Expectations() {{
            allowing(mockDocBridge).getCurrentWiki(); will(returnValue("wiki"));
            allowing(mockDocBridge).getCurrentUser(); will(returnValue("dummy"));

            // This is the document containing the wiki macro that will be put in the context available in the macro
            // Since we're not testing it here, it can be null.
            allowing(mockDocBridge).getDocument(wikiMacroDocumentReference); will(returnValue(wikiMacroDocument));
            
            allowing(xwiki).getDocument(wikiMacroDocumentReference, getContext()); will(returnValue(wikiMacroDocument));
        }});
    }

    @Test
    public void testExecuteWhenWikiMacroBinding() throws Exception
    {
        registerWikiMacro(
            "wikimacrobindings",
            "{{groovy}}"
            + "print xcontext.macro.doc"
            + "{{/groovy}}");

        Converter converter = getComponentManager().lookup(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        // Note: We're putting the macro after the "Hello" text to force it as an inline macro.
        converter.convert(new StringReader("{{wikimacrobindings param1=\"value2\" param2=\"value2\"/}}"),
            Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        Assert.assertEquals("<p>" + this.wikiMacroDocument.toString() + "</p>", printer.toString());
    }

    private void registerWikiMacro(String macroId, String macroContent) throws Exception
    {
        List<WikiMacroParameterDescriptor> parameterDescriptors =
            Arrays.asList(new WikiMacroParameterDescriptor("param1", "This is param1", true),
                new WikiMacroParameterDescriptor("param2", "This is param2", true));
        registerWikiMacro(macroId, macroContent, parameterDescriptors);
    }

    private void registerWikiMacro(String macroId, String macroContent,
        List<WikiMacroParameterDescriptor> parameterDescriptors) throws Exception
    {
        WikiMacroDescriptor descriptor =
            new WikiMacroDescriptor(new MacroId(macroId), "Wiki Macro", "Description", "Test",
                WikiMacroVisibility.GLOBAL, new DefaultContentDescriptor(false), parameterDescriptors);

        DefaultWikiMacro wikiMacro =
            new DefaultWikiMacro(wikiMacroDocumentReference, true, descriptor,
                this.xwiki20Parser.parse(new StringReader(macroContent)), Syntax.XWIKI_2_0, getComponentManager());

        this.wikiMacroManager.registerWikiMacro(wikiMacroDocumentReference, wikiMacro);
    }
}

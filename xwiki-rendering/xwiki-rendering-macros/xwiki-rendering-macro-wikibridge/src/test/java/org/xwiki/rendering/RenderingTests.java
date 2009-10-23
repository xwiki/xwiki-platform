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

package org.xwiki.rendering;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacro;
import org.xwiki.rendering.internal.macro.wikibridge.MockWikiMacro;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * Rendering tests for wiki macros.
 *
 * @version $Id$
 * @since 2.0M2
 */
public class RenderingTests extends TestCase
{
    /**
     * Builds and returns a new {@link ComponentManagerTestSetup}.
     * 
     * @return a {@link ComponentManagerTestSetup}.
     * @throws Exception if an error occurs while building the test setup.
     */
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test Wiki Macro Bridge");
        
        suite.addTestsFromResource("wikimacro1", true);
        suite.addTestsFromResource("wikimacro2", true);

        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        setUpMocks(testSetup.getComponentManager());

        return testSetup;
    }
    
    public static void setUpMocks(EmbeddableComponentManager componentManager) throws Exception
    {
        Mockery context = new Mockery();

        // Document Access Bridge Mock
        final DocumentAccessBridge mockDocumentAccessBridge = context.mock(DocumentAccessBridge.class);
        DefaultComponentDescriptor<DocumentAccessBridge> descriptorDAB =
            new DefaultComponentDescriptor<DocumentAccessBridge>();
        descriptorDAB.setRole(DocumentAccessBridge.class);
        componentManager.registerComponent(descriptorDAB, mockDocumentAccessBridge);
        
        // Register mock wiki macros.
        WikiMacroManager wikiMacroManager = componentManager.lookup(WikiMacroManager.class);
        
        // Mock wiki macro - 1.
        WikiMacroParameterDescriptor param1 = new WikiMacroParameterDescriptor("param1", "This is param1", true);
        WikiMacroParameterDescriptor param2 = new WikiMacroParameterDescriptor("param2", "This is param2", true);
        List<WikiMacroParameterDescriptor> params = new ArrayList<WikiMacroParameterDescriptor>();
        params.add(param1);
        params.add(param2);
        WikiMacroDescriptor descriptor = new WikiMacroDescriptor("Mock Wiki Macro - 1", "Description", "Test",
            new DefaultContentDescriptor(false), params);
        WikiMacro wikiMacro = new MockWikiMacro(new DefaultWikiMacro("xwiki:Main.MockWikiMacro1", "mockwikimacro1",
            true, descriptor, "This is **mockwikimacro1**", "xwiki/2.0", componentManager), componentManager);                     
        wikiMacroManager.registerWikiMacro("xwiki:Main.MockWikiMacro1", wikiMacro);
        
        // Mock wiki macro - 2.
        params = new ArrayList<WikiMacroParameterDescriptor>();
        descriptor = new WikiMacroDescriptor("Mock Wiki Macro - 2", "Description", "Test", 
            new DefaultContentDescriptor(false), params);
        wikiMacro = new MockWikiMacro(new DefaultWikiMacro("xwiki:Main.MockWikiMacro2", "mockwikimacro2",
            true, descriptor, "{{mockwikimacro1 param1=\"p1\" param2=\"p2\"/}}", "xwiki/2.0", componentManager),
            componentManager);
        wikiMacroManager.registerWikiMacro("xwiki:Main.MockWikiMacro2", wikiMacro);
        

        context.checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).getDocument("xwiki:Main.MockWikiMacro1"); will(returnValue(null));
            allowing(mockDocumentAccessBridge).getDocument("xwiki:Main.MockWikiMacro2"); will(returnValue(null));
        }});
    }    
}

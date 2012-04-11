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
package org.xwiki.rendering.internal.macro.wikibridge;

import java.io.StringReader;
import java.util.ArrayList;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.InsufficientPrivilegesException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit tests for {@link org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacroManager}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class DefaultWikiMacroManagerTest extends AbstractComponentTestCase
{
    /**
     * The {@link org.xwiki.rendering.macro.wikibridge.WikiMacroManager} component.
     */
    private WikiMacroManager wikiMacroManager;

    /**
     * The {@link MacroManager} component.
     */
    private MacroManager macroManager;

    private DocumentAccessBridge mockDocumentAccessBridge;
    
    private Parser xwiki20Parser;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Document Access Bridge Mock
        this.mockDocumentAccessBridge = registerMockComponent(DocumentAccessBridge.class);
    }

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.xwiki20Parser = getComponentManager().getInstance(Parser.class, "xwiki/2.0");

        this.macroManager = getComponentManager().getInstance(MacroManager.class);
        this.wikiMacroManager = getComponentManager().getInstance(WikiMacroManager.class);
    }
    
    @Test
    public void testRegisterWikiMacroWhenGlobalVisibilityAndAllowed() throws Exception
    {
        final DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user with programming rights
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).getCurrentWiki(); will(returnValue("wiki"));
            allowing(mockDocumentAccessBridge).getCurrentUser(); will(returnValue("dummy"));
            allowing(mockDocumentAccessBridge).setCurrentUser("dummy");
            allowing(mockDocumentAccessBridge).hasProgrammingRights(); will(returnValue(true));
        }});

        Assert.assertFalse(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        Assert.assertTrue(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        Macro< ? > registeredMacro = macroManager.getMacro(new MacroId("testwikimacro"));
        Assert.assertEquals(0, registeredMacro.compareTo(wikiMacro));

        wikiMacroManager.unregisterWikiMacro(wikiMacro.getDocumentReference());
        Assert.assertFalse(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        Assert.assertFalse(macroManager.exists(new MacroId("testwikimacro")));
    }

    @org.junit.Test
    public void testRegisterWikiMacroWhenWikiVisibilityAndAllowed() throws Exception
    {
        final DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.WIKI);

        // Simulate a user with programming rights
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).getCurrentWiki(); will(returnValue("wiki"));
            allowing(mockDocumentAccessBridge).getCurrentUser(); will(returnValue("dummy"));
            allowing(mockDocumentAccessBridge).isDocumentEditable(wikiMacro.getDocumentReference()); will(returnValue(true));
        }});

        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        Assert.assertTrue(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        Macro< ? > registeredMacro = macroManager.getMacro(new MacroId("testwikimacro"));
        Assert.assertEquals(0, registeredMacro.compareTo(wikiMacro));
    }

    @org.junit.Test
    public void testRegisterWikiMacroWhenUserVisibilityAndAllowed() throws Exception
    {
        final DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.USER);

        // Simulate a user with programming rights
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).getCurrentUser(); will(returnValue("dummy"));
            allowing(mockDocumentAccessBridge).isDocumentEditable(wikiMacro.getDocumentReference()); will(returnValue(true));
        }});

        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
        Assert.assertTrue(wikiMacroManager.hasWikiMacro(wikiMacro.getDocumentReference()));

        Macro< ? > registeredMacro = macroManager.getMacro(new MacroId("testwikimacro"));
        Assert.assertEquals(0, registeredMacro.compareTo(wikiMacro));
    }

    @org.junit.Test(expected=InsufficientPrivilegesException.class)
    public void testRegisterWikiMacroWhenGlobalVisibilityAndNotAllowed() throws Exception
    {
        final DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user without programming rights
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).hasProgrammingRights(); will(returnValue(false));
        }});

        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
    }

    @org.junit.Test(expected=InsufficientPrivilegesException.class)
    public void testRegisterWikiMacroWhenWikiVisibilityAndNotAllowed() throws Exception
    {
        final DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.WIKI);

        // Simulate a user without edit rights
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).isDocumentEditable(wikiMacro.getDocumentReference());
                will(returnValue(false));
        }});

        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
    }

    @org.junit.Test(expected=InsufficientPrivilegesException.class)
    public void testRegisterWikiMacroWhenUserVisibilityAndNotAllowed() throws Exception
    {
        final DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.USER);

        // Simulate a user without edit rights
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).isDocumentEditable(wikiMacro.getDocumentReference());
                will(returnValue(false));
        }});

        wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
    }

    private DefaultWikiMacro generateWikiMacro(WikiMacroVisibility visibility) throws Exception
    {
        DocumentReference wikiMacroDocReference = new DocumentReference("xwiki", "Main", "TestWikiMacro");

        WikiMacroDescriptor descriptor = new WikiMacroDescriptor(new MacroId("testwikimacro"), "Test Wiki Macro", "Description", 
            "Test", visibility, new DefaultContentDescriptor(), new ArrayList<WikiMacroParameterDescriptor>());
        DefaultWikiMacro wikiMacro = new DefaultWikiMacro(wikiMacroDocReference, true, descriptor,
            this.xwiki20Parser.parse(new StringReader("== Test ==")), Syntax.XWIKI_2_0, getComponentManager());

        return wikiMacro;
    }
}

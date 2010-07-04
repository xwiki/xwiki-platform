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

import java.util.ArrayList;

import org.jmock.Expectations;
import org.junit.*;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
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

    @Override protected void registerComponents() throws Exception
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

        this.macroManager = getComponentManager().lookup(MacroManager.class);
        this.wikiMacroManager = getComponentManager().lookup(WikiMacroManager.class);
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

    @org.junit.Test
    public void testRegisterWikiMacroWhenGlobalVisibilityAndNotAllowed() throws Exception
    {
        final DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.GLOBAL);

        // Simulate a user without programming rights
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).hasProgrammingRights(); will(returnValue(false));
        }});

        try {
            wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
            Assert.fail("Should have raised an exception here");
        } catch (WikiMacroException e) {
            Assert.assertEquals("Unable to register macro [testwikimacro] in "
                + "[name = [TestWikiMacro], type = [DOCUMENT], parent = [name = [Main], type = [SPACE], "
                + "parent = [name = [xwiki], type = [WIKI], parent = [null]]]] for visibility [GLOBAL] due to "
                + "insufficient privileges", e.getMessage());
        }
    }

    @org.junit.Test
    public void testRegisterWikiMacroWhenWikiVisibilityAndNotAllowed() throws Exception
    {
        final DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.WIKI);

        // Simulate a user without edit rights
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).isDocumentEditable(wikiMacro.getDocumentReference());
                will(returnValue(false));
        }});

        try {
            wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
            Assert.fail("Should have raised an exception here");
        } catch (WikiMacroException e) {
            Assert.assertEquals("Unable to register macro [testwikimacro] in [name = [TestWikiMacro], "
                + "type = [DOCUMENT], parent = [name = [Main], type = [SPACE], parent = [name = [xwiki], "
                + "type = [WIKI], parent = [null]]]] for visibility [WIKI] due to insufficient privileges",
                e.getMessage());
        }
    }

    @org.junit.Test
    public void testRegisterWikiMacroWhenUserVisibilityAndNotAllowed() throws Exception
    {
        final DefaultWikiMacro wikiMacro = generateWikiMacro(WikiMacroVisibility.USER);

        // Simulate a user without edit rights
        getMockery().checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).isDocumentEditable(wikiMacro.getDocumentReference());
                will(returnValue(false));
        }});

        try {
            wikiMacroManager.registerWikiMacro(wikiMacro.getDocumentReference(), wikiMacro);
            Assert.fail("Should have raised an exception here");
        } catch (WikiMacroException e) {
            Assert.assertEquals("Unable to register macro [testwikimacro] in [name = [TestWikiMacro], "
                + "type = [DOCUMENT], parent = [name = [Main], type = [SPACE], parent = [name = [xwiki], "
                + "type = [WIKI], parent = [null]]]] for visibility [USER] due to insufficient privileges",
                e.getMessage());
        }
    }

    private DefaultWikiMacro generateWikiMacro(WikiMacroVisibility visibility) throws Exception
    {
        DocumentReference wikiMacroDocReference = new DocumentReference("xwiki", "Main", "TestWikiMacro");

        WikiMacroDescriptor descriptor = new WikiMacroDescriptor(new MacroId("testwikimacro"), "Test Wiki Macro", "Description", 
            "Test", visibility, new DefaultContentDescriptor(), new ArrayList<WikiMacroParameterDescriptor>());
        DefaultWikiMacro wikiMacro = new DefaultWikiMacro(wikiMacroDocReference, true, descriptor,
            "== Test ==", "xwiki/2.0", getComponentManager());

        return wikiMacro;
    }
}

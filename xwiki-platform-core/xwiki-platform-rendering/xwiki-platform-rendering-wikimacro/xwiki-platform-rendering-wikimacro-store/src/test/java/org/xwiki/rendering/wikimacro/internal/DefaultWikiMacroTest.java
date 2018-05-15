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

import org.jmock.Expectations;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.converter.Converter;
import org.xwiki.rendering.internal.macro.wikibridge.DefaultWikiMacro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroManager;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroVisibility;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;

/**
 * Unit tests for {@link DefaultWikiMacro} in the context of XWiki core.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public class DefaultWikiMacroTest extends AbstractBridgedComponentTestCase
{
    private static final EntityReference XWIKIPREFERENCES_REFERENCE = new EntityReference("XWikiPreferences",
        EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /**
     * Dummy document reference of the document which contains the wiki macro.
     */
    private DocumentReference wikiMacroDocumentReference;

    /**
     * The {@link org.xwiki.rendering.macro.wikibridge.WikiMacroManager} component.
     */
    private WikiMacroManager wikiMacroManager;

    private Parser xwiki20Parser;

    private XWikiDocument wikiMacroDocument;

    private XWikiDocument user;

    private WikiDescriptorManager mockWikiDescriptorManager;

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        this.mockWikiDescriptorManager = registerMockComponent(WikiDescriptorManager.class);
    }

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockWikiDescriptorManager).getCurrentWikiId();
                will(new CustomAction("WikiDescriptorManager#getCurrentWikiId")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        return getContext().getWikiId();
                    }
                });

                allowing(mockWikiDescriptorManager).isMainWiki(with(any(String.class)));
                will(new CustomAction("WikiDescriptorManager#isMainWiki")
                {
                    @Override
                    public Object invoke(Invocation invocation) throws Throwable
                    {
                        return getContext().isMainWiki((String) invocation.getParameter(0));
                    }
                });
            }
        });

        final ContextualAuthorizationManager mockCam = getContextualAuthorizationManager();

        final XWiki mockXWiki = getMockery().mock(XWiki.class);
        final XWikiGroupService mockXWikiGroupService = getMockery().mock(XWikiGroupService.class);

        getContext().setWiki(mockXWiki);

        this.xwiki20Parser = getComponentManager().getInstance(Parser.class, "xwiki/2.0");

        this.wikiMacroDocumentReference = new DocumentReference(getContext().getWikiId(), "space", "macroPage");
        this.wikiMacroManager = getComponentManager().getInstance(WikiMacroManager.class);

        this.wikiMacroDocument = new XWikiDocument(wikiMacroDocumentReference);

        final XWikiRightService rightService = new XWikiRightServiceImpl();

        this.user = new XWikiDocument(new DocumentReference(getContext().getWikiId(), "XWiki", "user"));
        this.user.setNew(false);
        BaseObject userObject = new BaseObject();
        userObject.setXClassReference(new DocumentReference(getContext().getWikiId(), "XWiki", "XWikiusers"));
        this.user.addXObject(userObject);

        this.wikiMacroDocument.setCreatorReference(this.user.getAuthorReference());
        this.wikiMacroDocument.setAuthorReference(this.user.getAuthorReference());
        this.wikiMacroDocument.setContentAuthorReference(this.user.getAuthorReference());

        // Setup an XWikiPreferences document granting programming rights to user
        final XWikiDocument prefs =
            new XWikiDocument(new DocumentReference(getContext().getWikiId(), "XWiki", "XWikiPreferences"));
        final BaseObject mockGlobalRightObj = getMockery().mock(BaseObject.class);

        getMockery().checking(new Expectations()
        {
            {
                allowing(mockCam).hasAccess(Right.PROGRAM);
                will(returnValue(true));

                allowing(mockXWiki).getDocument(with(equal(wikiMacroDocumentReference)), with(any(XWikiContext.class)));
                will(returnValue(wikiMacroDocument));

                allowing(mockXWiki).isReadOnly();
                will(returnValue(false));
                allowing(mockXWiki).getLanguagePreference(with(any(XWikiContext.class)));
                will(returnValue(null));
                allowing(mockXWiki).getRightService();
                will(returnValue(rightService));
                allowing(mockXWiki).getGroupService(with(any(XWikiContext.class)));
                will(returnValue(mockXWikiGroupService));

                allowing(mockXWikiGroupService).getAllGroupsReferencesForMember(with(any(DocumentReference.class)),
                    with(any(int.class)), with(any(int.class)), with(any(XWikiContext.class)));
                will(returnValue(Collections.EMPTY_LIST));

                allowing(mockXWiki).getDocument(with(equal(XWIKIPREFERENCES_REFERENCE)), with(any(XWikiContext.class)));
                will(returnValue(prefs));
                allowing(mockGlobalRightObj).getStringValue("levels");
                will(returnValue("programming"));
                allowing(mockGlobalRightObj).getStringValue("users");
                will(returnValue(user.getFullName()));
                allowing(mockGlobalRightObj).getIntValue("allow");
                will(returnValue(1));
                allowing(mockGlobalRightObj).setNumber(with(any(int.class)));
                allowing(mockGlobalRightObj).setDocumentReference(with(any(DocumentReference.class)));
                allowing(mockGlobalRightObj).setOwnerDocument(with(any(XWikiDocument.class)));
            }
        });

        prefs.addObject("XWiki.XWikiGlobalRights", mockGlobalRightObj);

        getContext().setUserReference(this.user.getDocumentReference());
    }

    @Test
    public void testExecuteWhenWikiMacroBinding() throws Exception
    {
        registerWikiMacro("wikimacrobindings", "{{groovy}}" + "print xcontext.macro.doc" + "{{/groovy}}");

        Converter converter = getComponentManager().getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();
        converter.convert(new StringReader("{{wikimacrobindings param1=\"value2\" param2=\"value2\"/}}"),
            Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        Assert.assertEquals("<p>" + this.wikiMacroDocument.toString() + "</p>", printer.toString());
    }

    @Test
    public void testExecuteWhenWikiRequiringPRAfterDropPermission() throws Exception
    {
        registerWikiMacro("wikimacrobindings", "{{groovy}}" + "print xcontext.macro.doc" + "{{/groovy}}");

        Converter converter = getComponentManager().getInstance(Converter.class);

        DefaultWikiPrinter printer = new DefaultWikiPrinter();

        getContext().dropPermissions();
        this.wikiMacroDocument.newDocument(getContext()).dropPermissions();

        converter.convert(new StringReader("{{wikimacrobindings param1=\"value2\" param2=\"value2\"/}}"),
            Syntax.XWIKI_2_0, Syntax.XHTML_1_0, printer);

        // Note: We're using XHTML as the output syntax just to make it easy for asserting.
        Assert.assertEquals("<p>" + this.wikiMacroDocument.toString() + "</p>", printer.toString());
        Assert.assertTrue("Wiki macro did not properly restord persmission dropping", getContext()
            .hasDroppedPermissions());
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
            new DefaultWikiMacro(wikiMacroDocumentReference, this.user.getDocumentReference(), true, descriptor,
                this.xwiki20Parser.parse(new StringReader(macroContent)), Syntax.XWIKI_2_0, getComponentManager());

        this.wikiMacroManager.registerWikiMacro(wikiMacroDocumentReference, wikiMacro);
    }
}

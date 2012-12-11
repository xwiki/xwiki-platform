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
package org.xwiki.uiextension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Provider;

import org.jmock.Expectations;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;
import org.xwiki.uiextension.internal.WikiUIExtension;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;
import org.xwiki.uiextension.internal.filter.SortByIdFilter;
import org.xwiki.uiextension.internal.scripting.UIExtensionScriptService;

import junit.framework.Assert;

@MockingRequirement(value = UIExtensionScriptService.class)
public class UIExtensionScriptServiceTest extends AbstractMockingComponentTestCase
    implements WikiUIExtensionConstants
{
    private static final DocumentReference CLASS_REF = new DocumentReference("xwiki", "XWiki", "UIExtensionClass");

    private static final DocumentReference DOC_REF = new DocumentReference("xwiki", "XWiki", "MyUIExtension");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private ObjectReference OBJ1_REF = new ObjectReference(CLASS_REF + "[1]", DOC_REF);

    private ObjectReference OBJ2_REF = new ObjectReference(CLASS_REF + "[2]", DOC_REF);

    private ObjectReference OBJ3_REF = new ObjectReference(CLASS_REF + "[3]", DOC_REF);

    private ObjectReference OBJ4_REF = new ObjectReference(CLASS_REF + "[4]", DOC_REF);

    private ComponentManager contextComponentManager;

    private UIExtensionScriptService service;

    private List<WikiUIExtension> ep1Extensions = new ArrayList<WikiUIExtension>();

    private UIExtensionManager uiExtensionManager;

    XDOM xdom = new XDOM(new ArrayList<Block>());

    @Before
    public void configure() throws Exception
    {
        contextComponentManager =
            getComponentManager().registerMockComponent(getMockery(), ComponentManager.class, "context", "context");
        final Provider<ComponentManager> componentManagerProvider =
            getComponentManager().getInstance(
                new DefaultParameterizedType(null, Provider.class, ComponentManager.class), "context");
        this.service = getComponentManager().getInstance(ScriptService.class, "uix");
        this.uiExtensionManager = getComponentManager().getInstance(UIExtensionManager.class);

        ep1Extensions.add(new WikiUIExtension("1id3", "epId1", OBJ1_REF, AUTHOR_REFERENCE, getComponentManager()));
        ep1Extensions.add(new WikiUIExtension("1id1", "epId1", OBJ2_REF, AUTHOR_REFERENCE, getComponentManager()));
        ep1Extensions.add(new WikiUIExtension("1id2", "epId1", OBJ3_REF, AUTHOR_REFERENCE, getComponentManager()));
        for (WikiUIExtension uiExtension : ep1Extensions) {
            uiExtension.setXDOM(xdom);
            uiExtension.setSyntax(Syntax.XWIKI_2_1);
            uiExtension.setParameters(new HashMap<String, String>());
            uiExtension.setScope(WikiComponentScope.WIKI);
        }

        getMockery().checking(new Expectations()
        {
            {
                allowing(componentManagerProvider).get();
                will(returnValue(contextComponentManager));
            }
        });
    }

    @Test
    public void verifyExtensionsAreSortedAlphabeticallyById() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(contextComponentManager).getInstance(UIExtensionManager.class, "epId1");
                will(throwException(new ComponentLookupException("doesn't matter")));
                oneOf(contextComponentManager).getInstance(UIExtensionFilter.class, "sortById");
                will(returnValue(new SortByIdFilter()));
                oneOf(uiExtensionManager).get("epId1");
                will(returnValue(ep1Extensions));
            }
        });

        List<UIExtension> extensions = new ArrayList<UIExtension>();
        HashMap<String, String> filters = new HashMap<String, String>();
        filters.put("sortById", "");
        extensions.addAll(this.service.getExtensions("epId1", filters));

        Assert.assertEquals("1id1", extensions.get(0).getId());
        Assert.assertEquals("1id2", extensions.get(1).getId());
        Assert.assertEquals("1id3", extensions.get(2).getId());
    }
}

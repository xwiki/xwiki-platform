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
package org.xwiki.component.wiki;

import java.util.ArrayList;
import java.util.Vector;

import javax.inject.Provider;

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiComponentBridge;
import org.xwiki.component.wiki.internal.bridge.WikiComponentBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.jmock.annotation.MockingRequirement;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

@ComponentList({
    DefaultModelContext.class,
    DefaultModelConfiguration.class,
    LocalStringEntityReferenceSerializer.class,
    RelativeStringEntityReferenceResolver.class,
    CurrentReferenceDocumentReferenceResolver.class,
    CurrentReferenceEntityReferenceResolver.class,
    CurrentEntityReferenceValueProvider.class,
    CurrentMixedStringDocumentReferenceResolver.class,
    CurrentMixedEntityReferenceValueProvider.class,
    DefaultEntityReferenceValueProvider.class,
    CompactWikiStringEntityReferenceSerializer.class
})
@MockingRequirement(value = DefaultWikiComponentBridge.class,
exceptions = {EntityReferenceSerializer.class, Parser.class})
public class DefaultWikiComponentBridgeTest extends AbstractMockingComponentTestCase implements WikiComponentConstants
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private XWiki xwiki;

    private XWikiContext xwikiContext;

    private XWikiDocument componentDoc;

    private BaseObject componentObject;

    private WikiComponentBridge bridge;

    @Before
    public void configure() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);

        Utils.setComponentManager(getComponentManager());

        final Execution execution = registerMockComponent(Execution.class);
        final ExecutionContext context = new ExecutionContext();

        final Provider<XWikiContext> xcontextProvider = registerMockComponent(XWikiContext.TYPE_PROVIDER);

        this.xwiki = getMockery().mock(XWiki.class);

        this.xwikiContext = new XWikiContext();
        this.xwikiContext.setWikiId("xwiki");
        this.xwikiContext.setWiki(this.xwiki);

        context.setProperty("xwikicontext", this.xwikiContext);

        this.componentDoc = getMockery().mock(XWikiDocument.class);
        this.componentObject = getMockery().mock(BaseObject.class, "component");

        getMockery().checking(new Expectations()
        {
            {
                allowing(xcontextProvider).get();
                will(returnValue(xwikiContext));
                allowing(execution).getContext();
                will(returnValue(context));
                allowing(xwiki).getDocument(DOC_REFERENCE, xwikiContext);
                will(returnValue(componentDoc));
            }
        });

        this.bridge = getComponentManager().getInstance(WikiComponentBridge.class);
    }

    @Test
    public void getAuthorReference() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(componentDoc).getAuthorReference();
                will(returnValue(AUTHOR_REFERENCE));
            }
        });

        Assert.assertEquals(AUTHOR_REFERENCE, bridge.getAuthorReference(DOC_REFERENCE));
    }

    @Test
    public void getDeclaredInterfaces() throws Exception
    {
        final BaseObject interfaceObject = getMockery().mock(BaseObject.class, "interface");
        final Vector<BaseObject> interfaceObjects = new Vector<BaseObject>();
        interfaceObjects.add(interfaceObject);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObjectNumbers(INTERFACE_CLASS);
                will(returnValue(1));
                oneOf(componentDoc).getObjects(INTERFACE_CLASS);
                will(returnValue(interfaceObjects));
                allowing(interfaceObject).getStringValue(INTERFACE_NAME_FIELD);
                will(returnValue("org.xwiki.component.phase.Initializable"));
            }
        });

        Assert.assertEquals(1, bridge.getDeclaredInterfaces(DOC_REFERENCE).size());
    }

    @Test
    public void getDependencies() throws Exception
    {
        final BaseObject dependencyObject = getMockery().mock(BaseObject.class, "dependency");
        final Vector<BaseObject> dependencyObjects = new Vector<BaseObject>();
        dependencyObjects.add(dependencyObject);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObjectNumbers(DEPENDENCY_CLASS);
                will(returnValue(1));
                oneOf(componentDoc).getObjects(DEPENDENCY_CLASS);
                will(returnValue(dependencyObjects));
                allowing(dependencyObject).getStringValue(COMPONENT_ROLE_TYPE_FIELD);
                will(returnValue("org.xwiki.component.wiki.TestRole"));
                allowing(dependencyObject).getStringValue(COMPONENT_ROLE_HINT_FIELD);
                will(returnValue("default"));
                allowing(dependencyObject).getStringValue(DEPENDENCY_BINDING_NAME_FIELD);
                will(returnValue("test"));
            }
        });

        Assert.assertEquals(1, bridge.getDependencies(DOC_REFERENCE).size());
    }

    @Test
    public void getHandledMethods() throws Exception
    {
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class);
        final ContentParser contentParser = getComponentManager().getInstance(ContentParser.class);
        final Parser parser = getMockery().mock(Parser.class);
        final XDOM xdom = new XDOM(new ArrayList<Block>());
        final BaseObject methodObject = getMockery().mock(BaseObject.class, "method");
        final Vector<BaseObject> methodObjects = new Vector<BaseObject>();
        methodObjects.add(methodObject);

        getMockery().checking(new Expectations()
        {
            {

                oneOf(componentDoc).getObjectNumbers(METHOD_CLASS);
                will(returnValue(1));
                oneOf(componentDoc).getObjects(METHOD_CLASS);
                will(returnValue(methodObjects));
                allowing(methodObject).getStringValue(METHOD_NAME_FIELD);
                will(returnValue("test"));
                allowing(methodObject).getStringValue(METHOD_CODE_FIELD);
                will(returnValue("test"));
                oneOf(componentDoc).getSyntax();
                will(returnValue(Syntax.XWIKI_2_1));
                oneOf(contentParser).parse("test", Syntax.XWIKI_2_1, DOC_REFERENCE);
                will(returnValue(xdom));
            }
        });

        Assert.assertEquals(1, bridge.getHandledMethods(DOC_REFERENCE).size());
    }

    @Test
    public void getRoleHint() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(componentObject));
                oneOf(componentObject).getStringValue(COMPONENT_ROLE_HINT_FIELD);
                will(returnValue("roleHint"));
            }
        });

        Assert.assertEquals("roleHint", bridge.getRoleHint(DOC_REFERENCE));
    }

    @Test
    public void getRoleType() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(componentObject));
                oneOf(componentObject).getStringValue(COMPONENT_ROLE_TYPE_FIELD);
                will(returnValue("org.xwiki.component.wiki.TestRole"));
            }
        });

        Assert.assertEquals(TestRole.class, bridge.getRoleType(DOC_REFERENCE));
    }

    @Test
    public void getRoleTypeWithoutComponentObject() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(null));
            }
        });

        try {
            bridge.getRoleType(DOC_REFERENCE);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("No component object could be found in document [xwiki:XWiki.MyComponent]",
                expected.getMessage());
        }
    }

    @Test
    public void getRoleTypeWithWrongRole() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(componentObject));
                oneOf(componentObject).getStringValue(COMPONENT_ROLE_TYPE_FIELD);
                will(returnValue("org.xwiki.component.wiki.DoesNotExist"));
            }
        });

        try {
            this.bridge.getRoleType(DOC_REFERENCE);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("The role type [org.xwiki.component.wiki.DoesNotExist] does not exist",
                expected.getMessage());
        }
    }

    @Test
    public void getScope() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(componentObject));
                oneOf(componentObject).getStringValue(COMPONENT_SCOPE_FIELD);
                will(returnValue("user"));
            }
        });

        Assert.assertEquals(WikiComponentScope.USER, bridge.getScope(DOC_REFERENCE));
    }

    @Test
    public void getScopeWithWrongScope() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(componentObject));
                oneOf(componentObject).getStringValue(COMPONENT_SCOPE_FIELD);
                will(returnValue("doesnotexist"));
            }
        });

        // Wiki is the default value
        Assert.assertEquals(WikiComponentScope.WIKI, this.bridge.getScope(DOC_REFERENCE));
    }

    @Test
    public void getSyntax() throws Exception
    {
        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getSyntax();
                will(returnValue(Syntax.XWIKI_2_1));
            }
        });

        Assert.assertEquals(Syntax.XWIKI_2_1, bridge.getSyntax(DOC_REFERENCE));
    }
}

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

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.DefaultWikiComponentBuilder;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
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
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.annotation.MockingRequirement;

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
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

import junit.framework.Assert;

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
@MockingRequirement(value = DefaultWikiComponentBuilder.class,
    exceptions = {EntityReferenceSerializer.class, Parser.class})
public class DefaultWikiComponentBuilderTest extends AbstractMockingComponentTestCase implements WikiComponentConstants
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final String ROLE_HINT = "roleHint";

    private XWiki xwiki;

    private XWikiContext xwikiContext;

    private XWikiDocument componentDoc;

    private WikiComponentBuilder provider;

    @Before
    public void configure() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);

        Utils.setComponentManager(getComponentManager());

        final Execution execution = getComponentManager().getInstance(Execution.class);
        final ExecutionContext context = new ExecutionContext();

        this.xwiki = getMockery().mock(XWiki.class);

        this.xwikiContext = new XWikiContext();
        this.xwikiContext.setDatabase("xwiki");
        this.xwikiContext.setWiki(this.xwiki);

        context.setProperty("xwikicontext", this.xwikiContext);

        this.componentDoc = getMockery().mock(XWikiDocument.class);

        getMockery().checking(new Expectations()
        {
            {
                allowing(execution).getContext();
                will(returnValue(context));
                allowing(xwiki).getDocument(DOC_REFERENCE, xwikiContext);
                will(returnValue(componentDoc));
                allowing(componentDoc).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));
            }
        });

        this.provider = getComponentManager().getInstance(WikiComponentBuilder.class);
    }

    @Test
    public void buildComponentsWithoutComponentObject() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(null));
                oneOf(xwiki).getRightService();
                will(returnValue(rightService));
                oneOf(rightService).hasProgrammingRights(componentDoc, xwikiContext);
                will(returnValue(true));
            }
        });

        try {
            this.provider.buildComponents(DOC_REFERENCE);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("No component object could be found", expected.getMessage());
        }
    }

    @Test
    public void buildComponentsWithoutProgrammingRights() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(null));
                oneOf(xwiki).getRightService();
                will(returnValue(rightService));
                oneOf(rightService).hasProgrammingRights(componentDoc, xwikiContext);
                will(returnValue(false));
            }
        });

        try {
            this.provider.buildComponents(DOC_REFERENCE);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("Registering wiki components requires programming rights", expected.getMessage());
        }
    }

    @Test
    public void buildComponentsWithEmptyRole() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);
        final BaseObject componentObject = getMockery().mock(BaseObject.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(componentObject));
                oneOf(componentObject).getStringValue(COMPONENT_ROLE_TYPE_FIELD);
                will(returnValue(""));
                oneOf(xwiki).getRightService();
                will(returnValue(rightService));
                oneOf(rightService).hasProgrammingRights(componentDoc, xwikiContext);
                will(returnValue(true));
            }
        });

        try {
            this.provider.buildComponents(DOC_REFERENCE);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("No role were precised in the component", expected.getMessage());
        }
    }

    @Test
    public void buildComponentsWithWrongRole() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);
        final BaseObject componentObject = getMockery().mock(BaseObject.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(componentObject));
                oneOf(componentObject).getStringValue(COMPONENT_ROLE_TYPE_FIELD);
                will(returnValue("a.class.that.does.not.Exist"));
                oneOf(xwiki).getRightService();
                will(returnValue(rightService));
                oneOf(rightService).hasProgrammingRights(componentDoc, xwikiContext);
                will(returnValue(true));
            }
        });

        try {
            this.provider.buildComponents(DOC_REFERENCE);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("The role class could not be found", expected.getMessage());
        }
    }

    @Test
    public void buildComponentsWithWrongInterface() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class);
        final Parser parser = getMockery().mock(Parser.class);
        final BaseObject componentObject = getMockery().mock(BaseObject.class, "component");
        final BaseObject methodObject = getMockery().mock(BaseObject.class, "method");
        final BaseObject interfaceObject = getMockery().mock(BaseObject.class, "interface");
        final Vector<BaseObject> methodObjects = new Vector<BaseObject>();
        final Vector<BaseObject> interfaceObjects = new Vector<BaseObject>();
        final XDOM xdom = new XDOM(new ArrayList<Block>());
        methodObjects.add(methodObject);
        interfaceObjects.add(interfaceObject);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(componentObject));
                allowing(componentDoc).getPrefixedFullName();
                will(returnValue("xwiki:XWiki.MyComponent"));
                oneOf(componentObject).getStringValue(COMPONENT_ROLE_TYPE_FIELD);
                will(returnValue(TestRole.class.getCanonicalName()));
                oneOf(componentObject).getStringValue(COMPONENT_ROLE_HINT_FIELD);
                will(returnValue(ROLE_HINT));
                oneOf(componentDoc).getObjectNumbers(METHOD_CLASS);
                will(returnValue(1));
                oneOf(componentDoc).getObjectNumbers(INTERFACE_CLASS);
                will(returnValue(1));
                oneOf(componentDoc).getObjects(METHOD_CLASS);
                will(returnValue(methodObjects));
                oneOf(componentDoc).getObjects(INTERFACE_CLASS);
                will(returnValue(interfaceObjects));
                allowing(methodObject).getStringValue(METHOD_NAME_FIELD);
                will(returnValue("test"));
                allowing(interfaceObject).getStringValue(INTERFACE_NAME_FIELD);
                will(returnValue("an.interface.which.does.not.Exist"));
                oneOf(methodObject).getStringValue(METHOD_CODE_FIELD);
                will(returnValue(""));
                oneOf(xwiki).getRightService();
                will(returnValue(rightService));
                oneOf(rightService).hasProgrammingRights(componentDoc, xwikiContext);
                will(returnValue(true));
                oneOf(parser).parse(with(any(Reader.class)));
                will(returnValue(xdom));
                oneOf(componentManager).getInstance(Parser.class, Syntax.XWIKI_2_0.toIdString());
                will(returnValue(parser));
                oneOf(getMockLogger()).warn("Interface [{}] not found, declared for wiki component [{}]",
                    "an.interface.which.does.not.Exist", "xwiki:XWiki.MyComponent");
            }
        });

        List<WikiComponent> components = this.provider.buildComponents(DOC_REFERENCE);

        Assert.assertEquals(1, components.size());
        Assert.assertEquals(1, components.get(0).getHandledMethods().size());
        Assert.assertEquals(0, components.get(0).getImplementedInterfaces().size());
    }

    @Test
    public void buildComponents() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class);
        final Parser parser = getMockery().mock(Parser.class);
        final BaseObject componentObject = getMockery().mock(BaseObject.class, "component");
        final BaseObject methodObject = getMockery().mock(BaseObject.class, "method");
        final BaseObject interfaceObject = getMockery().mock(BaseObject.class, "interface");
        final Vector<BaseObject> methodObjects = new Vector<BaseObject>();
        final Vector<BaseObject> interfaceObjects = new Vector<BaseObject>();
        final XDOM xdom = new XDOM(new ArrayList<Block>());
        methodObjects.add(methodObject);
        interfaceObjects.add(interfaceObject);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getObject(COMPONENT_CLASS);
                will(returnValue(componentObject));
                oneOf(componentObject).getStringValue(COMPONENT_ROLE_TYPE_FIELD);
                will(returnValue(TestRole.class.getCanonicalName()));
                oneOf(componentObject).getStringValue(COMPONENT_ROLE_HINT_FIELD);
                will(returnValue(ROLE_HINT));
                oneOf(componentDoc).getObjectNumbers(METHOD_CLASS);
                will(returnValue(1));
                oneOf(componentDoc).getObjectNumbers(INTERFACE_CLASS);
                will(returnValue(1));
                oneOf(componentDoc).getObjects(METHOD_CLASS);
                will(returnValue(methodObjects));
                oneOf(componentDoc).getObjects(INTERFACE_CLASS);
                will(returnValue(interfaceObjects));
                allowing(methodObject).getStringValue(METHOD_NAME_FIELD);
                will(returnValue("test"));
                allowing(interfaceObject).getStringValue(INTERFACE_NAME_FIELD);
                will(returnValue("org.xwiki.component.phase.Initializable"));
                oneOf(methodObject).getStringValue(METHOD_CODE_FIELD);
                will(returnValue(""));
                oneOf(xwiki).getRightService();
                will(returnValue(rightService));
                oneOf(rightService).hasProgrammingRights(componentDoc, xwikiContext);
                will(returnValue(true));
                oneOf(parser).parse(with(any(Reader.class)));
                will(returnValue(xdom));
                oneOf(componentManager).getInstance(Parser.class, Syntax.XWIKI_2_0.toIdString());
                will(returnValue(parser));
            }
        });

        List<WikiComponent> components = this.provider.buildComponents(DOC_REFERENCE);

        Assert.assertEquals(1, components.size());
        Assert.assertEquals(1, components.get(0).getHandledMethods().size());
        Assert.assertEquals(1, components.get(0).getImplementedInterfaces().size());
    }
}

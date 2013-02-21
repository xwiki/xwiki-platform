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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.velocity.VelocityContext;
import org.jmock.Expectations;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.ModelContext;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.model.internal.reference.DefaultEntityReferenceValueProvider;
import org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.jmock.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.jmock.annotation.MockingRequirement;
import org.xwiki.uiextension.internal.WikiUIExtension;
import org.xwiki.uiextension.internal.WikiUIExtensionComponentBuilder;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedEntityReferenceValueProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
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
    CompactWikiStringEntityReferenceSerializer.class,
    DefaultStringDocumentReferenceResolver.class,
    DefaultStringEntityReferenceResolver.class,
    DefaultStringEntityReferenceResolver.class,
    DefaultStringEntityReferenceSerializer.class
})
@MockingRequirement(value = WikiUIExtensionComponentBuilder.class,
    exceptions = {EntityReferenceSerializer.class})
public class WikiUIExtensionComponentBuilderTest extends AbstractMockingComponentTestCase
    implements WikiUIExtensionConstants
{
    private static final DocumentReference DOC_REF = new DocumentReference("xwiki", "XWiki", "MyUIExtension");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private XWiki xwiki;

    private XWikiContext xwikiContext;

    private XWikiDocument componentDoc;

    private WikiUIExtensionComponentBuilder builder;

    private Execution execution;

    @Before
    public void configure() throws Exception
    {
        getMockery().setImposteriser(ClassImposteriser.INSTANCE);

        Utils.setComponentManager(getComponentManager());

        execution = getComponentManager().getInstance(Execution.class);
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

                allowing(xwiki).getDocument(DOC_REF, xwikiContext);
                will(returnValue(componentDoc));
                allowing(componentDoc).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));
            }
        });

        this.builder = getComponentManager().getInstance(WikiComponentBuilder.class, "uiextension");
    }

    @Test
    public void buildExtensionsWithoutExtensionObject() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getXObjects(UI_EXTENSION_CLASS);
                will(returnValue(new ArrayList()));
                oneOf(componentDoc).getPrefixedFullName();
                will(returnValue("xwiki:XWiki.MyUIExtension"));
            }
        });

        try {
            this.builder.buildComponents(DOC_REF);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("No UI extension object could be found in document [xwiki:XWiki.MyUIExtension]",
                expected.getMessage());
        }
    }

    @Test
    public void buildExtensionsWithoutAdminRights() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);
        final BaseObject extensionObject = getMockery().mock(BaseObject.class, "uiextension");
        final Vector<BaseObject> extensionObjects = new Vector<BaseObject>();
        extensionObjects.add(extensionObject);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getXObjects(UI_EXTENSION_CLASS);
                will(returnValue(extensionObjects));
                oneOf(extensionObject).getStringValue(ID_PROPERTY);
                will(returnValue("name"));
                oneOf(extensionObject).getStringValue(EXTENSION_POINT_ID_PROPERTY);
                will(returnValue("extensionPointId"));
                oneOf(extensionObject).getStringValue(CONTENT_PROPERTY);
                will(returnValue("content"));
                oneOf(extensionObject).getStringValue(PARAMETERS_PROPERTY);
                will(returnValue("key=value=foo\nkey2=value2\ninvalid=\n\n=invalid"));
                oneOf(extensionObject).getStringValue(SCOPE_PROPERTY);
                will(returnValue("wiki"));
                oneOf(componentDoc).getContentAuthor();
                will(returnValue("XWiki.Admin"));
                oneOf(xwiki).getRightService();
                will(returnValue(rightService));
                oneOf(rightService).hasAccessLevel("admin", "XWiki.Admin", "XWiki.XWikiPreferences", xwikiContext);
                will(returnValue(false));
            }
        });

        try {
            this.builder.buildComponents(DOC_REF);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("Registering UI extensions requires admin rights", expected.getMessage());
        }
    }

    @Test
    public void buildComponentsWithoutRightService() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);
        final BaseObject extensionObject = getMockery().mock(BaseObject.class, "uiextension");
        final Vector<BaseObject> extensionObjects = new Vector<BaseObject>();
        extensionObjects.add(extensionObject);

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getXObjects(UI_EXTENSION_CLASS);
                will(returnValue(extensionObjects));
                oneOf(extensionObject).getStringValue(ID_PROPERTY);
                will(returnValue("name"));
                oneOf(extensionObject).getStringValue(EXTENSION_POINT_ID_PROPERTY);
                will(returnValue("extensionPointId"));
                oneOf(extensionObject).getStringValue(CONTENT_PROPERTY);
                will(returnValue("content"));
                oneOf(extensionObject).getStringValue(PARAMETERS_PROPERTY);
                will(returnValue("key=value=foo\nkey2=value2\ninvalid=\n\n=invalid"));
                oneOf(extensionObject).getStringValue(SCOPE_PROPERTY);
                will(returnValue("wiki"));
                oneOf(componentDoc).getContentAuthor();
                will(returnValue("XWiki.Admin"));
                oneOf(xwiki).getRightService();
                will(returnValue(rightService));
                oneOf(rightService).hasAccessLevel("admin", "XWiki.Admin", "XWiki.XWikiPreferences", xwikiContext);
                will(throwException(new XWikiException()));
            }
        });

        try {
            this.builder.buildComponents(DOC_REF);
            Assert.fail("Should have thrown an exception");
        } catch (WikiComponentException expected) {
            Assert.assertEquals("Failed to check rights required to register UI Extension(s)", expected.getMessage());
        }
    }


    @Test
    public void buildComponents() throws Exception
    {
        final XWikiRightService rightService = getMockery().mock(XWikiRightService.class);
        final ComponentManager componentManager = getComponentManager().getInstance(ComponentManager.class, "wiki");
        final Transformation transformation = getMockery().mock(Transformation.class, "macro");
        final ModelContext modelContext = getMockery().mock(ModelContext.class);
        final ContentParser contentParser = getMockery().mock(ContentParser.class);
        final VelocityManager velocityManager = getMockery().mock(VelocityManager.class);
        final VelocityEngine velocityEngine = getMockery().mock(VelocityEngine.class);
        final VelocityContext velocityContext = new VelocityContext();

        final BaseObject extensionObject = getMockery().mock(BaseObject.class, "uiextension");
        final Vector<BaseObject> extensionObjects = new Vector<BaseObject>();
        extensionObjects.add(extensionObject);

        final ObjectReference extensionReference = new BaseObjectReference(DOC_REF, 1, DOC_REF);
        final StringWriter writer = new StringWriter();
        writer.append("value=foo");
        final XDOM xdom = new XDOM(new ArrayList<Block>());

        getMockery().checking(new Expectations()
        {
            {
                oneOf(componentDoc).getXObjects(UI_EXTENSION_CLASS);
                will(returnValue(extensionObjects));
                oneOf(componentDoc).getAuthorReference();
                will(returnValue(AUTHOR_REFERENCE));
                oneOf(xwiki).getRightService();
                will(returnValue(rightService));
                oneOf(componentDoc).getContentAuthor();
                will(returnValue("XWiki.Admin"));
                oneOf(rightService).hasAccessLevel("admin", "XWiki.Admin", "XWiki.XWikiPreferences", xwikiContext);
                will(returnValue(true));
                allowing(extensionObject).getReference();
                will(returnValue(extensionReference));
                oneOf(extensionObject).getStringValue(ID_PROPERTY);
                will(returnValue("name"));
                oneOf(extensionObject).getStringValue(EXTENSION_POINT_ID_PROPERTY);
                will(returnValue("extensionPointId"));
                oneOf(extensionObject).getStringValue(CONTENT_PROPERTY);
                will(returnValue("content"));
                oneOf(extensionObject).getStringValue(PARAMETERS_PROPERTY);
                will(returnValue("key=value=foo\nkey2=value2\ninvalid=\n\n=invalid"));
                oneOf(extensionObject).getStringValue(SCOPE_PROPERTY);
                will(returnValue("wiki"));
                oneOf(contentParser).parse("content", Syntax.XWIKI_2_0);
                will(returnValue(xdom));
                oneOf(componentManager).getInstance(Transformation.class, "macro");
                will(returnValue(transformation));
                oneOf(componentManager).getInstance(Execution.class);
                will(returnValue(execution));
                oneOf(componentManager).getInstance(ModelContext.class);
                will(returnValue(modelContext));
                oneOf(modelContext).getCurrentEntityReference();
                will(returnValue(new WikiReference("xwiki")));
                oneOf(componentManager).getInstance(ContentParser.class);
                will(returnValue(contentParser));
                oneOf(componentManager).getInstance(VelocityManager.class);
                will(returnValue(velocityManager));
                allowing(velocityManager).getVelocityEngine();
                will(returnValue(velocityEngine));
                allowing(velocityManager).getVelocityContext();
                will(returnValue(velocityContext));
                oneOf(velocityEngine).evaluate(with(any(VelocityContext.class)), with(any(StringWriter.class)),
                    with(equal("")), with(equal("value=foo")));
                oneOf(velocityEngine).evaluate(with(any(VelocityContext.class)), with(any(StringWriter.class)),
                    with(equal("")), with(equal("value2")));
                will(returnValue(true));
            }
        });

        List<WikiComponent> components = this.builder.buildComponents(DOC_REF);
        Assert.assertEquals(1, components.size());

        UIExtension uiExtension = (WikiUIExtension) components.get(0);
        Map<String, String> parameters = uiExtension.getParameters();
        Assert.assertEquals(2, parameters.size());
    }
}

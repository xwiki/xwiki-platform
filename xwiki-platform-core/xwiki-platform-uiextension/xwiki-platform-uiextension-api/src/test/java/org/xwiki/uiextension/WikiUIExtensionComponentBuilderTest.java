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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.internal.multi.DelegateComponentManager;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.component.wiki.internal.bridge.WikiBaseObjectComponentBuilder;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.uiextension.internal.WikiUIExtensionComponentBuilder;
import org.xwiki.uiextension.internal.WikiUIExtensionConstants;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WikiUIExtensionComponentBuilderTest implements WikiUIExtensionConstants
{
    public MockitoComponentMockingRule<WikiBaseObjectComponentBuilder> mocker =
        new MockitoComponentMockingRule<WikiBaseObjectComponentBuilder>(WikiUIExtensionComponentBuilder.class);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(mocker);

    private static final DocumentReference DOC_REF =
        new DocumentReference("xwiki", "XWiki", "MyUIExtension", Locale.ROOT);

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    private XWikiDocument componentDoc;

    @Before
    public void configure() throws Exception
    {
        // Required by BaseObjectReference
        DocumentReferenceResolver<String> resolver =
            this.mocker.registerMockComponent(DocumentReferenceResolver.TYPE_STRING);
        when(resolver.resolve("XWiki.UIExtension"))
            .thenReturn(new DocumentReference("xwiki", "XWiki", "UIExtensionClass"));

        DelegateComponentManager wikiComponentManager = new DelegateComponentManager();
        wikiComponentManager.setComponentManager(this.mocker);
        this.mocker.registerComponent(ComponentManager.class, "wiki", wikiComponentManager);

        // Components accessed through dynamic lookup.
        VelocityManager velocityManager = this.mocker.registerMockComponent(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(mock(VelocityEngine.class));
        when(velocityManager.getVelocityContext()).thenReturn(mock(VelocityContext.class));

        ModelContext modelContext = this.mocker.registerMockComponent(ModelContext.class);
        when(modelContext.getCurrentEntityReference()).thenReturn(DOC_REF);

        this.mocker.registerMockComponent(RenderingContext.class);
        this.mocker.registerMockComponent(Transformation.class, "macro");
        this.mocker.registerMockComponent(ContentParser.class);

        // The document holding the UI extension object.
        this.componentDoc = mock(XWikiDocument.class, "xwiki:XWiki.MyUIExtension");
        when(this.componentDoc.getDocumentReference()).thenReturn(DOC_REF);
        when(this.componentDoc.getAuthorReference()).thenReturn(AUTHOR_REFERENCE);
        when(this.componentDoc.getContentAuthorReference()).thenReturn(AUTHOR_REFERENCE);
        when(this.componentDoc.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        this.oldcore.getDocuments().put(DOC_REF, componentDoc);
    }

    @Test
    public void buildGlobalComponentsWithoutPR() throws Exception
    {
        BaseObject extensionObject = createExtensionObject("id", "extensionPointId", "content", "parameters", "global");

        try {
            this.mocker.getComponentUnderTest().buildComponents(extensionObject);
            fail("You shouldn't be able to register global UI extensions without PR.");
        } catch (WikiComponentException expected) {
            assertEquals("Registering global UI extensions requires programming rights", expected.getMessage());
        }
    }

    @Test
    public void buildWikiLevelComponentsWithoutAdminRights() throws Exception
    {
        BaseObject extensionObject = createExtensionObject("id", "extensionPointId", "content", "parameters", "wiki");

        try {
            this.mocker.getComponentUnderTest().buildComponents(extensionObject);
            fail("You shouldn't be able to register UI extensions at wiki level without wiki admin rights.");
        } catch (WikiComponentException expected) {
            assertEquals("Registering UI extensions at wiki level requires wiki administration rights",
                expected.getMessage());
        }
    }

    @Test
    public void buildComponents() throws Exception
    {
        BaseObject extensionObject = createExtensionObject("name", "extensionPointId", "content",
            "key=value=foo\nkey2=value2\ninvalid=\n\n=invalid", "user");

        ContentParser contentParser = this.mocker.getInstance(ContentParser.class);
        VelocityManager velocityManager = this.mocker.getInstance(VelocityManager.class);

        List<WikiComponent> components = this.mocker.getComponentUnderTest().buildComponents(extensionObject);

        assertEquals(1, components.size());
        verify(contentParser).parse("content", Syntax.XWIKI_2_1, DOC_REF);

        UIExtension uiExtension = (UIExtension) components.get(0);
        Map<String, String> parameters = uiExtension.getParameters();

        assertEquals(2, parameters.size());
        verify(velocityManager.getVelocityEngine()).evaluate(any(VelocityContext.class), any(StringWriter.class),
            eq("name:key"), eq("value=foo"));
        verify(velocityManager.getVelocityEngine()).evaluate(any(VelocityContext.class), any(StringWriter.class),
            eq("name:key2"), eq("value2"));
    }

    private BaseObject createExtensionObject(String id, String extensionPointId, String content, String parameters,
        String scope)
    {
        BaseObject extensionObject = mock(BaseObject.class, id);
        when(extensionObject.getStringValue(ID_PROPERTY)).thenReturn(id);
        when(extensionObject.getStringValue(EXTENSION_POINT_ID_PROPERTY)).thenReturn(extensionPointId);
        when(extensionObject.getStringValue(CONTENT_PROPERTY)).thenReturn(content);
        when(extensionObject.getStringValue(PARAMETERS_PROPERTY)).thenReturn(parameters);
        when(extensionObject.getStringValue(SCOPE_PROPERTY)).thenReturn(scope);
        BaseObjectReference objectReference =
            new BaseObjectReference(new ObjectReference("XWiki.UIExtensionClass[0]", DOC_REF));
        when(extensionObject.getReference()).thenReturn(objectReference);

        when(extensionObject.getOwnerDocument()).thenReturn(this.componentDoc);

        return extensionObject;
    }
}

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

import java.util.List;
import java.util.Vector;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.wiki.internal.WikiComponentConstants;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiComponentBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.RelativeStringEntityReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CompactWikiStringEntityReferenceSerializer;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link DefaultWikiComponentBridge}.
 *
 * @version $Id$
 */
@ComponentList({
    DefaultModelContext.class,
    DefaultModelConfiguration.class,
    LocalStringEntityReferenceSerializer.class,
    RelativeStringEntityReferenceResolver.class,
    CurrentReferenceDocumentReferenceResolver.class,
    CurrentReferenceEntityReferenceResolver.class,
    CurrentEntityReferenceProvider.class,
    CurrentMixedStringDocumentReferenceResolver.class,
    CurrentMixedEntityReferenceProvider.class,
    DefaultEntityReferenceProvider.class,
    CompactWikiStringEntityReferenceSerializer.class
})
@ComponentTest
class DefaultWikiComponentBridgeTest implements WikiComponentConstants
{
    private static final DocumentReference DOC_REFERENCE = new DocumentReference("xwiki", "XWiki", "MyComponent");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    @Mock
    private XWiki xwiki;

    @MockComponent
    private Provider<XWikiContext> xWikiContextProvider;

    @MockComponent
    private ContentParser contentParser;

    @Mock
    private XWikiDocument componentDoc;

    @Mock
    private BaseObject componentObject;

    @MockComponent
    private Execution execution;

    @InjectMockComponents
    private DefaultWikiComponentBridge bridge;

    @InjectComponentManager
    private MockitoComponentManager mockitoComponentManager;

    @BeforeEach
    public void configure() throws Exception
    {
        Utils.setComponentManager(this.mockitoComponentManager);

        XWikiContext xwikiContext = new XWikiContext();
        xwikiContext.setWikiId("xwiki");
        xwikiContext.setWiki(this.xwiki);
        when(this.xWikiContextProvider.get()).thenReturn(xwikiContext);

        ExecutionContext context = new ExecutionContext();
        context.setProperty("xwikicontext", xwikiContext);

        when(this.execution.getContext()).thenReturn(context);
        when(this.xwiki.getDocument(DOC_REFERENCE, xwikiContext)).thenReturn(this.componentDoc);
    }

    @Test
    void getAuthorReference() throws Exception
    {
        when(this.componentDoc.getAuthorReference()).thenReturn(AUTHOR_REFERENCE);

        assertEquals(AUTHOR_REFERENCE, this.bridge.getAuthorReference(DOC_REFERENCE));
    }

    @Test
    void getDeclaredInterfaces() throws Exception
    {
        BaseObject interfaceObject = mock();
        Vector<BaseObject> interfaceObjects = new Vector<>();
        interfaceObjects.add(interfaceObject);

        when(this.componentDoc.getObjectNumbers(INTERFACE_CLASS)).thenReturn(1);
        when(this.componentDoc.getObjects(INTERFACE_CLASS)).thenReturn(interfaceObjects);
        when(interfaceObject.getStringValue(INTERFACE_NAME_FIELD))
            .thenReturn("org.xwiki.component.phase.Initializable");

        assertEquals(1, this.bridge.getDeclaredInterfaces(DOC_REFERENCE).size());
    }

    @Test
    void getDependencies() throws Exception
    {
        BaseObject dependencyObject = mock();
        Vector<BaseObject> dependencyObjects = new Vector<>();
        dependencyObjects.add(dependencyObject);

        when(this.componentDoc.getObjectNumbers(DEPENDENCY_CLASS)).thenReturn(1);
        when(this.componentDoc.getObjects(DEPENDENCY_CLASS)).thenReturn(dependencyObjects);
        when(dependencyObject.getStringValue(COMPONENT_ROLE_TYPE_FIELD))
            .thenReturn("org.xwiki.component.wiki.TestRole");
        when(dependencyObject.getStringValue(COMPONENT_ROLE_HINT_FIELD))
            .thenReturn("default");
        when(dependencyObject.getStringValue(DEPENDENCY_BINDING_NAME_FIELD))
            .thenReturn("test");

        assertEquals(1, this.bridge.getDependencies(DOC_REFERENCE).size());
    }

    @Test
    void getHandledMethods() throws Exception
    {
        XDOM xdom = new XDOM(List.of());
        BaseObject methodObject = mock();
        Vector<BaseObject> methodObjects = new Vector<>();
        methodObjects.add(methodObject);

        when(this.componentDoc.getObjectNumbers(METHOD_CLASS)).thenReturn(1);
        when(this.componentDoc.getObjects(METHOD_CLASS)).thenReturn(methodObjects);
        when(methodObject.getStringValue(METHOD_NAME_FIELD))
            .thenReturn("test");
        when(methodObject.getStringValue(METHOD_CODE_FIELD)).thenReturn("test");
        when(this.componentDoc.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        when(this.contentParser.parse("test", Syntax.XWIKI_2_1, DOC_REFERENCE)).thenReturn(xdom);

        assertEquals(1, this.bridge.getHandledMethods(DOC_REFERENCE).size());
    }

    @Test
    void getRoleHint() throws Exception
    {
        when(this.componentDoc.getObject(COMPONENT_CLASS)).thenReturn(this.componentObject);
        when(this.componentObject.getStringValue(COMPONENT_ROLE_HINT_FIELD)).thenReturn("roleHint");

        assertEquals("roleHint", this.bridge.getRoleHint(DOC_REFERENCE));
    }

    @Test
    void getRoleType() throws Exception
    {
        when(this.componentDoc.getObject(COMPONENT_CLASS)).thenReturn(this.componentObject);
        when(this.componentObject.getStringValue(COMPONENT_ROLE_TYPE_FIELD))
            .thenReturn("org.xwiki.component.wiki.TestRole");

        assertEquals(TestRole.class, this.bridge.getRoleType(DOC_REFERENCE));
    }

    @Test
    void getRoleTypeWithoutComponentObject()
    {
        when(this.componentDoc.getObject(COMPONENT_CLASS)).thenReturn(null);

        WikiComponentException wikiComponentException =
            assertThrows(WikiComponentException.class, () -> this.bridge.getRoleType(DOC_REFERENCE));

        assertEquals("No component object could be found in document [xwiki:XWiki.MyComponent]",
            wikiComponentException.getMessage());
    }

    @Test
    void getRoleTypeWithWrongRole()
    {
        when(this.componentDoc.getObject(COMPONENT_CLASS)).thenReturn(this.componentObject);
        when(this.componentObject.getStringValue(COMPONENT_ROLE_TYPE_FIELD))
            .thenReturn("org.xwiki.component.wiki.DoesNotExist");

        WikiComponentException exception =
            assertThrows(WikiComponentException.class, () -> this.bridge.getRoleType(DOC_REFERENCE));

        assertEquals("The role type [org.xwiki.component.wiki.DoesNotExist] does not exist",
            exception.getMessage());
    }

    @Test
    void getScope() throws Exception
    {
        when(this.componentDoc.getObject(COMPONENT_CLASS)).thenReturn(this.componentObject);
        when(this.componentObject.getStringValue(COMPONENT_SCOPE_FIELD))
            .thenReturn("user");

        assertEquals(WikiComponentScope.USER, this.bridge.getScope(DOC_REFERENCE));
    }

    @Test
    void getScopeWithWrongScope() throws Exception
    {
        when(this.componentDoc.getObject(COMPONENT_CLASS)).thenReturn(this.componentObject);
        when(this.componentObject.getStringValue(COMPONENT_SCOPE_FIELD))
            .thenReturn("doesnotexist");

        // Wiki is the default value
        assertEquals(WikiComponentScope.WIKI, this.bridge.getScope(DOC_REFERENCE));
    }

    @Test
    void getSyntax() throws Exception
    {
        when(this.componentDoc.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        assertEquals(Syntax.XWIKI_2_1, this.bridge.getSyntax(DOC_REFERENCE));
    }
}

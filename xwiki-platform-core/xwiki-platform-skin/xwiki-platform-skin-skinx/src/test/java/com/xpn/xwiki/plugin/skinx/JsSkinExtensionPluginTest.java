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
package com.xpn.xwiki.plugin.skinx;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.skinx.internal.async.SkinExtensionAsync;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link JsSkinExtensionPlugin}.
 *
 * @version $Id$
 * @since 14.9RC1
 */
@OldcoreTest
class JsSkinExtensionPluginTest
{
    @InjectMockitoOldcore
    private MockitoOldcore mockitoOldcore;

    @MockComponent
    private Provider<DocumentReference> documentReferenceProvider;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    private SkinExtensionAsync skinExtensionAsync;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiContext context;
    private BaseClass pluginClass;
    private JsSkinExtensionPlugin skinExtensionPlugin;
    private XWikiURLFactory urlFactory;

    @BeforeEach
    void setup() throws XWikiException
    {
        XWikiDocument classDoc = mock(XWikiDocument.class);
        this.pluginClass = mock(BaseClass.class);

        this.context = mockitoOldcore.getXWikiContext();
        this.urlFactory = mock(XWikiURLFactory.class);
        context.setURLFactory(urlFactory);
        XWikiRequest xWikiRequest = mock(XWikiRequest.class);
        context.setRequest(xWikiRequest);
        XWiki wiki = mockitoOldcore.getSpyXWiki();
        when(this.currentMixedDocumentReferenceResolver.resolve(JsSkinExtensionPlugin.JSX_CLASS_NAME)).thenReturn(
            new DocumentReference(JsSkinExtensionPlugin.JSX_CLASS_REFERENCE, new WikiReference("xwiki")));
        when(wiki.getDocument(JsSkinExtensionPlugin.JSX_CLASS_NAME, context)).thenReturn(classDoc);
        when(classDoc.getXClass()).thenReturn(this.pluginClass);
        this.skinExtensionPlugin = new JsSkinExtensionPlugin("plugin1", "unused", context);
        skinExtensionPlugin.init(context);
    }

    @Test
    void use() throws XWikiException
    {
        String resource = "MySpace.MyJSXPage";
        Map<String, Object> parameters = Collections.singletonMap("fooParam", "barValue");

        DocumentReference documentReference = new DocumentReference("xwiki", "MySpace", "MyJSXPage");
        when(this.currentEntityReferenceResolver.resolve(resource, EntityType.DOCUMENT)).thenReturn(documentReference);
        when(this.entityReferenceSerializer.serialize(documentReference)).thenReturn(resource);

        when(this.documentReferenceResolver.resolve(resource)).thenReturn(documentReference);
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.mockitoOldcore.getSpyXWiki().getDocument(documentReference, context)).thenReturn(document);

        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Foo");
        when(document.getAuthorReference()).thenReturn(userReference);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference, documentReference))
            .thenReturn(true);

        this.skinExtensionPlugin.use(resource, parameters, context);
        String className = JsSkinExtensionPlugin.class.getCanonicalName();
        Set<String> resources = (Set<String>) context.get(className);

        assertEquals(Collections.singleton(resource), resources);

        Map<String, Map<String, Object>> parametersMap =
            (Map<String, Map<String, Object>>) context.get(className + "_parameters");
        Map<String, Map<String, Object>> expectedParameters = new HashMap<>();
        expectedParameters.put(resource, parameters);

        assertEquals(expectedParameters, parametersMap);
        verify(this.authorizationManager)
            .hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference, documentReference);
        verify(this.skinExtensionAsync).use("jsx", resource, parameters);

        String resource2 = "MySpace.MyOtherJSXPage";
        Map<String, Object> parameters2 = null;
        DocumentReference documentReference2 = new DocumentReference("xwiki", "MySpace", "MyOtherJSXPage");

        when(this.currentEntityReferenceResolver.resolve(resource2, EntityType.DOCUMENT))
            .thenReturn(documentReference2);
        when(this.entityReferenceSerializer.serialize(documentReference2)).thenReturn(resource2);

        when(this.documentReferenceResolver.resolve(resource2)).thenReturn(documentReference2);
        XWikiDocument document2 = mock(XWikiDocument.class);
        when(this.mockitoOldcore.getSpyXWiki().getDocument(documentReference2, context)).thenReturn(document2);

        DocumentReference userReference2 = new DocumentReference("xwiki", "XWiki", "Bar");
        when(document2.getAuthorReference()).thenReturn(userReference2);
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference2,
            documentReference2)).thenReturn(false);

        this.skinExtensionPlugin.use(resource2, parameters2, context);
        resources = (Set<String>) context.get(className);
        assertEquals(Collections.singleton(resource), resources);
        parametersMap =
            (Map<String, Map<String, Object>>) context.get(className + "_parameters");
        assertEquals(expectedParameters, parametersMap);
        verify(this.authorizationManager).hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference2,
            documentReference2);
        verify(this.skinExtensionAsync, never()).use("jsx", resource2, parameters2);

        assertEquals(1, this.logCapture.size());
        assertEquals("Extensions present in [MySpace.MyOtherJSXPage] ignored because of lack of script right "
            + "from the author.", this.logCapture.getMessage(0));

        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference2,
            documentReference2)).thenReturn(true);
        this.skinExtensionPlugin.use(resource2, parameters2, context);

        Set<String> expectedSet = new HashSet<>();
        expectedSet.add(resource);
        expectedSet.add(resource2);

        resources = (Set<String>) context.get(className);
        assertEquals(expectedSet, resources);

        parametersMap =
            (Map<String, Map<String, Object>>) context.get(className + "_parameters");
        assertEquals(expectedParameters, parametersMap);
        verify(this.authorizationManager, times(2))
            .hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference2, documentReference2);
        verify(this.skinExtensionAsync).use("jsx", resource2, null);

        parameters2 = Collections.singletonMap("buzValue", 42);
        this.skinExtensionPlugin.use(resource2, parameters2, context);

        resources = (Set<String>) context.get(className);
        assertEquals(expectedSet, resources);
        parametersMap =
            (Map<String, Map<String, Object>>) context.get(className + "_parameters");
        expectedParameters.put(resource2, parameters2);
        assertEquals(expectedParameters, parametersMap);
        verify(this.skinExtensionAsync).use("jsx", resource2, parameters2);

        this.skinExtensionPlugin.use(resource, null, context);
        expectedParameters.remove(resource);
        resources = (Set<String>) context.get(className);
        assertEquals(expectedSet, resources);
        parametersMap =
            (Map<String, Map<String, Object>>) context.get(className + "_parameters");
        expectedParameters.put(resource2, parameters2);
        assertEquals(expectedParameters, parametersMap);
        verify(this.skinExtensionAsync).use("jsx", resource, null);
    }

    @Test
    void hasPageExtensions()
    {
        this.context.setDoc(null);
        assertFalse(this.skinExtensionPlugin.hasPageExtensions(context));

        XWikiDocument currentDoc = mock(XWikiDocument.class, "currentDoc");
        this.context.setDoc(currentDoc);

        String className = JsSkinExtensionPlugin.JSX_CLASS_NAME;
        when(currentDoc.getObjects(className)).thenReturn(null);
        assertFalse(this.skinExtensionPlugin.hasPageExtensions(context));
        verify(currentDoc).getObjects(className);

        BaseObject baseObject = mock(BaseObject.class, "specificObj");
        when(baseObject.getStringValue("use")).thenReturn("wiki");
        Vector<BaseObject> objectList = new Vector<>();
        objectList.add(mock(BaseObject.class));
        objectList.add(null);
        objectList.add(mock(BaseObject.class));
        objectList.add(baseObject);
        objectList.add(null);
        objectList.add(mock(BaseObject.class));

        when(currentDoc.getObjects(className)).thenReturn(objectList);
        assertFalse(this.skinExtensionPlugin.hasPageExtensions(context));
        verifyNoInteractions(this.authorizationManager);

        when(baseObject.getStringValue("use")).thenReturn("currentPage");
        DocumentReference documentReference = new DocumentReference("xwiki", "MySpace", "SomePage");
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Foo");
        when(currentDoc.getDocumentReference()).thenReturn(documentReference);
        when(currentDoc.getAuthorReference()).thenReturn(userReference);

        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference, documentReference))
            .thenReturn(false);
        assertFalse(this.skinExtensionPlugin.hasPageExtensions(context));
        verify(this.authorizationManager)
            .hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference, documentReference);

        assertEquals(1, this.logCapture.size());
        assertEquals("Extensions present in [xwiki:MySpace.SomePage] ignored because of lack of script right "
            + "from the author.", this.logCapture.getMessage(0));

        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference, documentReference))
            .thenReturn(true);
        assertTrue(this.skinExtensionPlugin.hasPageExtensions(context));
    }
}

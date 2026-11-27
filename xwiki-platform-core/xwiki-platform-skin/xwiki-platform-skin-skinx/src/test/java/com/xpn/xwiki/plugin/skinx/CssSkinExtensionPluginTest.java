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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
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
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
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
import com.xpn.xwiki.store.XWikiStoreInterface;
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
 * Tests for {@link CssSkinExtensionPlugin}.
 *
 * @version $Id$
 * @since 13.10RC1
 */
@OldcoreTest
class CssSkinExtensionPluginTest
{
    @InjectMockitoOldcore
    private MockitoOldcore mockitoOldcore;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private Provider<DocumentReference> documentReferenceProvider;

    @MockComponent
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    @Named("current")
    private EntityReferenceResolver<String> currentEntityReferenceResolver;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private SkinExtensionAsync skinExtensionAsync;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiContext context;
    private BaseClass pluginClass;
    private CssSkinExtensionPlugin skinExtensionPlugin;
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
        when(this.currentMixedDocumentReferenceResolver.resolve(CssSkinExtensionPlugin.SSX_CLASS_NAME)).thenReturn(
            new DocumentReference(CssSkinExtensionPlugin.SSX_CLASS_REFERENCE, new WikiReference("xwiki")));
        when(wiki.getDocument(CssSkinExtensionPlugin.SSX_CLASS_NAME, context)).thenReturn(classDoc);
        when(classDoc.getXClass()).thenReturn(this.pluginClass);
        this.skinExtensionPlugin = new CssSkinExtensionPlugin("plugin1", "unused", context);
        skinExtensionPlugin.init(context);
    }

    @Test
    void endParsingNoLink()
    {
        String content = String.format("<body><head><!-- %s --></head><body><!-- %s --></body>",
            CssSkinExtensionPlugin.class.getCanonicalName(),
            CssSkinExtensionPlugin.class.getCanonicalName());

        String expectedFirst = String.format("<body><head></head><body><!-- %s --></body>",
            CssSkinExtensionPlugin.class.getCanonicalName());
        String obtainedContent = this.skinExtensionPlugin.endParsing(content, this.context);
        assertEquals(expectedFirst, obtainedContent);
    }

    @Test
    void endParsingAlwaysUsedExtensions() throws XWikiException, MalformedURLException
    {
        String content = String.format("<body><head><!-- %s --></head><body><!-- %s --></body>",
            CssSkinExtensionPlugin.class.getCanonicalName(),
            CssSkinExtensionPlugin.class.getCanonicalName());

        String alwaysUsedExtensionsQuery = String.format(", BaseObject as obj, StringProperty as use "
            + "where obj.className='%s' and obj.name=doc.fullName and use.id.id=obj.id and use.id.name='use' "
            + "and use.value='always'", CssSkinExtensionPlugin.SSX_CLASS_NAME);
        XWikiStoreInterface mockStore = this.mockitoOldcore.getMockStore();

        SpaceReference spaceReference = new SpaceReference("Space", new WikiReference("xwiki"));
        DocumentReference referenceExt1 = new DocumentReference("Extension1", spaceReference);
        DocumentReference referenceExt2 = new DocumentReference("Extension2", spaceReference);
        DocumentReference referenceExt3 = new DocumentReference("Extension3", spaceReference);

        when(this.localEntityReferenceSerializer.serialize(spaceReference)).thenReturn("Space");
        when(mockStore.searchDocumentReferences(alwaysUsedExtensionsQuery, this.context)).thenReturn(Arrays.asList(
            referenceExt1,
            referenceExt2,
            referenceExt3
        ));
        XWiki wiki = context.getWiki();
        XWikiDocument ext1 = mock(XWikiDocument.class);
        XWikiDocument ext2 = mock(XWikiDocument.class);
        XWikiDocument ext3 = mock(XWikiDocument.class);
        when(wiki.getDocument(referenceExt1, context)).thenReturn(ext1);
        when(wiki.getDocument(referenceExt2, context)).thenReturn(ext2);
        when(wiki.getDocument(referenceExt3, context)).thenReturn(ext3);

        when(ext1.getDocumentReference()).thenReturn(referenceExt1);
        when(ext2.getDocumentReference()).thenReturn(referenceExt2);
        when(ext3.getDocumentReference()).thenReturn(referenceExt3);

        DocumentReference author = mock(DocumentReference.class);
        when(ext1.getAuthorReference()).thenReturn(author);
        when(ext2.getAuthorReference()).thenReturn(author);
        when(ext3.getAuthorReference()).thenReturn(author);

        when(this.authorizationManager.hasAccess(Right.ADMIN, EntityType.WIKI, author, referenceExt1)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.ADMIN, EntityType.WIKI, author, referenceExt2))
            .thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.ADMIN, EntityType.WIKI, author, referenceExt3)).thenReturn(true);

        when(this.entityReferenceSerializer.serialize(referenceExt1)).thenReturn("extension1");
        when(this.entityReferenceSerializer.serialize(referenceExt3)).thenReturn("extension3");

        when(this.currentDocumentReferenceResolver.resolve("extension1")).thenReturn(referenceExt1);
        when(this.currentDocumentReferenceResolver.resolve("extension3")).thenReturn(referenceExt3);

        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, referenceExt1)).thenReturn(true);
        when(this.contextualAuthorizationManager.hasAccess(Right.VIEW, referenceExt3)).thenReturn(false);

        context.setLocale(Locale.ITALY);
        when(ext1.getVersion()).thenReturn("3.2");

        String expectedQueryString = "language=it_IT&docVersion=3.2";
        String extensionLink1 = "https://myextension1";
        URL extensionLink1Url = new URL(extensionLink1);

        when(this.urlFactory.createURL("Space", "Extension1", CssSkinExtensionPlugin.PLUGIN_NAME, expectedQueryString,
            "", "xwiki", context)).thenReturn(extensionLink1Url);
        when(this.urlFactory.getURL(extensionLink1Url, context)).thenReturn(extensionLink1);

        String expectedStyle =
            String.format("<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\" />\n", extensionLink1);

        String expectedContent = String.format("<body><head>%s</head><body><!-- %s --></body>",
            expectedStyle,
            CssSkinExtensionPlugin.class.getCanonicalName());
        String obtainedContent = this.skinExtensionPlugin.endParsing(content, this.context);
        assertEquals(expectedContent, obtainedContent);
    }

    @Test
    void use() throws XWikiException
    {
        String resource = "MySpace.MySSXPage";
        Map<String, Object> parameters = Collections.singletonMap("fooParam", "barValue");

        DocumentReference documentReference = new DocumentReference("xwiki", "MySpace", "MySSXPage");
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
        String className = CssSkinExtensionPlugin.class.getCanonicalName();
        Set<String> resources = (Set<String>) context.get(className);

        assertEquals(Collections.singleton(resource), resources);

        Map<String, Map<String, Object>> parametersMap =
            (Map<String, Map<String, Object>>) context.get(className + "_parameters");
        Map<String, Map<String, Object>> expectedParameters = new HashMap<>();
        expectedParameters.put(resource, parameters);

        assertEquals(expectedParameters, parametersMap);
        verify(this.authorizationManager)
            .hasAccess(Right.SCRIPT, EntityType.DOCUMENT, userReference, documentReference);
        verify(this.skinExtensionAsync).use("ssx", resource, parameters);

        String resource2 = "MySpace.MyOtherSSXPage";
        Map<String, Object> parameters2 = null;
        DocumentReference documentReference2 = new DocumentReference("xwiki", "MySpace", "MyOtherSSXPage");

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
        verify(this.skinExtensionAsync, never()).use("ssx", resource2, parameters2);

        assertEquals(1, this.logCapture.size());
        assertEquals("Extensions present in [MySpace.MyOtherSSXPage] ignored because of lack of script right "
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
        verify(this.skinExtensionAsync).use("ssx", resource2, null);

        parameters2 = Collections.singletonMap("buzValue", 42);
        this.skinExtensionPlugin.use(resource2, parameters2, context);

        resources = (Set<String>) context.get(className);
        assertEquals(expectedSet, resources);
        parametersMap =
            (Map<String, Map<String, Object>>) context.get(className + "_parameters");
        expectedParameters.put(resource2, parameters2);
        assertEquals(expectedParameters, parametersMap);
        verify(this.skinExtensionAsync).use("ssx", resource2, parameters2);

        this.skinExtensionPlugin.use(resource, null, context);
        expectedParameters.remove(resource);
        resources = (Set<String>) context.get(className);
        assertEquals(expectedSet, resources);
        parametersMap =
            (Map<String, Map<String, Object>>) context.get(className + "_parameters");
        expectedParameters.put(resource2, parameters2);
        assertEquals(expectedParameters, parametersMap);
        verify(this.skinExtensionAsync).use("ssx", resource, null);
    }

    @Test
    void hasPageExtensions()
    {
        this.context.setDoc(null);
        assertFalse(this.skinExtensionPlugin.hasPageExtensions(context));

        XWikiDocument currentDoc = mock(XWikiDocument.class, "currentDoc");
        this.context.setDoc(currentDoc);

        String className = CssSkinExtensionPlugin.SSX_CLASS_NAME;
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

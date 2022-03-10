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
import java.util.Locale;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CssSkinExtensionPlugin}.
 *
 * @version $Id$
 * @since 13.10RC1
 */
@OldcoreTest
public class CssSkinExtensionPluginTest
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
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

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
        when(this.documentReferenceResolver.resolve(CssSkinExtensionPlugin.SSX_CLASS_NAME)).thenReturn(
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

        when(this.authorizationManager.hasAccess(Right.PROGRAM, author, referenceExt1)).thenReturn(true);
        when(this.authorizationManager.hasAccess(Right.PROGRAM, author, referenceExt2)).thenReturn(false);
        when(this.authorizationManager.hasAccess(Right.PROGRAM, author, referenceExt3)).thenReturn(true);

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
}

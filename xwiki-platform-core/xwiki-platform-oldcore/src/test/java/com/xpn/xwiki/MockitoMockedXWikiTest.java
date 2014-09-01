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
package com.xpn.xwiki;

import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.ObjectReferenceResolver;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.template.PrivilegedTemplateRenderer;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link com.xpn.xwiki.XWiki}, using only Mockito Mocks. Supposed to be better than
 * {@link com.xpn.xwiki.XWikiTest} and should ideally be used when writing new tests for {@link com.xpn.xwiki.XWiki}.
 * However might require adding lots of new mocks or not, depending on what is tested...
 *
 * @version $Id$
 * @since 6.1
 */
public class MockitoMockedXWikiTest
{
    @Rule
    public MockitoComponentManagerRule componentManager = new MockitoComponentManagerRule();

    private ConfigurationSource xwikiCfgConfigurationSource;

    @Before
    public void setUp() throws Exception
    {
        // Mock components required for the XWiki class initialization
        Utils.setComponentManager(this.componentManager);
        EntityReferenceSerializer<String> localStringEntityReferenceSerializer =
            this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        EntityReferenceResolver<String> relativeStringEntityReferenceResolver =
            this.componentManager.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        DocumentReferenceResolver<EntityReference> entityReferenceDocumentReferenceResolver =
            this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "current");
        DocumentReferenceResolver<String> stringDocumentReferenceResolver =
            this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        EntityReferenceSerializer<String> defaultStringEntityReferenceSerializer =
            this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING);
        EntityReferenceValueProvider entityReferenceValueProvider =
            this.componentManager.registerMockComponent(EntityReferenceValueProvider.class);
        EntityReferenceResolver<String> currentMixedStringEntityReferenceResolver =
            this.componentManager.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "currentmixed");
        SyntaxFactory syntaxFactory = this.componentManager.registerMockComponent(SyntaxFactory.class);
        PrivilegedTemplateRenderer privilegedTemplateRenderer =
            this.componentManager.registerMockComponent(PrivilegedTemplateRenderer.class);
        ResourceReferenceManager resourceReferenceManager =
            this.componentManager.registerMockComponent(ResourceReferenceManager.class);
        ObservationManager observationManager = this.componentManager.registerMockComponent(ObservationManager.class);

        // Mock components required for the XWikiDocument class initialization
        DocumentReferenceResolver<String> currentStringDocumentReferenceResolver =
            this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        DocumentReferenceResolver<String> explicitStringDocumentReferenceResolver =
            this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "explicit");
        EntityReferenceResolver<String> xclassStringEntityReferenceResolver =
            this.componentManager.registerMockComponent(EntityReferenceResolver.TYPE_STRING, "xclass");
        DocumentReferenceResolver<EntityReference> explicitEntityReferenceResolver =
            this.componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_REFERENCE, "explicit");
        EntityReferenceSerializer<String> compactStringEntityReferenceSerializer =
            this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compact");
        EntityReferenceSerializer<String> compactWikiStringEntityReferenceSerializer =
            this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        EntityReferenceSerializer<String> uidStringEntityReferenceSerializer =
            this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "uid");
        EntityReferenceSerializer<String> localUidStringEntityReferenceSerializer =
            this.componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local/uid");
        ObjectReferenceResolver<EntityReference> currentEntityReferenceObjectReferenceResolver =
            this.componentManager.registerMockComponent(ObjectReferenceResolver.TYPE_REFERENCE, "current");
        this.xwikiCfgConfigurationSource =
            this.componentManager.registerMockComponent(ConfigurationSource.class, "xwikicfg");
    }

    @Test
    public void deleteAllDocumentsAndWithoutSendingToTrash() throws Exception
    {
        XWiki xwiki = new XWiki();

        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference reference = new DocumentReference("wiki", "space", "page");
        when(document.getDocumentReference()).thenReturn(reference);

        // Make sure we have a trash for the test.
        XWikiRecycleBinStoreInterface recycleBinStoreInterface = mock(XWikiRecycleBinStoreInterface.class);
        xwiki.setRecycleBinStore(recycleBinStoreInterface);
        when(xwikiCfgConfigurationSource.getProperty("xwiki.recyclebin", "1")).thenReturn("1");

        // Configure the mocked Store to later verify if it's called
        XWikiStoreInterface storeInterface = mock(XWikiStoreInterface.class);
        xwiki.setStore(storeInterface);
        XWikiContext xwikiContext = mock(XWikiContext.class);

        xwiki.deleteAllDocuments(document, false, xwikiContext);

        // Verify that saveToRecycleBin is never called since otherwise it would mean the doc has been saved in the
        // trash
        verify(recycleBinStoreInterface, never()).saveToRecycleBin(any(XWikiDocument.class), any(String.class),
            any(Date.class), any(XWikiContext.class), any(Boolean.class));

        // Verify that deleteXWikiDoc() is called
        verify(storeInterface).deleteXWikiDoc(document, xwikiContext);
    }
}

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
package org.xwiki.repository.internal.resources;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.repository.xwiki.model.jaxb.Extension;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionRepository;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersion;
import org.xwiki.extension.repository.xwiki.model.jaxb.Property;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.ratings.AverageRating;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.ratings.RatingsManagerFactory;
import org.xwiki.repository.internal.ExtensionStore;
import org.xwiki.repository.internal.RepositoryManager;
import org.xwiki.repository.internal.XWikiRepositoryModel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AbstractExtensionRESTResource}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(ExtensionFactory.class)
class AbstractExtensionRESTResourceTest
{
    private static final DocumentReference EXTENSION_DOCUMENT_REFERENCE =
        new DocumentReference("wiki", "Extension", "WebHome");

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ExtensionStore extensionStore;

    @MockComponent
    private RepositoryManager repositoryManager;

    @MockComponent
    private RatingsManagerFactory ratingsManagerFactory;

    @InjectMockComponents
    private ExtensionVersionRESTResource resource;

    @BeforeComponent
    void beforeComponent()
    {
        when(this.xcontextProvider.get()).thenReturn(mock(XWikiContext.class));
    }

    @Test
    void toExtensionRepositories()
    {
        List<ExtensionRepository> repositories = this.resource.toExtensionRepositories(
            List.of("id1:xwiki:https://host1", "id2:maven;maven.release=false;maven.snapshot=true:https://host2"));

        assertEquals(2, repositories.size());

        ExtensionRepository repository1 = repositories.get(0);
        assertEquals("id1", repository1.getId());
        assertEquals("xwiki", repository1.getType());
        assertEquals("https://host1", repository1.getUri());
        assertTrue(repository1.getProperties().isEmpty());

        ExtensionRepository repository2 = repositories.get(1);
        assertEquals("id2", repository2.getId());
        assertEquals("maven", repository2.getType());
        assertEquals("https://host2", repository2.getUri());
        assertEquals(Map.of("maven.release", "false", "maven.snapshot", "true"), repository2.getProperties().stream()
            .collect(Collectors.toMap(Property::getKey, Property::getStringValue)));
    }

    @Test
    void toExtensionRepositoriesNull()
    {
        assertNull(this.resource.toExtensionRepositories(null));
    }

    @Test
    void createExtensionWithoutVersion() throws Exception
    {
        XWikiDocument extensionDocument = mockExtensionDocument();
        // Resolving the version document of an extension using version pages fails without a version.
        when(this.extensionStore.getExtensionVersionDocument(eq(extensionDocument), isNull(String.class), any()))
            .thenThrow(new IllegalArgumentException("An Entity Reference name cannot be null or empty"));
        when(this.extensionStore.getValue(any(List.class), eq(XWikiRepositoryModel.PROP_EXTENSION_ID)))
            .thenReturn("extension-id");

        Extension extension = this.resource.createExtension(extensionDocument, null);

        assertEquals(Extension.class, extension.getClass());
        assertEquals("extension-id", extension.getId());
    }

    @Test
    void createExtensionWithVersion() throws Exception
    {
        XWikiDocument extensionDocument = mockExtensionDocument();
        BaseObject extensionVersionObject = mock();
        when(this.repositoryManager.getExtensionVersionObject(eq(extensionDocument), eq("1.0"), any()))
            .thenReturn(extensionVersionObject);
        when(this.extensionStore.getValue(extensionVersionObject, XWikiRepositoryModel.PROP_VERSION_VERSION))
            .thenReturn("1.0");
        BaseObject dependencyObject = mock();
        when(this.extensionStore.getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_EXTENSIONVERSION,
            null)).thenReturn("1.0");
        when(this.extensionStore.getValue(dependencyObject, XWikiRepositoryModel.PROP_DEPENDENCY_ID))
            .thenReturn("dependency-id");
        XWikiDocument versionDocument = mock();
        when(versionDocument.getXObjects(XWikiRepositoryModel.EXTENSIONDEPENDENCY_CLASSREFERENCE))
            .thenReturn(List.of(dependencyObject));
        when(this.extensionStore.getExtensionVersionDocument(eq(extensionDocument), eq("1.0"), any()))
            .thenReturn(versionDocument);

        ExtensionVersion extension = this.resource.createExtension(extensionDocument, "1.0");

        assertEquals("1.0", extension.getVersion());
        assertEquals(1, extension.getDependencies().size());
        assertEquals("dependency-id", extension.getDependencies().get(0).getId());
    }

    private XWikiDocument mockExtensionDocument() throws Exception
    {
        XWikiDocument extensionDocument = mock();
        when(extensionDocument.getDocumentReference()).thenReturn(EXTENSION_DOCUMENT_REFERENCE);
        when(extensionDocument.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE)).thenReturn(mock());

        RatingsManager ratingsManager = mock();
        when(this.ratingsManagerFactory.getRatingsManager(RatingsManagerFactory.DEFAULT_APP_HINT))
            .thenReturn(ratingsManager);
        when(ratingsManager.getAverageRating(EXTENSION_DOCUMENT_REFERENCE)).thenReturn(mock(AverageRating.class));

        return extensionDocument;
    }
}

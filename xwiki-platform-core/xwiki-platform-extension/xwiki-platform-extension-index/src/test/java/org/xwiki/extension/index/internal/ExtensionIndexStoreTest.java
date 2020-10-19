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
package org.xwiki.extension.index.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.internal.WikiDeletedListener;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.extension.AbstractRemoteExtension;
import org.xwiki.extension.DefaultExtensionAuthor;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.internal.converter.ExtensionAuthorConverter;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.ExtensionQuery.COMPARISON;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.search.solr.test.SolrComponentList;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link SolrEventStore}, {@link EventsSolrCoreInitializer} and {@link WikiDeletedListener}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({ExtensionIndexStore.class, ExtensionIndexSolrCoreInitializer.class, ExtensionIdConverter.class,
    ExtensionAuthorConverter.class, ExtensionFactory.class})
@ReferenceComponentList
@SolrComponentList
class ExtensionIndexStoreTest
{
    public static class TestExtension extends AbstractRemoteExtension
    {
        public TestExtension(ExtensionRepository repository, ExtensionId id, String type)
        {
            super(repository, id, type);
        }
    }

    @XWikiTempDir
    private File permanentDirectory;

    private ExtensionRepository testRepository;

    private ExtensionRepositoryDescriptor testRepositoryDescriptor;

    private ConfigurationSource mockXWikiProperties;

    private Environment mockEnvironment;

    @MockComponent
    private ExtensionManager extensionManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private ExtensionIndexStore indexStore;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.mockXWikiProperties =
            this.componentManager.registerMockComponent(ConfigurationSource.class, "xwikiproperties");
        this.mockEnvironment = this.componentManager.registerMockComponent(Environment.class);
        when(this.mockXWikiProperties.getProperty(anyString(), anyString())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgument(1);
            }
        });

        when(this.mockEnvironment.getPermanentDirectory()).thenReturn(this.permanentDirectory);
        FileUtils.deleteDirectory(this.permanentDirectory);
        this.permanentDirectory.mkdirs();

        this.testRepositoryDescriptor = new DefaultExtensionRepositoryDescriptor("test", "test", null);
        this.testRepository = mock(ExtensionRepository.class);
        when(this.testRepository.getDescriptor()).thenReturn(this.testRepositoryDescriptor);
        when(this.extensionManager.getRepository(this.testRepositoryDescriptor.getId()))
            .thenReturn(this.testRepository);
    }

    // Tests

    @Test
    void store() throws SolrServerException, IOException, ResolveException, SearchException
    {
        TestExtension extension = new TestExtension(this.testRepository, new ExtensionId("id", "version"), "test");

        assertTrue(this.indexStore.add(extension, false));
        this.indexStore.commit();

        assertFalse(this.indexStore.add(extension, false));

        SolrExtension storedExtension = this.indexStore.getSolrExtension(extension.getId());

        assertEquals(extension.getId(), storedExtension.getId());

        ExtensionId extensionId = new ExtensionId("id2", "version");
        extension = new TestExtension(this.testRepository, extensionId, "test");

        extension.setSummary("summary");
        extension.setWebsite("website");
        extension.setCategory("category");
        extension.setAllowedNamespaces(Arrays.asList("namespace1", "namespace2"));
        extension.setRecommended(true);
        extension.setAuthors(Arrays.asList(new DefaultExtensionAuthor("first1 last1", "url1"),
            new DefaultExtensionAuthor("first1 last1", "url2")));
        extension
            .setExtensionFeatures(Arrays.asList(new ExtensionId("feature1"), new ExtensionId("feature2", "version2")));

        ExtensionDependency dependency1 =
            new DefaultExtensionDependency("dependency1", new DefaultVersionConstraint("version1"));
        ExtensionDependency dependency2 =
            new DefaultExtensionDependency("dependency1", new DefaultVersionConstraint("version1"));
        extension.setDependencies(Arrays.asList(dependency1, dependency2));

        assertTrue(this.indexStore.add(extension, false));
        this.indexStore.commit();

        when(this.testRepository.resolve(extensionId)).thenReturn(extension);

        storedExtension = this.indexStore.getSolrExtension(extension.getId());

        assertEquals(extension.getSummary(), storedExtension.getSummary());
        assertEquals(extension.getWebSite(), storedExtension.getWebSite());
        assertEquals(extension.getCategory(), storedExtension.getCategory());
        assertEquals(new ArrayList<>(extension.getAllowedNamespaces()),
            new ArrayList<>(storedExtension.getAllowedNamespaces()));
        assertEquals(extension.isRecommended(), storedExtension.isRecommended());
        assertEquals(extension.getAuthors(), storedExtension.getAuthors());
        assertEquals(new ArrayList<>(extension.getExtensionFeatures()),
            new ArrayList<>(storedExtension.getExtensionFeatures()));
        assertEquals(new ArrayList<>(extension.getDependencies()), new ArrayList<>(storedExtension.getDependencies()));

        ExtensionQuery query = new ExtensionQuery();
        IterableResult<Extension> result = this.indexStore.search(query);

        assertEquals(0, result.getOffset());
        assertEquals(2, result.getSize());
        assertEquals(2, result.getTotalHits());

        query.setLimit(1);
        result = this.indexStore.search(query);

        assertEquals(0, result.getOffset());
        assertEquals(1, result.getSize());
        assertEquals(2, result.getTotalHits());

        query.addFilter(Extension.FIELD_AUTHOR, "first1 last1", COMPARISON.EQUAL);
        result = this.indexStore.search(query);

        assertEquals(0, result.getOffset());
        assertEquals(1, result.getSize());
        assertEquals(1, result.getTotalHits());
    }
}

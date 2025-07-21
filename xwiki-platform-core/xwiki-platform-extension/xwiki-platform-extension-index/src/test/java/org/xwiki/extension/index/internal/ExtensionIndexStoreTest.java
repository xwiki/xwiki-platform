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
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.internal.MapCache;
import org.xwiki.environment.Environment;
import org.xwiki.extension.AbstractRemoteExtension;
import org.xwiki.extension.DefaultExtensionAuthor;
import org.xwiki.extension.DefaultExtensionComponent;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.index.IndexedExtensionQuery;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.internal.converter.ExtensionAuthorConverter;
import org.xwiki.extension.internal.converter.ExtensionComponentConverter;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.extension.repository.search.ExtensionQuery.COMPARISON;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.rendering.macro.Macro;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate the extension index store.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ ExtensionIndexStore.class, ExtensionIndexSolrCoreInitializer.class, ExtensionIdConverter.class,
    ExtensionAuthorConverter.class, ExtensionFactory.class, ExtensionComponentConverter.class })
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

    private Environment mockEnvironment;

    @MockComponent
    private ExtensionManager extensionManager;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private ExtensionIndexStore indexStore;

    @MockComponent
    private ExtensionIndexSolrUtil extensionIndexSolrUtil;

    @MockComponent
    private CacheManager cacheManager;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.mockEnvironment = this.componentManager.registerMockComponent(Environment.class);

        when(this.mockEnvironment.getPermanentDirectory()).thenReturn(this.permanentDirectory);
        FileUtils.deleteDirectory(this.permanentDirectory);
        this.permanentDirectory.mkdirs();

        when(this.cacheManager.createNewCache(any())).thenReturn(new MapCache<>());

        this.testRepositoryDescriptor = new DefaultExtensionRepositoryDescriptor("test", "test", null);
        this.testRepository = mock(ExtensionRepository.class);
        when(this.testRepository.getDescriptor()).thenReturn(this.testRepositoryDescriptor);
        when(this.extensionManager.getRepository(this.testRepositoryDescriptor.getId()))
            .thenReturn(this.testRepository);
        when(this.extensionIndexSolrUtil.toSolrId(any()))
            .thenAnswer(invocation -> ExtensionIdConverter.toString(invocation.getArgument(0)));
    }

    private void assertSimpleSearch(String query, ExtensionId... expected) throws SearchException
    {
        IndexedExtensionQuery extensionQuery = new IndexedExtensionQuery(query);

        IterableResult<Extension> result = this.indexStore.search(extensionQuery);

        List<ExtensionId> resultIds = new ArrayList<>(result.getSize());
        for (Extension extension : result) {
            resultIds.add(extension.getId());
        }

        List<ExtensionId> expectIds = Arrays.asList(expected);

        assertEquals(expectIds, resultIds);
    }

    // Tests

    @Test
    void store() throws SolrServerException, IOException, ResolveException, SearchException
    {
        TestExtension extension = new TestExtension(this.testRepository, new ExtensionId("id", "version"), "test");

        this.indexStore.add(extension, true);
        this.indexStore.commit();

        SolrExtension storedExtension = this.indexStore.getSolrExtension(extension.getId());

        assertEquals(extension.getId(), storedExtension.getId());

        ExtensionId extensionId = new ExtensionId("id2", "version");
        extension = new TestExtension(this.testRepository, extensionId, "test");

        extension.setName("name");
        extension.setSummary("summary");
        extension.setWebsite("website");
        extension.setCategory("category");
        extension.setAllowedNamespaces(Arrays.asList("namespace1", "namespace2"));
        extension.setRecommended(true);
        extension.setAuthors(Arrays.asList(new DefaultExtensionAuthor("first1 last1", "url1"),
            new DefaultExtensionAuthor("first2 last2", "url2")));
        extension
            .setExtensionFeatures(Arrays.asList(new ExtensionId("feature1"), new ExtensionId("feature2", "version2")));
        extension.setComponents(
            Arrays.asList(new DefaultExtensionComponent("org.xwiki.contib.MyClassName<Generic1, Generic2>", "hint"),
                new DefaultExtensionComponent("org.xwiki.contib.MyClassName2<Generic12, Generic22>", "hint2"),
                new DefaultExtensionComponent(Macro.class.getName(), "mymacro")));

        ExtensionDependency dependency1 =
            new DefaultExtensionDependency("dependency1", new DefaultVersionConstraint("version1"));
        ExtensionDependency dependency2 =
            new DefaultExtensionDependency("dependency2", new DefaultVersionConstraint("version2"));
        extension.setDependencies(Arrays.asList(dependency1, dependency2));

        this.indexStore.add(extension, true);
        this.indexStore.commit();

        when(this.testRepository.resolve(extensionId)).thenReturn(extension);

        storedExtension = this.indexStore.getSolrExtension(extension.getId());

        assertEquals(extension.getName(), storedExtension.getName());
        assertEquals(extension.getSummary(), storedExtension.getSummary());
        assertEquals(extension.getWebSite(), storedExtension.getWebSite());
        assertEquals(extension.getCategory(), storedExtension.getCategory());
        assertEquals(new ArrayList<>(extension.getAllowedNamespaces()),
            new ArrayList<>(storedExtension.getAllowedNamespaces()));
        assertEquals(new ArrayList<>(extension.getAuthors()), new ArrayList<>(storedExtension.getAuthors()));
        assertEquals(new ArrayList<>(extension.getComponents()), new ArrayList<>(storedExtension.getComponents()));
        assertEquals(new ArrayList<>(extension.getExtensionFeatures()),
            new ArrayList<>(storedExtension.getExtensionFeatures()));
        assertEquals(new ArrayList<>(extension.getDependencies()), new ArrayList<>(storedExtension.getDependencies()));

        assertSame(storedExtension, this.indexStore.getSolrExtension(extension.getId()));

        extension.setName("other name");

        this.indexStore.add(extension, true);
        this.indexStore.commit();

        storedExtension = this.indexStore.getSolrExtension(extension.getId());

        assertEquals(extension.getName(), storedExtension.getName());

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

        result = this.indexStore
            .search(new ExtensionQuery(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_COMPONENTS_INDEX + ":hint2"));

        assertEquals(1, result.getSize());

        result = this.indexStore.search(new ExtensionQuery("hint2"));

        assertEquals(1, result.getSize());

        result = this.indexStore.search(new ExtensionQuery(ExtensionIndexSolrCoreInitializer
            .toComponentFieldName("org.xwiki.contib.MyClassName2<Generic12, Generic22>") + ":hint2"));

        assertEquals(1, result.getSize());

        result = this.indexStore.search(new ExtensionQuery(ExtensionIndexSolrCoreInitializer
            .toComponentFieldName("org.xwiki.contib.MyClassName2<Generic12, Generic22>") + ":otherhint"));

        assertEquals(0, result.getSize());

        result = this.indexStore.search(new ExtensionQuery("component_macro:mymacro"));

        assertEquals(1, result.getSize());

        result = this.indexStore.search(new ExtensionQuery("").addFilter(Extension.FIELD_COMPONENTS,
            Macro.class.getName() + "/mymacro", COMPARISON.EQUAL));

        assertEquals(1, result.getSize());

        result = this.indexStore.search(
            new ExtensionQuery("").addFilter(Extension.FIELD_COMPONENTS, Macro.class.getName(), COMPARISON.MATCH));

        assertEquals(1, result.getSize());
    }

    @Test
    void search() throws SolrServerException, IOException, SearchException
    {
        ExtensionId extensionId = new ExtensionId("id", "version");

        TestExtension extension = new TestExtension(this.testRepository, extensionId, "type");
        extension.setCategory("category");
        extension.setName("Name1 Name2");
        extension.setSummary("Summary1 Summary2");

        this.indexStore.add(extension, true);
        this.indexStore.commit();

        assertSimpleSearch("", extensionId);

        assertSimpleSearch(ExtensionIndexSolrCoreInitializer.SOLR_FIELD_EXTENSIONID + ':' + extensionId.getId(),
            extensionId);

        assertSimpleSearch("Name1", extensionId);
    }
}

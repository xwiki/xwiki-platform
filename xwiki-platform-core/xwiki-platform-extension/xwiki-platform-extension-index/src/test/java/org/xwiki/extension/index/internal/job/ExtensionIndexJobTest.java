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
package org.xwiki.extension.index.internal.job;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.index.internal.ExtensionIndexStore;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.search.SearchException;
import org.xwiki.extension.repository.search.SearchableExtensionRepository;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link ExtensionIndexJob}.
 * 
 * @version $Id$
 */
@ComponentTest
class ExtensionIndexJobTest
{
    @InjectMockComponents
    private ExtensionIndexJob job;

    @MockComponent
    private ExtensionIndexStore indexStore;

    @MockComponent
    private ExtensionRepositoryManager repositoryManager;

    private SearchableExtensionRepository repository1;

    private Extension extension11;

    private Extension extension12;

    private SearchableExtensionRepository repository2;

    private Extension extension21;

    private Extension extension22;

    public static class TestExtension extends AbstractExtension
    {
        public TestExtension(ExtensionId id, String type)
        {
            super(null, id, type);
        }
    }

    @BeforeEach
    void beforeEach() throws SearchException
    {
        this.repository1 = mock(SearchableExtensionRepository.class);
        this.extension11 = new TestExtension(new ExtensionId("id11", "version"), null);
        this.extension12 = new TestExtension(new ExtensionId("id12", "version"), null);
        when(this.repository1.search("", 0, 100)).thenReturn(
            new CollectionIterableResult<Extension>(0, 0, Arrays.asList(this.extension11, this.extension12)));

        this.repository2 = mock(SearchableExtensionRepository.class);
        this.extension21 = new TestExtension(new ExtensionId("id21", "version"), null);
        this.extension22 = new TestExtension(new ExtensionId("id22", "version"), null);
        when(this.repository2.search("", 0, 100)).thenReturn(
            new CollectionIterableResult<Extension>(0, 0, Arrays.asList(this.extension21, this.extension22)));

        when(this.repositoryManager.getRepositories()).thenReturn(Arrays.asList(this.repository1, this.repository2));
    }

    @Test
    void failingRepository1() throws SolrServerException, IOException, SearchException
    {
        when(this.repository1.search("", 0, 100)).thenThrow(SearchException.class);

        ExtensionIndexRequest request = new ExtensionIndexRequest(false, true, Collections.emptyList());

        this.job.initialize(request);
        this.job.run();

        verify(this.indexStore, never()).add(this.extension11, true);
        verify(this.indexStore, never()).add(this.extension12, true);
        verify(this.indexStore).add(this.extension21, true);
        verify(this.indexStore).add(this.extension22, true);
    }
}

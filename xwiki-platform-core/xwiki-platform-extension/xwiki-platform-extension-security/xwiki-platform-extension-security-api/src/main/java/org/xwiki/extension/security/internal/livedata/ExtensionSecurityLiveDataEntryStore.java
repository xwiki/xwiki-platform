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
package org.xwiki.extension.security.internal.livedata;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.security.internal.ExtensionSecuritySolrClient;
import org.xwiki.extension.security.internal.SolrToLiveDataEntryMapper;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;

import static org.xwiki.security.authorization.Right.ADMIN;

/**
 * Entries source for the {@link ExtensionSecurityLiveDataSource#ID} Live Data.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
@Named(ExtensionSecurityLiveDataSource.ID)
public class ExtensionSecurityLiveDataEntryStore implements LiveDataEntryStore
{
    @Inject
    private ExtensionSecuritySolrClient extensionSecuritySolrClient;

    @Inject
    private SolrToLiveDataEntryMapper solrToLiveDataEntryMapper;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    public Optional<Map<String, Object>> get(Object entryId)
    {
        return Optional.empty();
    }

    @Override
    public LiveData get(LiveDataQuery liveDataQuery) throws LiveDataException
    {
        try {
            this.authorization.checkAccess(ADMIN);
        } catch (AccessDeniedException e) {
            throw new LiveDataException("This source is restricted to admins", e);
        }

        try {
            QueryResponse searchResults = this.extensionSecuritySolrClient.solrQuery(liveDataQuery);

            LiveData liveData = new LiveData();
            liveData.setCount(searchResults.getResults().getNumFound());
            for (SolrDocument doc : searchResults.getResults()) {
                liveData.getEntries().add(this.solrToLiveDataEntryMapper.mapDocToEntries(doc));
            }
            return liveData;
        } catch (ArithmeticException e) {
            throw new LiveDataException("Failed to convert the limit for solr", e);
        } catch (SolrServerException | IOException e) {
            throw new LiveDataException("Failed to query the data source", e);
        }
    }
}

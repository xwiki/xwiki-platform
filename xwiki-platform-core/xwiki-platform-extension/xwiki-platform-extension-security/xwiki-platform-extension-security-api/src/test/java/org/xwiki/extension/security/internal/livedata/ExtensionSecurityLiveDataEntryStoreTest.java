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

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Test;
import org.xwiki.extension.security.internal.ExtensionSecuritySolrClient;
import org.xwiki.extension.security.internal.SolrToLiveDataEntryMapper;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ExtensionSecurityLiveDataEntryStore}.
 *
 * @version $Id$
 */
@ComponentTest
class ExtensionSecurityLiveDataEntryStoreTest
{
    @InjectMockComponents
    private ExtensionSecurityLiveDataEntryStore store;

    @MockComponent
    private ExtensionSecuritySolrClient extensionSecuritySolrClient;

    @MockComponent
    private SolrToLiveDataEntryMapper solrToLiveDataEntryMapper;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @Test
    void getNotAdmins() throws Exception
    {
        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.ADMIN);
        LiveDataQuery liveDataQuery = new LiveDataQuery();
        LiveDataException exception = assertThrows(LiveDataException.class, () -> this.store.get(liveDataQuery));
        assertEquals("This source is restricted to admins", exception.getMessage());
        assertEquals(AccessDeniedException.class, exception.getCause().getClass());
    }

    @Test
    void get() throws Exception
    {
        LiveDataQuery liveDataQuery = new LiveDataQuery();
        QueryResponse queryResponse = mock(QueryResponse.class);
        SolrDocumentList documentList = new SolrDocumentList();
        documentList.setNumFound(10);
        when(queryResponse.getResults()).thenReturn(documentList);
        when(this.extensionSecuritySolrClient.solrQuery(liveDataQuery)).thenReturn(queryResponse);
        
        LiveData liveData = this.store.get(liveDataQuery);
        
        LiveData expectedLD = new LiveData();
        expectedLD.setCount(10);
        assertEquals(expectedLD, liveData);
        verify(this.extensionSecuritySolrClient).solrQuery(liveDataQuery);
    }
}

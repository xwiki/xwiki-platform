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
package org.xwiki.search.solr.internal.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CursorMarkParams;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.api.FieldUtils;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Add the docId field.
 * 
 * @version $Id$
 * @since 17.6.0RC1
 */
@Component
@Named("170600000")
@Singleton
public class V170600000SearchCoreMigration extends AbstractSearchCoreMigration
{
    private static final WikiReference WIKI = new WikiReference("wiki");

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private DocumentReferenceResolver<SolrDocument> solrDocumentReferenceResolver;

    @Override
    public long getVersion()
    {
        return 170500000;
    }

    @Override
    public void migrate(XWikiSolrCore core) throws SolrException
    {
        // Add the docId field to the schema
        this.solrSchema.setStringField(core, FieldUtils.DOC_ID, false, false);

        // Commit
        this.solrSchema.commit(core);

        // Make sure all documents have the docId field
        try {
            setDocId(core);
        } catch (Exception e) {
            throw new SolrException("Failed to add the missing docId values to the Solr search code index", e);
        }
    }

    private void setDocId(XWikiSolrCore core) throws SolrServerException, IOException
    {
        SolrQuery query = new SolrQuery("-" + FieldUtils.DOC_ID + ":*");
        query.setFields(AbstractSolrCoreInitializer.SOLR_FIELD_ID, FieldUtils.SPACES, FieldUtils.NAME);
        query.setRows(10000);
        // Cursor functionality requires a sort containing a uniqueKey field tie breaker
        query.addSort(FieldUtils.ID, SolrQuery.ORDER.asc);

        String cursorMark = CursorMarkParams.CURSOR_MARK_START;
        boolean done = false;
        while (!done) {
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse response = core.getClient().query(query);
            String nextCursorMark = response.getNextCursorMark();
            setDocId(response, core);
            if (cursorMark.equals(nextCursorMark)) {
                done = true;
            }
            cursorMark = nextCursorMark;
        }
    }

    private void setDocId(QueryResponse response, XWikiSolrCore core) throws SolrServerException, IOException
    {
        List<SolrInputDocument> solrDocuments = new ArrayList<>(response.getResults().size());

        for (SolrDocument readDocument : response.getResults()) {
            // We don't really care about the wiki because the goal is to generate the local id
            DocumentReference documentReference = this.solrDocumentReferenceResolver.resolve(readDocument, WIKI);
            long docId = new XWikiDocument(documentReference).getId();

            SolrInputDocument writeDocument = new SolrInputDocument();

            this.solrUtils.setId(this.solrUtils.getId(readDocument), writeDocument);

            this.solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, FieldUtils.DOC_ID, docId, writeDocument);

            solrDocuments.add(writeDocument);
        }

        // Send docId values
        core.getClient().add(solrDocuments);
    }
}

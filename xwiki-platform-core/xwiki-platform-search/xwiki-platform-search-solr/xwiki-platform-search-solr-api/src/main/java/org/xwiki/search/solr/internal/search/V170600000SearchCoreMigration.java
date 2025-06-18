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
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
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
    @Inject
    private SpaceReferenceResolver<String> spaceResolver;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

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

    private void setDocId(XWikiSolrCore core) throws SolrServerException, IOException, SolrException
    {
        List<SolrInputDocument> solrDocuments = new ArrayList<>();

        SolrQuery query = new SolrQuery("-" + FieldUtils.DOC_ID + ":* OR -" + FieldUtils.FULLNAME + ":*");
        query.setFields(FieldUtils.SPACE_EXACT, FieldUtils.NAME_EXACT);
        query.setRows(10000);

        String cursorMark = CursorMarkParams.CURSOR_MARK_START;
        boolean done = false;
        while (!done) {
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            QueryResponse response = core.getClient().query(query);
            String nextCursorMark = response.getNextCursorMark();
            setDocId(response, solrDocuments);
            if (cursorMark.equals(nextCursorMark)) {
                done = true;
            }
            cursorMark = nextCursorMark;
        }

        // Send docId values
        core.getClient().add(solrDocuments);

        // Commit
        this.solrSchema.commit(core);
    }

    private void setDocId(QueryResponse response, List<SolrInputDocument> solrDocuments)
    {
        for (SolrDocument readDocument : response.getResults()) {
            String name = this.solrUtils.get(FieldUtils.NAME_EXACT, readDocument);
            String space = this.solrUtils.get(FieldUtils.SPACE_EXACT, readDocument);
            if (name != null && space != null) {
                SpaceReference spaceReference = this.spaceResolver.resolve(space);
                DocumentReference documentReference = new DocumentReference(name, spaceReference);
                String fullName = this.localSerializer.serialize(documentReference);
                long docId = new XWikiDocument(documentReference).getId();

                SolrInputDocument writeDocument = new SolrInputDocument();

                this.solrUtils.setId(this.solrUtils.getId(readDocument), writeDocument);

                this.solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, FieldUtils.DOC_ID, docId, writeDocument);
                this.solrUtils.setAtomic(SolrUtils.ATOMIC_UPDATE_MODIFIER_SET, FieldUtils.FULLNAME, fullName,
                    writeDocument);

                solrDocuments.add(writeDocument);
            }
        }
    }
}

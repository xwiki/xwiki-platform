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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;
import org.xwiki.search.solr.internal.api.FieldUtils;

/**
 * Add the docId field.
 * 
 * @version $Id$
 * @since 17.7.0RC1
 */
@Component
@Named("170700000")
@Singleton
public class V170700000SearchCoreMigration extends AbstractSearchCoreMigration
{
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
        if (this.solrSchema.getFields(core, false).get(FieldUtils.DOC_ID) == null) {
            // Add the docId field to the schema
            this.solrSchema.setStringField(core, FieldUtils.DOC_ID, false, false);
        }

        // The standard re-index is taking care of re-indexing document with missing docId
    }
}

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
package org.xwiki.index.tree.internal.parentchild;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

/**
 * Helper component for querying the documents.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Component(roles = DocumentQueryHelper.class)
@Singleton
public class DocumentQueryHelper
{
    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Inject
    @Named("hidden/document")
    private QueryFilter hiddenDocumentQueryFilter;

    @Inject
    private QueryManager queryManager;

    /**
     * Creates a query that returns documents matching the specified constraints.
     * 
     * @param constraints the document constraints
     * @param parameters the query parameters
     * @param config the tree configuration properties
     * @return the query
     * @throws QueryException if creating the query fails
     */
    public Query getQuery(List<String> constraints, Map<String, Object> parameters, Map<String, Object> config)
        throws QueryException
    {
        String fromClause = "";
        List<String> finalConstraints = new ArrayList<String>();
        Map<String, Object> finalParameters = new HashMap<String, Object>();
        String xclass = (String) config.get("filterByClass");
        if (!StringUtils.isEmpty(xclass)) {
            fromClause = ", BaseObject as obj";
            finalConstraints.add("obj.name = doc.fullName");
            finalConstraints.add("obj.className = :class");
            finalConstraints.add("doc.fullName <> :template");
            finalParameters.put("class", xclass);
            finalParameters.put("template", StringUtils.removeEnd(xclass, "Class") + "Template");
        }
        finalConstraints.addAll(constraints);
        finalParameters.putAll(parameters);
        String whereClause = "where " + StringUtils.join(finalConstraints, " and ");
        String statement =
            StringUtils.join(Arrays.asList(fromClause, whereClause, "order by lower(doc.name), doc.name"), ' ');
        Query query = this.queryManager.createQuery(statement, Query.HQL);
        for (Map.Entry<String, Object> entry : finalParameters.entrySet()) {
            query.bindValue(entry.getKey(), entry.getValue());
        }
        if (Boolean.TRUE.equals(config.get("filterHiddenDocuments"))) {
            query.addFilter(this.hiddenDocumentQueryFilter);
        }
        return query;
    }

    /**
     * Resolves the results obtained by executing {@link #getQuery(List, Map, Map)}.
     * 
     * @param query a query created with {@link #getQuery(List, Map, Map)}
     * @param offset the offset in the list of results
     * @param limit the maximum number of results to return
     * @param parentReference the base reference used when resolving the results
     * @return the list of document references
     * @throws QueryException if executing the query fails
     */
    public List<DocumentReference> resolve(Query query, int offset, int limit, EntityReference parentReference)
        throws QueryException
    {
        query.setOffset(offset);
        query.setLimit(limit);
        List<DocumentReference> documentReferences = new ArrayList<DocumentReference>();
        for (Object result : query.execute()) {
            documentReferences.add(this.explicitDocumentReferenceResolver.resolve((String) result, parentReference));
        }
        return documentReferences;
    }
}

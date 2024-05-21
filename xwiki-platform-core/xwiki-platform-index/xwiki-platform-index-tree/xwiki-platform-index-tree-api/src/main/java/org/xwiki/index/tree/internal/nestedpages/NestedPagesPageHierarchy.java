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
package org.xwiki.index.tree.internal.nestedpages;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.tree.PageHierarchy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;

/**
 * The nested pages hierarchy.
 *
 * @version $Id$
 */
@Component
@Named("nestedpages")
@Singleton
public class NestedPagesPageHierarchy implements PageHierarchy
{
    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("count")
    private QueryFilter countFilter;

    @Inject
    @Named("document")
    private QueryFilter documenFilter;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    private class NestedPagesChildrenQuery extends AbstractChildrenQuery
    {
        NestedPagesChildrenQuery(EntityReference parentReference)
        {
            super(parentReference);
        }

        @Override
        public List<DocumentReference> getDocumentReferences() throws QueryException
        {
            if (canHaveChildren()) {
                return buildQuery(true).addFilter(documenFilter).execute();
            } else {
                return List.of();
            }
        }

        @Override
        public int count() throws QueryException
        {
            if (canHaveChildren()) {
                Query query = buildQuery(false).addFilter(countFilter);
                return ((Long) query.execute().get(0)).intValue();
            } else {
                return 0;
            }
        }

        private boolean canHaveChildren()
        {
            return this.parentReference.getType() == EntityType.WIKI
                || (this.parentReference.getType() == EntityType.DOCUMENT
                    && "WebHome".equals(this.parentReference.getName()));
        }

        private Query buildQuery(boolean ordered) throws QueryException
        {
            String statement = ", XWikiSpace AS space WHERE doc.space = space.reference";
            if (this.parentReference.getType() == EntityType.WIKI) {
                statement += " AND doc.name = 'WebHome' AND space.parent IS NULL";
            } else {
                statement += " AND ((doc.name <> 'WebHome' AND doc.space = :parent) OR"
                    + " (doc.name = 'WebHome' AND space.parent = :parent))";
            }
            if (StringUtils.isNotEmpty(this.text)) {
                if (this.parentReference.getType() == EntityType.WIKI) {
                    statement += " AND space.name LIKE :text";
                } else {
                    statement += " AND ((doc.name <> 'WebHome' AND doc.name like :text) OR"
                        + " (doc.name = 'WebHome' AND space.name LIKE :text))";
                }
            }
            if (ordered) {
                statement += " ORDER BY doc.fullName";
            }

            Query query = queryManager.createQuery(statement, Query.HQL);

            EntityReference spaceReference = this.parentReference.extractReference(EntityType.SPACE);
            if (spaceReference != null) {
                query = query.bindValue("parent", localEntityReferenceSerializer.serialize(spaceReference));
            }

            if (StringUtils.isNotEmpty(this.text)) {
                query = query.bindValue("text").anyChars().literal(this.text).anyChars().query();
            }

            if (this.offset > 0) {
                query = query.setOffset(this.offset);
            }

            if (this.limit > 0) {
                query = query.setLimit(this.limit);
            }

            String wiki = this.parentReference.extractReference(EntityType.WIKI).getName();
            query = query.setWiki(wiki);

            return query;
        }
    }

    @Override
    public ChildrenQuery getChildren(WikiReference wikiReference)
    {
        return new NestedPagesChildrenQuery(wikiReference);
    }

    @Override
    public ChildrenQuery getChildren(DocumentReference documentReference)
    {
        return new NestedPagesChildrenQuery(documentReference);
    }
}

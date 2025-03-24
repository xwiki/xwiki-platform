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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.index.tree.internal.AbstractChildDocumentsTreeNodeGroup;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;

/**
 * A tree node group that contains the child documents of a specified parent entity, in the parent-child hierarchy.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@Component
@Named("childDocuments/parentChild")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class ChildDocumentsTreeNodeGroup extends AbstractChildDocumentsTreeNodeGroup
{
    private static final String IS_NOT_TRANSLATION = "doc.translation = 0";

    @Inject
    @Named("count")
    protected QueryFilter countQueryFilter;

    @Inject
    private DocumentQueryHelper documentQueryHelper;

    @Inject
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    protected ChildDocumentsTreeNodeGroup(String type)
    {
        super("childDocuments");
    }

    @Override
    protected List<DocumentReference> getChildDocuments(EntityReference parentReference, int offset, int limit)
        throws QueryException
    {
        if (parentReference.getType() == EntityType.WIKI) {
            return getChildDocuments(new WikiReference(parentReference), offset, limit);
        } else {
            return getChildDocuments(new DocumentReference(parentReference), offset, limit);
        }
    }

    private List<DocumentReference> getChildDocuments(WikiReference parentReference, int offset, int limit)
        throws QueryException
    {
        return this.documentQueryHelper.resolve(getChildrenQuery(parentReference), offset, limit, parentReference);
    }

    private List<DocumentReference> getChildDocuments(DocumentReference parentReference, int offset, int limit)
        throws QueryException
    {
        return this.documentQueryHelper.resolve(getChildrenQuery(parentReference), offset, limit, parentReference);
    }

    private Query getChildrenQuery(DocumentReference parentReference) throws QueryException
    {
        List<String> constraints = new ArrayList<>();
        constraints.add(IS_NOT_TRANSLATION);
        constraints
            .add("(doc.parent in (:absoluteRef, :localRef) or (doc.space = :space and doc.parent = :relativeRef))");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("space", this.localEntityReferenceSerializer.serialize(parentReference.getParent()));
        parameters.put("absoluteRef", this.defaultEntityReferenceSerializer.serialize(parentReference));
        parameters.put("localRef", this.localEntityReferenceSerializer.serialize(parentReference));
        parameters.put("relativeRef",
            this.compactEntityReferenceSerializer.serialize(parentReference, parentReference.getParent()));
        Query query = this.documentQueryHelper.getQuery(constraints, parameters, getProperties());
        query.setWiki(parentReference.getWikiReference().getName());
        return query;
    }

    private Query getChildrenQuery(WikiReference parentReference) throws QueryException
    {
        // In Oracle the empty parent is actually null.
        Query query = this.documentQueryHelper.getQuery(
            Arrays.asList("(doc.parent = '' or doc.parent is null)", IS_NOT_TRANSLATION),
            Collections.<String, Object>emptyMap(), getProperties());
        query.setWiki(parentReference.getName());
        return query;
    }

    @Override
    protected int getChildDocumentsCount(EntityReference parentReference) throws QueryException
    {
        if (parentReference.getType() == EntityType.WIKI) {
            return getChildDocumentsCount(new WikiReference(parentReference));
        } else {
            return getChildDocumentsCount(new DocumentReference(parentReference));
        }
    }

    private int getChildDocumentsCount(WikiReference parentReference) throws QueryException
    {
        Query query = getChildrenQuery(parentReference);
        query.addFilter(this.countQueryFilter);
        return ((Long) query.execute().get(0)).intValue();
    }

    private int getChildDocumentsCount(DocumentReference parentReference) throws QueryException
    {
        Query query = getChildrenQuery(parentReference);
        query.addFilter(this.countQueryFilter);
        return ((Long) query.execute().get(0)).intValue();
    }

    @Override
    protected boolean canHaveChildDocuments(EntityReference parentReference)
    {
        return parentReference.getType() == EntityType.WIKI || parentReference.getType() == EntityType.DOCUMENT;
    }
}

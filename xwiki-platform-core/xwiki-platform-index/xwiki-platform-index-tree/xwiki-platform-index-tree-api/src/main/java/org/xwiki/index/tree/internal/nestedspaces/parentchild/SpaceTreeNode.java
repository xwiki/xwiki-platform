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
package org.xwiki.index.tree.internal.nestedspaces.parentchild;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;

/**
 * The space node in the parent-child over nested spaces hierarchy.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("space/parentChildOnNestedSpaces")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class SpaceTreeNode extends org.xwiki.index.tree.internal.nestedspaces.SpaceTreeNode
{
    private static final String PARAMETER_ABSOLUTE_REFERENCE = "absoluteRef";

    private static final String PARAMETER_LOCAL_REFERENCE = "localRef";

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Inject
    @Named("topLevelPage/parentChildOnNestedSpaces")
    private QueryFilter topLevelPageFilter;

    @Override
    protected List<? extends EntityReference> getChildren(SpaceReference spaceReference, int offset, int limit)
        throws QueryException
    {
        Query query = getChildrenQuery(spaceReference, offset, limit);

        if (areTerminalDocumentsShown()) {
            // Include only the documents that either don't have a parent document or that have a parent document in a
            // different space.
            query.addFilter(this.topLevelPageFilter);
            DocumentReference absoluteReference =
                this.explicitDocumentReferenceResolver.resolve(String.valueOf('%'), spaceReference);
            query.bindValue(PARAMETER_ABSOLUTE_REFERENCE,
                this.defaultEntityReferenceSerializer.serialize(absoluteReference));
            query.bindValue(PARAMETER_LOCAL_REFERENCE,
                this.localEntityReferenceSerializer.serialize(absoluteReference));
        }

        return query.execute();
    }

    @Override
    protected int getChildDocumentsCount(SpaceReference spaceReference) throws QueryException
    {
        List<String> constraints = new ArrayList<String>();
        Map<String, Object> parameters = new HashMap<String, Object>();

        // Include only the documents that either don't have a parent document or that have a parent document in a
        // different space. Note that in Oracle the empty string is stored as null.
        String hasNoParent = "doc.parent = '' or doc.parent is null";
        String hasParentOutsideSpace =
            "doc.parent like '%.%' and doc.parent not like :absoluteRef and doc.parent not like :localRef";
        constraints.add(String.format("((%s) or (%s))", hasNoParent, hasParentOutsideSpace));

        DocumentReference absoluteReference =
            this.explicitDocumentReferenceResolver.resolve(String.valueOf('%'), spaceReference);
        parameters.put(PARAMETER_ABSOLUTE_REFERENCE,
            this.defaultEntityReferenceSerializer.serialize(absoluteReference));
        parameters.put(PARAMETER_LOCAL_REFERENCE, this.localEntityReferenceSerializer.serialize(absoluteReference));

        return getChildDocumentsCount(spaceReference, constraints, parameters);
    }
}

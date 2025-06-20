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
package org.xwiki.index.tree;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryException;
import org.xwiki.stability.Unstable;

/**
 * Used to navigate the hierarchy of wiki pages (documents).
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@Role
@Unstable
public interface PageHierarchy
{
    /**
     * Encapsulates a query to retrieve the child pages.
     */
    interface ChildrenQuery
    {
        /**
         * @param offset the index of the first child to return
         * @return this query
         */
        ChildrenQuery withOffset(int offset);

        /**
         * @param limit the maximum number of children to return
         * @return this query
         */
        ChildrenQuery withLimit(int limit);

        /**
         * @param text a search text to filter the child pages by their name or title
         * @return this query
         */
        ChildrenQuery matching(String text);

        /**
         * @return the list of child document references
         */
        List<DocumentReference> getDocumentReferences() throws QueryException;

        /**
         * @return the number of child pages
         */
        int count() throws QueryException;
    }

    /**
     * Base class for {@link ChildrenQuery} implementations.
     */
    abstract class AbstractChildrenQuery implements ChildrenQuery
    {
        protected final EntityReference parentReference;

        protected int offset;

        protected int limit = -1;

        protected String text = "";

        /**
         * Creates a new query to retrieve the children of the given parent entity.
         *
         * @param parentReference the reference of the parent entity for which to retrieve the children
         */
        protected AbstractChildrenQuery(EntityReference parentReference)
        {
            this.parentReference = parentReference;
        }

        @Override
        public ChildrenQuery withOffset(int offset)
        {
            this.offset = offset;
            return this;
        }

        @Override
        public ChildrenQuery withLimit(int limit)
        {
            this.limit = limit;
            return this;
        }

        @Override
        public ChildrenQuery matching(String text)
        {
            this.text = text;
            return this;
        }
    }

    /**
     * @param wikiReference the reference of the wiki for which to retrieve the top level pages
     * @return a query to retrieve the top level pages of the given wiki
     */
    ChildrenQuery getChildren(WikiReference wikiReference);

    /**
     * @param documentReference the reference of the parent document for which to retrieve the children
     * @return a query to retrieve the children of the given document
     */
    ChildrenQuery getChildren(DocumentReference documentReference);
}

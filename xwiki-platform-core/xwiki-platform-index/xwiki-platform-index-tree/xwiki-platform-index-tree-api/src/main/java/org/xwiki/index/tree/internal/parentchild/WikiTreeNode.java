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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;

/**
 * The wiki tree node for the (deprecated) parent-child hierarchy.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Component
@Named("wiki/parentChild")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class WikiTreeNode extends org.xwiki.index.tree.internal.nestedpages.WikiTreeNode
{
    @Inject
    @Named("count")
    private QueryFilter countQueryFilter;

    @Inject
    private DocumentQueryHelper documentQueryHelper;

    @Override
    protected List<? extends EntityReference> getChildren(WikiReference wikiReference, int offset, int limit)
        throws QueryException
    {
        return this.documentQueryHelper.resolve(getChildrenQuery(wikiReference), offset, limit, wikiReference);
    }

    private Query getChildrenQuery(WikiReference parentReference) throws QueryException
    {
        // In Oracle the empty parent is actually null.
        Query query = this.documentQueryHelper.getQuery(
            Arrays.asList("(doc.parent = '' or doc.parent is null)", "doc.translation = 0"),
            Collections.<String, Object>emptyMap(), getProperties());
        query.setWiki(parentReference.getName());
        return query;
    }

    @Override
    protected int getChildCount(WikiReference wikiReference) throws QueryException
    {
        Query query = getChildrenQuery(wikiReference);
        query.addFilter(this.countQueryFilter);
        return ((Long) query.execute().get(0)).intValue();
    }
}

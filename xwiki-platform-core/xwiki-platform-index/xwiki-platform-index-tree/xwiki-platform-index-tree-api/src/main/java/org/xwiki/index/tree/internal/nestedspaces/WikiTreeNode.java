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
package org.xwiki.index.tree.internal.nestedspaces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.index.tree.internal.AbstractEntityTreeNode;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

/**
 * The wiki node in the nested spaces tree.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("wiki/nestedSpaces")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class WikiTreeNode extends AbstractEntityTreeNode
{
    /**
     * Default constructor.
     */
    public WikiTreeNode()
    {
        super("wiki");
    }

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        EntityReference wikiReference = resolve(nodeId);
        if (wikiReference instanceof WikiReference) {
            try {
                return serialize(getChildren(new WikiReference(wikiReference), offset, limit));
            } catch (QueryException e) {
                this.logger.warn("Failed to retrieve the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return Collections.emptyList();
    }

    private List<? extends EntityReference> getChildren(WikiReference wikiReference, int offset, int limit)
        throws QueryException
    {
        List<String> constraints = new ArrayList<>();
        constraints.add("parent is null");
        if (!areHiddenEntitiesShown()) {
            constraints.add("hidden <> true");
        }
        String statement = StringUtils.join(
            Arrays.asList("select name from XWikiSpace", whereClause(constraints), "order by lower(name), name"), ' ');

        Query query = this.queryManager.createQuery(statement, Query.HQL);
        query.setWiki(wikiReference.getName());
        query.setOffset(offset);
        query.setLimit(limit);

        List<SpaceReference> spaceReferences = new ArrayList<>();
        for (Object result : query.execute()) {
            String name = (String) result;
            spaceReferences.add(new SpaceReference(name, wikiReference));
        }

        return spaceReferences;
    }

    @Override
    public int getChildCount(String nodeId)
    {
        EntityReference wikiReference = resolve(nodeId);
        if (wikiReference instanceof WikiReference) {
            try {
                return getChildCount(new WikiReference(wikiReference));
            } catch (QueryException e) {
                this.logger.warn("Failed to count the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return 0;
    }

    private int getChildCount(WikiReference wikiReference) throws QueryException
    {
        return getChildSpacesCount(wikiReference);
    }

    @Override
    public String getParent(String nodeId)
    {
        return FARM_NODE_ID;
    }
}

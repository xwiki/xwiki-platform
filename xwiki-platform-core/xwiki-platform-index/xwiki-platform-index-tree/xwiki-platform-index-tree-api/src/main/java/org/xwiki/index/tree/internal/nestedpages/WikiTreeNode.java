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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.index.tree.internal.AbstractEntityTreeNode;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;

/**
 * The wiki tree node.
 * 
 * @version $Id$
 * @since 8.3M2
 * @since 7.4.5
 */
@Component
@Named("wiki")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class WikiTreeNode extends AbstractEntityTreeNode
{
    @Inject
    private LocalizationContext localizationContext;

    @Inject
    @Named("topLevelPage/nestedPages")
    private QueryFilter topLevelPageFilter;

    @Inject
    @Named("hiddenPage/nestedPages")
    private QueryFilter hiddenPageFilter;

    @Inject
    @Named("documentReferenceResolver/nestedPages")
    private QueryFilter documentReferenceResolverFilter;

    @Override
    public List<String> getChildren(String nodeId, int offset, int limit)
    {
        EntityReference wikiReference = resolve(nodeId);
        if (wikiReference != null && wikiReference.getType() == EntityType.WIKI) {
            try {
                return serialize(getChildren(new WikiReference(wikiReference), offset, limit));
            } catch (QueryException e) {
                this.logger.warn("Failed to retrieve the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return Collections.emptyList();
    }

    protected List<? extends EntityReference> getChildren(WikiReference wikiReference, int offset, int limit)
        throws QueryException
    {
        String orderBy = getOrderBy();
        Query query;
        if ("title".equals(orderBy)) {
            query = this.queryManager.getNamedQuery("nonTerminalPagesOrderedByTitle");
            query.bindValue("locale", this.localizationContext.getCurrentLocale().toString());
        } else {
            // Query only the spaces table.
            query = this.queryManager.createQuery(
                "select reference, 0 as terminal from XWikiSpace page order by lower(name), name", Query.HQL);
        }
        query.setWiki(wikiReference.getName());
        query.setOffset(offset);
        query.setLimit(limit);

        query.addFilter(this.topLevelPageFilter);

        if (!areHiddenEntitiesShown()) {
            query.addFilter(this.hiddenPageFilter);
        }

        return query.addFilter(this.documentReferenceResolverFilter).execute();
    }

    @Override
    public int getChildCount(String nodeId)
    {
        EntityReference wikiReference = resolve(nodeId);
        if (wikiReference != null && wikiReference.getType() == EntityType.WIKI) {
            try {
                return getChildCount(new WikiReference(wikiReference));
            } catch (QueryException e) {
                this.logger.warn("Failed to count the children of [{}]. Root cause [{}].", nodeId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return 0;
    }

    protected int getChildCount(WikiReference wikiReference) throws QueryException
    {
        return getChildSpacesCount(wikiReference);
    }

    @Override
    public String getParent(String nodeId)
    {
        return "farm:*";
    }
}

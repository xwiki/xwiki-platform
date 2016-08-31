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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.index.tree.internal.AbstractEntityTreeNode;
import org.xwiki.localization.LocalizationContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

/**
 * The wiki tree node.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Component
@Named("wiki")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class WikiTreeNode extends AbstractEntityTreeNode
{
    @Inject
    private LocalizationContext localizationContext;

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
        List<String> constraints = new ArrayList<String>();
        constraints.add("page.parent is null");
        if (!areHiddenEntitiesShown()) {
            constraints.add("page.hidden <> true");
        }
        String whereClause = whereClause(constraints);

        Map<String, Object> parameters = new HashMap<String, Object>();
        String orderBy = getOrderBy();
        String statement;
        if ("title".equals(orderBy)) {
            statement = StringUtils.join(Arrays.asList("select page.name from XWikiPage page",
                "left join page.translations defaultTranslation with defaultTranslation.locale = ''",
                "left join page.translations translation with translation.locale = :locale", whereClause,
                "order by lower(coalesce(translation.title, defaultTranslation.title, page.name)), "
                    + "coalesce(translation.title, defaultTranslation.title, page.name)"),
                ' ');
            parameters.put("locale", this.localizationContext.getCurrentLocale().toString());
        } else {
            // Query only the spaces table.
            statement = StringUtils.join(
                Arrays.asList("select name from XWikiSpace page", whereClause, "order by lower(name), name"), ' ');
        }

        Query query = this.queryManager.createQuery(statement, Query.HQL);
        query.setWiki(wikiReference.getName());
        query.setOffset(offset);
        query.setLimit(limit);
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            query.bindValue(entry.getKey(), entry.getValue());
        }

        List<DocumentReference> documentReferences = new ArrayList<DocumentReference>();
        for (Object result : query.execute()) {
            String name = (String) result;
            documentReferences.add(new DocumentReference(wikiReference.getName(), name, getDefaultDocumentName()));
        }

        return documentReferences;
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

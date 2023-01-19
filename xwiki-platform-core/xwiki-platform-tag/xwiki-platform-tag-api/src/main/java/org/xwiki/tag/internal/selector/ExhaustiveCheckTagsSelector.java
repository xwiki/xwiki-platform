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
package org.xwiki.tag.internal.selector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.tag.internal.TagException;
import org.xwiki.tag.internal.TagsSelector;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static org.xwiki.security.authorization.Right.VIEW;

/**
 * Implementation of {@link TagsSelector} where all the elements are checked for view rights before being returned.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@Component
@Named(ExhaustiveCheckTagsSelector.HINT)
@Singleton
public class ExhaustiveCheckTagsSelector extends AbstractTagsSelector
{
    /**
     * Hint for this component.
     */
    public static final String HINT = "exhaustive";

    @Override
    public List<String> getAllTags() throws TagException
    {
        String hql = "select distinct doc.fullName as fullName, elements(prop.list) "
            + "from XWikiDocument as doc, BaseObject as obj, DBStringListProperty as prop "
            + "where obj.name=doc.fullName "
            + "and obj.className='XWiki.TagClass' "
            + "and obj.id=prop.id.id "
            + "and prop.id.name='tags' "
            + "order by fullName";

        try {
            List<Object[]> results = this.contextProvider.get()
                .getWiki()
                .getStore()
                .getQueryManager()
                .createQuery(hql, Query.HQL)
                .addFilter(this.hiddenDocumentQueryFilter)
                .execute();
            return computedTagsFromQuery(results);
        } catch (QueryException e) {
            throw new TagException(String.format("Failed to get all tags for query [%s]", hql), e);
        }
    }

    @Override
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, List<?> parameterValues)
        throws TagException
    {
        return getTagsFromViewableDocuments(fromHql, whereHql, parameterValues);
    }

    private List<String> computedTagsFromQuery(List<Object[]> results)
    {
        Set<String> tagsSet = new HashSet<>();
        String previousDoc = null;
        boolean previousDocViewRight = false;
        for (Object[] cols : results) {
            String documentReferenceStr = (String) cols[0];
            // Since the documents are sorted by their document reference, we know that we have to re-compute the 
            // rights only for the first result, or when we pass to the next document reference.
            String tag = (String) cols[1];
            // If the tag is already added to the list, there is no point in checking again if it's allowed to add it.
            if (tagsSet.contains(tag)) {
                continue;
            }
            if (!Objects.equals(previousDoc, documentReferenceStr)) {
                DocumentReference documentReference =
                    this.stringDocumentReferenceResolver.resolve(documentReferenceStr);
                previousDocViewRight = this.contextualAuthorizationManager.hasAccess(VIEW, documentReference);
                previousDoc = documentReferenceStr;
            }
            if (previousDocViewRight) {
                tagsSet.add(tag);
            }
        }

        List<String> tagsList = new ArrayList<>(tagsSet);
        tagsList.sort(CASE_INSENSITIVE_ORDER);
        return tagsList;
    }

    @Override
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, Map<String, ?> parameters)
        throws TagException
    {
        return getTagsFromViewableDocuments(fromHql, whereHql, parameters);
    }

    private Map<String, Integer> getTagsFromViewableDocuments(String fromHql, String whereHql, Object parameters)
        throws TagException
    {
        String from =
            "select distinct doc.fullName as fullName, elements(prop.list) from XWikiDocument as doc, "
                + "BaseObject as tagobject, DBStringListProperty as prop";
        String where = " where tagobject.name=doc.fullName and tagobject.className='XWiki.TagClass' and "
            + "tagobject.id=prop.id.id and prop.id.name='tags' and doc.translation=0";

        // If at least one of the fragments is passed, the query should be matching XWiki documents
        if (!StringUtils.isBlank(fromHql) || !StringUtils.isBlank(whereHql)) {
            from += fromHql;
        }
        if (!StringUtils.isBlank(whereHql)) {
            where += " and " + whereHql;
        }

        String hql = from + where + " order by fullName";

        try {
            Query query = this.contextProvider.get()
                .getWiki()
                .getStore()
                .getQueryManager()
                .createQuery(hql, Query.HQL)
                .addFilter(this.hiddenDocumentQueryFilter);
            if (parameters != null) {
                if (parameters instanceof Map) {
                    query.bindValues((Map) parameters);
                } else {
                    query.bindValues((List) parameters);
                }
            }

            return computeCountsFromQuery(query.execute());
        } catch (QueryException e) {
            throw new TagException(
                String.format("Failed to get tag count for query [%s], with parameters [%s]", hql, parameters), e);
        }
    }

    private Map<String, Integer> computeCountsFromQuery(List<Object[]> results)
    {
        Map<String, Integer> counts = new TreeMap<>(CASE_INSENSITIVE_ORDER);
        String previousDoc = null;
        boolean previousDocViewRight = false;
        for (Object[] cols : results) {
            String documentReferenceStr = (String) cols[0];
            // Since the documents are sorted by their document reference, we know that we have to re-compute the 
            // rights only for the first result, or when we pass to the next document reference.
            if (!Objects.equals(previousDoc, documentReferenceStr)) {
                DocumentReference documentReference =
                    this.stringDocumentReferenceResolver.resolve(documentReferenceStr);
                previousDocViewRight = this.contextualAuthorizationManager.hasAccess(VIEW, documentReference);
                previousDoc = documentReferenceStr;
            }
            if (previousDocViewRight) {
                counts.compute((String) cols[1], (s, count) -> count == null ? 1 : count + 1);
            }
        }
        return counts;
    }
}

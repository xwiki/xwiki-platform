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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.tag.internal.TagException;
import org.xwiki.tag.internal.TagsSelector;

import static org.xwiki.query.Query.HQL;

/**
 * Unsafe (but fastest) implementation of {@link TagsSelector} where no right check are performed on the returned tags
 * and documents. This implementation should be used only on context where tags and document paths are not considered a
 * critical data. It is also advised to switch to this implementation only if tag related operation are slow and other
 * performance improvements (e.g., increase the document cache size) are not applicable.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@Component
@Named(UnsafeTagsSelector.HINT)
@Singleton
public class UnsafeTagsSelector extends AbstractTagsSelector
{
    /**
     * The hint for this component.
     */
    public static final String HINT = "unsafe";

    @Override
    public List<String> getAllTags() throws TagException
    {
        String hql = "select distinct elements(prop.list) "
            + "from XWikiDocument as doc, BaseObject as obj, DBStringListProperty as prop "
            + "where obj.name=doc.fullName "
            + "and obj.className='XWiki.TagClass' "
            + "and obj.id=prop.id.id "
            + "and prop.id.name='tags'";
        try {
            List<String> results = this.contextProvider.get()
                .getWiki()
                .getStore()
                .getQueryManager()
                .createQuery(hql, HQL)
                .addFilter(this.hiddenDocumentQueryFilter)
                .execute();
            results.sort(String.CASE_INSENSITIVE_ORDER);
            return results;
        } catch (QueryException e) {
            throw new TagException(String.format("Failed to get all tags for query [%s]", hql), e);
        }
    }

    @Override
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, List<?> parameterValues)
        throws TagException
    {
        return internalGetTagCountForQuery(fromHql, whereHql, parameterValues);
    }

    @Override
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, Map<String, ?> parameters)
        throws TagException
    {
        return internalGetTagCountForQuery(fromHql, whereHql, parameters);
    }

    private Map<String, Integer> internalGetTagCountForQuery(String fromHql, String whereHql, Object parameters)
        throws TagException
    {
        List<Object[]> results;

        // The cardinality is computed by the database so that a single row is returned per distinct tag value instead
        // of one row per (document, tag) pair, which doesn't scale on wikis with a lot of tagged documents.
        String from = "select item, count(*) from XWikiDocument as doc, BaseObject as tagobject, "
            + "DBStringListProperty as prop join prop.list item";
        String where = " where tagobject.name=doc.fullName and tagobject.className='XWiki.TagClass' and "
            + "tagobject.id=prop.id.id and prop.id.name='tags' and doc.translation=0";

        // If at least one of the fragments is passed, the query should be matching XWiki documents
        if (!StringUtils.isBlank(fromHql) || !StringUtils.isBlank(whereHql)) {
            from += fromHql;
        }
        if (!StringUtils.isBlank(whereHql)) {
            where += " and " + whereHql;
        }

        String hql = from + where + " group by item";

        try {
            Query query = this.contextProvider.get().getWiki().getStore().getQueryManager().createQuery(hql, Query.HQL);
            if (parameters != null) {
                if (parameters instanceof Map map) {
                    query.bindValues(map);
                } else {
                    query.bindValues((List) parameters);
                }
            }
            query.addFilter(this.hiddenDocumentQueryFilter);
            results = query.execute();
        } catch (QueryException e) {
            throw new TagException(
                String.format("Failed to get tag count for query [%s], with parameters [%s]", hql, parameters), e);
        }

        // The collation is required to be binary on all supported databases, so the database groups the values by
        // their exact case and the case variants of a same tag still need to be merged here. Merging them in the
        // query instead would make the returned variant depend on the database's own case mapping, and would only
        // save a handful of rows. Sorting first makes the variant that is kept as the returned tag deterministic.
        results.sort(Comparator.comparing(row -> (String) row[0], String.CASE_INSENSITIVE_ORDER));

        // The map ignores the case, so merging on a tag whose case variant has already been inserted keeps that first
        // variant as the key and sums the counts.
        Map<String, Integer> tagCount = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Object[] result : results) {
            tagCount.merge((String) result[0], ((Number) result[1]).intValue(), Integer::sum);
        }

        return tagCount;
    }

    @Override
    boolean isUnsafe()
    {
        return true;
    }
}

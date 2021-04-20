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
package com.xpn.xwiki.plugin.tag;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.internal.HiddenDocumentFilter;
import org.xwiki.query.internal.UniqueDocumentFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

/**
 * TagQueryUtils handles queries allowing to search and count tags within the wiki.
 *
 * @version $Id$
 * @since 5.0M1
 * @deprecated since 13.1RC1, use {@link TagQueryService} instead
 */
@Deprecated
public final class TagQueryUtils
{
    /**
     * Hint of the "hidden" QueryFilter.
     */
    public static final String HIDDEN_QUERYFILTER_HINT = HiddenDocumentFilter.HINT;

    /**
     * Utility class, private constructor.
     */
    private TagQueryUtils()
    {
    }

    /**
     * Get all tags within the wiki.
     *
     * @param context XWiki context.
     * @return list of tags (alphabetical order).
     * @throws com.xpn.xwiki.XWikiException if search query fails (possible failures: DB access problems, etc).
     */
    public static List<String> getAllTags(XWikiContext context) throws XWikiException
    {
        List<String> results;

        String hql = "select distinct elements(prop.list) from XWikiDocument as doc, BaseObject as obj, "
            + "DBStringListProperty as prop where obj.name=doc.fullName and obj.className='XWiki.TagClass' and "
            + "obj.id=prop.id.id and prop.id.name='tags'";

        try {
            Query query = context.getWiki().getStore().getQueryManager().createQuery(hql, Query.HQL);
            query.addFilter(Utils.getComponent(QueryFilter.class, HiddenDocumentFilter.HINT));
            results = query.execute();
        } catch (QueryException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                String.format("Failed to get all tags for query [%s]", hql), e);
        }

        Collections.sort(results, String.CASE_INSENSITIVE_ORDER);

        return results;
    }

    /**
     * Get cardinality map of tags matching a parameterized hql query.
     *
     * @param fromHql the <code>from</code> fragment of the hql query
     * @param whereHql the <code>where</code> fragment of the hql query
     * @param parameterValues list of parameter values for the query
     * @param context XWiki context.
     * @return map of tags (alphabetical order) with their occurrences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 1.18
     * @see TagPluginApi#getTagCountForQuery(String, String, java.util.List)
     */
    public static Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, List<?> parameterValues,
        XWikiContext context) throws XWikiException
    {
        return getTagCountForQuery(fromHql, whereHql, (Object) parameterValues, context);
    }

    /**
     * Get cardinality map of tags matching a parameterized hql query.
     *
     * @param fromHql the <code>from</code> fragment of the hql query
     * @param whereHql the <code>where</code> fragment of the hql query
     * @param parameters map of named parameters for the query
     * @param context XWiki context.
     * @return map of tags (alphabetical order) with their occurrences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 11.7RC1
     * @see TagPluginApi#getTagCountForQuery(String, String, java.util.List)
     */
    public static Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, Map<String, ?> parameters,
        XWikiContext context) throws XWikiException
    {
        return getTagCountForQuery(fromHql, whereHql, (Object) parameters, context);
    }

    private static Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, Object parameters,
        XWikiContext context) throws XWikiException
    {
        List<String> results;
        Map<String, Integer> tagCount = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        String from = "select elements(prop.list) from XWikiDocument as doc, BaseObject as tagobject, "
            + "DBStringListProperty as prop";
        String where = " where tagobject.name=doc.fullName and tagobject.className='XWiki.TagClass' and "
            + "tagobject.id=prop.id.id and prop.id.name='tags' and doc.translation=0";

        // If at least one of the fragments is passed, the query should be matching XWiki documents
        if (!StringUtils.isBlank(fromHql) || !StringUtils.isBlank(whereHql)) {
            from += fromHql;
        }
        if (!StringUtils.isBlank(whereHql)) {
            where += " and " + whereHql;
        }

        String hql = from + where;

        try {
            Query query = context.getWiki().getStore().getQueryManager().createQuery(hql, Query.HQL);
            if (parameters != null) {
                if (parameters instanceof Map) {
                    query.bindValues((Map) parameters);
                } else {
                    query.bindValues((List) parameters);
                }
            }
            query.addFilter(Utils.getComponent(QueryFilter.class, HiddenDocumentFilter.HINT));
            results = query.execute();
        } catch (QueryException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                String.format("Failed to get tag count for query [%s], with parameters [%s]", hql, parameters), e);
        }

        Collections.sort(results, String.CASE_INSENSITIVE_ORDER);
        Map<String, String> processedTags = new HashMap<>();

        // We have to manually build a cardinality map since we have to ignore tags case.
        for (String result : results) {
            // This key allows to keep track of the case variants we've encountered.
            String lowerTag = result.toLowerCase();

            // We store the first case variant to reuse it in the final result set.
            if (!processedTags.containsKey(lowerTag)) {
                processedTags.put(lowerTag, result);
            }

            String tagCountKey = processedTags.get(lowerTag);
            int tagCountForTag = 0;
            if (tagCount.get(tagCountKey) != null) {
                tagCountForTag = tagCount.get(tagCountKey);
            }
            tagCount.put(tagCountKey, tagCountForTag + 1);
        }

        return tagCount;
    }

    /**
     * Get non-hidden documents with the passed tags.
     *
     * @param tag a list of tags to match.
     * @param context XWiki context.
     * @return list of docNames.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     */
    public static List<String> getDocumentsWithTag(String tag, XWikiContext context) throws XWikiException
    {
        return getDocumentsWithTag(tag, false, context);
    }

    /**
     * Get documents with the passed tags with the result depending on whether the caller decides to include hidden
     * documents or not.
     *
     * @param tag a list of tags to match.
     * @param includeHiddenDocuments if true then include hidden documents
     * @param context XWiki context.
     * @return list of docNames.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 6.2M1
     */
    public static List<String> getDocumentsWithTag(String tag, boolean includeHiddenDocuments, XWikiContext context)
        throws XWikiException
    {
        List<String> results;

        String hql = ", BaseObject as obj, DBStringListProperty as prop join prop.list item"
            + " where obj.className=:className and obj.name=doc.fullName and obj.id=prop.id.id and prop.id.name='tags'"
            + " and lower(item)=lower(:item) order by doc.fullName";

        try {
            Query query = context.getWiki().getStore().getQueryManager().createQuery(hql, Query.HQL);
            query.bindValue("className", TagPlugin.TAG_CLASS);
            query.bindValue("item", tag);
            query.addFilter(Utils.getComponent(QueryFilter.class, UniqueDocumentFilter.HINT));
            if (!includeHiddenDocuments) {
                query.addFilter(Utils.getComponent(QueryFilter.class, HiddenDocumentFilter.HINT));
            }

            results = query.execute();
        } catch (QueryException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                String.format("Failed to search for document with tag [%s]", tag), e);
        }

        return results;
    }
}

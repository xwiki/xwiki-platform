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

import java.util.List;
import java.util.Map;

import org.xwiki.query.internal.HiddenDocumentFilter;
import org.xwiki.stability.Unstable;
import org.xwiki.tag.internal.TagException;
import org.xwiki.tag.internal.TagsSelector;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

import static com.xpn.xwiki.XWikiException.ERROR_XWIKI_UNKNOWN;
import static com.xpn.xwiki.XWikiException.MODULE_XWIKI_STORE;

/**
 * TagQueryUtils handles queries allowing to search and count tags within the wiki.
 *
 * @version $Id$
 * @since 5.0M1
 */
public final class TagQueryUtils
{
    /**
     * Hint of the "hidden" QueryFilter.
     */
    public static final String HIDDEN_QUERYFILTER_HINT = HiddenDocumentFilter.HINT;

    private static TagsSelector tagsSelector;

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
        try {
            return getTagsSelector().getAllTags();
        } catch (TagException e) {
            throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_UNKNOWN, "Failed to get all tags", e);
        }
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
        try {
            return getTagsSelector().getTagCountForQuery(fromHql, whereHql, parameterValues);
        } catch (TagException e) {
            throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_UNKNOWN, String.format(
                "Failed to count tags for where fromHql = [%s] and whereHql = [%s] and parameterValues = [%s].",
                fromHql, whereHql, parameterValues), e);
        }
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
        try {
            return getTagsSelector().getTagCountForQuery(fromHql, whereHql, parameters);
        } catch (TagException e) {
            throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_UNKNOWN, String.format(
                "Failed to count tags for where fromHql = [%s] and whereHql = [%s] and parameters = [%s].", fromHql,
                whereHql, parameters), e);
        }
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
        return getDocumentsWithTag(tag, includeHiddenDocuments, false);
    }

    /**
     * Get documents with the passed tags with the result depending on whether the caller decides to include hidden
     * documents or not.
     *
     * @param tag a list of tags to match.
     * @param includeHiddenDocuments if true then include hidden documents
     * @param caseSensitive {@code true} if the case of the tag should be used in the query {@code false} for getting
     * results whatever the case of the tag.
     * @return list of docNames.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 17.0.0RC1
     * @since 16.10.1
     */
    @Unstable
    public static List<String> getDocumentsWithTag(String tag, boolean includeHiddenDocuments, boolean caseSensitive)
        throws XWikiException
    {
        try {
            return getTagsSelector().getDocumentsWithTag(tag, includeHiddenDocuments, caseSensitive);
        } catch (TagException e) {
            throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_UNKNOWN,
                String.format("Failed to get all documents with tag [%s]", tag), e);
        }
    }

    private static TagsSelector getTagsSelector()
    {
        if (tagsSelector == null) {
            tagsSelector = Utils.getComponent(TagsSelector.class);
        }
        return tagsSelector;
    }
}

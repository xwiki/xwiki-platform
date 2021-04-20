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
package org.xwiki.tag.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.query.internal.HiddenDocumentFilter;
import org.xwiki.query.internal.UniqueDocumentFilter;
import org.xwiki.tag.TagException;

import static com.xpn.xwiki.XWikiConstant.TAG_CLASS;
import static org.xwiki.query.Query.HQL;
import static org.xwiki.tag.internal.TagDocumentManager.TAGS_FIELD_NAME;
import static org.xwiki.tag.internal.TagDocumentManager.XWIKI_TAG_CLASS;

/**
 * This class provides the services to query and manipulate the page's tags.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Component(roles = { TagQueryManager.class })
@Singleton
public class TagQueryManager
{
    private static final String QUERY_TAG_BIND = "tag";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    @Named(HiddenDocumentFilter.HINT)
    private QueryFilter hiddenDocumentQueryFilter;

    @Inject
    @Named(UniqueDocumentFilter.HINT)
    private QueryFilter uniqueDocumentQueryFilter;

    /**
     * Count the number of pages with the provided tag.
     *
     * @param tag the tag
     * @return the number of pages with the provided tag
     * @throws TagException in case of error during the query
     */
    public long countPages(String tag) throws TagException
    {
        try {
            String statement = initQuery(tag, "select count(doc.fullName) ");
            Query query = this.queryManager.createQuery(statement, HQL);
            if (tag != null) {
                query = query.bindValue(QUERY_TAG_BIND, tag);
            }
            List<Long> execute = query.execute();
            return execute.get(0);
        } catch (QueryException e) {
            throw new TagException(String.format("Failed to count the number of documents with tag [%s]", tag), e);
        }
    }

    /**
     * Returns a paginated list of {@link DocumentReference} with the provided tag.
     *
     * @param tag the tag
     * @param limit the pagination limit
     * @param offset the pagination offset
     * @return the paginated list of {@link DocumentReference} with the provided tag
     * @throws TagException in case of error during the query
     */
    public List<DocumentReference> getPages(String tag, Integer limit, Integer offset) throws TagException
    {
        try {
            String statement = initQuery(tag, "select doc.fullName ");
            Query query = this.queryManager.createQuery(statement, HQL);
            if (tag != null) {
                query = query.bindValue(QUERY_TAG_BIND, tag);
            }
            return query
                .addFilter(this.hiddenDocumentQueryFilter)
                .setLimit(limit)
                .setOffset(offset)
                .<String>execute()
                .stream()
                .map(it -> this.documentReferenceResolver.resolve(it))
                .collect(Collectors.toList());
        } catch (QueryException e) {
            throw new TagException(
                String.format("Failed to get the documents with tag [%s], limit [%s], offset [%s]", tag, limit, offset),
                e);
        }
    }

    /**
     * Get all tags within the wiki.
     *
     * @return list of tags (alphabetical order).
     * @throws TagException if search query fails (possible failures: DB access problems, etc).
     */
    public List<String> getAllTags() throws TagException
    {
        String hql =
            "select distinct elements(prop.list) "
                + "from XWikiDocument as doc, "
                + "BaseObject as obj, "
                + "DBStringListProperty as prop "
                + "where obj.name = doc.fullName "
                + "and obj.className='" + XWIKI_TAG_CLASS + "' "
                + "and obj.id = prop.id.id "
                + "and prop.id.name='" + TAGS_FIELD_NAME + "'";

        try {
            Query query = this.queryManager.createQuery(hql, Query.HQL);
            query.addFilter(this.hiddenDocumentQueryFilter);
            List<String> results = query.execute();
            results.sort(String.CASE_INSENSITIVE_ORDER);
            return results;
        } catch (QueryException e) {
            throw new TagException(String.format("Failed to get all tags for query [%s]", hql), e);
        }
    }

    /**
     * Get cardinality map of tags matching a parameterized hql query.
     *
     * @param fromHql the <code>from</code> fragment of the hql query
     * @param whereHql the <code>where</code> fragment of the hql query
     * @param parameterValues list of parameter values for the query
     * @return map of tags (alphabetical order) with their occurrences counts.
     * @throws TagException if search query fails (possible failures: DB access problems, etc).
     * @see #getTagCountForQuery(String, String, List
     */
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, List<?> parameterValues)
        throws TagException
    {
        return getTagCountForQuery(fromHql, whereHql, (Object) parameterValues);
    }

    /**
     * Get cardinality map of tags matching a parameterized hql query.
     *
     * @param fromHql the <code>from</code> fragment of the hql query
     * @param whereHql the <code>where</code> fragment of the hql query
     * @param parameters map of named parameters for the query
     * @return map of tags (alphabetical order) with their occurrences counts.
     * @throws TagException if search query fails (possible failures: DB access problems, etc).
     * @see #getTagCountForQuery(String, String, Map
     */
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, Map<String, ?> parameters)
        throws TagException
    {
        return getTagCountForQuery(fromHql, whereHql, (Object) parameters);
    }

    private Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, Object parameters)
        throws TagException
    {
        List<String> results;

        String from = "select elements(prop.list) "
            + "from XWikiDocument as doc, "
            + "BaseObject as tagobject, "
            + "DBStringListProperty as prop";
        String where = " where tagobject.name = doc.fullName "
            + "and tagobject.className='" + XWIKI_TAG_CLASS + "' "
            + "and tagobject.id = prop.id.id "
            + "and prop.id.name='" + TAGS_FIELD_NAME + "' "
            + "and doc.translation = 0";

        // If at least one of the fragments is passed, the query should be matching XWiki documents
        if (!StringUtils.isBlank(fromHql) || !StringUtils.isBlank(whereHql)) {
            from += fromHql;
        }
        if (!StringUtils.isBlank(whereHql)) {
            where += " and " + whereHql;
        }

        String hql = from + where;

        try {
            Query query = this.queryManager.createQuery(hql, Query.HQL);
            if (parameters != null) {
                if (parameters instanceof Map) {
                    query.bindValues((Map) parameters);
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

        results.sort(String.CASE_INSENSITIVE_ORDER);

        Map<String, String> processedTags = new HashMap<>();
        Map<String, Integer> tagCount = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
     * @return list of docNames.
     * @throws TagException if search query fails (possible failures: DB access problems, etc).
     */
    public List<String> getDocumentsWithTag(String tag) throws TagException
    {
        return getDocumentsWithTag(tag, false);
    }

    /**
     * Get documents with the passed tags with the result depending on whether the caller decides to include hidden
     * documents or not.
     *
     * @param tag a list of tags to match.
     * @param includeHiddenDocuments if {@code true} then include hidden documents
     * @return list of docNames.
     * @throws TagException if search query fails (possible failures: DB access problems, etc).
     */
    public List<String> getDocumentsWithTag(String tag, boolean includeHiddenDocuments)
        throws TagException
    {
        String hql = ", BaseObject as obj, "
            + "DBStringListProperty as prop join prop.list item "
            + "where obj.className = :className "
            + "and obj.name = doc.fullName "
            + "and obj.id = prop.id.id "
            + "and prop.id.name='" + TAGS_FIELD_NAME + "' "
            + "and lower(item) = lower(:item) "
            + "order by doc.fullName";

        try {
            Query query = this.queryManager.createQuery(hql, Query.HQL);
            query.bindValue("className", TAG_CLASS);
            query.bindValue("item", tag);
            query.addFilter(this.uniqueDocumentQueryFilter);
            if (!includeHiddenDocuments) {
                query.addFilter(this.hiddenDocumentQueryFilter);
            }

            return query.execute();
        } catch (QueryException e) {
            throw new TagException(String.format("Failed to search for document with tag [%s]", tag), e);
        }
    }

    /**
     * Count the number of documents with the given tag.
     *
     * @param tag tag tag
     * @param includeHiddenDocuments if {@code true} then include hidden documents
     * @return the number of document with the given tag.
     * @throws TagException in case of error during the query execution
     */
    public Long countDocumentsWithTag(String tag, boolean includeHiddenDocuments) throws TagException
    {
        String hql = "select count(doc.fullName) "
            + "from XWikiDocument as doc, " 
            + "BaseObject as obj, "
            + "DBStringListProperty as prop join prop.list item "
            + "where obj.name = doc.fullName " 
            + "and obj.className = :className "
            + "and obj.name = doc.fullName "
            + "and obj.id = prop.id.id "
            + "and prop.id.name='" + TAGS_FIELD_NAME + "' "
            + "and lower(item) = lower(:item)";

        try {
            Query query = this.queryManager.createQuery(hql, Query.HQL);
            query.bindValue("className", TAG_CLASS);
            query.bindValue("item", tag);
            query.addFilter(this.uniqueDocumentQueryFilter);
            if (!includeHiddenDocuments) {
                query.addFilter(this.hiddenDocumentQueryFilter);
            }

            return query.<Long>execute().get(0);
        } catch (QueryException e) {
            throw new TagException(String.format("Failed to search for document with tag [%s]", tag), e);
        }
    }

    private String initQuery(String tag, String s)
    {
        String statement = s
            + "from XWikiDocument as  doc, "
            + "BaseObject as tagobject, "
            + "DBStringListProperty as tagsproperty "
            + "where tagobject.name=doc.fullName "
            + "and tagobject.className='" + XWIKI_TAG_CLASS + "' "
            + "and tagsproperty.id=tagobject.id.id "
            + "and tagsproperty.id.name='" + TAGS_FIELD_NAME + "' "
            + "and doc.translation=0 ";
        if (tag != null) {
            statement = statement + "and :tag IN elements(tagsproperty.list)";
        }
        return statement;
    }
}

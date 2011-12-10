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
package com.xpn.xwiki.plugin.globalsearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.core.plugin.XWikiPluginMessageTool;
import com.xpn.xwiki.plugin.globalsearch.tools.GlobalSearchQuery;
import com.xpn.xwiki.plugin.globalsearch.tools.GlobalSearchResult;

/**
 * Tool to be able to make and merge multi wikis search queries.
 * 
 * @version $Id$
 */
final class GlobalSearch
{
    /**
     * The logging tool.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(GlobalSearch.class);

    /**
     * Hql "select" keyword.
     */
    private static final String SELECT_KEYWORD = "select";

    /**
     * Hql "select distinct" keyword.
     */
    private static final String SELECT_DISTINCT_KEYWORD = "select distinct";

    /**
     * Hql "from" keyword.
     */
    private static final String FROM_KEYWORD = "from";

    /**
     * Hql "where" keyword.
     */
    private static final String WHERE_KEYWORD = "where";

    /**
     * Hql "order by" keyword.
     */
    private static final String ORDER_KEYWORD = "order by";

    /**
     * Hql "order by" descendant keyword.
     */
    private static final String ORDER_DESC = "desc";

    /**
     * Name of the field containing document space in the HQL query.
     */
    private static final String HQL_DOC_SPACE = "doc.space";

    /**
     * Name of the field containing document name in the HQL query.
     */
    private static final String HQL_DOC_NAME = "doc.name";

    /**
     * The searchDocument and searchDocumentsNames initial select query part.
     */
    private static final String SEARCHDOC_INITIAL_SELECT = "select distinct doc.space, doc.name";

    /**
     * The searchDocument and searchDocumentsNames initial select query part when distinct documents by language.
     */
    private static final String SEARCHDOC_INITIAL_SELECT_LANG = "select distinct doc.space, doc.name, doc.language";

    /**
     * The searchDocument and searchDocumentsNames initial from query part.
     */
    private static final String SEARCHDOC_INITIAL_FROM = " from XWikiDocument as doc";

    /**
     * Comma.
     */
    private static final String FIELD_SEPARATOR = ",";

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * The plugin internationalization service.
     */
    private XWikiPluginMessageTool messageTool;

    /**
     * Hidden constructor of GlobalSearch only access via getInstance().
     * 
     * @param messageTool the plugin internationalization service.
     */
    public GlobalSearch(XWikiPluginMessageTool messageTool)
    {
        this.messageTool = messageTool;
    }

    // ////////////////////////////////////////////////////////////////////////////

    /**
     * @param context the XWiki context.
     * @return the names of all virtual wikis.
     * @throws XWikiException error when getting the list of virtual wikis.
     */
    private Collection<String> getAllWikiNameList(XWikiContext context) throws XWikiException
    {
        Collection<String> wikiNames = context.getWiki().getVirtualWikisDatabaseNames(context);

        if (!wikiNames.contains(context.getMainXWiki())) {
            wikiNames.add(context.getMainXWiki());
        }

        return wikiNames;
    }

    /**
     * Execute query in all provided wikis and return list containing all results. Compared to XWiki Platform search,
     * searchDocuments and searchDocumentsName it's potentially "time-consuming" since it issues one request per
     * provided wiki.
     * 
     * @param query the query parameters.
     * @param context the XWiki context.
     * @return the search result as list of {@link GlobalSearchResult} containing all selected fields values.
     * @throws XWikiException error when executing query.
     */
    public Collection<GlobalSearchResult> search(GlobalSearchQuery query, XWikiContext context) throws XWikiException
    {
        List<GlobalSearchResult> resultList = Collections.emptyList();

        List<String> selectColumns = parseSelectColumns(query.getHql());
        List<Object[]> orderColumns = parseOrderColumns(query.getHql());

        Collection<String> wikiNameList;
        if (context.getWiki().isVirtualMode()) {
            wikiNameList = query.getWikiNameList();
            if (wikiNameList.isEmpty()) {
                wikiNameList = getAllWikiNameList(context);
            }
        } else {
            wikiNameList = Collections.singletonList(context.getMainXWiki());
        }

        int max = query.getMax() > 0 ? query.getMax() + (query.getStart() > 0 ? query.getStart() : 0) : 0;

        String database = context.getDatabase();
        try {
            resultList = new LinkedList<GlobalSearchResult>();

            for (String wikiName : wikiNameList) {
                context.setDatabase(wikiName);

                List< ? > resultsTmp =
                    context.getWiki().getStore().search(query.getHql(), max, 0, query.getParameterList(), context);

                insertResults(wikiName, resultList, resultsTmp, query, selectColumns, orderColumns, context);
            }
        } finally {
            context.setDatabase(database);
        }

        if (resultList.size() > max || query.getStart() > 0) {
            resultList =
                resultList.subList(query.getStart() > 0 ? query.getStart() : 0, resultList.size() > max ? max
                    : resultList.size());
        }

        return resultList;
    }

    /**
     * Insert a list of result in the sorted main list.
     * 
     * @param wikiName the name of the wiki from where the list <code>list</code> come.
     * @param sortedList the sorted main list.
     * @param list the list to insert.
     * @param query the query parameters.
     * @param selectColumns the names of selected fields.
     * @param orderColumns the fields to order.
     * @param context the XWiki context.
     */
    private void insertResults(String wikiName, List<GlobalSearchResult> sortedList, Collection< ? > list,
        GlobalSearchQuery query, List<String> selectColumns, List<Object[]> orderColumns, XWikiContext context)
    {
        boolean sort = !sortedList.isEmpty();

        for (Object item : list) {
            Object[] objects = (Object[]) item;

            GlobalSearchResult result = new GlobalSearchResult(wikiName, selectColumns, objects);

            if (sort) {
                insertResult(sortedList, result, query, selectColumns, orderColumns, context);
            } else {
                sortedList.add(result);
            }
        }
    }

    /**
     * Insert a result of result in the sorted main list.
     * 
     * @param sortedList the sorted main list.
     * @param result the fields values to insert.
     * @param query the query parameters.
     * @param selectColumns the names of selected fields.
     * @param orderColumns the fields to order.
     * @param context the XWiki context.
     */
    private void insertResult(List<GlobalSearchResult> sortedList, GlobalSearchResult result, GlobalSearchQuery query,
        List<String> selectColumns, List<Object[]> orderColumns, XWikiContext context)
    {
        int max = query.getMax() > 0 ? query.getMax() + (query.getStart() > 0 ? query.getStart() : 0) : -1;

        int index = 0;
        for (Iterator<GlobalSearchResult> itSorted = sortedList.iterator(); itSorted.hasNext()
            && (max <= 0 || index < max); ++index) {
            GlobalSearchResult sortedResult = itSorted.next();

            if (compare(sortedResult, result, orderColumns) > 0) {
                break;
            }
        }

        if (max <= 0 || index < max) {
            sortedList.add(index, result);
        }
    }

    /**
     * Compare two results depends on list of order fields.
     * 
     * @param result1 the first result to compare.
     * @param result2 the second result to compare.
     * @param orderColumns the list of order fields.
     * @return a negative integer, zero, or a positive integer as <code>map1</code> is less than, equal to, or greater
     *         than <code>map2</code>.
     */
    private int compare(GlobalSearchResult result1, GlobalSearchResult result2, List<Object[]> orderColumns)
    {
        for (Object[] orderField : orderColumns) {
            int result = compare(result1, result2, orderField);

            if (result != 0) {
                return result;
            }
        }

        return 0;
    }

    /**
     * Compare two results depends on order fields.
     * 
     * @param result1 the first result to compare.
     * @param result2 the second result to compare.
     * @param orderField the order fields.
     * @return a negative integer, zero, or a positive integer as <code>map1</code> is less than, equal to, or greater
     *         than <code>map2</code>.
     */
    private int compare(GlobalSearchResult result1, GlobalSearchResult result2, Object[] orderField)
    {
        int result = 0;

        String fieldName = (String) orderField[0];
        boolean fieldAsc = ((Boolean) orderField[1]).booleanValue();

        Object value1 = result1.get(fieldName);
        Object value2 = result2.get(fieldName);

        if (value1 instanceof String) {
            result = ((String) value1).compareToIgnoreCase((String) value2);
        } else if (value1 instanceof Comparable) {
            result = ((Comparable) value1).compareTo(value2);
        }

        return fieldAsc ? result : -result;
    }

    /**
     * Extract names of selected fields from hql query.
     * 
     * @param hql the hql query. The hql has some constraints:
     *            <ul>
     *            <li>"*" is not supported in SELECT clause.</li>
     *            <li>All ORDER BY fields has to be listed in SELECT clause.</li>
     *            </ul>
     * @return the names of selected fields from hql query.
     */
    private List<String> parseSelectColumns(String hql)
    {
        List<String> columnList = new ArrayList<String>();

        int selectEnd = 0;
        int selectIndex = hql.toLowerCase().indexOf(SELECT_DISTINCT_KEYWORD);
        if (selectIndex < 0) {
            selectIndex = hql.toLowerCase().indexOf(SELECT_KEYWORD);

            if (selectIndex < 0) {
                selectIndex = 0;
            } else {
                selectEnd = SELECT_KEYWORD.length();
            }
        } else {
            selectEnd = SELECT_DISTINCT_KEYWORD.length();
        }

        int fromIndex = hql.toLowerCase().indexOf(FROM_KEYWORD);

        if (fromIndex >= 0) {
            String selectContent = hql.substring(selectIndex + selectEnd + 1, fromIndex);
            String[] columnsTable = selectContent.split(FIELD_SEPARATOR);
            for (int i = 0; i < columnsTable.length; ++i) {
                String[] column = columnsTable[i].trim().split("\\s");
                String columnName = column[0];
                columnList.add(columnName);
            }
        }

        return columnList;
    }

    /**
     * Extract names of "order by" fields from hql query.
     * 
     * @param hql the hql query.
     * @return the names of "order by" fields from hql query.
     */
    private List<Object[]> parseOrderColumns(String hql)
    {
        List<Object[]> columnList = new ArrayList<Object[]>();

        int orderIndex = hql.toLowerCase().lastIndexOf(ORDER_KEYWORD);

        if (orderIndex >= 0) {
            String orderContent = hql.substring(orderIndex + ORDER_KEYWORD.length() + 1);
            String[] columnsTable = orderContent.split(FIELD_SEPARATOR);
            for (int i = 0; i < columnsTable.length; ++i) {
                String orderField = columnsTable[i];
                String[] orderFieldTable = orderContent.split("\\s+");

                orderField = orderFieldTable[0];

                Boolean asc = Boolean.TRUE;
                if (orderFieldTable.length > 1 && orderFieldTable[1].trim().toLowerCase().equals(ORDER_DESC)) {
                    asc = Boolean.FALSE;
                }

                columnList.add(new Object[] {orderField.trim(), asc});
            }
        }

        return columnList;
    }

    /**
     * @param queryPrefix the start of the SQL query (for example "select distinct doc.space, doc.name")
     * @param whereSQL the where clause to append
     * @return the full formed SQL query, to which the order by columns have been added as returned columns (this is
     *         required for example for HSQLDB).
     */
    protected String createSearchDocumentsHQLQuery(String queryPrefix, String whereSQL)
    {
        StringBuffer hql = new StringBuffer(queryPrefix);

        String normalizedWhereSQL;
        if (whereSQL == null) {
            normalizedWhereSQL = "";
        } else {
            normalizedWhereSQL = whereSQL.trim();
        }

        Collection<Object[]> orderColumns = parseOrderColumns(normalizedWhereSQL);

        for (Object[] orderField : orderColumns) {
            if (!orderField[0].equals(HQL_DOC_SPACE) && !orderField[0].equals(HQL_DOC_NAME)) {
                hql.append(FIELD_SEPARATOR);
                hql.append(orderField[0]);
            }
        }

        hql.append(SEARCHDOC_INITIAL_FROM);

        if (normalizedWhereSQL.length() != 0) {
            if ((!normalizedWhereSQL.startsWith(WHERE_KEYWORD)) && (!normalizedWhereSQL.startsWith(FIELD_SEPARATOR))) {
                hql.append(" ");
                hql.append(WHERE_KEYWORD);
                hql.append(" ");
            } else {
                hql.append(" ");
            }
            hql.append(normalizedWhereSQL);
        }

        return hql.toString();
    }

    /**
     * Search wiki pages in all provided wikis and return list containing found {@link XWikiDocument}. Compared to
     * XWiki Platform search, searchDocuments and searchDocumentsName it's potentially "time-consuming" since it issues
     * one request per provided wiki.
     * 
     * @param query the query parameters.
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param customMapping inject custom mapping in session.
     * @param checkRight if true check for each found document if context's user has "view" rights for it.
     * @param context the XWiki context.
     * @return the found {@link XWikiDocument}.
     * @throws XWikiException error when searching for documents.
     */
    private Collection<GlobalSearchResult> searchDocumentsNamesInfos(GlobalSearchQuery query,
        boolean distinctbylanguage, boolean customMapping, boolean checkRight, XWikiContext context)
        throws XWikiException
    {
        if (!query.getHql().toLowerCase().startsWith(SELECT_KEYWORD)) {
            String select = distinctbylanguage ? SEARCHDOC_INITIAL_SELECT_LANG : SEARCHDOC_INITIAL_SELECT;

            query.setHql(createSearchDocumentsHQLQuery(select, query.getHql()));
        }

        return search(query, context);
    }

    /**
     * Search wiki pages in all provided wikis and return list containing found {@link XWikiDocument}. Compared to
     * XWiki Platform search, searchDocuments and searchDocumentsName it's potentially "time-consuming" since it issues
     * one request per provided wiki.
     * 
     * @param query the query parameters.
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param customMapping inject custom mapping in session.
     * @param checkRight if true check for each found document if context's user has "view" rights for it.
     * @param context the XWiki context.
     * @return the found {@link XWikiDocument}.
     * @throws XWikiException error when searching for documents.
     */
    public Collection<XWikiDocument> searchDocuments(GlobalSearchQuery query, boolean distinctbylanguage,
        boolean customMapping, boolean checkRight, XWikiContext context) throws XWikiException
    {
        Collection<GlobalSearchResult> results =
            searchDocumentsNamesInfos(query, distinctbylanguage, customMapping, checkRight, context);

        List<XWikiDocument> documents = new ArrayList<XWikiDocument>(results.size());

        String database = context.getDatabase();
        try {
            for (GlobalSearchResult result : results) {
                XWikiDocument doc = new XWikiDocument();
                doc.setSpace((String) result.get(HQL_DOC_SPACE));
                doc.setName((String) result.get(HQL_DOC_NAME));

                context.setDatabase(result.getWikiName());

                doc = context.getWiki().getStore().loadXWikiDoc(doc, context);

                if (checkRight) {
                    if (!context.getWiki().getRightService().checkAccess("view", doc, context)) {
                        continue;
                    }
                }

                documents.add(doc);
            }
        } finally {
            context.setDatabase(database);
        }

        return documents;
    }

    /**
     * Search wiki pages in all provided wikis and return list containing found {@link XWikiDocument}. Compared to
     * XWiki Platform search, searchDocuments and searchDocumentsName it's potentially "time-consuming" since it issues
     * one request per provided wiki.
     * 
     * @param query the query parameters.
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param customMapping inject custom mapping in session.
     * @param checkRight if true check for each found document if context's user has "view" rights for it.
     * @param context the XWiki context.
     * @return the found {@link XWikiDocument}.
     * @throws XWikiException error when searching for documents.
     */
    public Collection<String> searchDocumentsNames(GlobalSearchQuery query, boolean distinctbylanguage,
        boolean customMapping, boolean checkRight, XWikiContext context) throws XWikiException
    {
        Collection<GlobalSearchResult> results =
            searchDocumentsNamesInfos(query, distinctbylanguage, customMapping, checkRight, context);

        List<String> documentsNames = new ArrayList<String>(results.size());

        for (GlobalSearchResult result : results) {
            documentsNames.add(result.getWikiName() + ":" + result.get(HQL_DOC_SPACE) + "." + result.get(HQL_DOC_NAME));
        }

        return documentsNames;
    }
}

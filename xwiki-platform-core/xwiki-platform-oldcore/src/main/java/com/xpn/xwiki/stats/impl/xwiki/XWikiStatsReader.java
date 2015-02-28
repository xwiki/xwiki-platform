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
package com.xpn.xwiki.stats.impl.xwiki;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.Duration;
import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.Range;
import com.xpn.xwiki.criteria.impl.RangeFactory;
import com.xpn.xwiki.criteria.impl.Scope;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.stats.impl.RefererStats;
import com.xpn.xwiki.stats.impl.StatsUtil;
import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;
import com.xpn.xwiki.stats.impl.VisitStats;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.web.DownloadAction;
import com.xpn.xwiki.web.SaveAction;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.ViewAction;

/**
 * Reader statistics from XWiki database.
 *
 * @version $Id$
 * @since 1.4M2
 */
public class XWikiStatsReader
{
    /**
     * Logging tool.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiStatsReader.class);

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    private EntityReferenceSerializer<String> compactwikiEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.TYPE_STRING, "compactwiki");

    /**
     * Return the statistics action stored.
     *
     * @param action the action.
     * @param size the maximum size of the list to return.
     * @param context the XWiki context.
     * @return the list of recent statistics action stored.
     */
    public Collection<Object> getRecentActions(String action, int size, XWikiContext context)
    {
        List<Object> list = new ArrayList<Object>();

        if ((action.equals(ViewAction.VIEW_ACTION) || (action.equals(SaveAction.ACTION_NAME)))) {
            Collection<?> actions = StatsUtil.getRecentActionFromSessions(context, action);

            if (actions != null) {
                Object[] actionsarray = actions.toArray();
                CollectionUtils.reverseArray(actionsarray);
                int nb = Math.min(actions.size(), size);
                for (int i = 0; i < nb; i++) {
                    list.add(actionsarray[i]);
                }
            }
        }

        return list;
    }

    /**
     * @param range the range.
     * @return the corresponding sort order.
     */
    private String getHqlSortOrderFromRange(Range range)
    {
        String sortOrder;

        if (range.getSize() < 0) {
            sortOrder = "asc";
        } else {
            sortOrder = "desc";
        }

        return sortOrder;
    }

    /**
     * @param domain the provided domain.
     * @return the domain to use in HQL query.
     */
    private String getHqlValidDomain(String domain)
    {
        if (domain == null || domain.trim().length() == 0) {
            return "%";
        }

        return domain;
    }

    /**
     * @param scope the set of documents for which to retrieve statistics.
     * @param paramList the values to insert in the SQL query.
     * @return the name filter HQL query part.
     */
    private String getHqlNameFilterFromScope(Scope scope, List<Object> paramList)
    {
        String nameFilter;

        // Note that these queries are made to work with Oracle which treats empty strings as null.
        if (scope.getType() == Scope.SPACE_SCOPE && StringUtils.isEmpty(scope.getName())) {
            // Select all names that are not page names (i.e. they are space names), excluding empty names which
            // represent global scopes.
            nameFilter = "name not like '%.%' and (name <> '' or (name is not null and '' is null))";
        } else if (scope.getType() == Scope.GLOBAL_SCOPE && StringUtils.isEmpty(scope.getName())) {
            // Select all names that are empty (i.e. global)
            nameFilter = "name = '' or name is null";
        } else if (scope.getType() == Scope.PAGE_SCOPE && StringUtils.isEmpty(scope.getName())) {
            // Select all names that are page names
            nameFilter = "name like '%.%'";
        } else {
            nameFilter = "name like ?";
            paramList.add(scope.getPattern());
        }

        return nameFilter;
    }

    /**
     * Shows how the statistics for the specified action have evolved over the specified period of time.
     *
     * @param action the action for which to retrieve statistics.
     * @param scope the set of documents to consider.
     * @param period the period of time, including its start date but excluding its end date.
     * @param step the step used for sampling the period.
     * @param context the XWiki context.
     * @return a map of (date, actionCount) pairs.
     */
    public Map<DateTime, Integer> getActionStatistics(String action, Scope scope, Period period, Duration step,
        XWikiContext context)
    {
        DateTime stepStart = new DateTime(period.getStart());
        DateTime periodEnd = new DateTime(period.getEnd());
        org.joda.time.Period stepDuration =
            new org.joda.time.Period(step.getYears(), step.getMonths(), step.getWeeks(), step.getDays(), 0, 0, 0, 0);

        Map<DateTime, Integer> activity = new HashMap<DateTime, Integer>();
        while (stepStart.compareTo(periodEnd) < 0) {
            DateTime stepEnd = stepStart.plus(stepDuration);
            if (stepEnd.compareTo(periodEnd) > 0) {
                stepEnd = periodEnd;
            }
            List<DocumentStats> stats =
                getDocumentStatistics(action, scope, new Period(stepStart.getMillis(), stepEnd.getMillis()),
                    RangeFactory.FIRST, context);
            int actionCount = 0;
            if (stats.size() > 0) {
                actionCount = stats.get(0).getPageViews();
            }
            activity.put(stepStart, new Integer(actionCount));
            stepStart = stepEnd;
        }

        return activity;
    }

    /**
     * Retrieves document statistics.
     *
     * @param action the action the results should be ordered by. It can be one of: "view", "save" or "download". If the
     *            action is "view" then the documents are ordered by the number of times they have been viewed so far.
     * @param scope the set of documents for which to retrieve statistics.
     * @param period the period of time, including its start date but excluding its end date.
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination.
     * @param context the XWiki context.
     * @return A list of DocumentStats objects
     */
    public List<DocumentStats> getDocumentStatistics(String action, Scope scope, Period period, Range range,
        XWikiContext context)
    {
        List<DocumentStats> documentStatsList;

        List<Object> paramList = new ArrayList<Object>(4);

        String nameFilter = getHqlNameFilterFromScope(scope, paramList);

        String sortOrder = getHqlSortOrderFromRange(range);

        XWikiHibernateStore store = context.getWiki().getHibernateStore();

        try {
            String query =
                MessageFormat.format("select name, sum(pageViews) from DocumentStats"
                    + " where ({0}) and action=? and ? <= period and period < ? group by name order"
                    + " by sum(pageViews) {1}", nameFilter, sortOrder);

            paramList.add(action);
            paramList.add(period.getStartCode());
            paramList.add(period.getEndCode());

            List<?> solist =
                store.search(query, range.getAbsoluteSize(), range.getAbsoluteStart(), paramList, context);

            documentStatsList = getDocumentStatistics(solist, action);
            if (range.getSize() < 0) {
                Collections.reverse(documentStatsList);
            }
        } catch (XWikiException e) {
            documentStatsList = Collections.emptyList();
        }

        return documentStatsList;
    }

    /**
     * Converts the rows retrieved from the database to a list of DocumentStats instances.
     *
     * @param resultSet the result of a database query for document statistics.
     * @param action the action for which the statistics were retrieved.
     * @return a list of {@link com.xpn.xwiki.stats.impl.DocumentStats} objects.
     * @see #getDocumentStatistics(String, Scope, com.xpn.xwiki.criteria.impl.Period , Range , XWikiContext)
     */
    private List<DocumentStats> getDocumentStatistics(List<?> resultSet, String action)
    {
        List<DocumentStats> documentStatsList = new ArrayList<DocumentStats>(resultSet.size());

        Date now = new Date();

        for (Object name : resultSet) {
            Object[] result = (Object[]) name;
            // We can't represent a custom period (e.g. year, week or some time interval) in the
            // database and thus we use a default one, which sould be ignored
            DocumentStats docStats = new DocumentStats((String) result[0], action, now, PeriodType.DAY);
            docStats.setPageViews(((Number) result[1]).intValue());
            documentStatsList.add(docStats);
        }

        return documentStatsList;
    }

    /**
     * Retrieves back-link statistics.
     *
     * @param domain the domain used for filtering the results.
     * @param scope the scope of referred documents for which to retrieve statistics.
     * @param period the period of time, including its start date but excluding its end date.
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination.
     * @param context the XWiki context.
     * @return a list of DocumentStats objects.
     */
    public List<DocumentStats> getBackLinkStatistics(String domain, Scope scope, Period period, Range range,
        XWikiContext context)
    {
        List<DocumentStats> documentStatsList;

        List<Object> paramList = new ArrayList<Object>(4);

        String nameFilter = getHqlNameFilterFromScope(scope, paramList);

        String sortOrder = getHqlSortOrderFromRange(range);

        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
            String query =
                MessageFormat.format("select name, sum(pageViews) from RefererStats"
                    + " where ({0}) and referer like ? and ? <= period and period < ? group by name"
                    + " order by sum(pageViews) {1}", nameFilter, sortOrder);

            paramList.add(getHqlValidDomain(domain));
            paramList.add(period.getStartCode());
            paramList.add(period.getEndCode());

            List<?> solist =
                store.search(query, range.getAbsoluteSize(), range.getAbsoluteStart(), paramList, context);

            documentStatsList = getDocumentStatistics(solist, "refer");
            if (range.getSize() < 0) {
                Collections.reverse(documentStatsList);
            }
        } catch (XWikiException e) {
            documentStatsList = Collections.emptyList();
        }

        return documentStatsList;
    }

    /**
     * Retrieves referrer statistics.
     *
     * @param domain the domain for which to retrieve statistics. To retrieve statistics for all domains use the empty
     *            string.
     * @param scope the scope of referred documents to use for filtering the results.
     * @param period the period of time, including its start date but excluding its end date.
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination.
     * @param context the XWiki context.
     * @return a list of RefererStats objects.
     */
    public List<RefererStats> getRefererStatistics(String domain, Scope scope, Period period, Range range,
        XWikiContext context)
    {
        List<RefererStats> refererList;

        List<Object> paramList = new ArrayList<Object>(4);

        String nameFilter = getHqlNameFilterFromScope(scope, paramList);

        String sortOrder = getHqlSortOrderFromRange(range);

        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
            String query =
                MessageFormat.format("select referer, sum(pageViews) from RefererStats"
                    + " where ({0}) and referer like ? and ? <= period and period < ?"
                    + " group by referer order by sum(pageViews) {1}", nameFilter, sortOrder);

            paramList.add(getHqlValidDomain(domain));
            paramList.add(period.getStartCode());
            paramList.add(period.getEndCode());

            List<?> solist =
                store.search(query, range.getAbsoluteSize(), range.getAbsoluteStart(), paramList, context);

            refererList = getRefererStatistics(solist);
            if (range.getSize() < 0) {
                Collections.reverse(refererList);
            }
        } catch (XWikiException e) {
            refererList = Collections.emptyList();
        }

        return refererList;
    }

    /**
     * Converts the rows retrieved from the database to a list of {@link com.xpn.xwiki.stats.impl.RefererStats}
     * instances.
     *
     * @param resultSet The result of a database query for referer statistics
     * @return A list of {@link com.xpn.xwiki.stats.impl.RefererStats} objects
     * @see #getRefererStatistics(String, Scope, Period, Range , XWikiContext)
     */
    private List<RefererStats> getRefererStatistics(List<?> resultSet)
    {
        Date now = new Date();
        List<RefererStats> stats = new ArrayList<RefererStats>(resultSet.size());

        for (Object name : resultSet) {
            Object[] result = (Object[]) name;

            // We can't represent a custom period (e.g. year, week or some time interval) in the
            // database and thus we use a default one, which sould be ignored
            RefererStats refStats = new RefererStats("", (String) result[0], now, PeriodType.DAY);
            refStats.setPageViews(((Number) result[1]).intValue());
            stats.add(refStats);
        }

        return stats;
    }

    /**
     * Generate the HQL query used in {@link #getVisitStatistics(String, Period, Range, XWikiContext)}.
     *
     * @param action the action the results should be ordered by. It can be one of: "view", "save" or "download". If the
     *            action is "view" then the visitors are ordered by the number of pages they have viewed so far.
     * @param period the period of time, including its start date but excluding its end date.
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination.
     * @param paramList the list of query parameters to fill.
     * @param context the XWiki context.
     * @return the HQL query
     */
    private String createVisitStatisticsQuery(String action, Period period, Range range, List<Object> paramList,
        XWikiContext context)
    {
        StringBuilder query = new StringBuilder("select name, sum(pageSaves), sum(pageViews), sum(downloads)");

        query.append(" from VisitStats");

        query.append(" where");

        // user filter

        StringBuilder userListWhere = new StringBuilder();
        try {
            for (DocumentReference user : StatsUtil.getRequestFilteredUsers(context)) {
                if (userListWhere.length() > 0) {
                    userListWhere.append(", ");
                }
                userListWhere.append('?');

                paramList.add(this.compactwikiEntityReferenceSerializer.serialize(user));
            }
        } catch (Exception e) {
            LOGGER.error("Faild to get filter users list", e);
        }

        if (userListWhere.length() > 0) {
            query.append(" name NOT IN (");
            query.append(userListWhere);
            query.append(") and ");
        }

        // date filter

        query.append(" ? <= startDate and endDate < ? group by name");
        paramList.add(new Date(period.getStart()));
        paramList.add(new Date(period.getEnd()));

        // order

        query.append(' ');

        String sortOrder = getHqlSortOrderFromRange(range);

        if (action.equals(SaveAction.ACTION_NAME)) {
            query.append("order by sum(pageSaves) " + sortOrder);
        } else if (action.equals(ViewAction.VIEW_ACTION)) {
            query.append("order by sum(pageViews) " + sortOrder);
        } else if (action.equals(DownloadAction.ACTION_NAME)) {
            query.append("order by sum(downloads) " + sortOrder);
        } else {
            query.append(MessageFormat.format("order by sum(pageSaves) {0}, sum(pageViews) {0}, sum(downloads) {0}",
                sortOrder));
        }

        return query.toString();
    }

    /**
     * Retrieves visit statistics.
     *
     * @param action the action the results should be ordered by. It can be one of: "view", "save" or "download". If the
     *            action is "view" then the visitors are ordered by the number of pages they have viewed so far.
     * @param period the period of time, including its start date but excluding its end date.
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination.
     * @param context the XWiki context.
     * @return a list of VisitStats objects.
     */
    public List<VisitStats> getVisitStatistics(String action, Period period, Range range, XWikiContext context)
    {
        List<VisitStats> visiStatList;

        List<Object> paramList = new ArrayList<Object>(2);

        String query = createVisitStatisticsQuery(action, period, range, paramList, context);

        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
            List<?> solist =
                store.search(query, range.getAbsoluteSize(), range.getAbsoluteStart(), paramList, context);

            visiStatList = getVisitStatistics(solist, new DateTime(period.getStart()), new DateTime(period.getEnd()));
            if (range.getSize() < 0) {
                Collections.reverse(visiStatList);
            }
        } catch (XWikiException e) {
            LOGGER.error("Faild to search for vist statistics", e);

            visiStatList = Collections.emptyList();
        }

        return visiStatList;
    }

    /**
     * Converts the rows retrieved from the database to a list of VisitStats instances.
     *
     * @param resultSet the result of a database query for visitor statistics.
     * @param startDate the start date used in the query.
     * @param endDate the end date used in the query.
     * @return a list of {@link com.xpn.xwiki.stats.impl.VisitStats} objects.
     * @see #getVisitStatistics(String, Period, Range, XWikiContext)
     */
    private List<VisitStats> getVisitStatistics(List<?> resultSet, DateTime startDate, DateTime endDate)
    {
        List<VisitStats> stats = new ArrayList<VisitStats>(resultSet.size());

        for (Object name2 : resultSet) {
            Object[] result = (Object[]) name2;

            String name = (String) result[0];
            String uniqueID = "";
            String cookie = "";
            String ip = "";
            String userAgent = "";
            int pageSaves = ((Number) result[1]).intValue();
            int pageViews = ((Number) result[2]).intValue();
            int downloads = ((Number) result[3]).intValue();

            VisitStats vs =
                new VisitStats(name, uniqueID, cookie, ip, userAgent, new Date(startDate.getMillis()), PeriodType.DAY);
            vs.setStartDate(new Date(startDate.getMillis()));
            vs.setEndDate(new Date(endDate.getMillis()));
            vs.setPageSaves(pageSaves);
            vs.setPageViews(pageViews);
            vs.setDownloads(downloads);

            stats.add(vs);
        }

        return stats;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////
    // Deprecated methods
    // ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets monthly statistics on a document for a specific action.
     *
     * @param docname fully qualified document name.
     * @param action can be "view", "edit", "save", etc..
     * @param month the month.
     * @param context the XWiki context.
     * @return DocumentStats - statistics object.
     * @deprecated use {@link #getDocumentStatistics(String, Scope, Period, Range , XWikiContext)} instead.
     */
    @Deprecated
    public DocumentStats getDocMonthStats(String docname, String action, Date month, XWikiContext context)
    {
        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        DocumentStats object = new DocumentStats(docname, action, month, PeriodType.MONTH);
        try {
            // TODO Fix use of deprecated call.
            store.loadXWikiCollection(object, context, true);
            return object;
        } catch (XWikiException e) {
            e.printStackTrace();
            return new DocumentStats();
        }
    }

    /**
     * Gets monthly referer statistics.
     *
     * @param docName fully qualified document name.
     * @param month the month.
     * @param context the XWiki context.
     * @return the monthly referer statistics.
     * @throws XWikiException error when searching for referer statistics.
     * @deprecated use {@link #getRefererStatistics(String, Scope, Period, Range, XWikiContext)} instead.
     */
    @Deprecated
    public List<?> getRefMonthStats(String docName, Date month, XWikiContext context) throws XWikiException
    {
        XWikiHibernateStore store = context.getWiki().getHibernateStore();

        List<?> solist;
        if (store != null) {
            List<Object> paramList = new ArrayList<Object>(1);
            paramList.add(docName);
            solist = store.search("from RefererStats as obj where obj.name=?", 0, 0, paramList, context);
        } else {
            solist = Collections.emptyList();
        }

        return solist;
    }
}

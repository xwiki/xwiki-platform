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
 *
 */

package com.xpn.xwiki.stats.api;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.Duration;
import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.Range;
import com.xpn.xwiki.criteria.impl.Scope;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.stats.impl.RefererStats;
import com.xpn.xwiki.stats.impl.VisitStats;

/**
 * Store and retrieve statistics.
 * 
 * @version $Id$
 */
public interface XWikiStatsService
{
    /**
     * Methods called just one time at XWiki initialization.
     * 
     * @param context the XWiki context.
     */
    void init(XWikiContext context);

    /**
     * Return the statistics action stored.
     * 
     * @param action the action.
     * @param size the maximum size of the list to return.
     * @param context the XWiki context.
     * @return the list of recent statistics action stored.
     */
    Collection< ? > getRecentActions(String action, int size, XWikiContext context);

    /**
     * Retrieves document statistics.
     * 
     * @param action the action the results should be ordered by. It can be one of: "view", "save" or "download". If the
     *            action is "view" then the documents are ordered by the number of times they have been viewed so far.
     * @param scope the set of documents for which to retrieve statistics.
     * @param period the period of time.
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination.
     * @param context the XWiki context.
     * @return A list of DocumentStats objects
     */
    List<DocumentStats> getDocumentStatistics(String action, Scope scope, Period period, Range range,
        XWikiContext context);

    /**
     * Retrieves visit statistics.
     * 
     * @param action the action the results should be ordered by. It can be one of: "view", "save" or "download". If the
     *            action is "view" then the visitors are ordered by the number of pages they have viewed so far.
     * @param period the period of time.
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination.
     * @param context the XWiki context.
     * @return a list of VisitStats objects.
     */
    List<VisitStats> getVisitStatistics(String action, Period period, Range range, XWikiContext context);

    /**
     * Retrieves referrer statistics.
     * 
     * @param domain the domain for which to retrieve statistics. To retrieve statistics for all domains use the empty
     *            string.
     * @param scope the scope of referred documents to use for filtering the results.
     * @param period the period of time.
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination.
     * @param context the XWiki context.
     * @return a list of RefererStats objects.
     */
    List<RefererStats> getRefererStatistics(String domain, Scope scope, Period period, Range range,
        XWikiContext context);

    /**
     * Retrieves back-link statistics.
     * 
     * @param domain the domain used for filtering the results.
     * @param scope the scope of referred documents for which to retrieve statistics.
     * @param period the period of time.
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination.
     * @param context the XWiki context.
     * @return a list of DocumentStats objects.
     */
    List< ? > getBackLinkStatistics(String domain, Scope scope, Period period, Range range, XWikiContext context);

    /**
     * Shows how the statistics for the specified action have evolved over the specified period of time.
     * 
     * @param action the action for which to retrieve statistics.
     * @param scope the set of documents to consider.
     * @param period the period of time.
     * @param step the step used for sampling the period.
     * @param context the XWiki context.
     * @return a map of (date, actionCount) pairs.
     */
    Map< ? , ? > getActionStatistics(String action, Scope scope, Period period, Duration step, XWikiContext context);

    // ////////////////////////////////////////////////////////////////////////////////////////
    // Deprecated methods
    // ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Gets total statistics on a document for a specific action.
     * 
     * @param docname fully qualified document name.
     * @param action can be "view", "edit", "save", etc..
     * @param context the XWiki context.
     * @return DocumentStats - statistics object.
     * @deprecated use {@link #getDocumentStatistics(String, Scope, Period, Range , XWikiContext)} instead.
     */
    @Deprecated
    DocumentStats getDocTotalStats(String docname, String action, XWikiContext context);

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
    DocumentStats getDocMonthStats(String docname, String action, Date month, XWikiContext context);

    /**
     * Gets day statistics on a document for a specific action.
     * 
     * @param docname fully qualified document name.
     * @param action can be "view", "edit", "save", etc..
     * @param day the day.
     * @param context the XWiki context.
     * @return DocumentStats - statistics object.
     * @deprecated use {@link #getDocumentStatistics(String, Scope, Period, Range , XWikiContext)} instead.
     */
    @Deprecated
    DocumentStats getDocDayStats(String docname, String action, Date day, XWikiContext context);

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
    List getRefMonthStats(String docName, Date month, XWikiContext context) throws XWikiException;
}

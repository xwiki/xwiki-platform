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
package com.xpn.xwiki.api;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.criteria.impl.Duration;
import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.Range;
import com.xpn.xwiki.criteria.impl.Scope;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.DocumentStats;

/**
 * Statistics api. The Statistics module needs to be activated (xwiki.stats=1 in xwiki.cfg)
 */
public class StatsService extends Api
{
    public StatsService(XWikiContext context)
    {
        super(context);
    }

    /**
     * The Statistics module is disabled by default for improved performances. It can be globally
     * activated by setting the value of <b><code>xwiki.stats</b></code> to <b><code>1</b></code>
     * in the <b><code>xwiki.cfg</b></code> configuration file. Use this method to test if at
     * some point the Statistics module is enabled or disabled.
     * 
     * @return true if the Statistics module is enabled
     */
    public boolean isEnabled()
    {
        return "1".equals(getXWikiContext().getWiki().Param("xwiki.stats"));
    }

    /**
     * Retrieves document statistics.
     * 
     * @param action The action the results should be ordered by. It can be one of: "view", "save"
     *            or "download". If the action is "view" then the documents are ordered by the
     *            number of times they have been viewed so far.
     * @param scope The set of documents for which to retrieve statistics
     * @param period The period of time
     * @param range The sub-range to return from the entire result set. Use this parameter for
     *            pagination
     * @return A list of DocumentStats objects
     */
    public List getDocumentStatistics(String action, Scope scope, Period period, Range range)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getDocumentStatistics(action, scope, period, range, getXWikiContext());
    }

    /**
     * Retrieves visit statistics
     * 
     * @param action The action the results should be ordered by. It can be one of: "view", "save"
     *            or "download". If the action is "view" then the visitors are ordered by the number
     *            of pages they have viewed so far.
     * @param period The period of time
     * @param range The sub-range to return from the entire result set. Use this parameter for
     *            pagination
     * @return A list of VisitStats objects
     */
    public List getVisitStatistics(String action, Period period, Range range)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getVisitStatistics(action, period, range, getXWikiContext());
    }

    /**
     * Retrieves referrer statistics.
     * 
     * @param domain The domain for which to retrieve statistics. To retrieve statistics for all
     *            domains use the empty string.
     * @param scope The scope of referred documents to use for filtering the results.
     * @param period The period of time
     * @param range The sub-range to return from the entire result set. Use this parameter for
     *            pagination
     * @return A list of RefererStats objects
     */
    public List getRefererStatistics(String domain, Scope scope, Period period, Range range)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getRefererStatistics(domain, scope, period, range, getXWikiContext());
    }

    /**
     * Retrieves back-link statistics.
     * 
     * @param domain The domain used for filtering the results
     * @param scope The scope of referred documents for which to retrieve statistics.
     * @param period The period of time
     * @param range The sub-range to return from the entire result set. Use this parameter for
     *            pagination
     * @return A list of DocumentStats objects
     */
    public List getBackLinkStatistics(String domain, Scope scope, Period period, Range range)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getBackLinkStatistics(domain, scope, period, range, getXWikiContext());
    }

    /**
     * Shows how the statistics for the specified action have evolved over the specified period of
     * time.
     * 
     * @param action The action for which to retrieve statistics
     * @param scope The set of documents to consider
     * @param period The period of time
     * @param step The step used for sampling the period
     * @return A map of (date, actionCount) pairs
     */
    public Map getActionStatistics(String action, Scope scope, Period period, Duration step)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_MAP;
        return stats.getActionStatistics(action, scope, period, step, getXWikiContext());
    }

    /**
     * API to access the current starts for the Wiki for a specific action It retrieves the number
     * of times the action was performed for the whole wiki.
     * 
     * @param action action for which to retrieve statistics (view/save/download)
     * @return A DocumentStats object with number of actions performed, unique visitors, number of
     *         visits
     * @deprecated use
     *             {@link #getDocumentStatistics(String, Scope, Period, com.xpn.xwiki.criteria.impl.Range)}
     *             instead
     */
    public DocumentStats getCurrentMonthXWikiStats(String action)
    {
        return getXWikiContext().getWiki().getStatsService(getXWikiContext()).getDocMonthStats(
            "", action, new Date(), getXWikiContext());
    }

    /**
     * Returns the recently visited pages for a specific action
     * 
     * @param action ("view" or "edit")
     * @param size how many recent actions to retrieve
     * @return a ArrayList of document names
     */
    public java.util.Collection getRecentActions(String action, int size)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getRecentActions(action, size, getXWikiContext());
    }
}

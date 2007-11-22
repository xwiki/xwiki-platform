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
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.stats.impl.Duration;
import com.xpn.xwiki.stats.impl.DurationFactory;
import com.xpn.xwiki.stats.impl.Interval;
import com.xpn.xwiki.stats.impl.IntervalFactory;
import com.xpn.xwiki.stats.impl.Period;
import com.xpn.xwiki.stats.impl.PeriodFactory;
import com.xpn.xwiki.stats.impl.Scope;
import com.xpn.xwiki.stats.impl.ScopeFactory;

/**
 * Statistics api. The statistics module need to be activated (xwiki.stats=1 in xwiki.cfg)
 */
public class StatsService extends Api
{
    public StatsService(XWikiContext context)
    {
        super(context);
    }

    /**
     * @return true if the statistics application is activated in the xwiki.cfg
     */
    public boolean isEnabled()
    {
        return "1".equals(getXWikiContext().getWiki().Param("xwiki.stats"));
    }

    /**
     * @return A helper factory for creating Scope objects in velocity.
     */
    public ScopeFactory getScopeFactory()
    {
        return ScopeFactory.getInstance();
    }

    /**
     * @return A helper factory for creating a Period objects in velocity.
     */
    public PeriodFactory getPeriodFactory()
    {
        return PeriodFactory.getInstance();
    }

    /**
     * @return A helper factory for creating a Period objects in velocity.
     */
    public DurationFactory getDurationFactory()
    {
        return DurationFactory.getInstance();
    }

    /**
     * @return A helper factory for creating Interval objects in velocity.
     */
    public IntervalFactory getIntervalFactory()
    {
        return IntervalFactory.getInstance();
    }

    /**
     * Retrieves document statistics.
     * 
     * @param action Can be one of: "view", "save", "download"
     * @param scope The set of documents for which to retrieve statistics
     * @param period The period of time
     * @param interval The sub-interval to return from the entire result set. Use this parameter for
     *            pagination
     * @return A list of DocumentStats objects
     */
    public List getDocumentStatistics(String action, Scope scope, Period period, Interval interval)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getDocumentStatistics(action, scope, period, interval, getXWikiContext());
    }

    /**
     * Retrieves visit statistics
     * 
     * @param action The action the results should be ordered by
     * @param period The period of time
     * @param interval The sub-interval to return from the entire result set. Use this parameter for
     *            pagination
     * @return A list of VisitStats objects
     */
    public List getVisitStatistics(String action, Period period, Interval interval)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getVisitStatistics(action, period, interval, getXWikiContext());
    }

    /**
     * Retrieves referrer statistics.
     * 
     * @param domain The domain for which to retrieve statistics. To retrieve statistics for all
     *            domains use the empty string.
     * @param scope The scope of referred documents to use for filtering the results.
     * @param period The period of time
     * @param interval The sub-interval to return from the entire result set. Use this parameter for
     *            pagination
     * @return A list of RefererStats objects
     */
    public List getRefererStatistics(String domain, Scope scope, Period period, Interval interval)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getRefererStatistics(domain, scope, period, interval, getXWikiContext());
    }

    /**
     * Retrieves back-link statistics.
     * 
     * @param domain The domain used for filtering the results
     * @param scope The scope of referred documents for which to retrieve statistics.
     * @param period The period of time
     * @param interval The sub-interval to return from the entire result set. Use this parameter for
     *            pagination
     * @return A list of DocumentStats objects
     */
    public List getBackLinkStatistics(String domain, Scope scope, Period period, Interval interval)
    {
        XWikiStatsService stats = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (stats == null)
            return Collections.EMPTY_LIST;
        return stats.getBackLinkStatistics(domain, scope, period, interval, getXWikiContext());
    }

    /**
     * Shows how the statistics for the specified action have evolved over the specified period of
     * time.
     * 
     * @param action
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
     * @deprecated use {@link #getDocumentStatistics(String, Scope, Period, Interval)} instead
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

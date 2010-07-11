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
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.criteria.impl.Duration;
import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.Range;
import com.xpn.xwiki.criteria.impl.Scope;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.StatsUtil;

/**
 * Statistics api. The Statistics module needs to be activated (xwiki.stats=1 in xwiki.cfg).
 * 
 * @version $Id$
 */
public class StatsService extends Api
{
    /**
     * Create new StatsService instance.
     * 
     * @param context the XWiki context.
     */
    public StatsService(XWikiContext context)
    {
        super(context);
    }

    /**
     * Indicate if statistics service is globally enabled. Note that it is possible for statistics to be enabled at the
     * global level, yet disabled in most or even all wikis. When statistics are globally enabled, session statistics
     * are available. To check if document statistics are enabled for the current wiki, use
     * {@link #isEnabledForCurrentWiki()}.
     * <p>
     * To be true the <code>xwiki.stats</code> setting in xwiki.cfg has to be <code>1</code>.
     * </p>
     * 
     * @return {@code true} if the statistics module is enabled
     */
    public boolean isEnabledGlobally()
    {
        return StatsUtil.isStatsEnabled(this.context);
    }

    /**
     * Indicate if statistics service is enabled for the current wiki.
     * <p>
     * To be true the <code>xwiki.stats</code> in xwiki.cfg has to be <code>1</code> and
     * <code>xwiki.stats.default</code> too.
     * </p>
     * <p>
     * <code>xwiki.stats.default</code> can be overwritten by <code>statistics</code> in <code>XWikiPreferences</code>.
     * </p>
     * 
     * @return true if statistics are enabled for the context's wiki.
     */
    public boolean isEnabledForCurrentWiki()
    {
        return StatsUtil.isWikiStatsEnabled(this.context);
    }

    /**
     * Indicate if statistics service is enabled for the current wiki.
     * <p>
     * To be true the <code>xwiki.stats</code> in xwiki.cfg has to be <code>1</code> and
     * <code>xwiki.stats.default</code> too.
     * </p>
     * <p>
     * <code>xwiki.stats.default</code> can be overwritten by <code>statistics</code> in <code>XWikiPreferences</code>.
     * </p>
     * 
     * @return true if statistics are enabled for the context's wiki.
     * @deprecated use {@link #isEnabledForCurrentWiki()}
     */
    @Deprecated
    public boolean isEnabled()
    {
        return StatsUtil.isWikiStatsEnabled(this.context);
    }

    /**
     * Retrieves document statistics.
     * 
     * @param action The action the results should be ordered by. It can be one of: "view", "save" or "download". If the
     *            action is "view" then the documents are ordered by the number of times they have been viewed so far.
     * @param scope The set of documents for which to retrieve statistics
     * @param period The period of time
     * @param range The sub-range to return from the entire result set. Use this parameter for pagination
     * @return A list of DocumentStats objects
     */
    public List< ? > getDocumentStatistics(String action, Scope scope, Period period, Range range)
    {
        List< ? > stats = Collections.emptyList();

        XWikiStatsService statsService = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (statsService != null) {
            stats = statsService.getDocumentStatistics(action, scope, period, range, getXWikiContext());
        }

        return stats;
    }

    /**
     * Retrieves visit statistics.
     * 
     * @param action The action the results should be ordered by. It can be one of: "view", "save" or "download". If the
     *            action is "view" then the visitors are ordered by the number of pages they have viewed so far.
     * @param period The period of time
     * @param range The sub-range to return from the entire result set. Use this parameter for pagination
     * @return A list of VisitStats objects
     */
    public List< ? > getVisitStatistics(String action, Period period, Range range)
    {
        List< ? > stats = Collections.emptyList();

        XWikiStatsService statsService = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (statsService != null) {
            stats = statsService.getVisitStatistics(action, period, range, getXWikiContext());
        }

        return stats;
    }

    /**
     * Retrieves referrer statistics.
     * 
     * @param domain The domain for which to retrieve statistics. To retrieve statistics for all domains use the empty
     *            string.
     * @param scope The scope of referred documents to use for filtering the results.
     * @param period The period of time
     * @param range The sub-range to return from the entire result set. Use this parameter for pagination
     * @return A list of RefererStats objects
     */
    public List< ? > getRefererStatistics(String domain, Scope scope, Period period, Range range)
    {
        List< ? > stats = Collections.emptyList();

        XWikiStatsService statsService = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (statsService != null) {
            stats = statsService.getRefererStatistics(domain, scope, period, range, getXWikiContext());
        }

        return stats;
    }

    /**
     * Retrieves back-link statistics.
     * 
     * @param domain the domain used for filtering the results
     * @param scope the scope of referred documents for which to retrieve statistics.
     * @param period the period of time
     * @param range the sub-range to return from the entire result set. Use this parameter for pagination
     * @return a list of DocumentStats objects
     */
    public List< ? > getBackLinkStatistics(String domain, Scope scope, Period period, Range range)
    {
        List< ? > stats = Collections.emptyList();

        XWikiStatsService statsService = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (statsService != null) {
            stats = statsService.getBackLinkStatistics(domain, scope, period, range, getXWikiContext());
        }

        return stats;
    }

    /**
     * Shows how the statistics for the specified action have evolved over the specified period of time.
     * 
     * @param action the action for which to retrieve statistics.
     * @param scope the set of documents to consider.
     * @param period the period of time.
     * @param step the step used for sampling the period.
     * @return a map of (date, actionCount) pairs.
     */
    public Map< ? , ? > getActionStatistics(String action, Scope scope, Period period,
        Duration step)
    {
        Map< ? , ? > stats = Collections.emptyMap();

        XWikiStatsService statsService = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (statsService != null) {
            stats = statsService.getActionStatistics(action, scope, period, step, getXWikiContext());
        }

        return stats;
    }

    /**
     * Returns the recently visited pages for a specific action.
     * 
     * @param action ("view" or "edit").
     * @param size how many recent actions to retrieve.
     * @return a ArrayList of document names.
     */
    public java.util.Collection< ? > getRecentActions(String action, int size)
    {
        java.util.Collection< ? > stats = Collections.emptyList();

        XWikiStatsService statsService = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        if (statsService != null) {
            stats = statsService.getRecentActions(action, size, getXWikiContext());
        }

        return stats;
    }
}

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
import com.xpn.xwiki.notify.XWikiActionNotificationInterface;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.stats.impl.Duration;
import com.xpn.xwiki.stats.impl.Interval;
import com.xpn.xwiki.stats.impl.Period;
import com.xpn.xwiki.stats.impl.Scope;

public interface XWikiStatsService extends XWikiActionNotificationInterface {
    public void init(XWikiContext context);
    
    /**
     * @deprecated use {@link #getDocumentStatistics(String, Scope, Period, Interval, XWikiContext)} instead
     */
    public DocumentStats getDocTotalStats(String docname, String action, XWikiContext context);
    
    /**
     * @deprecated use {@link #getDocumentStatistics(String, Scope, Period, Interval, XWikiContext)} instead
     */
    public DocumentStats getDocMonthStats(String docname, String action, Date month, XWikiContext context);
    
    /**
     * @deprecated use {@link #getDocumentStatistics(String, Scope, Period, Interval, XWikiContext)} instead
     */
    public DocumentStats getDocDayStats(String docname, String action, Date day, XWikiContext context);
    
    /**
     * @deprecated use {@link #getRefererStatistics(Period, Interval, XWikiContext)} instead
     */
    public List getRefMonthStats(String docName, Date month, XWikiContext context) throws XWikiException;
    public Collection getRecentActions(String action, int size, XWikiContext context);
    
    /**
     * @see com.xpn.xwiki.api.StatsService#getDocumentStatistics(String, Scope, Period, Interval)
     */
    List getDocumentStatistics(String action, Scope scope, Period period, Interval interval,
        XWikiContext context);

    /**
     * @see com.xpn.xwiki.api.StatsService#getActionStatistics(String, Scope, Period, Period)
     */
    List getVisitStatistics(String action, Period period, Interval interval, XWikiContext context);

    /**
     * @see com.xpn.xwiki.api.StatsService#getRefererStatistics(String, Scope, Period, Interval)
     */
    List getRefererStatistics(String domain, Scope scope, Period period, Interval interval,
        XWikiContext context);

    /**
     * @see com.xpn.xwiki.api.StatsService#getBackLinkStatistics(String, Scope, Period, Interval)
     */
    List getBackLinkStatistics(String domain, Scope scope, Period period, Interval interval,
        XWikiContext context);

    /**
     * @see com.xpn.xwiki.api.StatsService#getActionStatistics(String, Scope, Period, Period)
     */
    Map getActionStatistics(String action, Scope scope, Period period, Duration step,
        XWikiContext context);
}

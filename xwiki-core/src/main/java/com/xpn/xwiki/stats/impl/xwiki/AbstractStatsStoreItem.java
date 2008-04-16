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

package com.xpn.xwiki.stats.impl.xwiki;

import java.util.Date;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.stats.impl.StatsUtil;
import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;

/**
 * Base class of interface {@link XWikiStatsStoreItem}.
 * 
 * @version $Id: $
 * @since 1.4M2
 */
public abstract class AbstractStatsStoreItem implements XWikiStatsStoreItem
{
    /**
     * The XWiki context clone made when this statistics event occurred.
     */
    protected XWikiContext context;

    /**
     * The statistic name.
     */
    protected String name;

    /**
     * The period date.
     */
    protected Date periodDate;

    /**
     * The period type.
     */
    protected PeriodType periodType;

    /**
     * The period.
     */
    protected int period;

    /**
     * @param name the statistic name.
     * @param periodDate the period date.
     * @param periodType the period type.
     * @param context the XWiki context.
     */
    public AbstractStatsStoreItem(String name, Date periodDate, PeriodType periodType,
        XWikiContext context)
    {
        this.name = name;

        this.periodDate = periodDate;
        this.periodType = periodType;
        this.period = StatsUtil.getPeriodAsInt(this.periodDate, this.periodType);

        this.context = (XWikiContext) context.clone();
    }
}

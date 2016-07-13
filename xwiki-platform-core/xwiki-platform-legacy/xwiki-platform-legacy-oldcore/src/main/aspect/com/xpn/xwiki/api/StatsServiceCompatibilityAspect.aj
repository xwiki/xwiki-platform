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
package com.xpn.xwiki.api;

import java.util.Date;

import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.Scope;
import com.xpn.xwiki.stats.impl.DocumentStats;

/**
 * Add a backward compatibility layer to the {@link StatsService} class.
 * 
 * @version $Id$
 */
public privileged aspect StatsServiceCompatibilityAspect
{
    /**
     * API to access the current starts for the Wiki for a specific action It retrieves the number
     * of times the action was performed for the whole wiki.
     * 
     * @param action action for which to retrieve statistics (view/save/download)
     * @return A DocumentStats object with number of actions performed, unique visitors, number of
     *         visits
     * @deprecated use
     *             {@link #getDocumentStatistics(String, Scope, Period, com.xpn.xwiki.criteria.impl.Range)}
     *             instead since 1.4M2.
     */
    @Deprecated
    public DocumentStats StatsService.getCurrentMonthXWikiStats(String action)
    {
        return getXWikiContext().getWiki().getStatsService(getXWikiContext()).getDocMonthStats(
            "", action, new Date(), getXWikiContext());
    }
}

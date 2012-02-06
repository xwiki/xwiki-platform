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

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;
import com.xpn.xwiki.stats.impl.VisitStats;
import com.xpn.xwiki.store.XWikiHibernateStore;

/**
 * Store visit statistics into the database.
 * 
 * @version $Id$
 * @since 1.4M2
 */
public class VisitStatsStoreItem extends AbstractStatsStoreItem
{
    /**
     * Logging tools.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitStatsStoreItem.class);

    /**
     * The {@link VisitStats} object to store.
     */
    private VisitStats visitStats;

    /**
     * Create new instance of {@link VisitStatsStoreItem}.
     * 
     * @param visitStats the {@link VisitStats} object to store.
     * @param context the XWiki context.
     */
    public VisitStatsStoreItem(VisitStats visitStats, XWikiContext context)
    {
        super(visitStats.getName(), new Date(), PeriodType.MONTH, context);
        this.period = visitStats.getPeriod();

        this.visitStats = (VisitStats) visitStats.clone();
    }

    @Override
    public String getId()
    {
        return String.format("%s %s %s %s", getClass(), this.visitStats.getName(), this.visitStats.getUniqueID(),
            this.visitStats.getCookie());
    }

    @Override
    public void storeInternal(List<XWikiStatsStoreItem> stats)
    {
        VisitStatsStoreItem firstItem = (VisitStatsStoreItem) stats.get(0);
        VisitStats oldVisitStats = firstItem.visitStats.getOldObject();

        VisitStatsStoreItem lastItem = (VisitStatsStoreItem) stats.get(stats.size() - 1);
        VisitStats newVisitStats = lastItem.visitStats;

        XWikiHibernateStore store = this.context.getWiki().getHibernateStore();
        if (store == null) {
            return;
        }

        try {
            // In case we have store the old object then we need to remove it before saving the
            // other one because the ID info have changed
            if (oldVisitStats != null) {
                try {
                    // TODO Fix use of deprecated call.
                    store.deleteXWikiCollection(oldVisitStats, this.context, true, true);
                } catch (Exception e) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.error("Failed to delete old visit statistics object from database [{}]", getId(), e);
                    }
                }
            }

            // TODO Fix use of deprecated call.
            store.saveXWikiCollection(newVisitStats, this.context, true);
        } catch (XWikiException e) {
            LOGGER.error("Failed to save visit statistics object [{}]", getId(), e);
        }
    }
}

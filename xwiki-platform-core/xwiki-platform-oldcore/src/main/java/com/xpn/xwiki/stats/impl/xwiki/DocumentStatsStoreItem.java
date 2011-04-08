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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;
import com.xpn.xwiki.store.XWikiHibernateStore;

/**
 * Store document statistics into the database.
 * 
 * @version $Id$
 * @since 1.4M2
 */
public class DocumentStatsStoreItem extends AbstractStatsStoreItem
{
    /**
     * Logging tools.
     */
    private static final Log LOG = LogFactory.getLog(DocumentStatsStoreItem.class);

    /**
     * The action made on provided wiki/space/document.
     */
    private String action;

    /**
     * Is this part of a user visit.
     */
    private boolean isVisit;

    /**
     * Create new instance of {@link DocumentStatsStoreItem}.
     * 
     * @param name can be:
     *            <ul>
     *            <li>"" for the entire wiki.</li>
     *            <li>the space name.</li>
     *            <li>the full document name.</li>
     *            </ul>
     * @param periodDate the period date.
     * @param periodType the period type.
     * @param action the action made on provided wiki/space/document.
     * @param isVisit is this part of a user visit.
     * @param context the XWiki context.
     */
    public DocumentStatsStoreItem(String name, Date periodDate, PeriodType periodType,
        String action, boolean isVisit, XWikiContext context)
    {
        super(name, periodDate, periodType, context);
        
        this.action = action;
        this.isVisit = isVisit;
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.stats.impl.xwiki.XWikiStatsStoreItem#getId()
     */
    public String getId()
    {
        return String.format("%s %s %s %s", getClass(), this.name, this.action, this.period);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.stats.impl.xwiki.XWikiStatsStoreItem#store(java.util.List)
     */
    public void storeInternal(List<XWikiStatsStoreItem> stats)
    {
        DocumentStatsStoreItem lastItem = (DocumentStatsStoreItem) stats.get(stats.size() - 1);

        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        if (store == null) {
            return;
        }

        DocumentStats documentStat =
            new DocumentStats(lastItem.name, lastItem.action, lastItem.periodDate,
                lastItem.periodType);

        // Load old statistics object from database
        try {
            // TODO Fix use of deprecated call.
            store.loadXWikiCollection(documentStat, context, true);
        } catch (XWikiException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to load document statictics object [" + getId() + "]");
            }
        }

        // Increment counters
        documentStat.setIntValue("pageViews", documentStat.getPageViews() + stats.size());
        for (XWikiStatsStoreItem statItem : stats) {
            DocumentStatsStoreItem docStat = (DocumentStatsStoreItem) statItem;

            if (docStat.isVisit) {
                documentStat.incVisits();
            }
        }

        // Re-save statistics object
        try {
            // TODO Fix use of deprecated call.
            store.saveXWikiCollection(documentStat, context, true);
        } catch (XWikiException e) {
            LOG.error("Failed to save document statictics object [" + getId() + "]");
        }
    }
}

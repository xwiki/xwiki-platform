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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.stats.impl.StatsUtil;
import com.xpn.xwiki.stats.impl.VisitStats;
import com.xpn.xwiki.util.AbstractXWikiRunnable;
import com.xpn.xwiki.web.DownloadAction;
import com.xpn.xwiki.web.SaveAction;
import com.xpn.xwiki.web.ViewAction;

/**
 * Back-end statistics storing service.
 * 
 * @version $Id$
 * @since 1.4M2
 */
public class XWikiStatsStoreService extends AbstractXWikiRunnable
{
    /**
     * Logging tools.
     */
    private static final Log LOG = LogFactory.getLog(XWikiStatsStoreService.class);

    /**
     * The queue containing the statistics to store.
     */
    private ArrayBlockingQueue<XWikiStatsStoreItem> queue;

    /**
     * The thread on which the storing service is running.
     */
    private Thread thread;

    /**
     * Create new instance of XWikiStatsRegister and init statistics queue.
     * 
     * @param context the XWiki context.
     */
    public XWikiStatsStoreService(XWikiContext context)
    {
        super(XWikiContext.EXECUTIONCONTEXT_KEY, context);

        long queueSize = context.getWiki().ParamAsLong("stats.queue.size", 200);
        this.queue = new ArrayBlockingQueue<XWikiStatsStoreItem>((int) queueSize);
    }

    /**
     * Start storing thread.
     */
    public void start()
    {
        if (this.thread == null) {
            this.thread = new Thread(this, "Statistics storing daemon");
            // The JVM should be allowed to shutdown while this thread is running
            this.thread.setDaemon(true);
            this.thread.start();
        }
    }

    /**
     * Stop storing thread.
     */
    public void stop()
    {
        this.queue.clear();
        try {
            this.queue.put(new StopStatsRegisterObject());
            this.thread.join();
            this.thread = null;
        } catch (InterruptedException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Thread join has been interrupted", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.AbstractXWikiRunnable#runInternal()
     */
    @Override
    public void runInternal()
    {
        try {
            while (true) {
                register();
            }
        } catch (InterruptedException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Statistics storing thread has been interrupted.", e);
            }
        } catch (StopStatsStoreException e) {
            if (LOG.isInfoEnabled()) {
                LOG.warn("Statistics storing thread received stop order.", e);
            }
        }
    }

    /**
     * Store the statistics in the queue.
     * 
     * @throws InterruptedException thread has been interrupted.
     * @throws StopStatsStoreException service received stop order.
     */
    private void register() throws InterruptedException, StopStatsStoreException
    {
        XWikiStatsStoreItem stat = this.queue.take();

        List<List<XWikiStatsStoreItem>> statsList = new ArrayList<List<XWikiStatsStoreItem>>();
        Map<String, List<XWikiStatsStoreItem>> statsMap = new HashMap<String, List<XWikiStatsStoreItem>>();

        do {
            if (stat instanceof StopStatsRegisterObject) {
                throw new StopStatsStoreException();
            }

            String statId = stat.getId();

            List<XWikiStatsStoreItem> stats = statsMap.get(statId);

            if (stats == null) {
                stats = new ArrayList<XWikiStatsStoreItem>();

                statsMap.put(statId, stats);
                statsList.add(stats);
            }

            stats.add(stat);

            stat = this.queue.poll();
        } while (stat != null);

        for (List<XWikiStatsStoreItem> stats : statsList) {
            stats.get(0).store(stats);
        }
    }

    // ////////////////////////////////////////////////////////////////////////////
    // Add stats to queue
    // ////////////////////////////////////////////////////////////////////////////

    /**
     * Add new statistic to store.
     * 
     * @param statsRegisterItem the statistic store item.
     */
    public void add(XWikiStatsStoreItem statsRegisterItem)
    {
        try {
            this.queue.put(statsRegisterItem);
        } catch (InterruptedException e) {
            LOG.error("Statistics storage thread has been interrupted", e);
        }
    }

    /**
     * Add all the statistics to the save queue.
     * 
     * @param doc the document.
     * @param action the user action.
     * @param context the XWiki context.
     */
    public void addStats(XWikiDocument doc, String action, XWikiContext context)
    {
        VisitStats vobject = StatsUtil.findVisit(context);
        synchronized (vobject) {
            if (action.equals(ViewAction.VIEW_ACTION)) {
                // We count page views in the sessions only for the "view" action
                vobject.incPageViews();
            } else if (action.equals(SaveAction.ACTION_NAME)) {
                // We count "save" and "download" actions separately
                vobject.incPageSaves();
            } else if (action.equals(DownloadAction.ACTION_NAME)) {
                // We count "save" and "download" actions separately
                vobject.incDownloads();
            }

            addVisitStats(vobject, context);

            boolean isVisit = (vobject.getPageViews() == 1) && (action.equals(ViewAction.VIEW_ACTION));

            addDocumentStats(doc, action, isVisit, context);
        }

        // In case of a "view" action we want to store referer info
        if (action.equals(ViewAction.VIEW_ACTION)) {
            addRefererStats(doc, context);
        }
    }

    /**
     * Add visit statistics to the save queue.
     * 
     * @param vobject the visit statistics object.
     * @param context the XWiki context.
     */
    private void addVisitStats(VisitStats vobject, XWikiContext context)
    {
        Date currentDate = new Date();

        vobject.setEndDate(currentDate);
        add(new VisitStatsStoreItem(vobject, context));
        vobject.unrememberOldObject();
    }

    /**
     * Add document statistics to the save queue.
     * 
     * @param doc the document.
     * @param action the user action.
     * @param isVisit indicate if it's included in a visit.
     * @param context the XWiki context.
     */
    private void addDocumentStats(XWikiDocument doc, String action, boolean isVisit, XWikiContext context)
    {
        Date currentDate = new Date();

        add(new DocumentStatsStoreItem(doc.getFullName(), currentDate, StatsUtil.PeriodType.MONTH, action, isVisit,
            context));
        add(new DocumentStatsStoreItem(doc.getSpace(), currentDate, StatsUtil.PeriodType.MONTH, action, isVisit,
            context));
        add(new DocumentStatsStoreItem("", currentDate, StatsUtil.PeriodType.MONTH, action, false, context));
        add(new DocumentStatsStoreItem(doc.getFullName(), currentDate, StatsUtil.PeriodType.DAY, action, isVisit,
            context));
        add(new DocumentStatsStoreItem(doc.getSpace(), currentDate, StatsUtil.PeriodType.DAY, action, isVisit,
            context));
        add(new DocumentStatsStoreItem("", currentDate, StatsUtil.PeriodType.DAY, action, false, context));
    }

    /**
     * Add referer statistics to the save queue.
     * 
     * @param doc the document.
     * @param context the XWiki context.
     */
    private void addRefererStats(XWikiDocument doc, XWikiContext context)
    {
        String referer = StatsUtil.getReferer(context);
        if ((referer != null) && (!referer.equals(""))) {
            add(new RefererStatsStoreItem(doc.getFullName(), new Date(), StatsUtil.PeriodType.MONTH, referer,
                context));
        }
    }
}

/**
 * Item used to stop the statistics storing.
 * 
 * @version $Id$
 */
class StopStatsRegisterObject implements XWikiStatsStoreItem
{
    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.impl.xwiki.XWikiStatsStoreItem#getId()
     */
    public String getId()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.impl.xwiki.XWikiStatsStoreItem#store(java.util.List)
     */
    public void store(List<XWikiStatsStoreItem> register)
    {
    }
}

/**
 * Used to order stopping storing thread.
 * 
 * @version $Id$
 */
class StopStatsStoreException extends Exception
{

}

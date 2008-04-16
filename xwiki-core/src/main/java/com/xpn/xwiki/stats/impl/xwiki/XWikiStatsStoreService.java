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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;

/**
 * Back-end statistics storing service.
 * 
 * @version $Id: $
 * @since 1.4M2
 */
public class XWikiStatsStoreService implements Runnable
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
        long queueSize = context.getWiki().ParamAsLong("stats.queue.size", 200);
        queue = new ArrayBlockingQueue<XWikiStatsStoreItem>((int) queueSize);
    }

    /**
     * Start storing thread.
     */
    public void start()
    {
        if (thread != null) {
            thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Stop storing thread.
     */
    public void stop()
    {
        queue.clear();
        try {
            queue.put(new StopStatsRegisterObject());
            thread.join();
        } catch (InterruptedException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Thread join has been interrupted", e);
            }
        }
    }

    /**
     * Add new statistic to store.
     * 
     * @param statsRegisterItem the statistic store item.
     */
    public void add(XWikiStatsStoreItem statsRegisterItem)
    {
        queue.add(statsRegisterItem);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Runnable#run()
     */
    public void run()
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
        XWikiStatsStoreItem stat = queue.take();

        List<List<XWikiStatsStoreItem>> statsList = new ArrayList<List<XWikiStatsStoreItem>>();
        Map<String, List<XWikiStatsStoreItem>> statsMap =
            new HashMap<String, List<XWikiStatsStoreItem>>();

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

            stat = queue.poll();
        } while (stat != null);

        for (List<XWikiStatsStoreItem> stats : statsList) {
            stats.get(0).store(stats);
        }
    }
}

/**
 * Item used to stop the statistics storing.
 * 
 * @version $Id: $
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
 * @version $Id: $
 */
class StopStatsStoreException extends Exception
{
    
}
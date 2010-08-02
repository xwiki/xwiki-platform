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

package com.xpn.xwiki.stats.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ActionExecutionEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.Duration;
import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.Range;
import com.xpn.xwiki.criteria.impl.Scope;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.xwiki.XWikiStatsReader;
import com.xpn.xwiki.stats.impl.xwiki.XWikiStatsStoreService;
import com.xpn.xwiki.web.DownloadAction;
import com.xpn.xwiki.web.SaveAction;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.ViewAction;

/**
 * Store and retrieve statistics.
 * 
 * @version $Id$
 */
public class XWikiStatsServiceImpl implements XWikiStatsService, EventListener
{
    /**
     * Logging tools.
     */
    private static final Log LOG = LogFactory.getLog(XWikiStatsServiceImpl.class);

    /**
     * The name of the listener.
     */
    private static final String NAME = "statistics";

    /**
     * User actions statistics module saves.
     */
    private static final List<Event> EVENTS = new ArrayList<Event>()
    {
        {
            add(new ActionExecutionEvent(ViewAction.VIEW_ACTION));
            add(new ActionExecutionEvent(SaveAction.ACTION_NAME));
            add(new ActionExecutionEvent(DownloadAction.ACTION_NAME));
        }
    };

    /**
     * Used to resolve reference based on context.
     */
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.class, "current");

    /**
     * The statistics storing thread.
     */
    private XWikiStatsStoreService statsRegister;

    /**
     * The statistics database reader.
     */
    private XWikiStatsReader statsReader = new XWikiStatsReader();

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#init(com.xpn.xwiki.XWikiContext)
     */
    public void init(XWikiContext context)
    {
        if (LOG.isInfoEnabled()) {
            LOG.info("Start statistics service initialization");
        }

        if (StatsUtil.isStatsEnabled(context)) {
            // Start statistics store thread
            this.statsRegister = new XWikiStatsStoreService(context);
            this.statsRegister.start();

            // Adding the rule which will allow this module to be called on each page view
            Utils.getComponent(ObservationManager.class).addListener(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#getRecentActions(java.lang.String, int,
     *      com.xpn.xwiki.XWikiContext)
     */
    public Collection<Object> getRecentActions(String action, int size, XWikiContext context)
    {
        return this.statsReader.getRecentActions(action, size, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        if (Utils.getComponent(RemoteObservationManagerContext.class).isRemoteState()) {
            // we do nothing when the event comes from remote instance since the remote instance is supposed to already
            // take care of this
            return;
        }

        ActionExecutionEvent actionEvent = (ActionExecutionEvent) event;
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        // If the server is in read-only mode, forget about the statistics (since it's in read only mode we don't write
        // anything in the database)
        if (context.getWiki().isReadOnly()) {
            return;
        }

        // Initialize cookie used as unique identifier of a user visit and put it in the context
        StatsUtil.findCookie(context);

        String action = actionEvent.getActionName();

        // Let's save in the session the last elements view, saved
        synchronized (this) {
            if (!action.equals(DownloadAction.ACTION_NAME)) {
                Collection actions = StatsUtil.getRecentActionFromSessions(context, action);
                if (actions == null) {
                    actions = new CircularFifoBuffer(StatsUtil.getRecentVisitSize(context));
                    StatsUtil.setRecentActionsFromSession(context, action, actions);
                }

                String element = document.getPrefixedFullName();
                if (actions.contains(element)) {
                    actions.remove(element);
                }
                actions.add(element);
            }
        }

        try {
            if (StatsUtil.isWikiStatsEnabled(context)
                && !StatsUtil.getStorageFilteredUsers(context).contains(
                    this.currentDocumentReferenceResolver.resolve(context.getUser()))) {
                this.statsRegister.addStats(document, action, context);
            }
        } catch (Exception e) {
            LOG.error("Faild to get filter users list", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getActionStatistics(String, Scope, com.xpn.xwiki.criteria.impl.Period ,
     *      com.xpn.xwiki.criteria.impl.Duration , XWikiContext)
     */
    public Map< ? , ? > getActionStatistics(String action, Scope scope, Period period, Duration step,
        XWikiContext context)
    {
        return this.statsReader.getActionStatistics(action, scope, period, step, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getDocumentStatistics(String, Scope, Period, Range , XWikiContext)
     */
    public List<DocumentStats> getDocumentStatistics(String action, Scope scope, Period period, Range range,
        XWikiContext context)
    {
        return this.statsReader.getDocumentStatistics(action, scope, period, range, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getBackLinkStatistics(String, Scope, Period, Range , XWikiContext)
     */
    public List<DocumentStats> getBackLinkStatistics(String domain, Scope scope, Period period, Range range,
        XWikiContext context)
    {
        return this.statsReader.getBackLinkStatistics(domain, scope, period, range, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getRefererStatistics(String, Scope, Period, Range , XWikiContext)
     */
    public List<RefererStats> getRefererStatistics(String domain, Scope scope, Period period, Range range,
        XWikiContext context)
    {
        return this.statsReader.getRefererStatistics(domain, scope, period, range, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getVisitStatistics(String, com.xpn.xwiki.criteria.impl.Period , Range , XWikiContext)
     */
    public List<VisitStats> getVisitStatistics(String action, Period period, Range range, XWikiContext context)
    {
        return this.statsReader.getVisitStatistics(action, period, range, context);
    }

    // ////////////////////////////////////////////////////////////////////////////////////////
    // Deprecated methods
    // ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#getDocTotalStats(java.lang.String, java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public DocumentStats getDocTotalStats(String docname, String action, XWikiContext context)
    {
        return new DocumentStats();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#getDocMonthStats(java.lang.String, java.lang.String,
     *      java.util.Date, com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public DocumentStats getDocMonthStats(String docname, String action, Date month, XWikiContext context)
    {
        return this.statsReader.getDocMonthStats(docname, action, month, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#getDocDayStats(java.lang.String, java.lang.String, java.util.Date,
     *      com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public DocumentStats getDocDayStats(String docname, String action, Date day, XWikiContext context)
    {
        return new DocumentStats();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#getRefMonthStats(java.lang.String, java.util.Date,
     *      com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public List< ? > getRefMonthStats(String docName, Date month, XWikiContext context) throws XWikiException
    {
        return this.statsReader.getRefMonthStats(docName, month, context);
    }
}

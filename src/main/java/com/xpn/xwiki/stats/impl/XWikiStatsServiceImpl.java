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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.Duration;
import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.Range;
import com.xpn.xwiki.criteria.impl.RangeFactory;
import com.xpn.xwiki.criteria.impl.Scope;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiActionRule;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;
import com.xpn.xwiki.stats.impl.xwiki.DocumentStatsStoreItem;
import com.xpn.xwiki.stats.impl.xwiki.RefererStatsStoreItem;
import com.xpn.xwiki.stats.impl.xwiki.VisitStatsStoreItem;
import com.xpn.xwiki.stats.impl.xwiki.XWikiStatsStoreService;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.DownloadAction;
import com.xpn.xwiki.web.SaveAction;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.ViewAction;
import com.xpn.xwiki.web.XWikiRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.query.Filter;
import org.apache.portals.graffito.jcr.query.QueryManager;
import org.joda.time.DateTime;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Store and retrieve statistics.
 * 
 * @version $Id: $
 */
public class XWikiStatsServiceImpl implements XWikiStatsService
{
    /**
     * Logging tools.
     */
    private static final Log LOG = LogFactory.getLog(XWikiStatsServiceImpl.class);

    /**
     * The name of the property in XWiki configuration file containing the list of cookie domains.
     */
    private static final String CFGPROP_COOKIEDOMAINS = "xwiki.authentication.cookiedomains";

    /**
     * The name of the property in XWiki configuration file indicating if statistics are enabled.
     */
    private static final String CFGPROP_STATS = "xwiki.stats";

    /**
     * The name of the property in XWiki configuration file indicating if a virtual wiki store
     * statistics by default.
     */
    private static final String CFGPROP_STATS_DEFAULT = "xwiki.stats.default";

    /**
     * The prefix name of the session property containing recent statistics actions.
     */
    private static final String SESSPROP_RECENT_PREFFIX = "recent_";

    /**
     * The prefix name of the session property containing the current visit object.
     */
    private static final String SESSPROP_VISITOBJECT = "visitObject";

    /**
     * The name of the session property containing the size of the recent list of visit statistics
     * actions.
     */
    private static final String PREFPROP_RECENT_VISITS_SIZE = "recent_visits_size";

    /**
     * The name of the XWiki preferences property indicating if current wiki store statistics.
     */
    private static final String PREFPROP_STATISTICS = "statistics";

    /**
     * The name of the request property containing the referer.
     */
    private static final String REQPROP_REFERER = "referer";

    /**
     * The name of the request property containing the user agent.
     */
    private static final String REQPROP_USERAGENT = "User-Agent";

    /**
     * The name of the context property containing the statistics cookie name.
     */
    private static final String CONTPROP_STATS_COOKIE = "stats_cookie";

    /**
     * The name of the context property indicating if the cookie in the context is new.
     */
    private static final String CONTPROP_STATS_NEWCOOKIE = "stats_newcookie";

    /**
     * The name of the cookie property containing the unique id of the visit object.
     */
    private static final String COOKPROP_VISITID = "visitid";
    
    /**
     * The full name of the guest virtual user.
     */
    private static final String GUEST_FULLNAME = "XWiki.XWikiGuest";

    /**
     * The expiration date of the cookie.
     */
    private Date cookieExpirationDate;

    /**
     * The list of cookie domains.
     */
    private String[] cookieDomains;

    /**
     * The statistics storing thread.
     */
    private XWikiStatsStoreService statsRegister;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#init(com.xpn.xwiki.XWikiContext)
     */
    public void init(XWikiContext context)
    {
        // Let's init the expirationDate for the cookie
        Calendar cal = Calendar.getInstance();
        cal.set(2030, 0, 0);
        cookieExpirationDate = cal.getTime();

        cookieDomains = StringUtils.split(context.getWiki().Param(CFGPROP_COOKIEDOMAINS), ",");

        if ("1".equals(context.getWiki().Param(CFGPROP_STATS, "1"))) {
            // Start statistics store thread
            statsRegister = new XWikiStatsStoreService(context);
            statsRegister.start();

            // Adding the rule which will allow this module to be called on each page view
            context.getWiki().getNotificationManager().addGeneralRule(
                new XWikiActionRule(this, true, true));
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
        List<Object> list = new ArrayList<Object>();

        if ((action.equals(ViewAction.VIEW_ACTION) || (action.equals(SaveAction.ACTION_NAME)))) {
            HttpSession session = context.getRequest().getSession();
            Collection< ? > actions =
                (Collection< ? >) session.getAttribute(SESSPROP_RECENT_PREFFIX + action);

            if (actions != null) {
                Object[] actionsarray = actions.toArray();
                CollectionUtils.reverseArray(actionsarray);
                int nb = Math.min(actions.size(), size);
                for (int i = 0; i < nb; i++) {
                    list.add(actionsarray[i]);
                }
            }
        }

        return list;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.notify.XWikiActionNotificationInterface#notify(com.xpn.xwiki.notify.XWikiNotificationRule,
     *      com.xpn.xwiki.doc.XWikiDocument, java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    public void notify(XWikiNotificationRule rule, XWikiDocument doc, String action,
        XWikiContext context)
    {

        if (context.getWiki().isReadOnly()) {
            // the server is in read-only mode, forget about the statistics
            return;
        }
        // Unless this is a "view", "save" or "download" action, we are not interested
        if (!action.equals(ViewAction.VIEW_ACTION) && !action.equals(SaveAction.ACTION_NAME)
            && !action.equals(DownloadAction.ACTION_NAME)) {
            return;
        }

        if (findCookie(context)) {
            return;
        }

        // Let's save in the session the last elements view, saved
        synchronized (this) {
            if (!action.equals(DownloadAction.ACTION_NAME)) {
                HttpSession session = context.getRequest().getSession();
                Collection actions =
                    (Collection) session.getAttribute(SESSPROP_RECENT_PREFFIX + action);
                if (actions == null) {
                    actions =
                        new CircularFifoBuffer(context.getWiki().getXWikiPreferenceAsInt(
                            PREFPROP_RECENT_VISITS_SIZE, 20, context));
                    session.setAttribute(SESSPROP_RECENT_PREFFIX + action, actions);
                }

                String element = context.getDatabase() + ":" + doc.getFullName();
                if (actions.contains(element)) {
                    actions.remove(element);
                }
                actions.add(element);
            }
        }

        // Let's check if this wiki should have statistics disabled
        String statsdefault = context.getWiki().Param(CFGPROP_STATS_DEFAULT);
        String statsactive =
            context.getWiki().getXWikiPreference(PREFPROP_STATISTICS, "", context);
        if ("0".equals(statsactive)) {
            return;
        }
        // If nothing is said we use the default parameter
        if (("".equals(statsactive)) && ("0".equals(statsdefault))) {
            return;
        }

        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        if (store == null) {
            return;
        }

        VisitStats vobject = findVisit(context);
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

            Date currentDate = new Date();

            vobject.setEndDate(currentDate);
            this.statsRegister.add(new VisitStatsStoreItem(vobject, context));
            vobject.unrememberOldObject();

            boolean isVisit =
                (vobject.getPageViews() == 1) && (action.equals(ViewAction.VIEW_ACTION));

            this.statsRegister.add(new DocumentStatsStoreItem(doc.getFullName(), currentDate,
                PeriodType.MONTH, action, isVisit, context));
            this.statsRegister.add(new DocumentStatsStoreItem(doc.getSpace(), currentDate,
                PeriodType.MONTH, action, isVisit, context));
            this.statsRegister.add(new DocumentStatsStoreItem("", currentDate, PeriodType.MONTH,
                action, false, context));
            this.statsRegister.add(new DocumentStatsStoreItem(doc.getFullName(), currentDate,
                PeriodType.DAY, action, isVisit, context));
            this.statsRegister.add(new DocumentStatsStoreItem(doc.getSpace(), currentDate,
                PeriodType.DAY, action, isVisit, context));
            this.statsRegister.add(new DocumentStatsStoreItem("", currentDate, PeriodType.DAY,
                action, false, context));
        }

        // In case of a "view" action we want to store referer info
        if (action.equals(ViewAction.VIEW_ACTION)) {
            String referer = getReferer(context);
            if ((referer != null) && (!referer.equals(""))) {
                this.statsRegister.add(new RefererStatsStoreItem(doc.getFullName(), new Date(),
                    PeriodType.MONTH, referer, context));
            }
        }

    }

    /**
     * @param context the XWiki context.
     * @return the referer.
     */
    private String getReferer(XWikiContext context)
    {
        String referer = context.getRequest().getHeader(REQPROP_REFERER);

        try {
            URL url = new URL(referer);
            URL baseurl = context.getURL();
            if (baseurl.getHost().equals(url.getHost())) {
                referer = null;
            }
        } catch (MalformedURLException e) {
            referer = null;
        }

        return referer;
    }

    /**
     * Try to find the cookie of the current request or create it.
     * 
     * @param context The context of this request.
     * @return The visiting session, retrieved from the database or created.
     */
    private boolean findCookie(XWikiContext context)
    {
        if (context.get(CONTPROP_STATS_COOKIE) != null) {
            return false;
        }

        Cookie cookie = Util.getCookie(COOKPROP_VISITID, context);
        boolean newcookie = false;

        // If the cookie does not exist we need to set it
        if (cookie == null) {
            cookie = addCookie(context);
            newcookie = true;
        }

        context.put(CONTPROP_STATS_COOKIE, cookie);
        context.put(CONTPROP_STATS_NEWCOOKIE, Boolean.valueOf(newcookie));

        return true;
    }

    /**
     * Try to find the visiting session of the current request, or create a new one if this request
     * is not part of a visit. The session is searched in the following way:
     * <ol>
     * <li>the java session is searched for the visit object</li>
     * <li>try to find the stored session using the cookie</li>
     * <li>try to find the session by matching the IP and User Agent</li>
     * </ol>
     * The session is invalidated if:
     * <ul>
     * <li>the cookie is not the same as the stored cookie</li>
     * <li>more than 30 minutes have elapsed from the previous request</li>
     * <li>the user is not the same</li>
     * </ul>
     * 
     * @param context The context of this request.
     * @return The visiting session, retrieved from the database or created.
     */
    private VisitStats findVisit(XWikiContext context)
    {
        XWikiRequest request = context.getRequest();
        HttpSession session = request.getSession(true);
        String ip = null;
        String ua = null;

        VisitStats visitObject = (VisitStats) session.getAttribute(SESSPROP_VISITOBJECT);

        Date nowDate = new Date();
        Cookie cookie = (Cookie) context.get(CONTPROP_STATS_COOKIE);
        boolean newcookie = ((Boolean) context.get(CONTPROP_STATS_NEWCOOKIE)).booleanValue();

        if (visitObject == null) {
            if (!newcookie) {
                try {
                    visitObject = findVisitByCookie(cookie.getValue(), context);
                } catch (XWikiException e) {
                    LOG.error("Failed to find visit by cookie", e);
                }
            } else {
                try {
                    ip = request.getRemoteAddr();
                    ua = request.getHeader(REQPROP_USERAGENT);
                    visitObject = findVisitByIPUA(ip + ua, context);
                } catch (XWikiException e) {
                    LOG.error("Failed to find visit by unique id", e);
                }

            }
        }

        if (visitObject != null) {
            // Let's verify if the session is valid
            // If the cookie is not the same
            if (!visitObject.getCookie().equals(cookie.getValue())) {
                // Let's log a message here
                // Since the session is also maintained using a cookie
                // then there is something wrong here
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Found visit with cookie " + visitObject.getCookie()
                        + " in session " + session.getId() + " for request with cookie "
                        + cookie.getValue());
                }

                // And forget about this session
                visitObject = null;
            } else if ((nowDate.getTime() - visitObject.getEndDate().getTime()) > 30 * 60 * 1000) {
                // If session is longer than 30 minutes we should invalidate it
                // and create a new one
                visitObject = null;
            } else if (visitObject != null && !context.getUser().equals(visitObject.getName())) {
                // If the user is not the same, we should invalidate the session
                // and create a new one
                visitObject = null;
            }
        }

        if (visitObject == null) {
            // we need to create the session
            if (ip == null) {
                ip = request.getRemoteAddr();
            }
            if (ua == null) {
                ua = request.getHeader(REQPROP_USERAGENT);
            }
            if (ua == null) {
                ua = "";
            }
            String uniqueID;

            if (newcookie) {
                // We cannot yet ID the user using the cookie
                // we need to use the IP and UA
                uniqueID = ip + ua;
            } else {
                // In this case we got the cookie from the request
                // so we id the user using the cookie
                uniqueID = cookie.getValue();
            }

            visitObject =
                new VisitStats(context.getUser(), uniqueID, cookie.getValue(), ip, ua, nowDate,
                    PeriodType.MONTH);
            visitObject.setEndDate(nowDate);
        } else {
            if (!newcookie) {
                // If the cookie is not yet the unique ID we need to change that
                String uniqueID = visitObject.getUniqueID();
                String oldcookie = visitObject.getCookie();

                if (!uniqueID.equals(oldcookie)) {
                    // We need to store the oldID so that we can remove the older entry
                    // since the entry identifiers are changing
                    VisitStats newVisitObject = (VisitStats) visitObject.clone();
                    newVisitObject.rememberOldObject(visitObject);
                    newVisitObject.setUniqueID(cookie.getValue());
                    visitObject = newVisitObject;
                }
            }

            if ((!context.getUser().equals(GUEST_FULLNAME))
                && (visitObject.getUser().equals(GUEST_FULLNAME))) {
                // The user has changed from guest to an authenticated user
                // We want to record this
                VisitStats newVisitObject = visitObject;
                newVisitObject.rememberOldObject(visitObject);
                newVisitObject.setName(context.getUser());
                visitObject = newVisitObject;
            }
        }

        // Keep the visit object in the session
        session.setAttribute(SESSPROP_VISITOBJECT, visitObject);

        return visitObject;
    }

    /**
     * Search visit statistics object in the database based on cookie name.
     * 
     * @param fieldName the field name.
     * @param fieldValue the field value.
     * @param context the XWiki context.
     * @return the visit object, null if no object was found.
     * @throws XWikiException error when searching for visit object.
     */
    protected VisitStats findVisitByField(String fieldName, String fieldValue,
        XWikiContext context) throws XWikiException
    {
        VisitStats visitStats = null;

        Date currentDate = new Date(new Date().getTime() - 30 * 60 * 1000);

        if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
            XWikiJcrStore store = (XWikiJcrStore) context.getWiki().getNotCacheStore();

            try {
                QueryManager qm = store.getObjectQueryManager(context);
                Filter filter =
                    qm.createFilter(VisitStats.class).addEqualTo(fieldName, fieldValue)
                        .addGreaterThan(VisitStats.Property.endDate.toString(), currentDate);
                org.apache.portals.graffito.jcr.query.Query query = qm.createQuery(filter);
                query.addOrderByDescending(VisitStats.Property.endDate.toString());
                List< ? > solist = store.getObjects(query, context);
                if (solist.size() > 0) {
                    visitStats = (VisitStats) solist.get(0);
                }
            } catch (Exception e) {
                LOG.error("Failed to search visit object in the jcr store from cookie name", e);
            }
        } else {
            XWikiHibernateStore store = context.getWiki().getHibernateStore();

            try {
                List<Object> paramList = new ArrayList<Object>(2);

                String query =
                    "from VisitStats as obj where obj." + fieldName + "=? and obj.endDate > ?"
                        + " order by obj.endDate desc";

                paramList.add(fieldValue);
                paramList.add(currentDate);

                List< ? > solist = store.search(query, 0, 0, paramList, context);

                if (solist.size() > 0) {
                    visitStats = (VisitStats) solist.get(0);
                }
            } catch (Exception e) {
                LOG.error("Failed to search visit object in the database from " + fieldName, e);
            }
        }

        return visitStats;
    }

    /**
     * Search visit statistics object in the database based on cookie name.
     * 
     * @param cookie the cookie name.
     * @param context the XWiki context.
     * @return the visit object, null if no object was found.
     * @throws XWikiException error when searching for visit object.
     */
    protected VisitStats findVisitByCookie(String cookie, XWikiContext context)
        throws XWikiException
    {
        return findVisitByField("cookie", cookie, context);
    }

    /**
     * Search visit statistics object in the database based on visit unique id.
     * 
     * @param uniqueID the visit unique id.
     * @param context the XWiki context.
     * @return the visit object.
     * @throws XWikiException error when searching for visit object.
     */
    protected VisitStats findVisitByIPUA(String uniqueID, XWikiContext context)
        throws XWikiException
    {
        return findVisitByField("uniqueID", uniqueID, context);
    }

    /**
     * Create a new visit cookie and return it.
     * 
     * @param context the XWiki context.
     * @return the newly created cookie.
     */
    protected Cookie addCookie(XWikiContext context)
    {
        Cookie cookie =
            new Cookie(COOKPROP_VISITID, RandomStringUtils.randomAlphanumeric(32).toUpperCase());
        cookie.setPath("/");

        int time = (int) (cookieExpirationDate.getTime() - (new Date()).getTime()) / 1000;
        cookie.setMaxAge(time);

        String cookieDomain = null;
        if (cookieDomains != null) {
            String servername = context.getRequest().getServerName();
            for (int i = 0; i < cookieDomains.length; i++) {
                if (servername.indexOf(cookieDomains[i]) != -1) {
                    cookieDomain = cookieDomains[i];
                    break;
                }
            }
        }

        if (cookieDomain != null) {
            cookie.setDomain(cookieDomain);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Setting cookie " + cookie.getValue() + " for name " + cookie.getName()
                + " with domain " + cookie.getDomain() + " and path " + cookie.getPath()
                + " and maxage " + cookie.getMaxAge());
        }

        context.getResponse().addCookie(cookie);

        return cookie;
    }

    /**
     * @param range the range.
     * @return the corresponding sort order.
     */
    private String getHqlSortOrderFromRange(Range range)
    {
        String sortOrder;

        if (range.getSize() < 0) {
            sortOrder = "asc";
        } else {
            sortOrder = "desc";
        }

        return sortOrder;
    }

    /**
     * @param domain the provided domain.
     * @return the domain to use in HQL query.
     */
    private String getHqlValidDomain(String domain)
    {
        if (domain == null || domain.trim().length() == 0) {
            return "%";
        }

        return domain;
    }

    /**
     * @param scope the set of documents for which to retrieve statistics.
     * @param paramList the values to insert in the SQL query.
     * @return the name filter HQL query part.
     */
    private String getHqlNameFilterFromScope(Scope scope, List<Object> paramList)
    {
        String nameFilter;

        if (scope.getType() == Scope.SPACE_SCOPE && "".equals(scope.getName())) {
            nameFilter = "name not like '%.%' and name <> ''";
        } else {
            nameFilter = "name like ?";
            paramList.add(scope.getPattern());
        }

        return nameFilter;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getActionStatistics(String, Scope, com.xpn.xwiki.criteria.impl.Period ,
     *      com.xpn.xwiki.criteria.impl.Duration , XWikiContext)
     */
    public Map<DateTime, Integer> getActionStatistics(String action, Scope scope, Period period,
        Duration step, XWikiContext context)
    {
        DateTime stepStart = new DateTime(period.getStart());
        DateTime periodEnd = new DateTime(period.getEnd());
        org.joda.time.Period stepDuration =
            new org.joda.time.Period(step.getYears(), step.getMonths(), step.getWeeks(), step
                .getDays(), 0, 0, 0, 0);

        Map<DateTime, Integer> activity = new HashMap<DateTime, Integer>();
        while (stepStart.compareTo(periodEnd) < 0) {
            DateTime stepEnd = stepStart.plus(stepDuration);
            if (stepEnd.compareTo(periodEnd) > 0) {
                stepEnd = periodEnd;
            }
            List<DocumentStats> stats =
                getDocumentStatistics(action, scope, new Period(stepStart.getMillis(), stepEnd
                    .getMillis()), RangeFactory.FIRST, context);
            int actionCount = 0;
            if (stats.size() > 0) {
                actionCount = stats.get(0).getPageViews();
            }
            activity.put(stepStart, new Integer(actionCount));
            stepStart = stepEnd;
        }

        return activity;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getDocumentStatistics(String, Scope, Period, Range , XWikiContext)
     */
    public List<DocumentStats> getDocumentStatistics(String action, Scope scope, Period period,
        Range range, XWikiContext context)
    {
        List<DocumentStats> documentStatsList;

        List<Object> paramList = new ArrayList<Object>(4);

        String nameFilter = getHqlNameFilterFromScope(scope, paramList);

        String sortOrder = getHqlSortOrderFromRange(range);

        XWikiHibernateStore store = context.getWiki().getHibernateStore();

        try {
            String query =
                MessageFormat.format("select name, sum(pageViews) from DocumentStats"
                    + " where {0} and action=? and ? <= period and period <= ?"
                    + " group by name order by sum(pageViews) {1}", nameFilter, sortOrder);

            paramList.add(action);
            paramList.add(period.getStartCode());
            paramList.add(period.getEndCode());

            List< ? > solist =
                store.search(query, range.getAbsoluteSize(), range.getAbsoluteStart(), paramList,
                    context);

            documentStatsList = getDocumentStatistics(solist, action);
            if (range.getSize() < 0) {
                Collections.reverse(documentStatsList);
            }
        } catch (XWikiException e) {
            documentStatsList = Collections.emptyList();
        }

        return documentStatsList;
    }

    /**
     * Converts the rows retrieved from the database to a list of DocumentStats instances.
     * 
     * @param resultSet the result of a database query for document statistics.
     * @param action the action for which the statistics were retrieved.
     * @return a list of {@link com.xpn.xwiki.stats.impl.DocumentStats} objects.
     * @see #getDocumentStatistics(String, Scope, com.xpn.xwiki.criteria.impl.Period , Range ,
     *      XWikiContext)
     */
    private List<DocumentStats> getDocumentStatistics(List< ? > resultSet, String action)
    {
        List<DocumentStats> documentStatsList = new ArrayList<DocumentStats>(resultSet.size());

        Date now = Calendar.getInstance().getTime();

        for (Iterator< ? > it = resultSet.iterator(); it.hasNext();) {
            Object[] result = (Object[]) it.next();
            // We can't represent a custom period (e.g. year, week or some time interval) in the
            // database and thus we use a default one, which sould be ignored
            DocumentStats docStats =
                new DocumentStats((String) result[0], action, now, PeriodType.DAY);
            docStats.setPageViews(((Number) result[1]).intValue());
            documentStatsList.add(docStats);
        }

        return documentStatsList;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getBackLinkStatistics(String, Scope, Period, Range , XWikiContext)
     */
    public List<DocumentStats> getBackLinkStatistics(String domain, Scope scope, Period period,
        Range range, XWikiContext context)
    {
        List<DocumentStats> documentStatsList;

        List<Object> paramList = new ArrayList<Object>(4);

        String nameFilter = getHqlNameFilterFromScope(scope, paramList);

        String sortOrder = getHqlSortOrderFromRange(range);

        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
            String query =
                MessageFormat.format("select name, sum(pageViews) from RefererStats"
                    + " where {0} and referer like ? and ? <= period and period <= ?"
                    + " group by name order by sum(pageViews) {1}", nameFilter, sortOrder);

            paramList.add(getHqlValidDomain(domain));
            paramList.add(period.getStartCode());
            paramList.add(period.getEndCode());

            List< ? > solist =
                store.search(query, range.getAbsoluteSize(), range.getAbsoluteStart(), paramList,
                    context);

            documentStatsList = getDocumentStatistics(solist, "refer");
            if (range.getSize() < 0) {
                Collections.reverse(documentStatsList);
            }
        } catch (XWikiException e) {
            documentStatsList = Collections.emptyList();
        }

        return documentStatsList;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getRefererStatistics(String, Scope, Period, Range , XWikiContext)
     */
    public List<RefererStats> getRefererStatistics(String domain, Scope scope, Period period,
        Range range, XWikiContext context)
    {
        List<RefererStats> refererList;

        List<Object> paramList = new ArrayList<Object>(4);

        String nameFilter = getHqlNameFilterFromScope(scope, paramList);

        String sortOrder = getHqlSortOrderFromRange(range);

        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
            String query =
                MessageFormat.format("select referer, sum(pageViews) from RefererStats"
                    + " where {0} and referer like ? and ? <= period and period <= ?"
                    + " group by referer order by sum(pageViews) {1}", nameFilter, sortOrder);

            paramList.add(getHqlValidDomain(domain));
            paramList.add(period.getStartCode());
            paramList.add(period.getEndCode());

            List< ? > solist =
                store.search(query, range.getAbsoluteSize(), range.getAbsoluteStart(), paramList,
                    context);

            refererList = getRefererStatistics(solist);
            if (range.getSize() < 0) {
                Collections.reverse(refererList);
            }
        } catch (XWikiException e) {
            refererList = Collections.emptyList();
        }

        return refererList;
    }

    /**
     * Converts the rows retrieved from the database to a list of
     * {@link com.xpn.xwiki.stats.impl.RefererStats} instances.
     * 
     * @param resultSet The result of a database query for referer statistics
     * @return A list of {@link com.xpn.xwiki.stats.impl.RefererStats} objects
     * @see #getRefererStatistics(String, Scope, Period, Range , XWikiContext)
     */
    private List<RefererStats> getRefererStatistics(List< ? > resultSet)
    {
        Date now = Calendar.getInstance().getTime();
        List<RefererStats> stats = new ArrayList<RefererStats>(resultSet.size());

        for (Iterator< ? > it = resultSet.iterator(); it.hasNext();) {
            Object[] result = (Object[]) it.next();

            // We can't represent a custom period (e.g. year, week or some time interval) in the
            // database and thus we use a default one, which sould be ignored
            RefererStats refStats = new RefererStats("", (String) result[0], now, PeriodType.DAY);
            refStats.setPageViews(((Number) result[1]).intValue());
            stats.add(refStats);
        }

        return stats;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getVisitStatistics(String, com.xpn.xwiki.criteria.impl.Period , Range ,
     *      XWikiContext)
     */
    public List<VisitStats> getVisitStatistics(String action, Period period, Range range,
        XWikiContext context)
    {
        List<VisitStats> visiStatList;

        List<Object> paramList = new ArrayList<Object>(2);

        String sortOrder = getHqlSortOrderFromRange(range);

        String orderByClause;
        if (action.equals(SaveAction.ACTION_NAME)) {
            orderByClause = "order by sum(pageSaves) " + sortOrder;
        } else if (action.equals(ViewAction.VIEW_ACTION)) {
            orderByClause = "order by sum(pageViews) " + sortOrder;
        } else if (action.equals(DownloadAction.ACTION_NAME)) {
            orderByClause = "order by sum(downloads) " + sortOrder;
        } else {
            orderByClause =
                MessageFormat.format("order by sum(pageSaves) {0}," + " sum(pageViews) {0},"
                    + " sum(downloads) {0}", sortOrder);
        }

        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
            String query =
                "select name, sum(pageSaves), sum(pageViews), sum(downloads)"
                    + " from VisitStats" + " where ? <= startDate and endDate < ? group by name "
                    + orderByClause;

            paramList.add(new Date(period.getStart()));
            paramList.add(new Date(period.getEnd()));

            List< ? > solist =
                store.search(query, range.getAbsoluteSize(), range.getAbsoluteStart(), paramList,
                    context);

            visiStatList =
                getVisitStatistics(solist, new DateTime(period.getStart()), new DateTime(period
                    .getEnd()));
            if (range.getSize() < 0) {
                Collections.reverse(visiStatList);
            }
        } catch (XWikiException e) {
            visiStatList = Collections.emptyList();
        }

        return visiStatList;
    }

    /**
     * Converts the rows retrieved from the database to a list of VisitStats instances.
     * 
     * @param resultSet the result of a database query for visitor statistics.
     * @param startDate the start date used in the query.
     * @param endDate the end date used in the query.
     * @return a list of {@link com.xpn.xwiki.stats.impl.VisitStats} objects.
     * @see #getVisitStatistics(com.xpn.xwiki.criteria.impl.Period , Range , XWikiContext)
     */
    private List<VisitStats> getVisitStatistics(List< ? > resultSet, DateTime startDate,
        DateTime endDate)
    {
        List<VisitStats> stats = new ArrayList<VisitStats>(resultSet.size());

        for (Iterator< ? > it = resultSet.iterator(); it.hasNext();) {
            Object[] result = (Object[]) it.next();

            String name = (String) result[0];
            String uniqueID = "";
            String cookie = "";
            String ip = "";
            String userAgent = "";
            int pageSaves = ((Number) result[1]).intValue();
            int pageViews = ((Number) result[2]).intValue();
            int downloads = ((Number) result[3]).intValue();

            VisitStats vs =
                new VisitStats(name, uniqueID, cookie, ip, userAgent, new Date(startDate
                    .getMillis()), PeriodType.DAY);
            vs.setStartDate(new Date(startDate.getMillis()));
            vs.setEndDate(new Date(endDate.getMillis()));
            vs.setPageSaves(pageSaves);
            vs.setPageViews(pageViews);
            vs.setDownloads(downloads);

            stats.add(vs);
        }

        return stats;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////
    // Deprecated methods
    // ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#getDocTotalStats(java.lang.String,
     *      java.lang.String, com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public DocumentStats getDocTotalStats(String docname, String action, XWikiContext context)
    {
        return new DocumentStats();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#getDocMonthStats(java.lang.String,
     *      java.lang.String, java.util.Date, com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public DocumentStats getDocMonthStats(String docname, String action, Date month,
        XWikiContext context)
    {
        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        DocumentStats object = new DocumentStats(docname, action, month, PeriodType.MONTH);
        try {
            store.loadXWikiCollection(object, context, true);
            return object;
        } catch (XWikiException e) {
            e.printStackTrace();
            return new DocumentStats();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#getDocDayStats(java.lang.String,
     *      java.lang.String, java.util.Date, com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public DocumentStats getDocDayStats(String docname, String action, Date day,
        XWikiContext context)
    {
        return new DocumentStats();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.stats.api.XWikiStatsService#getRefMonthStats(java.lang.String,
     *      java.util.Date, com.xpn.xwiki.XWikiContext)
     */
    @Deprecated
    public List< ? > getRefMonthStats(String docname, Date month, XWikiContext context)
        throws XWikiException
    {
        XWikiStoreInterface store = context.getWiki().getNotCacheStore();

        List< ? > solist;
        if (store instanceof XWikiHibernateStore) {
            solist =
                ((XWikiHibernateStore) store).search("from RefererStats as obj where obj.name='"
                    + Utils.SQLFilter(docname) + "'", 0, 0, context);
        } else if (store instanceof XWikiJcrStore) {
            solist = ((XWikiJcrStore) store).getAllObjectsByClass(RefererStats.class, context);
        } else {
            solist = Collections.emptyList();
        }

        return solist;
    }
}

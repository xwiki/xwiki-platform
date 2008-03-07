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
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.stats.impl.StatsUtil;
import com.xpn.xwiki.stats.impl.VisitStats;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.portals.graffito.jcr.query.Filter;
import org.apache.portals.graffito.jcr.query.QueryManager;
import org.hibernate.Query;
import org.hibernate.Session;
import org.joda.time.DateTime;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class XWikiStatsServiceImpl implements XWikiStatsService {

    private static final Log log = LogFactory.getLog(XWikiStatsServiceImpl.class);

    public static Date expirationDate;
    public static String[] cookieDomains;

    /**
     * Initializes the Stats Service by inserting the notification rule
     * to be notified of all actions on documents
     * @param context
     */
    public void init(XWikiContext context) {
        // Let's init the expirationDate for the cookie
        Calendar cal = Calendar.getInstance();
        cal.set(2030,0,0);
        expirationDate = cal.getTime();

        cookieDomains = StringUtils.split(context.getWiki().Param("xwiki.authentication.cookiedomains"),",");

        // Adding the rule which will allow this module to be called on each page view
        if ("1".equals(context.getWiki().Param("xwiki.stats","1")))
         context.getWiki().getNotificationManager().addGeneralRule(new XWikiActionRule(this, true, true));
    }

    /**
     * Gets total statistics on a document for a specific action
     *
     * @param docname fully qualified document named
     * @param action can be "view", "edit", "save", etc..
     * @param context
     * @return DocStats - statistics object
     */
    public DocumentStats getDocTotalStats(String docname, String action, XWikiContext context) {
        return new DocumentStats();
    }

    /**
     * Gets monthly statistics on a document for a specific action
     * @param docname
     * @param action
     * @param month
     * @param context
     * @return
     */
    public DocumentStats getDocMonthStats(String docname, String action, Date month, XWikiContext context) {
        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        DocumentStats object = new DocumentStats(docname, action, month, StatsUtil.PERIOD_MONTH);
        try {
            store.loadXWikiCollection(object, context, true);
            return object;
        } catch (XWikiException e) {
            e.printStackTrace();
            return new DocumentStats();
        }
    }

    /**
     * Gets daily statistics on a document for a specific action
     * @param docname
     * @param action
     * @param day
     * @param context
     * @return
     */
    public DocumentStats getDocDayStats(String docname, String action, Date day, XWikiContext context) {
        return new DocumentStats();
    }

    public List getRefMonthStats(String docname, Date month, XWikiContext context) throws XWikiException {
    	XWikiStoreInterface store = context.getWiki().getNotCacheStore();
    	List solist = new ArrayList();
    	if (store instanceof XWikiHibernateStore) {
    		solist = ((XWikiHibernateStore)store).search("from RefererStats as obj where obj.name='" + Utils.SQLFilter(docname) + "'", 0, 0, context);
    	} else if (store instanceof XWikiJcrStore) {
    		solist = ((XWikiJcrStore)store).getAllObjectsByClass(RefererStats.class, context);
    	}
        return solist;
    }

    public Collection getRecentActions(String action, int size, XWikiContext context) {
        ArrayList list = new ArrayList();
        if ((action.equals("view")||(action.equals("save")))) {
            HttpSession session = context.getRequest().getSession();
            Collection actions = (Collection) session.getAttribute("recent_" + action);

            if (actions!=null) {
               Object[] actionsarray = actions.toArray();
               CollectionUtils.reverseArray(actionsarray);
               int nb = Math.min(actions.size(), size);
               for (int i=0;i<nb;i++)
                    list.add(actionsarray[i]);
               }
        }
        return list;
    }

    /**
     * Notification rule to store usage statistics
     * @param rule
     * @param doc
     * @param action
     * @param context
     */
    public void notify(XWikiNotificationRule rule, XWikiDocument doc, String action, XWikiContext context) {

        if (context.getWiki().isReadOnly()) {
            // the server is in read-only mode, forget about the statistics
            return;
        }
        // Unless this is a "view", "save" or "download" action, we are not interested
        if (!(action.equals("view")||action.equals("save")||action.equals("download")))
            return;

        if (findCookie(context)) {
            return;
        }

        // Let's save in the session the last elements view, saved
        synchronized (this) {
            if (!action.equals("download")) {
                HttpSession session = context.getRequest().getSession();
                Collection actions = (Collection) session.getAttribute("recent_" + action);
                if (actions==null) {
                    actions = new CircularFifoBuffer(context.getWiki().getXWikiPreferenceAsInt("recent_visits_size", 20, context));
                    session.setAttribute("recent_" + action, actions);
                }
                String element = context.getDatabase() + ":" + doc.getFullName();
                if (actions.contains(element))
                    actions.remove(element);
                actions.add(element);
            }
        }

        // Let's check if this wiki should have statistics disabled
        String statsdefault = context.getWiki().Param("xwiki.stats.default");
        String statsactive = context.getWiki().getXWikiPreference("statistics", "", context);
        if ("0".equals(statsactive))
            return;
        // If nothing is said we use the default parameter
        if (("".equals(statsactive))&&("0".equals(statsdefault)))
            return;

        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        if (store == null) return;

        VisitStats vobject = findVisit(context);
        synchronized(vobject) {
            // We count page views in the sessions only for the "view" action
            if (action.equals("view"))
                vobject.incPageViews();
                // We count "save" and "download" actions separately
            else if (action.equals("save"))
                vobject.incPageSaves();
            else if (action.equals("download"))
                vobject.incDownloads();

            vobject.setEndDate(new Date());
            try {
                // In case we have store the old object
                // then we need to remove it
                // before saving the other one
                // because the ID info have changed
                VisitStats oldObject  = vobject.getOldObject();
                if (oldObject!=null) {
                    // Catch exception to not fail here
                    try {
                        store.deleteXWikiCollection(oldObject, context, true, true);
                        vobject.unrememberOldObject();
                    } catch (Exception e) {}
                }

                store.saveXWikiCollection(vobject, context, true);
            } catch (XWikiException e) {
                // Statistics should never make xwiki fail !
                e.printStackTrace();
            }

            addPageView(doc.getFullName(), action, StatsUtil.PERIOD_MONTH, store, context, vobject);
            addPageView(doc.getSpace(), action, StatsUtil.PERIOD_MONTH, store, context, vobject);
            addPageView("", action, StatsUtil.PERIOD_MONTH, store, context, vobject);
            addPageView(doc.getFullName(), action, StatsUtil.PERIOD_DAY, store, context, vobject);
            addPageView(doc.getSpace(), action, StatsUtil.PERIOD_DAY, store, context, vobject);
            addPageView("", action, StatsUtil.PERIOD_DAY, store, context, vobject);
        }

        // In case of a "view" action we want to store referer info
        if (action.equals("view")) {
            String referer = getReferer(context);
            if ((referer !=null)&&(!referer.equals(""))) {
                // Visits of the web
                RefererStats robject = new RefererStats(doc.getFullName(), referer, new Date(), StatsUtil.PERIOD_MONTH);
                synchronized(robject) {
                    try {
                        store.loadXWikiCollection(robject, context, true);
                    } catch (XWikiException e) {
                    }

                    robject.incPageViews();
                    try {
                        store.saveXWikiCollection(robject, context, true);
                    } catch (XWikiException e) {
                        // Statistics should never make xwiki fail !
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private void addPageView(String docname, String action, int periodtype, XWikiHibernateStore store, XWikiContext context, VisitStats vobject) {
        DocumentStats object;
        object = new DocumentStats(docname, action, new Date(), periodtype);
        try {
            store.loadXWikiCollection(object, context, true);
        } catch (XWikiException e) {
        }
        object.incPageViews();

        // Visits are only for viewing pages
        if (docname.equals("")) {
         if ((vobject.getPageViews()==1)&&(action.equals("view"))) {
            object.incVisits();
         }
        }

        try {
            store.saveXWikiCollection(object, context, true);
        } catch (XWikiException e) {
            // Statistics should never make xwiki fail !
            e.printStackTrace();
        }
    }

    private String getReferer(XWikiContext context) {
        String referer = context.getRequest().getHeader("referer");
        try {
            URL url = new URL(referer);
            URL baseurl = context.getURL();
            if (baseurl.getHost().equals(url.getHost()))
                return null;
            return referer;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
      * Try to find the cookie of the current request or create it
      *
      * @param context The context of this request.
      * @return The visiting session, retrieved from the database or created.
      */
    private boolean findCookie(XWikiContext context) {
        if (context.get("stats_cookie")!=null)
         return false;

        Cookie cookie = Util.getCookie("visitid", context);
        boolean newcookie = false;

        // If the cookie does not exist we need to set it
        if (cookie==null) {
            cookie = addCookie(context);
            newcookie = true;
        }

        context.put("stats_cookie", cookie);
        context.put("stats_newcookie", new Boolean(newcookie));
        return true;
    }

    /**
      * Try to find the visiting session of the current request, or create a new one
      * if this request is not part of a visit.
      *
      * The session is searched in the following way:
      * <ol><li>the java session is searched for the visit object</li>
      * <li>try to find the stored session using the cookie</li>
      * <li>try to find the session by matching the IP and User Agent</li></ol>
      * The session is invalidated if:
      * <ul><li>the cookie is not the same as the stored cookie</li>
      * <li>more than 30 minutes have elapsed from the previos request</li>
      * <li>the user is not the same</li></ul>
      *
      * @param context The context of this request.
      * @return The visiting session, retrieved from the database or created.
      */
    private VisitStats findVisit(XWikiContext context) {
        XWikiRequest request = context.getRequest();
        HttpSession session = request.getSession(true);
        String ip = null, ua = null;

        VisitStats vobject = (VisitStats) session.getAttribute("visitObject");

        Date nowDate = new Date();
        Cookie cookie = (Cookie) context.get("stats_cookie");
        boolean newcookie = ((Boolean)context.get("stats_newcookie")).booleanValue();

        if (vobject==null) {
           if (!newcookie) {
               try {
                   vobject = findVisitByCookie(cookie.getValue(), context);
               } catch (XWikiException e) {
                   e.printStackTrace();
               }
           } else {
               try {
                   ip = request.getRemoteAddr();
                   ua = request.getHeader("User-Agent");
                   vobject = findVisitByIPUA(ip + ua, context);
               } catch (XWikiException e) {
                   e.printStackTrace();
               }

           }
        }

        if (vobject!=null) {
            // Let's verify if the session is valid
            // If the cookie is not the same
            if (!vobject.getCookie().equals(cookie.getValue())) {
                // Let's log a message here
                // Since the session is also maintained using a cookie
                // then there is something wrong here
                if (log.isDebugEnabled())
                  log.debug("Found visit with cookie " + vobject.getCookie() + " in session "
                           + session.getId() + " for request with cookie " + cookie.getValue());
                // And forget about this session
                vobject = null;
            } else if ((nowDate.getTime() - vobject.getEndDate().getTime()) > 30 * 60 * 1000) {
                // If session is longer than 30 minutes we should invalidate it
                // and create a new one
                vobject = null;
            } else if(vobject != null && !context.getUser().equals(vobject.getName())) {
                // If the user is not the same, we should invalidate the session
                // and create a new one
                vobject = null;
            }
        }

        if (vobject==null) {
            // we need to create the session
            if (ip == null) ip = request.getRemoteAddr();
            if (ua == null) ua = request.getHeader("User-Agent");
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

            vobject = new VisitStats(context.getUser(), uniqueID, cookie.getValue(),
                                     ip, ua, nowDate, StatsUtil.PERIOD_MONTH);
            vobject.setEndDate(nowDate);
        } else {
            if (!newcookie) {
             // If the cookie is not yet the unique ID we need to change that
             String uniqueID = vobject.getUniqueID();
             String oldcookie = vobject.getCookie();

             if (!uniqueID.equals(oldcookie)) {
              // We need to store the oldID so that we can remove the older entry
              // since the entry identifiers are changing
              VisitStats newvobject = (VisitStats) vobject.clone();
              newvobject.rememberOldObject(vobject);
              newvobject.setUniqueID(cookie.getValue());
              vobject = newvobject;
             }
            }

            if ((!context.getUser().equals("XWiki.XWikiGuest"))
                 &&(vobject.getUser().equals("XWiki.XWikiGuest")))
            {
              // The user has changed from guest to an authenticated user
              // We want to record this
                VisitStats newvobject = vobject;
                newvobject.rememberOldObject(vobject);
                newvobject.setName(context.getUser());
                vobject = newvobject;
            }
        }

        // Keep the visit object in the session
        session.setAttribute("visitObject", vobject);
        return vobject;
    }


    protected VisitStats findVisitByCookie(String cookie, XWikiContext context) throws XWikiException {
    	Date cdate = new Date();
        cdate = new Date(cdate.getTime() - 30 * 60 * 1000);
    	if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
    		XWikiJcrStore store = (XWikiJcrStore) context.getWiki().getNotCacheStore();
    		try {
    			QueryManager qm = store.getObjectQueryManager(context);
				Filter filter = qm.createFilter(VisitStats.class)
					.addEqualTo("coockie", cookie)
					.addGreaterThan("endDate", cdate);
				org.apache.portals.graffito.jcr.query.Query query = qm.createQuery(filter);
				query.addOrderByDescending("endDate");
				List solist = store.getObjects(query, context);
				if (solist.size()>0)
					return (VisitStats) solist.get(0);
			    else
			    	return null;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
    	}
        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
        store.beginTransaction(context);
        Session session = store.getSession(context);
        Query query = session.createQuery("from VisitStats as obj where obj.cookie=:cookie and obj.endDate > :cdate order by obj.endDate desc");
        query.setString("cookie", cookie);
        query.setDate("cdate", cdate);

        List solist = store.search(query, 0, 0, context);
        if (solist.size()>0)
         return (VisitStats) solist.get(0);
        else
         return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
            store.endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    protected VisitStats findVisitByIPUA(String ipua, XWikiContext context) throws XWikiException {
    	Date cdate = new Date();
        cdate = new Date(cdate.getTime() - 30 * 60 * 1000);
    	if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
    		XWikiJcrStore store = (XWikiJcrStore) context.getWiki().getNotCacheStore();    		
    		try {
    			QueryManager qm = store.getObjectQueryManager(context);
				Filter filter = qm.createFilter(VisitStats.class)
					.addEqualTo("uniqueID", ipua)
					.addGreaterThan("endDate", cdate);
				org.apache.portals.graffito.jcr.query.Query query = qm.createQuery(filter);
				query.addOrderByDescending("endDate");
				List solist = store.getObjects(query, context);
				if (solist.size()>0)
					return (VisitStats) solist.get(0);
			    else
			    	return null;
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
    	}
        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
        store.beginTransaction(context);
        Session session = store.getSession(context);
        Query query = session.createQuery("from VisitStats as obj where obj.uniqueID=:ipua and obj.endDate > :cdate order by obj.endDate desc");
        query.setString("ipua", ipua);
        query.setDate("cdate", cdate);

        List solist = store.search(query, 0, 0, context);
        if (solist.size()>0)
         return (VisitStats) solist.get(0);
        else
         return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
            store.endTransaction(context, false);
            } catch (Exception e) {}
        }
    }

    protected Cookie addCookie(XWikiContext context) {
      Cookie cookie = new Cookie("visitid", RandomStringUtils.randomAlphanumeric(32).toUpperCase());
      cookie.setPath("/");

     int time = (int)(expirationDate.getTime() - (new Date()).getTime())/1000;
     cookie.setMaxAge(time);

     String cookieDomain = null;
     if (cookieDomains!=null) {
        String servername = context.getRequest().getServerName();
        for (int i=0;i<cookieDomains.length;i++) {
         if (servername.indexOf(cookieDomains[i])!=-1) {
           cookieDomain = cookieDomains[i];
           break;
         }
        }
    }

    if (cookieDomain!=null) {
            cookie.setDomain(cookieDomain);
    }

    if (log.isDebugEnabled()) {
        log.debug("Setting cookie " + cookie.getValue() + " for name " + cookie.getName()
                + " with domain " + cookie.getDomain() + " and path " + cookie.getPath()
                + " and maxage " + cookie.getMaxAge());
    }

    context.getResponse().addCookie(cookie);
    return cookie;
   }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getActionStatistics(String, Scope, com.xpn.xwiki.criteria.impl.Period , com.xpn.xwiki.criteria.impl.Duration , XWikiContext)
     */
    public Map getActionStatistics(String action, Scope scope, Period period, Duration step,
        XWikiContext context)
    {
        DateTime stepStart = new DateTime(period.getStart());
        DateTime periodEnd = new DateTime(period.getEnd());
        org.joda.time.Period stepDuration =
            new org.joda.time.Period(step.getYears(), step.getMonths(), step.getWeeks(), step
                .getDays(), 0, 0, 0, 0);
        Map activity = new HashMap();
        while (stepStart.compareTo(periodEnd) < 0) {
            DateTime stepEnd = stepStart.plus(stepDuration);
            if (stepEnd.compareTo(periodEnd) > 0) {
                stepEnd = periodEnd;
            }
            List stats =
                this.getDocumentStatistics(action, scope, new Period(stepStart.getMillis(),
                    stepEnd.getMillis()), RangeFactory.FIRST, context);
            int actionCount = 0;
            if (stats.size() > 0) {
                actionCount = ((DocumentStats) stats.get(0)).getPageViews();
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
    public List getDocumentStatistics(String action, Scope scope, Period period,
        Range range, XWikiContext context)
    {
        String nameFilter = "name like :name";
        boolean hasNameParam = true;
        if (scope.getType() == Scope.SPACE_SCOPE && "".equals(scope.getName())) {
            nameFilter = "name not like '%.%' and name <> ''";
            hasNameParam = false;
        }
        String sortOrder = "desc";
        if (range.getSize() < 0) {
            sortOrder = "asc";
        }
        XWikiHibernateStore store = null;
        try {
            store = context.getWiki().getHibernateStore();
            store.beginTransaction(context);
            Session session = store.getSession(context);
            Query query =
                session
                    .createQuery("select name, sum(pageViews) from DocumentStats where action=:action and "
                        + nameFilter
                        + " and :startDate <= period and period <= :endDate group by name order by sum(pageViews) "
                        + sortOrder);
            query.setString("action", action);
            if (hasNameParam) {
                query.setString("name", scope.getPattern());
            }
            query.setInteger("startDate", period.getStartCode());
            query.setInteger("endDate", period.getEndCode());

            List results =
                getDocumentStatistics(store.search(query, range.getAbsoluteSize(), range
                    .getAbsoluteStart(), context), action);
            if (range.getSize() < 0) {
                Collections.reverse(results);
            }
            return results;
        } catch (XWikiException e) {
            return Collections.EMPTY_LIST;
        } finally {
            try {
                store.endTransaction(context, false);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Converts the rows retrieved from the database to a list of DocumentStats instances
     * 
     * @param resultSet the result of a database query for document statistics
     * @param action the action for which the statistics were retrieved
     * @return a list of {@link com.xpn.xwiki.stats.impl.DocumentStats} objects
     * @see #getDocumentStatistics(String, Scope, com.xpn.xwiki.criteria.impl.Period , Range , XWikiContext)
     */
    private List getDocumentStatistics(List resultSet, String action)
    {
        Date now = Calendar.getInstance().getTime();
        List stats = new ArrayList(resultSet.size());
        Iterator it = resultSet.iterator();
        while (it.hasNext()) {
            Object[] result = (Object[]) it.next();
            // We can't represent a custom period (e.g. year, week or some time interval) in the
            // database and thus we use a default one, which sould be ignored
            DocumentStats docStats =
                new DocumentStats((String) result[0], action, now, StatsUtil.PERIOD_DAY);
            docStats.setPageViews(((Number) result[1]).intValue());
            stats.add(docStats);
        }
        return stats;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getBackLinkStatistics(String, Scope, Period, Range , XWikiContext)
     */
    public List getBackLinkStatistics(String domain, Scope scope, Period period,
        Range range, XWikiContext context)
    {
        if (domain == null || domain.trim().length() == 0) {
            domain = "%";
        }
        String nameFilter = "name like :name";
        boolean hasNameParam = true;
        if (scope.getType() == Scope.SPACE_SCOPE && "".equals(scope.getName())) {
            nameFilter = "name not like '%.%' and name <> ''";
            hasNameParam = false;
        }
        String sortOrder = "desc";
        if (range.getSize() < 0) {
            sortOrder = "asc";
        }
        XWikiHibernateStore store = null;
        try {
            store = context.getWiki().getHibernateStore();
            store.beginTransaction(context);
            Session session = store.getSession(context);
            Query query =
                session
                    .createQuery("select name, sum(pageViews) from RefererStats where "
                        + nameFilter
                        + " and referer like :referer and :startDate <= period and period <= :endDate group by name order by sum(pageViews) "
                        + sortOrder);
            if (hasNameParam) {
                query.setString("name", scope.getPattern());
            }
            query.setString("referer", domain);
            query.setInteger("startDate", period.getStartCode());
            query.setInteger("endDate", period.getEndCode());

            List results =
                getDocumentStatistics(store.search(query, range.getAbsoluteSize(), range
                    .getAbsoluteStart(), context), "refer");
            if (range.getSize() < 0) {
                Collections.reverse(results);
            }
            return results;
        } catch (XWikiException e) {
            return Collections.EMPTY_LIST;
        } finally {
            try {
                store.endTransaction(context, false);
            } catch (Exception e) {
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getRefererStatistics(String, Scope, Period, Range , XWikiContext)
     */
    public List getRefererStatistics(String domain, Scope scope, Period period,
        Range range, XWikiContext context)
    {
        if (domain == null || domain.trim().length() == 0) {
            domain = "%";
        }
        String nameFilter = "name like :name";
        boolean hasNameParam = true;
        if (scope.getType() == Scope.SPACE_SCOPE && "".equals(scope.getName())) {
            nameFilter = "name not like '%.%' and name <> ''";
            hasNameParam = false;
        }
        String sortOrder = "desc";
        if (range.getSize() < 0) {
            sortOrder = "asc";
        }
        XWikiHibernateStore store = null;
        try {
            store = context.getWiki().getHibernateStore();
            store.beginTransaction(context);
            Session session = store.getSession(context);
            Query query =
                session
                    .createQuery("select referer, sum(pageViews) from RefererStats where "
                        + nameFilter
                        + " and referer like :referer and :startDate <= period and period <= :endDate group by referer order by sum(pageViews) "
                        + sortOrder);
            if (hasNameParam) {
                query.setString("name", scope.getPattern());
            }
            query.setString("referer", domain);
            query.setInteger("startDate", period.getStartCode());
            query.setInteger("endDate", period.getEndCode());

            List results =
                getRefererStatistics(store.search(query, range.getAbsoluteSize(), range
                    .getAbsoluteStart(), context));
            if (range.getSize() < 0) {
                Collections.reverse(results);
            }
            return results;
        } catch (XWikiException e) {
            return Collections.EMPTY_LIST;
        } finally {
            try {
                store.endTransaction(context, false);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Converts the rows retrieved from the database to a list of
     * {@link com.xpn.xwiki.stats.impl.RefererStats} instances
     * 
     * @param resultSet The result of a database query for referer statistics
     * @return A list of {@link com.xpn.xwiki.stats.impl.RefererStats} objects
     * @see #getRefererStatistics(String, Scope, Period, Range , XWikiContext)
     */
    private List getRefererStatistics(List resultSet)
    {
        Date now = Calendar.getInstance().getTime();
        List stats = new ArrayList(resultSet.size());
        Iterator it = resultSet.iterator();
        while (it.hasNext()) {
            Object[] result = (Object[]) it.next();
            // We can't represent a custom period (e.g. year, week or some time interval) in the
            // database and thus we use a default one, which sould be ignored
            RefererStats refStats =
                new RefererStats("", (String) result[0], now, StatsUtil.PERIOD_DAY);
            refStats.setPageViews(((Number) result[1]).intValue());
            stats.add(refStats);
        }
        return stats;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiStatsService#getVisitStatistics(String, com.xpn.xwiki.criteria.impl.Period , Range , XWikiContext)
     */
    public List getVisitStatistics(String action, Period period, Range range,
        XWikiContext context)
    {
        String sortOrder = "desc";
        if (range.getSize() < 0) {
            sortOrder = "asc";
        }
        String orderByClause =
            "order by sum(pageSaves) " + sortOrder + ", sum(pageViews) " + sortOrder
                + ", sum(downloads) " + sortOrder;
        if (action.equals("save")) {
            orderByClause = "order by sum(pageSaves) " + sortOrder;
        } else if (action.equals("view")) {
            orderByClause = "order by sum(pageViews) " + sortOrder;
        } else if (action.equals("download")) {
            orderByClause = "order by sum(downloads) " + sortOrder;
        }
        XWikiHibernateStore store = null;
        try {
            store = context.getWiki().getHibernateStore();
            store.beginTransaction(context);
            Session session = store.getSession(context);
            Query query =
                session
                    .createQuery("select name, sum(pageSaves), sum(pageViews), sum(downloads) from VisitStats where :startDate <= startDate and endDate < :endDate group by name "
                        + orderByClause);
            query.setDate("startDate", new Date(period.getStart()));
            query.setDate("endDate", new Date(period.getEnd()));

            List results =
                getVisitStatistics(store.search(query, range.getAbsoluteSize(), range
                    .getAbsoluteStart(), context), new DateTime(period.getStart()),
                    new DateTime(period.getEnd()));
            if (range.getSize() < 0) {
                Collections.reverse(results);
            }
            return results;
        } catch (XWikiException e) {
            return Collections.EMPTY_LIST;
        } finally {
            try {
                store.endTransaction(context, false);
            } catch (Exception e) {
            }
        }
    }

    /**
     * Converts the rows retrieved from the database to a list of VisitStats instances
     * 
     * @param resultSet the result of a database query for visitor statistics
     * @param startDate the start date used in the query
     * @param endDate the end date used in the query
     * @return a list of {@link com.xpn.xwiki.stats.impl.VisitStats} objects
     * @see #getVisitStatistics(com.xpn.xwiki.criteria.impl.Period , Range , XWikiContext)
     */
    private List getVisitStatistics(List resultSet, DateTime startDate, DateTime endDate)
    {
        List stats = new ArrayList(resultSet.size());
        Iterator it = resultSet.iterator();
        while (it.hasNext()) {
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
                    .getMillis()), StatsUtil.PERIOD_DAY);
            vs.setStartDate(new Date(startDate.getMillis()));
            vs.setEndDate(new Date(endDate.getMillis()));
            vs.setPageSaves(pageSaves);
            vs.setPageViews(pageViews);
            vs.setDownloads(downloads);
            stats.add(vs);
        }
        return stats;
    }
}

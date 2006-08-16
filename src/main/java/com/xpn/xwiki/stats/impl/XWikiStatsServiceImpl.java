/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author erwan
 * @author sdumitriu
 */

package com.xpn.xwiki.stats.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiActionRule;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

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
         context.getWiki().getNotificationManager().addGeneralRule(new XWikiActionRule(this));
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
        DocumentStats object = new DocumentStats(docname, action, month, XWikiStats.PERIOD_MONTH);
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
        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        List solist = store.search("from RefererStats as obj where obj.name='" + Utils.SQLFilter(docname) + "'", 0, 0, context);
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
                    list.add((String) actionsarray[i]);
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

        VisitStats vobject = findVisit(context);
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
                    } catch (Exception e) {};
                }

                store.saveXWikiCollection(vobject, context, true);
            } catch (XWikiException e) {
                // Statistics should never make xwiki fail !
                e.printStackTrace();
            }

        addPageView(doc.getFullName(), action, XWikiStats.PERIOD_MONTH, store, context, vobject);
        addPageView(doc.getWeb(), action, XWikiStats.PERIOD_MONTH, store, context, vobject);
        addPageView("", action, XWikiStats.PERIOD_MONTH, store, context, vobject);
        addPageView(doc.getFullName(), action, XWikiStats.PERIOD_DAY, store, context, vobject);
        addPageView(doc.getWeb(), action, XWikiStats.PERIOD_DAY, store, context, vobject);
        addPageView("", action, XWikiStats.PERIOD_DAY, store, context, vobject);

        // In case of a "view" action we want to store referer info
        if (action.equals("view")) {
            String referer = getReferer(context);
            if ((referer !=null)&&(!referer.equals(""))) {
                // Visits of the web
                RefererStats robject = new RefererStats(doc.getFullName(), referer, new Date(), XWikiStats.PERIOD_MONTH);
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


    private VisitStats findVisit(XWikiContext context) {
        XWikiRequest request = context.getRequest();
        HttpSession session = request.getSession(true);
        String ip = null, ua = null;

        VisitStats vobject = (VisitStats) session.getAttribute("visitObject");

        Date nowDate = new Date();
        Cookie cookie = Util.getCookie("visitid", context);
        boolean newcookie = false;

        // If the cookie does not exist we need to set it
        if (cookie==null) {
            cookie = addCookie(context);
            newcookie = true;
        }

        if (vobject!=null) {
            // Let's verify if the session is valid
            Date endDate = vobject.getEndDate();

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
            }

            // If session is longer than 30 minutes we should invalidate it
            // and create a new one
            if ((nowDate.getTime()-endDate.getTime()) > 30 * 60 * 1000)
                vobject = null;
        }


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

        if (vobject==null) {
            // we need to create the session
            if (ip==null) ip = request.getRemoteAddr();
            if (ua==null) ua = request.getHeader("User-Agent");
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
                                     ip, ua, nowDate, XWikiStats.PERIOD_MONTH);
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
        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
        store.beginTransaction(context);
        Session session = store.getSession(context);
        Query query = session.createQuery("from VisitStats as obj where obj.cookie=:cookie and obj.endDate > :cdate order by obj.endDate desc");
        query.setString("cookie", cookie);
        Date cdate = new Date();
        cdate = new Date(cdate.getTime() - 30 * 60 * 1000);
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
        XWikiHibernateStore store = context.getWiki().getHibernateStore();
        try {
        store.beginTransaction(context);
        Session session = store.getSession(context);
        Query query = session.createQuery("from VisitStats as obj where obj.uniqueID=:ipua and obj.endDate > :cdate order by obj.endDate desc");
        query.setString("ipua", ipua);
        Date cdate = new Date();
        cdate = new Date(cdate.getTime() - 30 * 60 * 1000);
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

}


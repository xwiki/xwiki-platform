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

package com.xpn.xwiki.user.impl.xwiki;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.cache.api.XWikiCacheService;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.query.QueryPlugin;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class XWikiGroupServiceImpl implements XWikiGroupService, XWikiDocChangeNotificationInterface {
    protected XWikiCache groupCache;

    public synchronized void init(XWiki xwiki, XWikiContext context) throws XWikiException {
        initCache(context);
        xwiki.getNotificationManager().addGeneralRule(new DocChangeRule(this));
    }

    public synchronized void initCache(XWikiContext context) throws XWikiException {
        int iCapacity = 100;
        try {
            String capacity = context.getWiki().Param("xwiki.authentication.group.cache.capacity");
            if (capacity != null)
                iCapacity = Integer.parseInt(capacity);
        } catch (Exception e) {}
        initCache(iCapacity, context);
    }

    public synchronized void initCache(int iCapacity, XWikiContext context) throws XWikiException {
        XWikiCacheService cacheService = context.getWiki().getCacheService();
        groupCache = cacheService.newCache("xwiki.authentication.group.cache", iCapacity);
    }

    public void flushCache() {
        if (groupCache!=null) {
            groupCache.flushAll();
            groupCache = null;
        }
    }

    public Collection listGroupsForUser(String username, XWikiContext context) throws XWikiException {
        List list = null;
        String database = context.getDatabase();
        try {
            String shortname = Util.getName(username);
            String veryshortname = shortname.substring(shortname.indexOf(".") + 1);
            String key = database + ":" + shortname;
            synchronized (key) {
                if (groupCache==null)
                    initCache(context);
                try {
                    list = (List) groupCache.getFromCache(key);
                } catch (XWikiCacheNeedsRefreshException e) {
                    groupCache.cancelUpdate(key);
                    
                    if (context.getWiki().getNotCacheStore() instanceof XWikiHibernateStore) {
                    	list = context.getWiki().getStore().searchDocumentsNames(", BaseObject as obj, StringProperty as prop "
                            + "where obj.name=" + context.getWiki().getFullNameSQL() + " and obj.className='XWiki.XWikiGroups' "
                            + "and obj.id = prop.id.id and prop.id.name='member' "
                            + "and (prop.value='" + Utils.SQLFilter(username)
                            + "' or prop.value='" + Utils.SQLFilter(shortname) + "' or prop.value='"
                            + Utils.SQLFilter(veryshortname) + "')", context);
                    } else if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
                    	list = ((XWikiJcrStore) context.getWiki().getNotCacheStore()).listGroupsForUser(username, context);
                    }
                    groupCache.putInCache(key, list);
                }
            }
            return list;
        } finally {
            context.setDatabase(database);
        }
    }

    /*
       Adding the user to the group cache
    */
    public void addUserToGroup(String username, String database, String group, XWikiContext context) throws XWikiException {
        String shortname = Util.getName(username);
        List list = null;
        String key = database + ":" + shortname;
        if (groupCache==null)
            initCache(context);
        synchronized (key) {
            try {
                list = (List) groupCache.getFromCache(key);
            } catch (XWikiCacheNeedsRefreshException e) {
                groupCache.cancelUpdate(key);
                list = new ArrayList();
                groupCache.putInCache(key, list);
            }
        }
        list.add(group);
    }

    public List listMemberForGroup(String group, XWikiContext context) throws XWikiException {
        List list = new ArrayList();
        String database = context.getDatabase();
        String sql = "";

        try {
            if (group == null) {
            	if (context.getWiki().getHibernateStore() != null) {
            		sql = ", BaseObject as obj where obj.name=doc.fullName and obj.className='XWiki.XWikiUsers'";
            		return context.getWiki().getStore().searchDocumentsNames(sql, context);
            	} else if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
            		String xpath = "/*/*/obj/XWiki/XWikiUsers/jcr:deref(@doc, '*')/@fullName";
            		QueryPlugin qp = (QueryPlugin) context.getWiki().getPlugin("query", context);
            		return qp.xpath(xpath).list();
            	} else
            		return list;
            } else {
                String gshortname = Util.getName(group, context);
                XWikiDocument docgroup = context.getWiki().getDocument(gshortname, context);
                Vector v = docgroup.getObjects("XWiki.XWikiGroups");
                for (int i = 0; i < v.size(); i++) {
                    BaseObject bobj = (BaseObject) v.get(i);
                    if (bobj != null) {
                        String members = bobj.getStringValue("member");
                        if (members != null) {
                            String[] members2 = members.split(" ,");
                            for (int j = 0; j < members2.length; j++) {
                                list.add(members2[i]);
                            }
                        }
                    }
                }
                return list;
            }
        } catch (XWikiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            context.setDatabase(database);
        }
		return null;
    }

    public List listAllGroups(XWikiContext context) throws XWikiException {
    	if (context.getWiki().getHibernateStore()!=null) {
    		String sql = ", BaseObject as obj where obj.name=doc.fullName and obj.className='XWiki.XWikiGroups'";
    		return context.getWiki().getStore().searchDocumentsNames(sql, context);
    	} else if (context.getWiki().getNotCacheStore() instanceof XWikiJcrStore) {
    		String xpath = "/*/*/obj/XWiki/XWikiGroups/jcr:deref(@doc, '*')/@fullName";
    		QueryPlugin qp = (QueryPlugin) context.getWiki().getPlugin("query", context);
			return qp.xpath(xpath).list();			
    	} else
    		return null;
    }

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event, XWikiContext context) {
        try {
            if (event == XWikiNotificationInterface.EVENT_CHANGE) {
                boolean flushCache = false;

                if ((olddoc != null) && (olddoc.getObjects("XWiki.XWikiGroups") != null))
                    flushCache = true;

                if ((newdoc != null) && (newdoc.getObjects("XWiki.XWikiGroups") != null))
                    flushCache = true;

                if (flushCache)
                    flushCache();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

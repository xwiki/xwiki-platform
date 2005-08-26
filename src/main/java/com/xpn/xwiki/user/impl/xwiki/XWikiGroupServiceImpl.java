/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 4 juin 2004
 * Time: 10:52:35
 */
package com.xpn.xwiki.user.impl.xwiki;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

public class XWikiGroupServiceImpl implements XWikiGroupService, XWikiDocChangeNotificationInterface {
    private XWikiCache groupCache;

    public void init(XWiki xwiki) {
        XWikiCacheService cacheService = xwiki.getCacheService();
        groupCache = cacheService.newCache();
        xwiki.getNotificationManager().addGeneralRule(new DocChangeRule(this));
    }

    public void flushCache() {
        groupCache.flushAll();
    }

    public Collection listGroupsForUser(String username, XWikiContext context) throws XWikiException {
        List list;
        String database = context.getDatabase();
        try {
            String shortname = Util.getName(username);
            String veryshortname = shortname.substring(shortname.indexOf(".")+1);
            String key = database + ":" + shortname;
            try {
                list = (List) groupCache.getFromCache(key);
            } catch (XWikiCacheNeedsRefreshException e) {
                list = context.getWiki().getStore().searchDocumentsNames(", BaseObject as obj, StringProperty as prop "
                        + "where obj.name=" + context.getWiki().getFullNameSQL() + " and obj.className='XWiki.XWikiGroups' "
                        + "and obj.id = prop.id.id and prop.id.name='member' "
                        + "and (prop.value='" + Utils.SQLFilter(username)
                        + "' or prop.value='" + Utils.SQLFilter(shortname) + "' or prop.value='"
                        + Utils.SQLFilter(veryshortname) + "')", context);
                groupCache.putInCache(key, list);
            }
            return list;
        } finally {
            context.setDatabase(database);
        }
    }

    /*
       Adding the user to the group cache
    */
    public void addUserToGroup(String username, String database, String group) {
        String shortname = Util.getName(username);
        List list = null;
        String key = database + ":" + shortname;
        try {
            list = (List) groupCache.getFromCache(key);
        } catch (XWikiCacheNeedsRefreshException e) {
            list = new ArrayList();
            groupCache.putInCache(key, list);
            groupCache.cancelUpdate(key);
        }
        list.add(group);
    }

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event, XWikiContext context) {
        try {
            if (event==XWikiNotificationInterface.EVENT_CHANGE) {
                groupCache.flushEntry(newdoc.getDatabase() + ":" + newdoc.getFullName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

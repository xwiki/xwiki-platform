/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 * Date: 24 janv. 2004
 * Time: 17:35:44
 */
package com.xpn.xwiki.user;

import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;
import com.opensymphony.user.provider.AccessProvider;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

import java.util.*;

public class XWikiGroupProvider extends XWikiBaseProvider implements AccessProvider {

    public boolean init(Properties properties) {
        super.init(properties);
        return true;
    }
    
    public boolean create(String name) {
        name = getName(name);
        try {
            BaseClass bclass = getxWiki().getGroupClass(context);
            XWikiDocInterface doc = getDocument(name);
            if (doc.isNew()) {
                BaseObject obj = (BaseObject) bclass.newObject();
                obj.setxWikiClass(bclass);
                obj.setName(name);
                doc.setObject("XWiki.XWikiGroups", 0, obj);
                getxWiki().saveDocument(doc, context);
                getPropertySets().put(name, doc);
                return true;
            } else
                return false;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean handles(String name) {
        if (super.handles(name))
            return true;
        try {
            name = getName(name);
            List list = getxWiki().searchDocuments(", BaseObject as obj where obj.name=CONCAT(XWD_WEB,'.',XWD_NAME)"
                    + " and obj.className in ('XWiki.XWikiUsers','XWiki.XWikiGroups') and obj.name = '" + name + "'", context);
            boolean result = (list.size()>0);
            if (result)
                getHandledNames().add(name);
            return result;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List list() {
        try {
            return getxWiki().searchDocuments(", BaseObject as obj where obj.name=CONCAT(XWD_WEB,'.',XWD_NAME) and obj.className='XWiki.XWikiGroups'", context);
        } catch (XWikiException e) {
            return new ArrayList();
        }
    }


    public PropertySet getPropertySet(String name) {
        try {
            name = getName(name);
            HashMap args = new HashMap();
            XWikiDocInterface doc = getDocument(name);
            args.put("doc", doc);
            args.put("globalKey", name);
            args.put("classKey", "XWiki.XWikiGroups");
            // args.put("propertyMap", propertyMap);
            return PropertySetManager.getInstance("xwiki", args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean addToGroup(String username, String groupname) {
        try {
            username = getName(username);
            groupname = getName(groupname);
            if (inGroup(username, groupname))
                return true;
            XWikiDocInterface doc = getDocument(groupname);
            BaseClass bclass = null;
            bclass = getxWiki().getGroupClass(context);
            BaseObject bobj = new BaseObject();
            bobj.setName(groupname);
            bobj.setStringValue("member", username);
            bobj.setxWikiClass(bclass);
            doc.addObject("XWiki.XWikiGroups", bobj);
            getxWiki().saveDocument(doc, context);
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean inGroup(String username, String groupname) {
        username = getName(username);
        groupname = getName(groupname);
        XWikiDocInterface doc = getDocument(groupname);
        Vector vobj = doc.getObjects("XWiki.XWikiGroups");
        if (vobj==null)
            return false;
        for (int i=0;i<vobj.size();i++) {
            BaseObject object = (BaseObject)vobj.get(i);
            if (object==null)
             continue;
            String member = object.getStringValue("member");
            if (member==null)
                return false;
            if (member.equals(username))
                return true;
        }
        return false;
    }

    public List listGroupsContainingUser(String username) {
        List list;
        try {
            username = getName(username);
            list = getxWiki().searchDocuments(", BaseObject as obj, StringProperty as prop "
                    + "where obj.name=CONCAT(XWD_WEB,'.',XWD_NAME) and obj.className='XWiki.XWikiGroups' "
                    + "and obj.id = prop.id.id and prop.id.name='member' and prop.value='" + username + "'", context);
            // we might need to deduplicate..
            return list;
        } catch (XWikiException e) {
            e.printStackTrace();
            return new ArrayList();
        }
    }

    public List listUsersInGroup(String groupname) {
        groupname = getName(groupname);
        List list = new ArrayList();
        XWikiDocInterface doc = getDocument(groupname);
        Vector vobj = doc.getObjects("XWiki.XWikiGroups");
        if (vobj==null)
            return list;
        for (int i=0;i<vobj.size();i++) {
            BaseObject object = (BaseObject)vobj.get(i);
            if (object==null)
                continue;
            String member = object.getStringValue("member");
            list.add(member);
        }
        return list;
    }

    public boolean removeFromGroup(String username, String groupname) {
        try {
            username = getName(username);
            groupname = getName(groupname);
            boolean needsUpdate = false;
            XWikiDocInterface doc = getDocument(groupname);
            Vector vobj = doc.getObjects("XWiki.XWikiGroups");
            if (vobj==null)
                return true;
            for (int i=0;i<vobj.size();i++) {
                BaseObject object = (BaseObject)vobj.get(i);
                if (object==null)
                    continue;
                String member = object.getStringValue("member");
                if (member.equals(username)) {
                    doc.getObjectsToRemove().add(object);
                    vobj.set(i, null);
                    needsUpdate = true;
                }
            }
            if (needsUpdate)
                getxWiki().saveDocument(doc, context);
            return true;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }

}

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
 * Time: 17:30:21
 */
package com.xpn.xwiki.user;

import com.opensymphony.user.provider.ProfileProvider;
import com.opensymphony.user.provider.CredentialsProvider;
import com.opensymphony.user.User;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;

import java.util.*;

public class XWikiUserProvider extends XWikiBaseProvider implements ProfileProvider, CredentialsProvider {

    private static Map propertyMap;

    public boolean init(Properties properties) {
        super.init(properties);
        propertyMap = new HashMap();
        propertyMap.put(User.PROPERTY_EMAIL, "email");
        propertyMap.put(User.PROPERTY_FULLNAME, "fullname");
        return true;
    }

    public boolean create(String name) {
        try {
            name = getName(name);
            BaseClass bclass = getxWiki().getUserClass();
            XWikiDocInterface doc = getDocument(name);
            if (doc.isNew()) {
                BaseObject obj = (BaseObject) bclass.newObject();
                obj.setxWikiClass(bclass);
                obj.setName(name);
                doc.setObject("XWiki.XWikiUsers", 0, obj);
                getPropertySets().put(name, doc);
                getxWiki().saveDocument(doc);
                return true;
            } else
                return false;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean handles(String name) {
        try {
            name = getName(name);
            List list = getxWiki().searchDocuments(", BaseObject as obj where obj.name=CONCAT(XWD_WEB,'.',XWD_NAME)"
                    + " and obj.className='XWiki.XWikiUsers' and obj.name = '" + name + "'");
            return (list.size()>0);
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List list() {
        try {
            return getxWiki().searchDocuments(", BaseObject as obj where obj.name=CONCAT(XWD_WEB,'.',XWD_NAME) and obj.className='XWiki.XWikiUsers'");
        } catch (XWikiException e) {
            e.printStackTrace();
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
            args.put("classKey", "XWiki.XWikiUsers");
            args.put("propertyMap", propertyMap);
            return PropertySetManager.getInstance("xwiki", args);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean authenticate(String name, String password) {
        try {
            name = getName(name);
            XWikiDocInterface doc = getDocument(name);
            String passwd = doc.getObject("XWiki.XWikiUsers", 0).get("password").toString();
            return (password.equals(passwd));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean changePassword(String name, String password) {
        name = getName(name);
        XWikiDocInterface doc = getDocument(name);
        BaseObject bobj = doc.getObject("XWiki.XWikiUsers", 0);
        BaseProperty bprop = (BaseProperty) bobj.safeget("password");
        if (bprop==null) {
            bprop = new StringProperty();
            bobj.safeput("password", bprop);
        }
        bprop.setValue(new String(password));
        return store(name, null);
    }
}

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
 * Date: 26 janv. 2004
 * Time: 10:58:10
 */
package com.xpn.xwiki.user;

import com.opensymphony.module.user.Entity;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;

import java.util.*;

public class XWikiBaseProvider {

    private XWiki xWiki;
    private Set handledNames = new HashSet();
    private static Map propertySets;
    protected XWikiContext context = new XWikiContext();

    public XWiki getxWiki() {
        return xWiki;
    }

    public void setxWiki(XWiki xWiki) {
        this.xWiki = xWiki;
    }

    public static Map getPropertySets() {
        return propertySets;
    }

    public static void setPropertySets(Map propertySets) {
        XWikiBaseProvider.propertySets = propertySets;
    }

    public String getName(String name) {
        String database = null;
        int i0 = name.indexOf(":");
        if (i0!=-1) {
             database = name.substring(0,i0);
             name = name.substring(i0+1);
             context.setDatabase(database);
             return name;
        }

        context.setDatabase(null);
        if (name.startsWith("XWiki."))
            return name;
        else
            return "XWiki." + name;
    }

    public String getFullName(String name) {
        return context.getDatabase() + ":" + name;
    }

    public XWikiDocInterface getDocument(String name) {
        XWikiDocInterface doc = null;
        try {
            name = getName(name);
            doc = getxWiki().getDocument(name, context);
            getPropertySets().put(getFullName(name), doc);
            return doc;
        } catch (XWikiException e) {
            return null;
        }
    }

    public boolean init(Properties properties) {
        propertySets = new HashMap();
        return true;
    }

    public boolean handles(String name) {
        return (getHandledNames().contains(name));
    }

    public void flushCaches() {
        getxWiki().flushCache();
        setHandledNames(new HashSet());
    }


    public boolean load(String name, Entity.Accessor accessor) {
        XWikiDocInterface doc = getDocument(name);
        accessor.setMutable(true);
        return (doc!=null);
    }

    public boolean remove(String name) {
        name = getName(name);
        // Currently a user cannot be removed
        return false;
    }

    public boolean store(String name, Entity.Accessor accessor) {
        name = getName(name);
        XWikiDocInterface doc = (XWikiDocInterface) getPropertySets().get(getFullName(name));
        if (doc==null)
            return false;

        try {
            getxWiki().saveDocument(doc, context);
            return true;
        } catch (XWikiException e) {
            return false;
        }
    }

    public Set getHandledNames() {
        return handledNames;
    }

    public void setHandledNames(Set handledNames) {
        this.handledNames = handledNames;
    }
}

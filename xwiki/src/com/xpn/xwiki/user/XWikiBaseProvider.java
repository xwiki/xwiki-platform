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

import com.opensymphony.user.Entity;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class XWikiBaseProvider {

    private XWiki xWiki;
    private static Map propertySets;

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
        if (name.startsWith("XWiki."))
            return name;
        else
            return "XWiki." + name;
    }

    public XWikiDocInterface getDocument(String name) {
        XWikiDocInterface doc = null;
        try {
            doc = getxWiki().getDocument(name);
            getPropertySets().put(name, doc);
            return doc;
        } catch (XWikiException e) {
            return null;
        }
    }

    public boolean init(Properties properties) {
        propertySets = new HashMap();
        return true;
    }

    public void flushCaches() {
        getxWiki().flushCache();
    }


    public boolean load(String name, Entity.Accessor accessor) {
        name = getName(name);
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
        XWikiDocInterface doc = (XWikiDocInterface) getPropertySets().get(name);
        if (doc==null)
            return false;

        try {
            getxWiki().saveDocument(doc);
            return true;
        } catch (XWikiException e) {
            return false;
        }
    }
}

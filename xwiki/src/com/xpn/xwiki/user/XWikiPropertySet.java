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
 * Date: 25 janv. 2004
 * Time: 20:57:26
 */
package com.xpn.xwiki.user;

import com.opensymphony.module.propertyset.AbstractPropertySet;
import com.opensymphony.module.propertyset.PropertyException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.doc.XWikiDocInterface;

import java.util.Collection;
import java.util.Map;

public class XWikiPropertySet extends AbstractPropertySet {
    private String globalKey;
    private String classKey;
    private XWikiDocInterface doc;
    private Map propertyMap;

    public BaseObject getObject() {
        return getDoc().getObject(getClassKey(),0);
    }

    public void init(Map config, Map args) {
        super.init(config, args);

        setDoc((XWikiDocInterface) args.get("doc"));
        setGlobalKey((String) args.get("globalKey"));
        setClassKey((String) args.get("classKey"));
        setPropertyMap((Map) args.get("propertyMap"));
    }

    protected String getPropertyName(String name) {
        String propname = (String)propertyMap.get(name);
        if (propname==null)
            return name;
        else
            return propname;
    }

    protected void setImpl(int type, String key, Object value) throws PropertyException {
        ((BaseProperty)getObject().safeget(getPropertyName(key))).setValue(value);
    }

    protected Object get(int type, String key) throws PropertyException {
        return ((BaseProperty)getObject().safeget(getPropertyName(key))).getValue();
    }

    public Collection getKeys(String prefix, int type) throws PropertyException {
        return null;
    }

    public int getType(String key) throws PropertyException {
        return 0;
    }

    public boolean exists(String key) throws PropertyException {
        return (((BaseProperty)getObject().safeget(getPropertyName(key))).getValue()!=null);
    }

    public void remove(String key) throws PropertyException {
        try {
            getObject().put(getPropertyName(key), null);
        } catch (XWikiException e) {
        }
    }

    public void remove() throws PropertyException {
        getDoc().getxWikiObjects().remove(getClassKey());
    }

    public String getGlobalKey() {
        return globalKey;
    }

    public void setGlobalKey(String globalKey) {
        this.globalKey = globalKey;
    }

    public String getClassKey() {
        return classKey;
    }

    public void setClassKey(String classKey) {
        this.classKey = classKey;
    }

    public XWikiDocInterface getDoc() {
        return doc;
    }

    public void setDoc(XWikiDocInterface doc) {
        this.doc = doc;
    }

    public Map getPropertyMap() {
        return propertyMap;
    }

    public void setPropertyMap(Map propertyMap) {
        this.propertyMap = propertyMap;
    }

}

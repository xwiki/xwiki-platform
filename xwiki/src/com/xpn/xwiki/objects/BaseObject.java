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
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 9 déc. 2003
 * Time: 11:36:06
 */
package com.xpn.xwiki.objects;


import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.*;

import javax.mail.internet.ParameterList;
import java.util.HashMap;
import java.util.Map;

public class BaseObject extends BaseProperty implements ObjectInterface {
    private BaseClass xWikiClass;
    protected Map fields = new HashMap();

    public Object[] getProperties() {
        Object[] array = fields.values().toArray();
        return array;
    }

    public Object[] getPropertyNames() {
        Object[] array = fields.keySet().toArray();
        return array;
    }

    public void checkField(String name) throws XWikiException {
        if (getxWikiClass().safeget(name)==null) {
            Object[] args = { name, getxWikiClass().getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_CLASSES, XWikiException.ERROR_XWIKI_CLASSES_FIELD_DOES_NOT_EXIST,
                    "Field {0} does not exist in class {1}", null, args);
        }
    }

    public PropertyInterface safeget(String name) {
        return (PropertyInterface) fields.get(name);
    }

    public PropertyInterface get(String name) throws XWikiException {
        checkField(name);
        return safeget(name);
    }

    public void safeput(String name,PropertyInterface property) {
        fields.put(name, property);
    }

    public void put(String name,PropertyInterface property) throws XWikiException {
        checkField(name);
        safeput(name, property);
    }


    public BaseClass getxWikiClass() {
        return xWikiClass;
    }

    public void setxWikiClass(BaseClass xWikiClass) {
        this.xWikiClass = xWikiClass;
    }

    public String getStringValue(String name) {
        return ((StringProperty)safeget(name)).getValue();
    }

    public void setStringValue(String name, String value) {
        StringProperty property = new StringProperty();
        property.setValue(value);
        safeput(name, property);
    }

    public int getIntValue(String name) {
        return ((NumberProperty)safeget(name)).getValue().intValue();
    }

    public void setIntValue(String name, int value) {
        NumberProperty property = new NumberProperty();
        property.setValue(new Integer(value));
        safeput(name, property);
    }

    public void displayHidden(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        getxWikiClass().displayHidden(buffer, name, prefix, this, context);
    }

    public void displaySearch(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        getxWikiClass().displaySearch(buffer, name, prefix, this, context);
    }

    public void displayView(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        getxWikiClass().displayView(buffer, name, prefix, this, context);
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        getxWikiClass().displayEdit(buffer, name, prefix, this, context);
    }

    public String displayHidden(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        getxWikiClass().displayHidden(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displaySearch(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        getxWikiClass().displaySearch(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displayView(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        getxWikiClass().displayView(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displayEdit(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        getxWikiClass().displayEdit(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displayHidden(String name, XWikiContext context) {
        return displayHidden(name, "", context);
    }

    public String displaySearch(String name, XWikiContext context) {
        return displaySearch(name, "", context);
    }

    public String displayView(String name, XWikiContext context) {
        return displayView(name, "", context);
    }

    public String displayEdit(String name, XWikiContext context) {
        return displayEdit(name, "", context);
    }
}

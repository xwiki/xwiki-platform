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


    public void checkField(String name) throws XWikiException {
        if (getxWikiClass().get(name)==null) {
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

    protected String getStringValue(String name) {
        return ((StringProperty)safeget(name)).getValue();
    }

    public void setStringValue(String name, String value) {
      StringProperty property = new StringProperty();
      property.setValue(value);
      safeput(name, property);
    }

}

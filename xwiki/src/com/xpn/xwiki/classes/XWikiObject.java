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
package com.xpn.xwiki.classes;


import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import javax.mail.internet.ParameterList;
import java.util.HashMap;
import java.util.Map;

public class XWikiObject extends XWikiObjectProperty implements XWikiObjectInterface {
    private XWikiClass xWikiClass;
    protected Map fields = new HashMap();


    public void checkField(String name) throws XWikiException {
        if (getxWikiClass().get(name)==null) {
            Object[] args = { name, getxWikiClass().getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_CLASSES, XWikiException.ERROR_XWIKI_CLASSES_FIELD_DOES_NOT_EXIST,
                    "Field {0} does not exist in class {1}", null, args);
        }
    }

    public XWikiObjectPropertyInterface get(String name) throws XWikiException {
        checkField(name);
        return (XWikiObjectPropertyInterface) fields.get(name);
    }

    public void put(String name,XWikiObjectPropertyInterface property) throws XWikiException {
        checkField(name);
        fields.put(name, property);
    }


    public XWikiClass getxWikiClass() {
        return xWikiClass;
    }

    public void setxWikiClass(XWikiClass xWikiClass) {
        this.xWikiClass = xWikiClass;
    }

}

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
import com.xpn.xwiki.objects.classes.*;

import java.io.Serializable;

public class BaseObject extends BaseCollection implements ObjectInterface, Serializable {

    public void displayHidden(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        ((PropertyClass)getxWikiClass().get(name)).displayHidden(buffer, name, prefix, this, context);
    }

    public void displaySearch(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        ((PropertyClass)getxWikiClass().get(name)).displaySearch(buffer, name, prefix, this, context);
    }

    public void displayView(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        ((PropertyClass)getxWikiClass().get(name)).displayView(buffer, name, prefix, this, context);
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        ((PropertyClass)getxWikiClass().get(name)).displayEdit(buffer, name, prefix, this, context);
    }

    public String displayHidden(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass)getxWikiClass().get(name)).displayHidden(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displaySearch(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass)getxWikiClass().get(name)).displaySearch(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displayView(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass)getxWikiClass().get(name)).displayView(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displayEdit(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass)getxWikiClass().get(name)).displayEdit(buffer, name, prefix, this, context);
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

    public Object clone() {
        BaseObject object = (BaseObject) super.clone();
        return object;
    }
}

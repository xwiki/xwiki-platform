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
 * Time: 13:41:33
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.meta.MetaClass;

public abstract class PropertyClass extends BaseObject implements PropertyClassInterface {

    public String toString(BaseProperty property) {
        return property.toString();  //To change body of implemented methods use Options | File Templates.
    }

    public void displayHidden(StringBuffer buffer, String name, BaseObject object, XWikiContext context) {
    }

    public void displaySearch(StringBuffer buffer, String name, BaseObject object, XWikiContext context) {
    }

    public void displayView(StringBuffer buffer, String name, BaseObject object, XWikiContext context) {
    }

    public void displayEdit(StringBuffer buffer, String name, BaseObject object, XWikiContext context) {
    }

    public BaseClass getxWikiClass() {
        BaseClass wclass = (BaseClass)super.getxWikiClass();
        if (wclass==null) {
            wclass = new MetaClass();
            setxWikiClass(wclass);
        }
        return wclass;
    }

    public String getName() {
        return getStringValue("name");
    }

    public void setName(String name) {
      setStringValue("name", name);
    }

    public String getType() {
      return getStringValue("type");
    }

    public void setType(String type) {
        setStringValue("type", type);
    }

    public String getPrettyName() {
        return getStringValue("prettyName");
    }

    public void setPrettyName(String prettyName) {
        setStringValue("prettyName", prettyName);
    }

}

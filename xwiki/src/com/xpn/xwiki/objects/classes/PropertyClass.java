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
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.meta.MetaClass;
import org.apache.ecs.html.Input;

public abstract class PropertyClass extends BaseObject implements PropertyClassInterface {

    public void checkField(String name) throws XWikiException {
        if ((getxWikiClass().safeget(name)==null)&&
            (getxWikiClass().safeget("meta" + name)==null)){
            Object[] args = { name, getxWikiClass().getName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_CLASSES, XWikiException.ERROR_XWIKI_CLASSES_FIELD_DOES_NOT_EXIST,
                    "Field {0} does not exist in class {1}", null, args);
        }
    }

    public String toString(BaseProperty property) {
        return property.toString();  //To change body of implemented methods use Options | File Templates.
    }

    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseObject object, XWikiContext context) {
       Input input = new Input();
       PropertyInterface prop = object.safeget(name);
       if (prop!=null) input.setValue(prop.toString());

       input.setType("hidden");
       input.setName(prefix + name);
       buffer.append(input.toString());
    }

    public void displaySearch(StringBuffer buffer, String name, String prefix, BaseObject object, XWikiContext context) {
        Input input = new Input();
        PropertyInterface prop = object.safeget(name);
        if (prop!=null) input.setValue(prop.toString());

        input.setType("text");
        input.setName(prefix + name);
        buffer.append(input.toString());
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseObject object, XWikiContext context) {
        buffer.append(object.safeget(name).toString());
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseObject object, XWikiContext context) {
        Input input = new Input();

        PropertyInterface prop = object.safeget(name);
        if (prop!=null) input.setValue(prop.toString());

        input.setType("text");
        input.setName(prefix + name);
        buffer.append(input.toString());
    }

    public String displayHidden(String name, String prefix, BaseObject object, XWikiContext context) {
      StringBuffer buffer = new StringBuffer();
      displayHidden(buffer, name, prefix, object, context);
      return buffer.toString();
    }
    public String displayHidden(String name, BaseObject object, XWikiContext context) {
      return displayHidden(name, "", object, context);
    }

    public String displaySearch(String name, String prefix, BaseObject object, XWikiContext context) {
      StringBuffer buffer = new StringBuffer();
      displaySearch(buffer, name, prefix, object, context);
      return buffer.toString();
    }
    public String displaySearch(String name, BaseObject object, XWikiContext context) {
      return displaySearch(name, "", object, context);
    }

    public String displayView(String name, String prefix, BaseObject object, XWikiContext context) {
      StringBuffer buffer = new StringBuffer();
      displayView(buffer, name, prefix, object, context);
      return buffer.toString();
    }
    public String displayView(String name, BaseObject object, XWikiContext context) {
      return displayView(name, "", object, context);
    }

    public String displayEdit(String name, String prefix, BaseObject object, XWikiContext context) {
      StringBuffer buffer = new StringBuffer();
      displayEdit(buffer, name, prefix, object, context);
      return buffer.toString();
    }
    public String displayEdit(String name, BaseObject object, XWikiContext context) {
      return displayEdit(name, "", object, context);
    }


    public BaseClass getxWikiClass() {
        BaseClass wclass = (BaseClass)super.getxWikiClass();
        if (wclass==null) {
            MetaClass metaclass = MetaClass.getMetaClass();
            wclass = (BaseClass)metaclass.get(getType());
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

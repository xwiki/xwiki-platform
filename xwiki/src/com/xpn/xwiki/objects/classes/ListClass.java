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
 * Date: 1 févr. 2004
 * Time: 21:46:26
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.XWikiContext;
import org.apache.ecs.html.Select;
import org.apache.ecs.html.Option;
import org.apache.ecs.html.Input;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import java.util.*;

public abstract class ListClass extends PropertyClass {

    public ListClass(String name, String prettyname, PropertyMetaClass wclass) {
        super(name, prettyname, wclass);
        setRelationalStorage(false);
        setDisplayType("select");
        setMultiSelect(false);
        setSize(1);
    }


    public ListClass(PropertyMetaClass wclass) {
        this("list", "List", wclass);
    }

    public ListClass() {
        this(null);
    }

    public String getDisplayType() {
        return getStringValue("displayType");
    }

    public void setDisplayType(String type) {
        setStringValue("displayType", type);
    }

    public int getSize() {
        return getIntValue("size");
    }

    public void setSize(int size) {
        setIntValue("size", size);
    }

    public boolean isMultiSelect() {
        return (getIntValue("multiSelect")==1);
    }

    public void setMultiSelect(boolean multiSelect) {
        setIntValue("multiSelect", multiSelect ? 1 : 0);
    }

    public boolean isRelationalStorage() {
        return (getIntValue("relationalStorage")==1);
    }

    public void setRelationalStorage(boolean storage) {
        setIntValue("relationalStorage", storage ? 1 : 0);
    }

    public static List getListFromString(String value) {
        List list = new ArrayList();
        if (value==null)
            return list;

        String val = StringUtils.replace(value, "\\|", "%PIPE%");
        String[] result = StringUtils.split(value,"|");
        for (int i=0;i<result.length;i++)
            list.add(StringUtils.replace(result[i],"%PIPE%", "|"));
        return list;
    }

    public BaseProperty newProperty() {
        BaseProperty lprop;

        if (isRelationalStorage()&&isMultiSelect())
            lprop = new DBStringListProperty();
        else if (isMultiSelect())
            lprop = new StringListProperty();
        else
            lprop = new StringProperty();
        return lprop;
    }

    public BaseProperty fromString(String value) {
        BaseProperty prop = newProperty();
        if (isMultiSelect())
            ((ListProperty)prop).setList(getListFromString(value));
        else
            prop.setValue(value);
        return prop;
    }

    public BaseProperty fromStringArray(String[] strings) {
        if ((!isMultiSelect())||(strings.length==1))
            return fromString(strings[0]);
        else {
            List list = new ArrayList();
            for (int i=0;i<strings.length;i++)
                list.add(strings[i]);
            BaseProperty prop = newProperty();
            ((ListProperty)prop).setList(list);
            return prop;
        }
    }


    public BaseProperty newPropertyfromXML(Element ppcel) {
        if ((!isRelationalStorage())&&(!isMultiSelect()))
            return super.newPropertyfromXML(ppcel);

        List elist = ppcel.elements("value");
        BaseProperty lprop = (BaseProperty)newProperty();

        List llist;

        if ((isRelationalStorage())&&(isMultiSelect()))
            llist = ((DBStringListProperty)lprop).getList();
        else
            llist = ((ListProperty)lprop).getList();

        for (int i=0;i<elist.size();i++) {
            Element el = (Element) elist.get(i);
            llist.add(el.getText());
        }
        return lprop;
    }


    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        Input input = new Input();
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop!=null) input.setValue(prop.toFormString());

        input.setType("hidden");
        input.setName(prefix + name);
        buffer.append(input.toString());
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        List selectlist;
        BaseProperty prop =  (BaseProperty)object.safeget(name);
        if ((prop instanceof ListProperty)||(prop instanceof DBStringListProperty)) {
            selectlist = (List) prop.getValue();
            buffer.append(StringUtils.join(selectlist.toArray(), " "));
        } else {
            buffer.append(prop.getValue().toString());
        }
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        if (getDisplayType()=="input") {
            Input input = new Input();
            BaseProperty prop = (BaseProperty) object.safeget(name);
            if (prop!=null) input.setValue(prop.toFormString());
            input.setType("text");
            input.setSize(60);
            input.setName(prefix + name);
            buffer.append(input.toString());
        } else {
            Select select = new Select(prefix + name, 1);
            select.setMultiple(isMultiSelect());

            List list = getList();
            List selectlist;

            BaseProperty prop =  (BaseProperty)object.safeget(name);
            if (prop==null) {
                selectlist = new ArrayList();
            } else if ((prop instanceof ListProperty)||(prop instanceof DBStringListProperty)) {
                selectlist = (List) prop.getValue();
            } else {
                selectlist = new ArrayList();
                selectlist.add(prop.getValue());
            }

            // Add options from Set
            for (Iterator it=list.iterator();it.hasNext();) {
                String value = it.next().toString();
                Option option = new Option(value, value, value);
                if (selectlist.contains(value))
                    option.setSelected(true);
                select.addElement(option);
            }

            buffer.append(select.toString());
        }
    }

    public abstract List getList();


}

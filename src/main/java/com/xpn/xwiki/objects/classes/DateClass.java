/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 9 déc. 2003
 * Time: 13:58:38

 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import org.apache.ecs.xhtml.input;
import org.dom4j.Element;
import org.hibernate.mapping.Property;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

public class DateClass  extends PropertyClass {

    public DateClass(PropertyMetaClass wclass) {
        super("date", "Date", wclass);
        setSize(20);
        setDateFormat("dd/MM/yyyy HH:mm:ss");
        setEmptyIsToday(1);
    }

    public DateClass() {
        this(null);
    }

    public int getSize() {
        return getIntValue("size");
    }

    public void setSize(int size) {
        setIntValue("size", size);
    }

    public int getEmptyIsToday() {
        return getIntValue("emptyIsToday");
    }

    public void setEmptyIsToday(int emptyIsToday) {
        setIntValue("emptyIsToday", emptyIsToday);
    }

    public String getDateFormat() {
        return getStringValue("dateFormat");
    }

    public void setDateFormat(String dformat) {
        setStringValue("dateFormat", dformat);
    }

    public BaseProperty fromString(String value) {
        BaseProperty property = newProperty();

        if ((value==null)||(value.equals(""))) {
            property.setValue(new Date());
            return property;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat());
            property.setValue(sdf.parse(value));
        } catch (ParseException e) {
            return null;
        }
        return property;
    }

    public BaseProperty newProperty() {
        BaseProperty property = new DateProperty();
        property.setName(getName());
        return property;
    }

    public String toFormString(BaseProperty property) {
        SimpleDateFormat sdf = new SimpleDateFormat(getDateFormat());
        return sdf.format(property.getValue());
    }

    public BaseProperty newPropertyfromXML(Element ppcel) {
        String value = ppcel.getText();
        BaseProperty property = newProperty();

        if ((value==null)||(value.equals(""))) {
            property.setValue(new Date());
            return property;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
            property.setValue(sdf.parse(value));
        } catch (ParseException e) {
            try {
                e.printStackTrace();
                SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
                property.setValue(sdf.parse(value));
            } catch (ParseException e2) {
                e2.printStackTrace();
                property.setValue(new Date());
            }
        }
        return property;
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        BaseProperty prop = (BaseProperty) object.safeget(name);
        buffer.append(toFormString(prop));
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        input input = new input();

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop!=null) input.setValue(toFormString(prop));

        input.setType("text");
        input.setName(prefix + name);
        input.setSize(getSize());
        buffer.append(input.toString());
    }
}

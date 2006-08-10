/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.objects.classes;

import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;
import org.apache.ecs.xhtml.input;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.web.XWikiMessageTool;

public class BooleanClass extends PropertyClass {

    public BooleanClass(PropertyMetaClass wclass) {
        super("boolean", "Boolean", wclass);
    }

    public BooleanClass() {
        this(null);
    }

    public String getDisplayType() {
        String dtype = getStringValue("displayType");
        if ((dtype==null)||(dtype.equals(""))) {
            return "yesno";
        }
        return dtype;
    }

    public void setDefaultValue(int dvalue) {
        setIntValue("defaultValue", dvalue);
    }

    public int getDefaultValue() {
        return getIntValue("defaultValue", -1);
    }

    public void setDisplayType(String type) {
        setStringValue("displayType", type);
    }


    public BaseProperty fromString(String value) {
        BaseProperty property = newProperty();
        Number nvalue = null;
        if ((value!=null)&&(!value.equals("")))
                nvalue = new Integer(value);
        property.setValue(nvalue);
        return property;
    }

    public BaseProperty newProperty() {
        BaseProperty property = new IntegerProperty();
        property.setName(getName());
        return property;
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        IntegerProperty prop = (IntegerProperty) object.safeget(name);
        if (prop==null)
            return;

        Integer iValue = (Integer)prop.getValue();
        if (iValue!=null) {
            int value = iValue.intValue();
            buffer.append(getDisplayValue(context, value));
        }
    }


    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
    	if(getDisplayType().equals("checkbox")){
    		displayCheckboxEdit(buffer, name, prefix, object, context);
    	}
    	else {
    		displaySelectEdit(buffer, name, prefix, object, context);
    	}
    }
    public void displaySelectEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        select select = new select(prefix + name, 1);
        String String0 = getDisplayValue(context, 0);
        String String1 = getDisplayValue(context, 1);

        option[] options = { new option("---", "" ), new option(String1, "1" ), new option(String0, "0")};
        options[0].addElement("---");
        options[1].addElement(String1);
        options[2].addElement(String0);

        try {
        IntegerProperty prop = (IntegerProperty) object.safeget(name);
        if (prop!=null) {
            Integer ivalue = (Integer)prop.getValue();
            if (ivalue!=null) {
                int value = ivalue.intValue();
                if (value==1)
                    options[1].setSelected(true);
                else if (value==0)
                    options[2].setSelected(true);
            }  else {
                int value = getDefaultValue();
                if (value==1)
                    options[1].setSelected(true);
                else if (value==0)
                    options[2].setSelected(true);
            }
	        }
        } catch (Exception e) {
            // This should not happen
            e.printStackTrace();
        }
        select.addElement(options);
        buffer.append(select.toString());
    }

    public void displayCheckboxEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        org.apache.ecs.xhtml.input check = new input(input.checkbox, prefix + name, 1);
        org.apache.ecs.xhtml.input checkNo = new input(input.hidden, prefix + name, 0);

        try {
        IntegerProperty prop = (IntegerProperty) object.safeget(name);
        if (prop!=null) {
            Integer ivalue = (Integer)prop.getValue();
            if (ivalue!=null) {
                int value = ivalue.intValue();
                if (value==1)
                    check.setChecked(true);
                else if (value==0)
                	check.setChecked(false);
            }
            else {
                int value = getDefaultValue();
                if (value==1)
                    check.setChecked(true);
                else
                    check.setChecked(false);
            }
        }} catch (Exception e) {
            // This should not happen
            e.printStackTrace();
        }
        buffer.append(check.toString());
        buffer.append(checkNo.toString());
    }


    private String getDisplayValue(XWikiContext context, int value) {
        try {
            XWikiMessageTool msg = (XWikiMessageTool) context.get("msg");
            String strname = getDisplayType() + "_" + value;
            String result = msg.get(strname);
            if (result.equals(strname)){
             return "" + value;
            }
            else{
             return result;
            }
        } catch (Exception e) {
            return "" + value;
        }
    }

}

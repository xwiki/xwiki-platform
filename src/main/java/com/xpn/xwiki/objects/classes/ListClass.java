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
 * @author erwan
 * @author sdumitriu
 */

package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.web.XWikiMessageTool;
import org.apache.commons.lang.StringUtils;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;
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
		return (getIntValue("multiSelect") == 1);
	}

	public void setMultiSelect(boolean multiSelect) {
		setIntValue("multiSelect", multiSelect ? 1 : 0);
	}

	public boolean isRelationalStorage() {
		return (getIntValue("relationalStorage") == 1);
	}

	public void setRelationalStorage(boolean storage) {
		setIntValue("relationalStorage", storage ? 1 : 0);
	}

	public static List getListFromString(String value) {
		List list = new ArrayList();
		if (value == null)
			return list;

		String val = StringUtils.replace(value, "\\|", "%PIPE%");
		String[] result = StringUtils.split(val, "|");
		for (int i = 0; i < result.length; i++)
			list.add(StringUtils.replace(result[i], "%PIPE%", "|"));
		return list;
	}

	public BaseProperty newProperty() {
		BaseProperty lprop;

		if (isRelationalStorage() && isMultiSelect())
			lprop = new DBStringListProperty();
		else if (isMultiSelect())
			lprop = new StringListProperty();
		else
			lprop = new StringProperty();

		if (isMultiSelect() && getDisplayType().equals("input")) {
			((ListProperty) lprop).setFormStringSeparator("|");
		}

		return lprop;
	}

	public BaseProperty fromString(String value) {
		BaseProperty prop = newProperty();
		if (isMultiSelect()) {
			if (!getDisplayType().equals("input")) {
				((ListProperty) prop).setList(getListFromString(value));
			} else {
				((ListProperty) prop).setList(Arrays.asList(StringUtils.split(value, " ,|")));
			}
		} else
			prop.setValue(value);
		return prop;
	}

	public BaseProperty fromStringArray(String[] strings) {
		if ((!isMultiSelect()) || (strings.length == 1))
			return fromString(strings[0]);
		List list = new ArrayList();
		for (int i = 0; i < strings.length; i++)
			list.add(strings[i]);
		BaseProperty prop = newProperty();
		((ListProperty) prop).setList(list);
		return prop;
	}

	public BaseProperty newPropertyfromXML(Element ppcel) {
		if ((!isRelationalStorage()) && (!isMultiSelect()))
			return super.newPropertyfromXML(ppcel);

		List elist = ppcel.elements("value");
		BaseProperty lprop = newProperty();

		if (lprop instanceof ListProperty) {
			List llist = ((ListProperty) lprop).getList();
			for (int i = 0; i < elist.size(); i++) {
				Element el = (Element) elist.get(i);
				llist.add(el.getText());
			}
		} else {
			for (int i = 0; i < elist.size(); i++) {
				Element el = (Element) elist.get(i);
				((StringProperty) lprop).setValue(el.getText());
			}
		}
		return lprop;
	}

	public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
		input input = new input();
		BaseProperty prop = (BaseProperty) object.safeget(name);
		if (prop != null)
			input.setValue(prop.toFormString());

		input.setType("hidden");
		input.setName(prefix + name);
		input.setID(prefix + name);
		buffer.append(input.toString());
	}

	public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
		List selectlist;
		BaseProperty prop = (BaseProperty) object.safeget(name);
		if ((prop instanceof ListProperty) || (prop instanceof DBStringListProperty)) {
			selectlist = (List) prop.getValue();
			buffer.append(StringUtils.join(selectlist.toArray(), " "));
		} else {
			buffer.append(prop.getValue().toString());
		}
	}

	public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
		if (getDisplayType().equals("input")) {
			input input = new input();
			BaseProperty prop = (BaseProperty) object.safeget(name);
			if (prop != null)
				input.setValue(prop.toFormString());
			input.setType("text");
			input.setSize(60);
			input.setName(prefix + name);
			input.setID(prefix + name);
			buffer.append(input.toString());
		} else if (getDisplayType().equals("radio")) {
			displayRadioEdit(buffer, name, prefix, object, context);
		} else {
			displaySelectEdit(buffer, name, prefix, object, context);
		}
	}

	protected void displayRadioEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
		List list = getList(context);
		List selectlist;

		BaseProperty prop = (BaseProperty) object.safeget(name);
		if (prop == null) {
			selectlist = new ArrayList();
		} else if ((prop instanceof ListProperty) || (prop instanceof DBStringListProperty)) {
			selectlist = (List) prop.getValue();
		} else {
			selectlist = new ArrayList();
			selectlist.add(prop.getValue());
		}

		// Add options from Set
		for (Iterator it = list.iterator(); it.hasNext();) {
			String value = it.next().toString();
			input radio = new input(input.radio, prefix + name, value);

			if (selectlist.contains(value))
				radio.setChecked(true);
			radio.addElement(value);
			buffer.append(radio.toString());
			if (it.hasNext()) {
				buffer.append("<br/>");
			}
		}
	}

	protected void displaySelectEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
		select select = new select(prefix + name, 1);
		select.setMultiple(isMultiSelect());
		select.setSize(getSize());
		select.setName(prefix + name);
		select.setID(prefix + name);

		List list = getList(context);
		List selectlist;

		BaseProperty prop = (BaseProperty) object.safeget(name);
		if (prop == null) {
			selectlist = new ArrayList();
		} else if ((prop instanceof ListProperty) || (prop instanceof DBStringListProperty)) {
			selectlist = (List) prop.getValue();
		} else {
			selectlist = new ArrayList();
			selectlist.add(prop.getValue());
		}

		// Add options from Set
		for (Iterator it = list.iterator(); it.hasNext();) {
			String value = it.next().toString();
			String display = getDisplayValue(context, value);
			option option = new option(value, value);
			option.addElement(display);
			if (selectlist.contains(value))
				option.setSelected(true);
			select.addElement(option);
		}

		buffer.append(select.toString());
	}

	public abstract List getList(XWikiContext context);

	private String getDisplayValue(XWikiContext context, String value) {
		try {
			XWikiMessageTool msg = (XWikiMessageTool) context.get("msg");
			String strname = "option_" + value;
			String result = msg.get(strname);
			if (result.equals(strname)) {
				return value;
			}
			return result;
		} catch (Exception e) {
			return value;
		}
	}

    protected void displayRadioSearch(StringBuffer buffer, String name, String prefix, XWikiCriteria criteria, XWikiContext context){
        List list = getList(context);
        List selectlist = new ArrayList();

        /*
        BaseProperty prop =  (BaseProperty)object.safeget(name);
        if (prop==null) {
            selectlist = new ArrayList();
        } else if ((prop instanceof ListProperty)||(prop instanceof DBStringListProperty)) {
            selectlist = (List) prop.getValue();
        } else {
            selectlist = new ArrayList();
            selectlist.add(prop.getValue());
        }
        */

        // Add options from Set
        for (Iterator it=list.iterator();it.hasNext();) {
            String value = it.next().toString();
            input radio = new input(input.radio, prefix + name, value);

            if (selectlist.contains(value))
                radio.setChecked(true);
            radio.addElement(value);
            buffer.append(radio.toString());
            if(it.hasNext()){
            	buffer.append("<br/>");
            }
        }
    }

    protected void displaySelectSearch(StringBuffer buffer, String name, String prefix, XWikiCriteria criteria, XWikiContext context){
        select select = new select(prefix + name, 1);
        select.setMultiple(true);
        select.setSize(getSize());
        select.setName(prefix + name);
        select.setID(prefix + name);

        List list = getList(context);
        List selectlist = new ArrayList();

        /*
        BaseProperty prop =  (BaseProperty)object.safeget(name);
        if (prop==null) {
            selectlist = new ArrayList();
        } else if ((prop instanceof ListProperty)||(prop instanceof DBStringListProperty)) {
            selectlist = (List) prop.getValue();
        } else {
            selectlist = new ArrayList();
            selectlist.add(prop.getValue());
        }
        */

        // Add options from Set
        for (Iterator it=list.iterator();it.hasNext();) {
            String value = it.next().toString();
            option option = new option(value, value);
            option.addElement(value);
            if (selectlist.contains(value))
                option.setSelected(true);
            select.addElement(option);
        }

        buffer.append(select.toString());
    }
    
    public void makeQuery(Map map, String prefix, XWikiCriteria query, List criteriaList) {
        Object values = map.get(prefix);
        if ((values==null)||(values.equals(""))) {
            return;
        }

        if (values instanceof String) {
            criteriaList.add("jcr:contains(@f:" + getName() + ",'" + values.toString() + "')");
            // testQueryGenerator(query, "//*/*/obj/Test/TestClass[jcr:contains(@f:category, '1') or jcr:contains(@f:category, '2')]");
        }
        else {
            String[] valuesarray = (String[])values;
            String[] criteriaarray = new String[valuesarray.length];
            for (int i=0;i<valuesarray.length;i++) {
                criteriaarray[i] = "jcr:contains(@f:" + getName() + ",'" + valuesarray[i] + "')";
            }
            criteriaList.add("(" + StringUtils.join(criteriaarray, " or ") + ")");
        }
        return;
    }

    public void fromSearchMap(XWikiQuery query, Map map) {
        String[] data  = (String[])map.get("");
        if (data!=null)
            query.setParam(getObject().getName() + "_" + getName(), fromStringArray(data).getValue());
    }
}

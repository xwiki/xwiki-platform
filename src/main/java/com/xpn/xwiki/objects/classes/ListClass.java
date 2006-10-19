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

    public String getSeparators() {
        return null;
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
        return getListFromString(value, "|", true);
    }

    public static List getListFromString(String value, String separators, boolean withMap) {
		List list = new ArrayList();
		if (value == null)
			return list;

        if (separators==null)
         separators = "|";

        String val = value;
        if (separators.length()==1)
          val = StringUtils.replace(val, "\\" + separators, "%PIPE%");

        String[] result = StringUtils.split(val, separators);
		for (int i = 0; i < result.length; i++) {
		    String element = StringUtils.replace(result[i], "%PIPE%", separators);
            if (withMap&&(element.indexOf('=')!=-1)) {
              list.add(StringUtils.split(element,"=")[0]);                
            }
            else
              list.add(element);
        }
        return list;
	}

    public static Map getMapFromString(String value) {
        Map map = new HashMap();
        if (value == null)
            return map;

        String val = StringUtils.replace(value, "\\|", "%PIPE%");
        String[] result = StringUtils.split(val, "|");
        for (int i = 0; i < result.length; i++) {
            String element = StringUtils.replace(result[i], "%PIPE%", "|");
            if (element.indexOf('=')!=-1) {
                String[] data = StringUtils.split(element,"=");
                map.put(data[0], data[1]);
            }
            else
              map.put(element, element);
        }
        return map;
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
			((ListProperty) lprop).setFormStringSeparator("" + getSeparators().charAt(0));
		}

		return lprop;
	}

	public BaseProperty fromString(String value) {
		BaseProperty prop = newProperty();
        if (isMultiSelect()) {
            ((ListProperty) prop).setList(getListFromString(value, getSeparators(), false));
        } else
			prop.setValue(value);
		return prop;
	}

	public BaseProperty fromStringArray(String[] strings) {
        BaseProperty prop = newProperty();
        List list = new ArrayList();
        ((ListProperty) prop).setList(list);
		if (strings.length==0)
         return prop;

        if (!isMultiSelect())
			return fromString(strings[0]);

        if ((strings.length==1)&&getDisplayType().equals("input")) {
            ((ListProperty) prop).setList(getListFromString(strings[0], getSeparators(), false));
            return prop;
        }

        // If Multiselect and multiple results
        for (int i = 0; i < strings.length; i++)
			list.add(strings[i]);
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

    protected String getDisplayValue(String value, Map map, XWikiContext context) {
        String result = (String) map.get(value);
        if (result==null)
         result = value;
        if ((context==null)||(context.getWiki()==null))
         return result;
        else {
            String msgname = getFieldFullName() + "_" + result;
            String newresult = context.getWiki().getMessage(msgname, context);
            if (msgname.equals(newresult))
             return result;
            else
             return newresult;
        }
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
        Map map = getMap(context);
		if ((prop instanceof ListProperty) || (prop instanceof DBStringListProperty)) {
			selectlist = (List) prop.getValue();
            List newlist = new ArrayList();
            for (int i=0; i<selectlist.size();i++) {
                newlist.add(getDisplayValue((String)selectlist.get(i), map, context));
            }
            buffer.append(StringUtils.join(newlist.toArray(), " "));
        } else {
			buffer.append(getDisplayValue((String)prop.getValue(), map, context));
		}
	}

	public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
		if (getDisplayType().equals("input")) {
			input input = new input();
			BaseProperty prop = (BaseProperty) object.safeget(name);
			if (prop != null)
				input.setValue(prop.toFormString());
			input.setType("text");
			input.setSize(getSize());
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
        Map map = getMap(context);
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
			radio.addElement(getDisplayValue(value, map, context));
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
        Map map = getMap(context);
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
			String display = getDisplayValue(value, map, context);
			option option = new option(value, value);
			option.addElement(display);
			if (selectlist.contains(value))
				option.setSelected(true);
			select.addElement(option);
		}

		buffer.append(select.toString());
	}

	public abstract List getList(XWikiContext context);
    public abstract Map getMap(XWikiContext context);

    public String displaySearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context){
        if (getDisplayType().equals("input")) {
            return super.displaySearch(name, prefix, criteria, context);
        } else if (getDisplayType().equals("radio")) {
            return displayRadioSearch(name, prefix, criteria, context);
        } else {
            return displaySelectSearch(name, prefix, criteria, context);
        }
    }

    protected String displayRadioSearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context){
        StringBuffer buffer = new StringBuffer();
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
            radio.addElement(getDisplayValue(value, getMap(context), context));
            buffer.append(radio.toString());
            if(it.hasNext()){
            	buffer.append("<br/>");
            }
        }
        return buffer.toString();
    }

    protected String displaySelectSearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context){
        select select = new select(prefix + name, 1);
        select.setMultiple(true);
        select.setSize(5);
        select.setName(prefix + name);
        select.setID(prefix + name);

        List list = getList(context);
        String fieldFullName = getFieldFullName();
        String[] selectArray = ((String[])criteria.getParameter(fieldFullName));
        List selectlist = (selectArray!=null) ? Arrays.asList(selectArray) : new ArrayList();

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
            option.addElement(getDisplayValue(value, getMap(context), context));
            if (selectlist.contains(value))
                option.setSelected(true);
            select.addElement(option);
        }

        return select.toString();
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
            query.setParam(getObject().getName() + "_" + getName(), data);
    }
}

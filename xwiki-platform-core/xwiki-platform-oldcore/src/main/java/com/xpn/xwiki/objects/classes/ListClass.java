/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.objects.classes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;
import org.dom4j.Element;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.xar.internal.property.ListXarObjectPropertySerializer;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.xml.XMLAttributeValueFilter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public abstract class ListClass extends PropertyClass
{
    private static final String XCLASSNAME = "list";

    protected static final String DISPLAYTYPE_INPUT = "input";

    protected static final String DISPLAYTYPE_RADIO = "radio";

    protected static final String DISPLAYTYPE_CHECKBOX = "checkbox";

    protected static final String DISPLAYTYPE_SELECT = "select";

    public ListClass(String name, String prettyname, PropertyMetaClass wclass)
    {
        super(name, prettyname, wclass);
        setRelationalStorage(false);
        setDisplayType(DISPLAYTYPE_SELECT);
        setMultiSelect(false);
        setSize(1);
        setSeparator(" ");
        setCache(false);
    }

    public ListClass(PropertyMetaClass wclass)
    {
        this(XCLASSNAME, "List", wclass);
    }

    public ListClass()
    {
        this(null);
    }

    public String getSeparators()
    {
        String separators = getStringValue("separators");
        if (separators == null || separators.equals("")) {
            separators = "|,";
        }
        return separators;
    }

    public void setSeparators(String separators)
    {
        setStringValue("separators", separators);
    }

    public String getDisplayType()
    {
        return getStringValue("displayType");
    }

    public void setDisplayType(String type)
    {
        setStringValue("displayType", type);
    }

    public String getSort()
    {
        return getStringValue("sort");
    }

    public void setSort(String sort)
    {
        setStringValue("sort", sort);
    }

    public int getSize()
    {
        return getIntValue("size");
    }

    public void setSize(int size)
    {
        setIntValue("size", size);
    }

    public boolean isCache()
    {
        return (getIntValue("cache") == 1);
    }

    public void setCache(boolean cache)
    {
        setIntValue("cache", cache ? 1 : 0);
    }

    public boolean isMultiSelect()
    {
        return (getIntValue("multiSelect") == 1);
    }

    public void setMultiSelect(boolean multiSelect)
    {
        setIntValue("multiSelect", multiSelect ? 1 : 0);
    }

    public boolean isRelationalStorage()
    {
        return (getIntValue("relationalStorage") == 1);
    }

    public void setRelationalStorage(boolean storage)
    {
        setIntValue("relationalStorage", storage ? 1 : 0);
    }

    public boolean isPicker()
    {
        return (getIntValue("picker") == 1);
    }

    public void setPicker(boolean picker)
    {
        setIntValue("picker", picker ? 1 : 0);
    }

    public String getSeparator()
    {
        return getStringValue("separator");
    }

    public void setSeparator(String separator)
    {
        setStringValue("separator", separator);
    }

    public static List<String> getListFromString(String value)
    {
        return getListFromString(value, "|", true);
    }

    public static List<String> getListFromString(String value, String separators, boolean withMap)
    {
        List<String> list = new ArrayList<String>();
        if (value == null) {
            return list;
        }
        if (separators == null) {
            separators = "|";
        }

        String val = value;
        if (separators.length() == 1) {
            val = StringUtils.replace(val, "\\" + separators, "%PIPE%");
        }

        String[] result = StringUtils.split(val, separators);
        String item = "";
        for (int i = 0; i < result.length; i++) {
            String element = StringUtils.replace(result[i], "%PIPE%", separators);
            if (withMap && (element.indexOf('=') != -1)) {
                item = StringUtils.split(element, "=")[0];
            } else {
                item = element;
            }
            if (!item.trim().equals("")) {
                list.add(item);
            }
        }
        return list;
    }

    public static Map<String, ListItem> getMapFromString(String value)
    {
        Map<String, ListItem> map = new HashMap<String, ListItem>();
        if (value == null) {
            return map;
        }

        String val = StringUtils.replace(value, "\\|", "%PIPE%");
        String[] result = StringUtils.split(val, "|");
        for (int i = 0; i < result.length; i++) {
            String element = StringUtils.replace(result[i], "%PIPE%", "|");
            if (element.indexOf('=') != -1) {
                String[] data = StringUtils.split(element, "=");
                map.put(data[0], new ListItem(data[0], data[1]));
            } else {
                map.put(element, new ListItem(element, element));
            }
        }
        return map;
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty lprop;

        if (isRelationalStorage() && isMultiSelect()) {
            lprop = new DBStringListProperty();
        } else if (isMultiSelect()) {
            lprop = new StringListProperty();
        } else {
            lprop = new StringProperty();
        }

        return lprop;
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty prop = newProperty();
        if (isMultiSelect()) {
            ((ListProperty) prop).setList(getListFromString(value, getSeparators(), false));
        } else {
            prop.setValue(value);
        }
        return prop;
    }

    @Override
    public BaseProperty fromStringArray(String[] strings)
    {
        if (!isMultiSelect()) {
            return fromString(strings[0]);
        }
        BaseProperty prop = newProperty();
        if (prop instanceof StringProperty) {
            return fromString(strings[0]);
        }

        List<String> list = new ArrayList<String>();

        if (strings.length == 0) {
            return prop;
        }

        if ((strings.length == 1) && (getDisplayType().equals(DISPLAYTYPE_INPUT) || isMultiSelect())) {
            ((ListProperty) prop).setList(getListFromString(strings[0], getSeparators(), false));
            return prop;
        }

        // If Multiselect and multiple results
        for (int i = 0; i < strings.length; i++) {
            String item = strings[i];
            if (!item.trim().equals("")) {
                list.add(item);
            }
        }

        // setList will copy the list, so this call must be made last.
        ((ListProperty) prop).setList(list);

        return prop;
    }

    @Override
    public BaseProperty newPropertyfromXML(Element ppcel)
    {
        if (!isMultiSelect()) {
            return super.newPropertyfromXML(ppcel);
        }

        @SuppressWarnings("unchecked")
        List<Element> elist = ppcel.elements(ListXarObjectPropertySerializer.ELEMENT_VALUE);
        BaseProperty lprop = newProperty();

        if (lprop instanceof ListProperty) {
            List<String> llist = ((ListProperty) lprop).getList();
            for (int i = 0; i < elist.size(); i++) {
                Element el = elist.get(i);
                llist.add(el.getText());
            }
        } else {
            for (int i = 0; i < elist.size(); i++) {
                Element el = elist.get(i);
                ((StringProperty) lprop).setValue(el.getText());
            }
        }
        return lprop;
    }

    /**
     * Search for an internationalizable display text for the current value. The search process is:
     * <ol>
     * <li>let V = the internal value of the option, used as the "value" attribute of the <option> element, and D = the
     * displayed value</li>
     * <li>if a message with the key <fieldFullName>_<V> exists, return it as D</li>
     * <li>else, if a message with the key option_<fieldName>_<V> exists, return it as D</li>
     * <li>else, if a message with the key option_<V> exists, return it as D</li>
     * <li>else, D can be specified in the values parameter of the property by using V=D</li>
     * <li>else return V</li>
     * </ol>
     * 
     * @param value The internal value.
     * @param name The name of the ListProperty.
     * @param map The value=name mapping specified in the "values" parameter of the property.
     * @param context The request context.
     * @return The text that should be displayed, representing a human-understandable name for the internal value.
     */
    protected String getDisplayValue(String value, String name, Map<String, ListItem> map, XWikiContext context)
    {
        ListItem item = map.get(value);
        String displayValue;
        if (item == null) {
            displayValue = value;
        } else {
            displayValue = item.getValue();
        }
        if ((context == null) || (context.getWiki() == null)) {
            return displayValue;
        }
        String msgname = getFieldFullName() + "_" + value;
        String newresult = context.getMessageTool().get(msgname);
        if (msgname.equals(newresult)) {
            msgname = "option_" + name + "_" + value;
            newresult = context.getMessageTool().get(msgname);
            if (msgname.equals(newresult)) {
                msgname = "option_" + value;
                newresult = context.getMessageTool().get(msgname);
                if (msgname.equals(newresult)) {
                    newresult = displayValue;
                }
            }
        }
        return newresult;
    }

    /**
     * Search for an internationalizable display text for the current value. The value can be either a simple string, or
     * a value=name pair selected from the database.
     * 
     * @see #getDisplayValue(String, String, Map, XWikiContext)
     * @param rawvalue The internal value, or a value=name pair.
     * @param name The name of the ListProperty.
     * @param map The value=name mapping specified in the "values" parameter of the property.
     * @param context The request context.
     * @return The text that should be displayed, representing a human-understandable name for the internal value.
     */
    protected String getDisplayValue(Object rawvalue, String name, Map<String, ListItem> map, XWikiContext context)
    {
        if (rawvalue == null) {
            return "";
        }
        if (rawvalue instanceof Object[]) {
            return ((Object[]) rawvalue)[1].toString();
        }
        return getDisplayValue(rawvalue.toString(), name, map, context);
    }

    /**
     * If the list is populated with value=name pairs selected from the database, then return only the value. Otherwise,
     * it is a simple value.
     * 
     * @param rawvalue
     * @return The list value
     */
    protected String getElementValue(Object rawvalue)
    {
        if (rawvalue == null) {
            return "";
        }
        if (rawvalue instanceof Object[]) {
            return ((Object[]) rawvalue)[0].toString();
        }
        return rawvalue.toString();
    }

    @Override
    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        input input = new input();
        input.setAttributeFilter(new XMLAttributeValueFilter());
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toText());
        }

        input.setType("hidden");
        input.setName(prefix + name);
        input.setID(prefix + name);
        buffer.append(input.toString());
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        List<String> selectlist;
        String separator = getSeparator();
        BaseProperty prop = (BaseProperty) object.safeget(name);
        Map<String, ListItem> map = getMap(context);

        // Skip unset values.
        if (prop == null) {
            return;
        }

        if (prop instanceof ListProperty) {
            selectlist = ((ListProperty) prop).getList();
            List<String> newlist = new ArrayList<String>();
            for (String value : selectlist) {
                newlist.add(getDisplayValue(value, name, map, context));
            }
            buffer.append(StringUtils.join(newlist, separator));
        } else {
            buffer.append(getDisplayValue(prop.getValue(), name, map, context));
        }
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        if (getDisplayType().equals(DISPLAYTYPE_INPUT)) {
            input input = new input();
            input.setAttributeFilter(new XMLAttributeValueFilter());
            BaseProperty prop = (BaseProperty) object.safeget(name);
            if (prop != null) {
                input.setValue(prop.toText());
            }
            input.setType("text");
            input.setSize(getSize());
            input.setName(prefix + name);
            input.setID(prefix + name);
            input.setDisabled(isDisabled());
            buffer.append(input.toString());
        } else if (getDisplayType().equals(DISPLAYTYPE_RADIO) || getDisplayType().equals(DISPLAYTYPE_CHECKBOX)) {
            displayRadioEdit(buffer, name, prefix, object, context);
        } else {
            displaySelectEdit(buffer, name, prefix, object, context);
        }

        if (!getDisplayType().equals(DISPLAYTYPE_INPUT)) {
            org.apache.ecs.xhtml.input hidden = new input(input.hidden, prefix + name, "");
            hidden.setAttributeFilter(new XMLAttributeValueFilter());
            buffer.append(hidden);
        }
    }

    protected void displayRadioEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        List<String> list = getList(context);
        Map<String, ListItem> map = getMap(context);

        BaseProperty prop = (BaseProperty) object.safeget(name);
        List<String> selectlist = toList(prop);

        // Add the selected values that are not in the predefined list.
        for (String item : selectlist) {
            // The empty value means no selection when it's not in the predefined list. Both the radio and the checkbox
            // input support empty selection (unlike the select input which automatically selects the first value when
            // single selection is on) so we don't have to generate a radio/checkbox for the empty value.
            if (!StringUtils.isEmpty(item) && !list.contains(item)) {
                list.add(item);
            }
        }

        // Add options from Set
        int count = 0;
        for (Object rawvalue : list) {
            String value = getElementValue(rawvalue);
            String display = XMLUtils.escape(getDisplayValue(rawvalue, name, map, context));
            input radio =
                new input((getDisplayType().equals(DISPLAYTYPE_RADIO) && !isMultiSelect()) ? input.radio
                    : input.checkbox, prefix + name, value);
            radio.setAttributeFilter(new XMLAttributeValueFilter());
            radio.setID("xwiki-form-" + name + "-" + object.getNumber() + "-" + count);
            radio.setDisabled(isDisabled());

            if (selectlist.contains(value)) {
                radio.setChecked(true);
            }
            radio.addElement(display);

            buffer.append("<label class=\"xwiki-form-listclass\" for=\"xwiki-form-" + XMLUtils.escape(name) + "-"
                + object.getNumber() + "-" + count++ + "\">");
            buffer.append(radio.toString());
            buffer.append("</label>");
        }

        // We need a hidden input with an empty value to be able to clear the selected values when no value is selected
        // from the above radio/checkbox buttons.
        org.apache.ecs.xhtml.input hidden = new input(input.hidden, prefix + name, "");
        hidden.setAttributeFilter(new XMLAttributeValueFilter());
        hidden.setDisabled(isDisabled());
        buffer.append(hidden);
    }

    protected class MapComparator implements Comparator<String>
    {
        protected Map<String, ListItem> map;

        public MapComparator(Map<String, ListItem> map)
        {
            this.map = map;
        }

        @Override
        public int compare(String o1, String o2)
        {
            ListItem s1 = this.map.get(o1);
            ListItem s2 = this.map.get(o2);

            if ((s1 == null) && (s2 == null)) {
                return 0;
            }

            if (s1 == null) {
                return -1;
            }

            if (s2 == null) {
                return 1;
            }

            return s1.getValue().compareTo(s2.getValue());
        }
    }

    protected void displaySelectEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        select select = new select(prefix + name, 1);
        select.setAttributeFilter(new XMLAttributeValueFilter());
        select.setMultiple(isMultiSelect());
        select.setSize(getSize());
        select.setName(prefix + name);
        select.setID(prefix + name);
        select.setDisabled(isDisabled());

        List<String> list = getList(context);
        Map<String, ListItem> map = getMap(context);

        String sort = getSort();
        if (!"none".equals(sort)) {
            if ("id".equals(sort)) {
                Collections.sort(list);
            }
            if ("value".equals(sort)) {
                Collections.sort(list, new MapComparator(map));
            }
        }

        List<String> selectlist = toList((BaseProperty) object.safeget(name));

        // Add the selected values that are not in the predefined list.
        for (String item : selectlist) {
            if (!list.contains(item)) {
                list.add(item);
            }
        }

        // Add options from Set
        for (String rawvalue : list) {
            String value = getElementValue(rawvalue);
            String display = getDisplayValue(rawvalue, name, map, context);
            option option = new option(display, value);
            option.setAttributeFilter(new XMLAttributeValueFilter());
            option.addElement(XMLUtils.escape(display));
            if (selectlist.contains(value)) {
                option.setSelected(true);
            }
            select.addElement(option);
        }

        buffer.append(select.toString());
    }

    public abstract List<String> getList(XWikiContext context);

    public abstract Map<String, ListItem> getMap(XWikiContext context);

    /**
     * {@link ListClass} does not produce only {@link ListProperty}s and this method allows to access the value as
     * {@link List} whatever property is actually storing it.
     * <p>
     * There is no guarantees the returned {@link List} will be modifiable.
     * 
     * @param property the property created by this class
     * @return the {@link List} representation of this property
     * @since 6.2M1
     */
    public List<String> toList(BaseProperty< ? > property)
    {
        List<String> list;

        if (property == null) {
            list = Collections.emptyList();
        } else if (property instanceof ListProperty) {
            list = ((ListProperty) property).getList();
        } else {
            list = Arrays.asList(String.valueOf(property.getValue()));
        }

        return list;
    }

    /**
     * Set the passed {@link List} into the passed property.
     * 
     * @param property the property to modify
     * @param list the list to set
     * @since 6.2M1
     */
    public void fromList(BaseProperty< ? > property, List<String> list)
    {
        if (property instanceof ListProperty) {
            ((ListProperty) property).setList(list);
        } else {
            property.setValue(list == null || list.isEmpty() ? null : list.get(0));
        }
    }

    @Override
    public <T extends EntityReference> void mergeProperty(BaseProperty<T> currentProperty,
        BaseProperty<T> previousProperty, BaseProperty<T> newProperty, MergeConfiguration configuration,
        XWikiContext context, MergeResult mergeResult)
    {
        // If it's not a multiselect then we don't have any special merge to do. We keep default StringProperty behavior
        if (isMultiSelect()) {
            // If not a free input assume it's not an ordered list
            if (!DISPLAYTYPE_INPUT.equals(getDisplayType()) && currentProperty instanceof ListProperty) {
                mergeNotOrderedListProperty(currentProperty, previousProperty, newProperty, configuration, context,
                    mergeResult);

                return;
            }
        }

        // Fallback on default ListProperty merging
        super.mergeProperty(currentProperty, previousProperty, newProperty, configuration, context, mergeResult);
    }

    protected <T extends EntityReference> void mergeNotOrderedListProperty(BaseProperty<T> currentProperty,
        BaseProperty<T> previousProperty, BaseProperty<T> newProperty, MergeConfiguration configuration,
        XWikiContext context, MergeResult mergeResult)
    {
        List<String> currentList = new LinkedList<String>(toList(currentProperty));
        List<String> previousList = toList(previousProperty);
        List<String> newList = toList(newProperty);

        // Remove elements to remove
        if (previousList != null) {
            for (String element : previousList) {
                if (newList == null || !newList.contains(element)) {
                    currentList.remove(element);
                    mergeResult.setModified(true);
                }
            }
        }

        // Add missing elements
        if (newList != null) {
            for (String element : newList) {
                if ((previousList == null || !previousList.contains(element))) {
                    if (!currentList.contains(element)) {
                        currentList.add(element);
                        mergeResult.setModified(true);
                    }
                }
            }
        }

        fromList(currentProperty, currentList);

        return;
    }
}

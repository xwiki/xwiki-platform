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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;
import org.dom4j.Element;
import org.hibernate.collection.PersistentCollection;
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
    /**
     * @since 6.2RC1
     */
    public static final String DISPLAYTYPE_INPUT = "input";

    /**
     * @since 6.2RC1
     */
    public static final String DISPLAYTYPE_RADIO = "radio";

    /**
     * @since 6.2RC1
     */
    public static final String DISPLAYTYPE_CHECKBOX = "checkbox";

    /**
     * @since 6.2RC1
     */
    public static final String DISPLAYTYPE_SELECT = "select";

    /**
     * Default separator/delimiter to use to split or join a list stored as a string. Not to be confused with
     * {@link #getSeparator()} and {@link #getSeparator()} which are used only for UI view and edit operations.
     *
     * @since 7.0M2
     */
    public static final String DEFAULT_SEPARATOR = "|";

    /**
     * Used to escape a separator character inside a string serialized list item.
     *
     * @since 7.0M2
     */
    public static final char SEPARATOR_ESCAPE = '\\';

    /**
     * @since 10.11RC1
     */
    public static final String FREE_TEXT_DISCOURAGED = "discouraged";

    /**
     * @since 10.11RC1
     */
    public static final String FREE_TEXT_FORBIDDEN = "forbidden";

    /**
     * @since 10.11RC1
     */
    public static final String FREE_TEXT_ALLOWED = "allowed";

    private static final String XCLASSNAME = "list";

    /**
     * Regex used to split lists stored in a string. Supports escaped separators inside values. The individually
     * regex-escaped separators string needs to be passed as parameter.
     */
    private static final String LIST_ITEM_SEPARATOR_REGEX_FORMAT = "(?<!\\\\)[%s]";

    /**
     * Regex used to unescape separators inside the actual values of the list. The individually regex-escaped separators
     * string needs to be passed as parameter.
     */
    private static final String ESCAPED_SEPARATORS_REGEX_FORMAT = "\\%s([%s])";

    /**
     * Regex used to find unescaped separators in a list item's value. Regex-escaped separators string needs to be
     * passed as parameter.
     */
    private static final String UNESCAPED_SEPARATORS_REGEX_FORMAT = "([%s])";

    /**
     * Replacement string used to escaped a separator found by the String.replace regex.
     */
    private static final String UNESCAPED_SEPARATOR_REPLACEMENT = String.format("\\%s$1", SEPARATOR_ESCAPE);

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

    /**
     * @return a string of separator characters used to split/deserialize an input string coming from the UI (filled by
     *         the user) that represents a serialized list
     * @see #displayEdit(StringBuffer, String, String, BaseCollection, XWikiContext)
     * @see #fromString(String)
     */
    public String getSeparators()
    {
        String separators = getStringValue("separators");
        if (separators == null || separators.equals("")) {
            separators = "|,";
        }
        return separators;
    }

    /**
     * @param separators a string of characters used to split/deserialize an input string coming from the UI (filled by
     *            the user) that represents a serialized list
     */
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

    /**
     * @param relationalStorage if false, the list items will be concatenated into a VARCHAR column on a single row.
     *            Otherwise, items are stored in their own entries in the database. In most cases, this property should
     *            have the same value as the {@code multiSelect} property
     */
    public void setRelationalStorage(boolean relationalStorage)
    {
        setIntValue("relationalStorage", relationalStorage ? 1 : 0);
    }

    public boolean isPicker()
    {
        return (getIntValue("picker") == 1);
    }

    public void setPicker(boolean picker)
    {
        setIntValue("picker", picker ? 1 : 0);
    }

    /**
     * @return a string (usually just 1 character long) used to join this list's items when displaying it in the UI in
     *         view mode.
     * @see #displayView(StringBuffer, String, String, BaseCollection, XWikiContext)
     */
    public String getSeparator()
    {
        return getStringValue("separator");
    }

    /**
     * @param separator a string (usually just 1 character long) used to join this list's items when displaying it in
     *            the UI in view mode.
     */
    public void setSeparator(String separator)
    {
        setStringValue("separator", separator);
    }

    /**
     * @return the default value used in the select editor
     * @since 10.9
     * @since 10.8.1
     */
    public String getDefaultValue()
    {
        return getStringValue("defaultValue");
    }

    /**
     * @param separator the default value used in the select editor
     * @since 10.9
     * @since 10.8.1
     */
    public void setDefaultValue(String separator)
    {
        setStringValue("defaultValue", separator);
    }


    /**
     * @return the value of freeText (forbidden, discouraged or allowed)
     * @since 10.11RC1
     */
    public String getFreeText()
    {
        return getStringValue("freeText");
    }

    /**
     * @param type the value of freeText (forbidden, discouraged or allowed)
     * @since 10.11RC1
     */
    public void setFreeText(String type)
    {
        setStringValue("freeText", type);
    }

    /**
     * Convenience method, using {@value #DEFAULT_SEPARATOR} as separator and parsing key=value items.
     *
     * @param value the string holding a serialized list
     * @return the list that was stored in the input string
     * @see #getListFromString(String, String, boolean)
     */
    public static List<String> getListFromString(String value)
    {
        return getListFromString(value, null, true);
    }

    /**
     * @param value the string holding a serialized list
     * @param separators the separator characters (given as a string) used to delimit the list's items inside the input
     *            string. These separators can also be present, in escaped ({@value #SEPARATOR_ESCAPE}) form, inside
     *            list items
     * @param withMap set to true if the list's values contain map entries (key=value pairs) that should also be parsed.
     *            Only the keys are extracted from such list items
     * @return the list that was stored in the input string
     */
    public static List<String> getListFromString(String value, String separators, boolean withMap)
    {
        List<String> list = new ArrayList<>();
        if (value == null) {
            return list;
        }
        if (separators == null) {
            separators = DEFAULT_SEPARATOR;
        }

        // Escape the list of separators individually to be safely used in regexes.
        String regexEscapedSeparatorsRegexPart =
            SEPARATOR_ESCAPE + StringUtils.join(separators.toCharArray(), SEPARATOR_ESCAPE);

        String escapedSeparatorsRegex =
            String.format(ESCAPED_SEPARATORS_REGEX_FORMAT, SEPARATOR_ESCAPE, regexEscapedSeparatorsRegexPart);

        // Split the values and process each list item.
        String listItemSeparatorRegex =
            String.format(LIST_ITEM_SEPARATOR_REGEX_FORMAT, regexEscapedSeparatorsRegexPart);
        String[] elements = value.split(listItemSeparatorRegex);
        for (String element : elements) {
            // Adjacent separators are treated as one separator.
            if (StringUtils.isBlank(element)) {
                continue;
            }

            // Unescape any escaped separator in the individual list item.
            String unescapedElement = element.replaceAll(escapedSeparatorsRegex, "$1");
            String item = unescapedElement;

            // Check if it is a map entry, i.e. "key=value"
            if (withMap && (unescapedElement.indexOf('=') != -1)) {
                // Get just the key, ignore the value/label.
                item = StringUtils.split(unescapedElement, '=')[0];
            }

            // Ignore empty items.
            if (StringUtils.isNotBlank(item.trim())) {
                list.add(item);
            }
        }

        return list;
    }

    /**
     * Convenience method, using {@value #DEFAULT_SEPARATOR} as separator.
     *
     * @param list the list to serialize
     * @return a string representing a serialized list, delimited by the first separator character (from the ones inside
     *         the separators string). Separators inside list items are safely escaped ({@value #SEPARATOR_ESCAPE}).
     * @see #getStringFromList(List, String)
     */
    public static String getStringFromList(List<String> list)
    {
        return getStringFromList(list, null);
    }

    /**
     * @param list the list to serialize
     * @param separators the separator characters (given as a string) used when the list was populated with values. The
     *            list's items can contain these separators in plain/unescaped form. The first separator character will
     *            be used to join the list in the output.
     * @return a string representing a serialized list, delimited by the first separator character (from the ones inside
     *         the separators string). Separators inside list items are safely escaped ({@value #SEPARATOR_ESCAPE}).
     */
    public static String getStringFromList(List<String> list, String separators)
    {
        if ((list instanceof PersistentCollection) && (!((PersistentCollection) list).wasInitialized())) {
            return "";
        }

        if (separators == null) {
            separators = DEFAULT_SEPARATOR;
        }

        // Escape the list of separators individually to be safely used in regexes.
        String regexEscapedSeparatorsRegexPart =
            SEPARATOR_ESCAPE + StringUtils.join(separators.toCharArray(), SEPARATOR_ESCAPE);

        String unescapedSeparatorsRegex =
            String.format(UNESCAPED_SEPARATORS_REGEX_FORMAT, regexEscapedSeparatorsRegexPart);

        List<String> escapedValues = new ArrayList<>();
        for (String value : list) {
            String escapedValue = value.replaceAll(unescapedSeparatorsRegex, UNESCAPED_SEPARATOR_REPLACEMENT);
            escapedValues.add(escapedValue);
        }

        // Use the first separator to join the list.
        return StringUtils.join(escapedValues, separators.charAt(0));
    }

    public static Map<String, ListItem> getMapFromString(String value)
    {
        Map<String, ListItem> map = new LinkedHashMap<>();
        if (value == null) {
            return map;
        }

        String val = StringUtils.replace(value, SEPARATOR_ESCAPE + DEFAULT_SEPARATOR, "%PIPE%");
        String[] result = StringUtils.split(val, "|");
        for (String element2 : result) {
            String element = StringUtils.replace(element2, "%PIPE%", DEFAULT_SEPARATOR);
            if (element.indexOf('=') != -1) {
                String[] data = StringUtils.split(element, "=", 2);
                map.put(data[0], new ListItem(data[0], data[1]));
            } else {
                map.put(element, new ListItem(element, element));
            }
        }
        return map;
    }

    /**
     * Used in {@link #displayEdit(StringBuffer, String, String, BaseCollection, XWikiContext)}.
     *
     * @param property a property to be used in an form input
     * @return the text value to be used in an form input. If a {@link ListProperty} is passed, the list's separators
     *         defined by {@link #getSeparators()} are escaped for each list item and the items are joined by the first
     *         separator
     * @see #getStringFromList(List, String)
     */
    public String toFormString(BaseProperty property)
    {
        String result;
        if (property instanceof ListProperty) {
            ListProperty listProperty = (ListProperty) property;
            result = ListClass.getStringFromList(listProperty.getList(), getSeparators());
        } else {
            result = property.toText();
        }
        return result;
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

        lprop.setName(getName());
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

        List<String> list = new ArrayList<>();

        if (strings.length == 0) {
            return prop;
        }

        if ((strings.length == 1) && (getDisplayType().equals(DISPLAYTYPE_INPUT) || isMultiSelect())) {
            ((ListProperty) prop).setList(getListFromString(strings[0], getSeparators(), false));
            return prop;
        }

        // If Multiselect and multiple results
        for (String item : strings) {
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
     * <li>let V = the internal value of the option, used as the "value" attribute of the {@code <option>} element, and
     * D = the displayed value</li>
     * <li>if a message with the key {@code <fieldFullName>_<V>} exists, return it as D</li>
     * <li>else, if a message with the key {@code option_<fieldName>_<V>} exists, return it as D</li>
     * <li>else, if a message with the key {@code option_<V>} exists, return it as D</li>
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
        return getDisplayValue(value, name, map, value, context);
    }

    private String getDisplayValue(String value, String name, Map<String, ListItem> map, String def,
        XWikiContext context)
    {
        ListItem item = map.get(value);
        String displayValue;
        if (item == null) {
            displayValue = def;
        } else {
            displayValue = item.getValue();
        }
        if ((context == null) || (context.getWiki() == null)) {
            return displayValue;
        }
        String msgname = getFieldFullName() + "_" + value;
        String newresult = localizePlain(msgname);
        if (newresult == null) {
            msgname = "option_" + name + "_" + value;
            newresult = localizePlain(msgname);
            if (newresult == null) {
                msgname = "option_" + value;
                newresult = localizePlain(msgname);
                if (newresult == null) {
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
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
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
            List<String> newlist = new ArrayList<>();
            for (String value : selectlist) {
                newlist.add(getDisplayValue(value, name, map, context));
            }
            buffer.append(StringUtils.join(newlist, separator));
        } else {
            buffer.append(getDisplayValue(prop.getValue(), name, map, context));
        }
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        if (getDisplayType().equals(DISPLAYTYPE_INPUT)) {
            input input = new input();
            input.setAttributeFilter(new XMLAttributeValueFilter());
            BaseProperty prop = (BaseProperty) object.safeget(name);
            if (prop != null) {
                input.setValue(this.toFormString(prop));
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
            input radio = new input(
                (getDisplayType().equals(DISPLAYTYPE_RADIO) && !isMultiSelect()) ? input.radio : input.checkbox,
                prefix + name, value);
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

        String defaultValue = getDefaultValue();

        // Add the selected values that are not in the predefined list.
        for (String item : selectlist) {
            if (!StringUtils.isEmpty(item) && !defaultValue.equals(item) && !list.contains(item)) {
                list.add(item);
            }
        }

        // Add default if not already part of the list
        if (!isMultiSelect() && !list.contains(defaultValue)) {
            String display =
                getDisplayValue(defaultValue, name, map, defaultValue.isEmpty() ? "---" : defaultValue, context);

            select.addElement(createOption("", display, selectlist));
        }

        // Add options from Set
        for (String rawvalue : list) {
            String value = getElementValue(rawvalue);
            String display = getDisplayValue(rawvalue, name, map, context);

            select.addElement(createOption(value, display, selectlist));
        }

        buffer.append(select.toString());
    }

    private option createOption(String value, String display, List<String> selectlist)
    {
        option option = new option(display, value);
        option.setAttributeFilter(new XMLAttributeValueFilter());
        option.addElement(XMLUtils.escape(display));
        if (selectlist.contains(value)) {
            option.setSelected(true);
        }

        return option;
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
    public List<String> toList(BaseProperty<?> property)
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
    public void fromList(BaseProperty<?> property, List<String> list)
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
        List<String> currentList = new LinkedList<>(toList(currentProperty));
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

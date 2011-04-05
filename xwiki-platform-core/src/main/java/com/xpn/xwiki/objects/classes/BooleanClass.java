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
 *
 */
package com.xpn.xwiki.objects.classes;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.ecs.xhtml.div;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.label;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.web.XWikiMessageTool;

public class BooleanClass extends PropertyClass
{
    public BooleanClass(PropertyMetaClass wclass)
    {
        super("boolean", "Boolean", wclass);
    }

    public BooleanClass()
    {
        this(null);
        setDisplayFormType("select");
    }

    public void setDisplayType(String type)
    {
        setStringValue("displayType", type);
    }

    public String getDisplayType()
    {
        String dtype = getStringValue("displayType");
        if ((dtype == null) || (dtype.equals(""))) {
            return "yesno";
        }
        return dtype;
    }

    public String getDisplayFormType()
    {
        String dtype = getStringValue("displayFormType");
        if ((dtype == null) || (dtype.equals(""))) {
            return "radio";
        }
        return dtype;
    }

    public void setDisplayFormType(String type)
    {
        setStringValue("displayFormType", type);
    }

    public void setDefaultValue(int dvalue)
    {
        setIntValue("defaultValue", dvalue);
    }

    public int getDefaultValue()
    {
        return getIntValue("defaultValue", -1);
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty property = newProperty();
        Number nvalue = null;
        if (StringUtils.isNotEmpty(value)) {
            nvalue = new Integer(value);
        }
        property.setValue(nvalue);
        return property;
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new IntegerProperty();
        property.setName(getName());
        return property;
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        IntegerProperty prop = (IntegerProperty) object.safeget(name);
        if (prop == null) {
            return;
        }

        Integer iValue = (Integer) prop.getValue();
        if (iValue != null) {
            int value = iValue.intValue();
            buffer.append(getDisplayValue(context, value));
        }
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        String displayFormType = getDisplayFormType();

        if (getDisplayType().equals("checkbox")) {
            displayFormType = "checkbox";
        }

        if (displayFormType.equals("checkbox")) {
            displayCheckboxEdit(buffer, name, prefix, object, context);
        } else if (displayFormType.equals("select")) {
            displaySelectEdit(buffer, name, prefix, object, context);
        } else {
            displayRadioEdit(buffer, name, prefix, object, context);
        }
    }

    public void displaySelectEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        select select = new select(prefix + name, 1);
        select.setName(prefix + name);
        select.setID(prefix + name);
        select.setDisabled(isDisabled());

        String String0 = getDisplayValue(context, 0);
        String String1 = getDisplayValue(context, 1);
        int nb1 = 1;
        int nb2 = 2;

        option[] options;

        if (getDefaultValue() == -1) {
            options = new option[] {new option("---", ""), new option(String1, "1"), new option(String0, "0")};
            options[0].addElement("---");
            options[1].addElement(String1);
            options[2].addElement(String0);
        } else {
            options = new option[] {new option(String1, "1"), new option(String0, "0")};
            options[0].addElement(String1);
            options[1].addElement(String0);
            nb1 = 0;
            nb2 = 1;
        }

        try {
            IntegerProperty prop = (IntegerProperty) object.safeget(name);
            Integer ivalue = (prop == null) ? null : (Integer) prop.getValue();
            if (ivalue != null) {
                int value = ivalue.intValue();
                if (value == 1) {
                    options[nb1].setSelected(true);
                } else if (value == 0) {
                    options[nb2].setSelected(true);
                }
            } else {
                int value = getDefaultValue();
                if (value == 1) {
                    options[nb1].setSelected(true);
                } else if (value == 0) {
                    options[nb2].setSelected(true);
                } else if (value == -1) {
                    options[0].setSelected(true);
                }
            }
        } catch (Exception e) {
            // This should not happen
            e.printStackTrace();
        }
        select.addElement(options);
        buffer.append(select.toString());
    }

    public void displayRadioEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        String StringNone = getDisplayValue(context, 2);
        String StringTrue = getDisplayValue(context, 1);
        String StringFalse = getDisplayValue(context, 0);
        div[] inputs;

        input radioNone = new input(input.radio, prefix + name, "");
        input radioTrue = new input(input.radio, prefix + name, "1");
        input radioFalse = new input(input.radio, prefix + name, "0");
        radioNone.setDisabled(isDisabled());
        radioTrue.setDisabled(isDisabled());
        radioFalse.setDisabled(isDisabled());
        label labelNone = new label();
        label labelTrue = new label();
        label labelFalse = new label();
        div divNone = new div();
        div divTrue = new div();
        div divFalse = new div();
        labelNone.addElement(radioNone);
        labelNone.addElement(StringNone);
        divNone.addElement(labelNone);
        labelTrue.addElement(radioTrue);
        labelTrue.addElement(StringTrue);
        divTrue.addElement(labelTrue);
        labelFalse.addElement(radioFalse);
        labelFalse.addElement(StringFalse);
        divFalse.addElement(labelFalse);

        radioNone.setID(prefix + name + "_none");
        labelNone.setFor(prefix + name + "_none");
        
        radioTrue.setID(prefix + name);
        labelTrue.setFor(prefix + name);
        
        radioFalse.setID(prefix + name + "_false");
        labelFalse.setFor(prefix + name + "_false");

        if (getDefaultValue() == -1) {
            inputs = new div[] {divNone, divTrue, divFalse};
        } else {
            inputs = new div[] {divTrue, divFalse};
        }

        try {
            IntegerProperty prop = (IntegerProperty) object.safeget(name);
            Integer ivalue = (prop == null) ? null : (Integer) prop.getValue();
            if (ivalue != null) {
                int value = ivalue.intValue();
                if (value == 1) {
                    radioTrue.setChecked(true);
                } else if (value == 0) {
                    radioFalse.setChecked(true);
                }
            } else {
                int value = getDefaultValue();
                if (value == 1) {
                    radioTrue.setChecked(true);
                } else if (value == 0) {
                    radioFalse.setChecked(true);
                } else if (value == -1) {
                    radioNone.setChecked(true);
                }
            }
        } catch (Exception e) {
            // This should not happen
            e.printStackTrace();
        }

        for (int i = 0; i < inputs.length; i++) {
            buffer.append(inputs[i].toString());
        }
    }

    public void displayCheckboxEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        org.apache.ecs.xhtml.input check = new input(input.checkbox, prefix + name, 1);
        check.setID(prefix + name);
        check.setDisabled(isDisabled());
        // If the (visible) checkbox is unchecked, it will not post anything back so the hidden input by the same
        // name will post back 0 and the save function will save the first entry it finds.
        org.apache.ecs.xhtml.input checkNo = new input(input.hidden, prefix + name, 0);

        try {
            IntegerProperty prop = (IntegerProperty) object.safeget(name);
            if (prop != null) {
                Integer ivalue = (Integer) prop.getValue();
                if (ivalue != null) {
                    int value = ivalue.intValue();
                    if (value == 1) {
                        check.setChecked(true);
                    } else if (value == 0) {
                        check.setChecked(false);
                    }
                } else {
                    int value = getDefaultValue();
                    if (value == 1) {
                        check.setChecked(true);
                    } else {
                        check.setChecked(false);
                    }
                }
            }
        } catch (Exception e) {
            // This should not happen
            e.printStackTrace();
        }
        buffer.append(check.toString());
        buffer.append(checkNo.toString());
    }

    /**
     * Search for an internationalizable display text for the current value. The search process is:
     * <ol>
     * <li>let V = the internal value of the option, 0 1 or 2, T = the value of the displayType meta-property, and D =
     * the displayed value</li>
     * <li>if a message with the key <fieldFullName>_<V> exists, return it as D</li>
     * <li>else, if a message with the key <T>_<V> exists, return it as D</li>
     * <li>else return V if V is 0 or 1, or --- if V is 2 (undecided)</li>
     * </ol>
     * 
     * @param context The request context.
     * @param value The internal value.
     * @return The text that should be displayed, representing a human-understandable name for the internal value.
     */
    private String getDisplayValue(XWikiContext context, int value)
    {
        try {
            XWikiMessageTool msg = context.getMessageTool();

            // <classname>_<property>_<value>
            String key = getFieldFullName() + "_" + value;
            String result = msg.get(key);
            if (key.equals(result)) {
                // <display type>_<value>
                key = getDisplayType() + "_" + value;
                result = msg.get(key);
                if (key.equals(result)) {
                    // Just return the value
                    if (value == 2) {
                        result = "---";
                    } else {
                        result = "" + value;
                    }
                }
            }
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "" + value;
        }
    }

    @Override
    public String displaySearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        if (getDisplayType().equals("input")) {
            return super.displaySearch(name, prefix, criteria, context);
        } else if (getDisplayType().equals("radio")) {
            return displayCheckboxSearch(name, prefix, criteria, context);
        } else {
            return displaySelectSearch(name, prefix, criteria, context);
        }
    }

    public String displaySelectSearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        select select = new select(prefix + name, 1);
        select.setMultiple(true);
        select.setSize(3);
        String String0 = getDisplayValue(context, 0);
        String String1 = getDisplayValue(context, 1);
        String fieldFullName = getFieldFullName();
        Number[] selectArray = ((Number[]) criteria.getParameter(fieldFullName));
        List<Number> selectlist = (selectArray != null) ? Arrays.asList(selectArray) : new ArrayList<Number>();

        option[] options = {new option(String1, "1"), new option(String0, "0")};
        options[0].addElement(String1);
        options[1].addElement(String0);
        if (selectlist.contains(new Integer(1)))
         options[0].setSelected(true); 
        if (selectlist.contains(new Integer(0)))
         options[1].setSelected(true); 

        /*
         * try { IntegerProperty prop = (IntegerProperty) object.safeget(name); if (prop!=null) { Integer ivalue =
         * (Integer)prop.getValue(); if (ivalue!=null) { int value = ivalue.intValue(); if (value==1)
         * options[1].setSelected(true); else if (value==0) options[2].setSelected(true); } else { int value =
         * getDefaultValue(); if (value==1) options[1].setSelected(true); else if (value==0)
         * options[2].setSelected(true); } } } catch (Exception e) { // This should not happen e.printStackTrace(); }
         */
        select.addElement(options);
        return select.toString();
    }

    public String displayCheckboxSearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        org.apache.ecs.xhtml.input check = new input(input.checkbox, prefix + name, 1);
        org.apache.ecs.xhtml.input checkNo = new input(input.hidden, prefix + name, 0);

        /*
         * try { IntegerProperty prop = (IntegerProperty) object.safeget(name); if (prop!=null) { Integer ivalue =
         * (Integer)prop.getValue(); if (ivalue!=null) { int value = ivalue.intValue(); if (value==1)
         * check.setChecked(true); else if (value==0) check.setChecked(false); } else { int value = getDefaultValue();
         * if (value==1) check.setChecked(true); else check.setChecked(false); } }} catch (Exception e) { // This should
         * not happen e.printStackTrace(); }
         */
        buffer.append(check.toString());
        buffer.append(checkNo.toString());
        return buffer.toString();
    }

    @Override
    public void makeQuery(Map<String, Object> map, String prefix, XWikiCriteria query, List<String> criteriaList)
    {
        Object values = map.get(prefix);
        if ((values == null) || (values.equals(""))) {
            return;
        }

        // :value = doc.object(XWiki.ArticleClass).category

        Number[] valuesarray = (Number[]) values;
        String[] criteriaarray = new String[valuesarray.length];
        for (int i = 0; i < valuesarray.length; i++) {
            criteriaarray[i] =  "" + valuesarray[i] + " = " + getFullQueryPropertyName();
        }
        criteriaList.add("(" + StringUtils.join(criteriaarray, " or ") + ")");
        return;
    }


    @Override
    public void fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
        String[] data = map.get("");
        if (data != null) {
            Number[] data2 = new Number[data.length];
            for (int i = 0; i < data.length; i++) {
                data2[i] = (Number) fromString(data[i]).getValue();
            }
            query.setParam(getObject().getName() + "_" + getName(), data2);
        }
    }
}

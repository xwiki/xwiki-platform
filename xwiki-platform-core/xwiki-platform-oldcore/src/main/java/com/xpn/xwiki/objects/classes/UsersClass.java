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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.button;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;
import org.dom4j.Element;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.user.api.XWikiRightService;

public class UsersClass extends ListClass
{
    private static final String XCLASSNAME = "userslist";

    public UsersClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, "Users List", wclass);
        setSize(6);
        setUsesList(true);
    }

    public UsersClass()
    {
        this(null);
    }

    @Override
    public List<String> getList(XWikiContext context)
    {
        List<String> list;
        try {
            list =
                (List<String>) context.getWiki().getGroupService(context).getAllMatchedUsers(null, false, 0, 0,
                null, context);
        } catch (XWikiException e) {
            // TODO add log exception
            list = new ArrayList<String>();
        }
        return list;
    }

    @Override
    public Map<String, ListItem> getMap(XWikiContext context)
    {
        return new HashMap<String, ListItem>();
    }

    public boolean isUsesList()
    {
        return (getIntValue("usesList") == 1);
    }

    public void setUsesList(boolean usesList)
    {
        setIntValue("usesList", usesList ? 1 : 0);
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new LargeStringProperty();
        property.setName(getName());
        return property;
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty prop = newProperty();
        prop.setValue(value);
        return prop;
    }

    @Override
    public BaseProperty fromStringArray(String[] strings)
    {
        List<String> list = new ArrayList<String>();
        for (int i = 0; i < strings.length; i++) {
            if (!StringUtils.isBlank(strings[i])) {
                list.add(strings[i]);
            }
        }
        BaseProperty prop = newProperty();
        prop.setValue(StringUtils.join(list, ","));
        return prop;
    }

    public String getText(String value, XWikiContext context)
    {
        return context.getWiki().getUserName(value, null, false, context);
    }

    public static List<String> getListFromString(String value)
    {
        return getListFromString(value, ",", false);
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        select select = new select(prefix + name, 1);
        select.setMultiple(isMultiSelect());
        select.setSize(getSize());
        select.setName(prefix + name);
        select.setID(prefix + name);
        select.setDisabled(isDisabled());

        List<String> list;
        if (isUsesList()) {
            list = getList(context);
        } else {
            list = new ArrayList<String>();
        }

        List<String> selectlist;
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop == null) {
            selectlist = new ArrayList<String>();
        } else {
            selectlist = getListFromString((String) prop.getValue());
        }

        // Add options from Set

        for (String value : selectlist) {
            if (!list.contains(value)) {
                list.add(value);
            }
        }

        // Make sure we have guest
        list.remove(XWikiRightService.GUEST_USER_FULLNAME);
        list.add(0, XWikiRightService.GUEST_USER_FULLNAME);

        // Sort the user list
        TreeMap<String, String> map = new TreeMap<String, String>();
        for (String value : list) {
            map.put(getText(value, context), value);
        }

        // Add options from Set
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String display = entry.getKey();
            String value = entry.getValue();

            option option = new option(display, value);
            option.addElement(display);
            if (selectlist.contains(value)) {
                option.setSelected(true);
            }
            select.addElement(option);
        }

        buffer.append(select.toString());

        if (!isUsesList()) {
            input in = new input();
            in.setName(prefix + "newuser");
            in.setSize(15);
            in.setDisabled(isDisabled());
            buffer.append("<br />");
            buffer.append(in.toString());

            if (!isDisabled()) {
                button button = new button();
                button.setTagText("Add");

                button.setOnClick("addUser(this.form,'" + prefix + "'); return false;");
                buffer.append(button.toString());
            }
        }

        input in = new input();
        in.setType("hidden");
        in.setName(prefix + name);
        in.setDisabled(isDisabled());
        buffer.append(in.toString());
    }

    @Override
    public BaseProperty newPropertyfromXML(Element ppcel)
    {
        String value = ppcel.getText();
        return fromString(value);
    }
}

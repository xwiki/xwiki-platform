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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;
import org.dom4j.Element;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.xml.XMLAttributeValueFilter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.web.XWikiRequest;

public class LevelsClass extends ListClass
{
    private static final long serialVersionUID = 1L;

    private static final String XCLASSNAME = "levelslist";

    private static final String COMMA = ",";

    public LevelsClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, "Levels List", wclass);
        setSize(6);
    }

    public LevelsClass()
    {
        this(null);
    }

    @Override
    protected String getFirstSeparator()
    {
        return COMMA;
    }

    @Override
    public List<String> getList(XWikiContext context)
    {
        List<String> list;
        try {
            list = context.getWiki().getRightService().listAllLevels(context);
        } catch (XWikiException e) {
            // TODO add log exception
            list = new ArrayList<String>();
        }

        XWikiRequest req = context.getRequest();
        if (("editrights".equals(req.get("xpage")) || "rights".equals(req.get("editor")))
            && (!"1".equals(req.get("global")))) {
            list.remove("admin");
            list.remove("programming");
            list.remove("delete");
            list.remove("register");
        }
        return list;
    }

    @Override
    public Map<String, ListItem> getMap(XWikiContext context)
    {
        return new HashMap<String, ListItem>();
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new StringProperty();
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
        List<String> list;
        if ((strings.length == 1) && (getDisplayType().equals(DISPLAYTYPE_INPUT) || isMultiSelect())) {
            list = getListFromString(strings[0], getSeparators(), false);
        } else {
            list = Arrays.asList(strings);
        }

        BaseProperty prop = newProperty();
        fromList(prop, list, true);
        return prop;
    }

    @Override
    public void fromList(BaseProperty<?> property, List<String> list)
    {
        fromList(property, list, true);
    }

    public String getText(String value, XWikiContext context)
    {
        return value;
    }

    public static List<String> getListFromString(String value)
    {
        return getListFromString(value, COMMA, false, true);
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
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

        BaseProperty prop = (BaseProperty) object.safeget(name);
        List<String> selectlist = toList(prop);

        // Add options from Set
        for (String value : list) {
            String display = getText(value, context);
            option option = new option(display, value);
            option.setAttributeFilter(new XMLAttributeValueFilter());
            option.addElement(XMLUtils.escape(display));
            // If we don't have this option in the list then add it
            if (!list.contains(value)) {
                list.add(value);
            }
            // The right mechanism does not care about the case used for the rights
            // so we rely on an equals ignoring case here to know if we should mark it selected or not.
            if (selectlist.stream().anyMatch(value::equalsIgnoreCase)) {
                option.setSelected(true);
            }
            select.addElement(option);
        }

        buffer.append(select.toString());
        input in = new input();
        in.setAttributeFilter(new XMLAttributeValueFilter());
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

    @Override
    public List<String> toList(BaseProperty<?> property)
    {
        List<String> selectlist;

        if (property == null) {
            selectlist = Collections.emptyList();
        } else {
            selectlist = getListFromString((String) property.getValue());
        }

        return selectlist;
    }

    @Override
    public <T extends EntityReference> void mergeProperty(BaseProperty<T> currentProperty,
        BaseProperty<T> previousProperty, BaseProperty<T> newProperty, MergeConfiguration configuration,
        XWikiContext xcontext, MergeResult mergeResult)
    {
        // always a not ordered list
        mergeNotOrderedListProperty(currentProperty, previousProperty, newProperty, configuration, xcontext,
            mergeResult);
    }
}

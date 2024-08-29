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

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

/**
 * Defines an XClass property type whose value is a list of group references.
 *
 * @version $Id$
 */
public class GroupsClass extends ListClass
{
    private static final long serialVersionUID = 1L;

    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupsClass.class);

    private static final String COMMA = ",";

    /**
     * The meta property that specifies if the list box that is used to select the groups should be filled with all the
     * available groups. This property should not be set when the number of groups is very large.
     */
    private static final String META_PROPERTY_USES_LIST = "usesList";

    /**
     * Creates a new Groups List property that is described by the given meta class.
     *
     * @param metaClass the meta class that defines the list of meta properties associated with this property type
     */
    public GroupsClass(PropertyMetaClass metaClass)
    {
        super("groupslist", "Groups List", metaClass);

        setSize(20);
        setDisplayType("input");
        setPicker(true);
    }

    /**
     * Default constructor.
     */
    public GroupsClass()
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
        try {
            return (List<String>) context.getWiki().getGroupService(context)
                .getAllMatchedGroups(null, false, 0, 0, null, context);
        } catch (XWikiException e) {
            LOGGER.warn("Failed to retrieve the list of groups.", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, ListItem> getMap(XWikiContext context)
    {
        return new HashMap<String, ListItem>();
    }

    /**
     * @return {@code true} if the list box that is used to select the groups should be filled with all the available
     *         groups, {@code false} otherwise
     * @deprecated since 4.3M2 this meta property is not used anymore because we changed the default displayer
     */
    @Deprecated
    public boolean isUsesList()
    {
        return getIntValue(META_PROPERTY_USES_LIST) == 1;
    }

    /**
     * Sets whether to list all the available groups in the list box used to select the groups. This property should not
     * be set when the number of groups is very large.
     *
     * @param usesList {@code true} to fill the list box that is used to select the groups with all the available
     *            groups, {@code false} otherwise
     * @deprecated since 4.3M2 this meta property is not used anymore because we changed the default displayer
     */
    @Deprecated
    public void setUsesList(boolean usesList)
    {
        setIntValue(META_PROPERTY_USES_LIST, usesList ? 1 : 0);
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
        List<String> list;
        if ((strings.length == 1) && (getDisplayType().equals(DISPLAYTYPE_INPUT) || isMultiSelect())) {
            list = getListFromString(strings[0], getSeparators(), false);
        } else {
            list = Arrays.asList(strings);
        }

        BaseProperty prop = newProperty();
        fromList(prop, list);
        return prop;
    }

    @Override
    public void fromList(BaseProperty<?> property, List<String> list)
    {
        fromList(property, list, true);
    }

    /**
     * @param value a group string reference
     * @param context the XWiki context
     * @return the name of the specified group (the document name component from the given reference)
     */
    public String getText(String value, XWikiContext context)
    {
        if (value.indexOf(":") != -1) {
            return value;
        }
        return value.substring(value.lastIndexOf(".") + 1);
    }

    /**
     * Splits the given string into a list of group names.
     *
     * @param value a comma separate list of group names
     * @return the list of group names
     */
    public static List<String> getListFromString(String value)
    {
        return getListFromString(value, COMMA, false, true);
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
            selectlist = new ArrayList<String>();
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

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

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
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
 * Defines an XClass property type whose value is a list of user references.
 *
 * @version $Id$
 */
public class UsersClass extends ListClass
{
    private static final long serialVersionUID = 1L;

    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UsersClass.class);

    /**
     * The meta property that specifies if the list box that is used to select the users should be filled with all the
     * available users. This property should not be set when the number of users is very large.
     */
    private static final String META_PROPERTY_USES_LIST = "usesList";

    private static final String COMMA = ",";

    /**
     * Creates a new Users List property that is described by the given meta class.
     *
     * @param metaClass the meta class that defines the list of meta properties associated with this property type
     */
    public UsersClass(PropertyMetaClass metaClass)
    {
        super("userslist", "Users List", metaClass);

        setSize(20);
        setDisplayType("input");
        setPicker(true);
    }

    /**
     * Default constructor.
     */
    public UsersClass()
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
        // TODO: Make this scale. If we have lots of users in the wiki it won't perform fast. One solution is to
        // return an iterator instead that gets users by batches.
        try {
            return (List<String>) context.getWiki().getGroupService(context)
                .getAllMatchedUsers(null, false, 0, 0, null, context);
        } catch (XWikiException e) {
            LOGGER.warn("Failed to retrieve the list of users.", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, ListItem> getMap(XWikiContext context)
    {
        // TODO: Make this scale. If we have lots of users in the wiki it won't perform fast. One solution is to
        // return an iterator instead that gets users by batches.
        Map<String, ListItem> result;
        List<String> users = getList(context);
        if (users != null && users.size() > 0) {
            result = new LinkedHashMap<>();
            for (String userName : users) {
                // Get the user name for pretty display
                String prettyUserName = context.getWiki().getUserName(userName, null, false, context);
                result.put(userName, new ListItem(userName, prettyUserName));
            }
        } else {
            result = Collections.emptyMap();
        }
        return result;
    }

    /**
     * @return {@code true} if the list box that is used to select the users should be filled with all the available
     *         users, {@code false} otherwise
     * @deprecated since 4.3M2 this meta property is not used anymore because we changed the default displayer
     */
    @Deprecated
    public boolean isUsesList()
    {
        return getIntValue(META_PROPERTY_USES_LIST) == 1;
    }

    /**
     * Sets whether to list all the available users in the list box used to select the users. This property should not
     * be set when the number of users is very large.
     *
     * @param usesList {@code true} to fill the list box that is used to select the users with all the available users,
     *            {@code false} otherwise
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
     * @param value a user name
     * @param context the XWiki context
     * @return the real name of the specified user, that can be used for display
     */
    public String getText(String value, XWikiContext context)
    {
        return context.getWiki().getUserName(value, null, false, context);
    }

    /**
     * Splits the given string into a list of user names.
     *
     * @param value a comma separate list of user names
     * @return the list of user names
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

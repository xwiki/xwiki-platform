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

package com.xpn.xwiki.api;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.util.Programming;

public class PropertyClass extends Collection
{
    public PropertyClass(com.xpn.xwiki.objects.classes.PropertyClass property,
        XWikiContext context)
    {
        super(property, context);
    }

    protected com.xpn.xwiki.objects.classes.PropertyClass getBasePropertyClass()
    {
        return (com.xpn.xwiki.objects.classes.PropertyClass) getCollection();
    }

    public String getClassType()
    {
        return getBasePropertyClass().getClassType();
    }

    public String getType()
    {
        String result = getBasePropertyClass().getClassType();
        return result.substring(result.lastIndexOf(".") + 1);
    }

    public String getClassName()
    {
        return getCollection().getClassName();
    }

    public com.xpn.xwiki.objects.classes.PropertyClass getPropertyClass()
    {
        if (hasProgrammingRights()) {
            return (com.xpn.xwiki.objects.classes.PropertyClass) getCollection();
        }
        return null;
    }

    public String getPrettyName()
    {
        return getBasePropertyClass().getPrettyName();
    }

    public String getValidationMessage()
    {
        return getBasePropertyClass().getValidationMessage();
    }

    public String getValidationRegExp()
    {
        return getBasePropertyClass().getValidationRegExp();
    }

    public String getTooltip()
    {
        return getBasePropertyClass().getTooltip();
    }

    /**
     * If the property is a {@link ListClass}, returns the possible values. These are the internal values (keys), and
     * not the user-friendly or translated values that would be displayed to the user.
     * 
     * @return the list of possible ({@code String}) values
     * @see #getMapValues() {@code getMapValues()} returns both the keys and their user-friendly displayed values
     **/
    public List<String> getListValues()
    {
        com.xpn.xwiki.objects.classes.PropertyClass pclass = getBasePropertyClass();
        if (pclass instanceof ListClass) {
            return ((ListClass) pclass).getList(this.context);
        } else {
            // Although we prefer to return empty lists from API methods, here returning any kind of list doesn't make
            // sense, since the property does not have a list of possible values at all (like, a number property).
            return null;
        }
    }

    /**
     * If the property is a {@link ListClass}, returns the possible values as a map {@code internal key <-> displayed
     * value}.
     * 
     * @return the map of possible ({@code String}) values and their associated ({@code ListItem}) displayed values
     * @see #getListValues() {@code getListValues()} returns only the list of possible internal keys
     **/
    public Map<String, ListItem> getMapValues()
    {
        com.xpn.xwiki.objects.classes.PropertyClass pclass = getBasePropertyClass();
        if (pclass instanceof ListClass) {
            return ((ListClass) pclass).getMap(this.context);
        } else {
            // Although we prefer to return empty maps from API methods, here returning any kind of map doesn't make
            // sense, since the property does not have a list of possible values at all (like, a number property).
            return null;
        }
    }
}

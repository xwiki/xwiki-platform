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
package com.xpn.xwiki.plugin.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiRequest;

public class XWikiQuery extends XWikiCriteria
{
    protected boolean distinct = false;

    protected List<String> displayProperties = new ArrayList<String>();

    protected List<String> addProperties = new ArrayList<String>();

    protected List<String> groupbyProperties = new ArrayList<String>();

    protected List<OrderClause> orderProperties = new ArrayList<OrderClause>();

    public XWikiQuery()
    {
        super();
    }

    public XWikiQuery(XWikiRequest request, String className, XWikiContext context)
        throws XWikiException
    {
        super();
        String[] columns = request.getParameterValues(className + "_" + "searchcolumns");
        setDisplayProperties(columns);
        String[] order = request.getParameterValues(className + "_" + "searchorder");
        setOrderProperties(order);
        BaseClass bclass = context.getWiki().getDocument(className, context).getxWikiClass();
        Set<String> properties = bclass.getPropertyList();
        Iterator<String> propid = properties.iterator();
        while (propid.hasNext()) {
            String propname = (String) propid.next();
            Map<String, String[]> map = Util.getObject(request, className + "_" + propname);
            ((PropertyClass) (bclass.get(propname))).fromSearchMap(this, map);
        }
    }

    private void setOrderProperties(String[] order)
    {
        orderProperties.clear();
        if (order != null) {
            for (int i = 0; i < order.length; i++) {
                OrderClause oclause = new OrderClause(order[i], OrderClause.ASC);
                orderProperties.add(oclause);
            }
        }
    }

    public void reset()
    {
        displayProperties = new ArrayList<String>();
        addProperties = new ArrayList<String>();
        groupbyProperties = new ArrayList<String>();
        orderProperties = new ArrayList<OrderClause>();
    }

    public void setDistinct(boolean distinct)
    {
        this.distinct = distinct;
    }

    public boolean isDistinct()
    {
        return distinct;
    }

    public void setDisplayProperties(List<String> properties)
    {
        displayProperties = properties;
    }

    public void setDisplayProperties(String[] properties)
    {
        if (properties != null) {
            displayProperties = Arrays.asList(properties);
        }
    }

    public List<String> getDisplayProperties()
    {
        return displayProperties;
    }

    public void setDisplayProperty(String property)
    {
        displayProperties = new ArrayList<String>();
        displayProperties.add(property);
    }

    public void addProperty(String property)
    {
        addProperties.add(property);
    }

    public void addGroupByProperty(String property)
    {
        groupbyProperties.add(property);
    }

    public void addOrderProperty(String property)
    {
        orderProperties.add(new OrderClause(property, OrderClause.DESC));
    }

    public void addOrderProperty(String property, String order)
    {
        addOrderProperty(property, order.equals("desc") ? OrderClause.DESC : OrderClause.ASC);
    }

    public void addOrderProperty(String property, int order)
    {
        orderProperties.add(new OrderClause(property, order));
    }

    public List<OrderClause> getOrderProperties()
    {
        return orderProperties;
    }
}

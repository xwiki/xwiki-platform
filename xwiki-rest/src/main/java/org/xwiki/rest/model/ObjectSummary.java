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
package org.xwiki.rest.model;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * @version $Id$
 */
@XStreamAlias("objectSummary")
public class ObjectSummary extends LinkCollection
{
    private String id;

    private String pageId;

    private String className;

    private Integer number;

    private String guid;

    private String prettyName;

    private Properties propertyList;

    public ObjectSummary()
    {
        propertyList = new Properties();
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getPageId()
    {
        return pageId;
    }

    public void setPageId(String pageId)
    {
        this.pageId = pageId;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public Integer getNumber()
    {
        return number;
    }

    public void setNumber(Integer number)
    {
        this.number = number;
    }

    public String getGuid()
    {
        return guid;
    }

    public void setGuid(String guid)
    {
        this.guid = guid;
    }

    public String getPrettyName()
    {
        return prettyName;
    }

    public void setPrettyName(String prettyName)
    {
        this.prettyName = prettyName;
    }

    public Properties getPropertyList()
    {
        return propertyList;
    }

    public String getPropertyValue(String propertyName)
    {
        for (Property property : propertyList.getProperties()) {
            if (propertyName.equals(property.getName())) {
                return property.getValue();
            }
        }

        return null;
    }

    public Property getProperty(String propertyName)
    {
        for (Property property : propertyList.getProperties()) {
            if (propertyName.equals(property.getName())) {
                return property;
            }
        }

        return null;
    }
}

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
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * @version $Id$
 */
@XStreamAlias("property")
public class Property extends LinkCollection
{
    @XStreamAsAttribute
    private String type;

    private String name;

    private String value;

    @XStreamAsAttribute
    private String allowedValues;

    @XStreamAsAttribute
    private String separators;

    @XStreamAsAttribute
    private String dateFormat;

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public void setAllowedValues(String allowedValues)
    {
        this.allowedValues = allowedValues;
    }

    public String getAllowedValues()
    {
        return allowedValues;
    }

    public void setSeparators(String separators)
    {
        this.separators = separators;
    }

    public String getSeparators()
    {
        return separators;
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public String getDateFormat()
    {
        return dateFormat;
    }

}

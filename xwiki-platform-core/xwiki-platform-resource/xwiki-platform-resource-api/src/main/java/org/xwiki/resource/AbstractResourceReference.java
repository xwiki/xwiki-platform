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
package org.xwiki.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Base XWiki Resource Reference implementation common to all extending classes. Manages XWiki Resource Reference
 * parameters.
 * 
 * @version $Id$
 * @since 6.1M2
 */
public abstract class AbstractResourceReference implements ResourceReference
{
    /**
     * @see #getParameters()
     */
    private final Map<String, List<String>> parameters = new LinkedHashMap<>();

    /**
     * @see #getType()
     */
    private ResourceType type;

    @Override
    public ResourceType getType()
    {
        return this.type;
    }

    /**
     * @param type see {@link #getType()}
     */
    public void setType(ResourceType type)
    {
        this.type = type;
    }

    @Override
    public void addParameter(String name, Object value)
    {
        List<String> list = this.parameters.get(name);
        if (list == null) {
            list = new ArrayList<>();
        }
        if (value != null) {
            // If the value is a Collection or an Array then add a multivalued parameter!
            if (value.getClass().isArray()) {
                Object[] objectValues = (Object[]) value;
                for (Object objectValue : objectValues) {
                    list.add(objectValue.toString());
                }
            } else if (Collection.class.isAssignableFrom(value.getClass())) {
                Collection<?> collectionValues = (Collection<?>) value;
                for (Object collectionValue : collectionValues) {
                    list.add(collectionValue.toString());
                }
            } else {
                list.add(value.toString());
            }
        }
        this.parameters.put(name, list);
    }

    @Override
    public Map<String, List<String>> getParameters()
    {
        return Collections.unmodifiableMap(this.parameters);
    }

    @Override
    public List<String> getParameterValues(String name)
    {
        return this.parameters.get(name);
    }

    @Override
    public String getParameterValue(String name)
    {
        String result = null;
        List<String> list = this.parameters.get(name);
        if (list != null) {
            result = list.get(0);
        }
        return result;
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append("type", getType());
        builder.append("parameters", getParameters());
        return builder.toString();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(9, 5)
            .append(getType())
            .append(getParameters())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        ResourceReference rhs = (ResourceReference) object;
        return new EqualsBuilder()
            .append(getType(), rhs.getType())
            .append(getParameters(), rhs.getParameters())
            .isEquals();
    }
}

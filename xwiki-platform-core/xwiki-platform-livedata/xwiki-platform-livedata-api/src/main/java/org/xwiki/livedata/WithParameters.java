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
package org.xwiki.livedata;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.text.XWikiToStringBuilder;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

/**
 * Base class for components and POJOs that can have unknown parameters. When extended by POJOs the unknown parameters
 * are serialized as JSON side by side with the other (known) POJO fields. When extended by components, they should have
 * {@link ComponentInstantiationStrategy#PER_LOOKUP}.
 * 
 * @version $Id$
 * @since 12.10
 */
public class WithParameters
{
    private final Map<String, Object> parameters = new HashMap<>();

    /**
     * @return the parameters
     */
    @JsonAnyGetter
    public Map<String, Object> getParameters()
    {
        return this.parameters;
    }

    /**
     * Set the value of a parameter.
     * 
     * @param key the parameter name
     * @param value the parameter value
     * @return the previous parameter value
     */
    @JsonAnySetter
    public Object setParameter(String key, Object value)
    {
        return this.parameters.put(key, value);
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(getParameters()).build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof WithParameters) {
            WithParameters other = (WithParameters) obj;
            return new EqualsBuilder().append(getParameters(), other.getParameters()).build();
        }

        return false;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("parameters", parameters)
            .toString();
    }
}

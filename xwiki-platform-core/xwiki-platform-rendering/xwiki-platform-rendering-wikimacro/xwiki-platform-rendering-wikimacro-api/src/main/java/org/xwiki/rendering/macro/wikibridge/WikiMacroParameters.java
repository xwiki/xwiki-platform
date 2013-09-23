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
package org.xwiki.rendering.macro.wikibridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xwiki.properties.RawProperties;

/**
 * Generic parameters class for all wiki macros.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class WikiMacroParameters implements RawProperties
{
    /**
     * A map holding all the parameter and their values.
     */
    private Map<String, Object> parametersMap;

    /**
     * Creates a new {@link WikiMacroParameters} instance.
     */
    public WikiMacroParameters()
    {
        this.parametersMap = new HashMap<String, Object>();
    }

    @Override
    public void set(String propertyName, Object value)
    {
        this.parametersMap.put(propertyName, value);
    }

    /**
     * Returns the set of parameter names provided by user.
     * 
     * @return set of parameter names provided by user
     */
    public Set<String> getParameterNames()
    {
        return Collections.unmodifiableSet(this.parametersMap.keySet());
    }

    /**
     * Returns the parameter value associated with the propertyName provided.
     * 
     * @param propertyName the property name.
     * @return the property value if set, null otherwise.
     */
    public Object get(String propertyName)
    {
        return this.parametersMap.get(propertyName);
    }
}

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
package org.xwiki.filter.instance.input;

import java.util.HashMap;

import org.xwiki.filter.FilterStreamProperties;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.properties.RawProperties;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyHidden;
import org.xwiki.properties.annotation.PropertyName;

/**
 * The properties passed to the instance input wiki stream.
 * <p>
 * The properties are also passed to implementations of {@link InstanceInputEventGenerator}.
 * 
 * @version $Id$
 * @since 6.2M1
 */
public class InstanceInputProperties extends HashMap<String, Object> implements RawProperties, FilterStreamProperties
{
    private static final String PROPNAME_ENTITIES = "entities";

    /**
     * @return The entities to generate events from
     */
    @PropertyName("Entities")
    @PropertyDescription("The entities to generate events from")
    // TODO: implement Converter for EntityReferenceSet
    @PropertyHidden
    public EntityReferenceSet getEntities()
    {
        return (EntityReferenceSet) get(PROPNAME_ENTITIES);
    }

    /**
     * @param entities The entities to generate events from
     */
    public void setEntities(EntityReferenceSet entities)
    {
        put(PROPNAME_ENTITIES, entities);
    }

    @PropertyName("Verbose")
    @PropertyDescription("Indicate if the filter should produce verbose log")
    @Override
    public boolean isVerbose()
    {
        Boolean verbose = (Boolean) get(PROPNAME_VERBOSE);

        return verbose != Boolean.FALSE;
    }

    @Override
    public void setVerbose(boolean verbose)
    {
        put(PROPNAME_VERBOSE, verbose);
    }

    @Override
    public void set(String propertyName, Object value)
    {
        put(propertyName, value);
    }
}

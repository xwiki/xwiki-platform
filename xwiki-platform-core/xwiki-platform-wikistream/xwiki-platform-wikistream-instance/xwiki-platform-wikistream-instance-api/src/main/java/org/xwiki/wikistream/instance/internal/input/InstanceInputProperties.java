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
package org.xwiki.wikistream.instance.internal.input;

import java.util.HashMap;

import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.properties.RawProperties;
import org.xwiki.stability.Unstable;
import org.xwiki.wikistream.instance.input.InstanceInputEventGenerator;

/**
 * The properties passed to the instance input wiki stream.
 * <p>
 * The properties are also passed to implementations of {@link InstanceInputEventGenerator}.
 * 
 * @version $Id$
 * @since 5.2M2
 */
@Unstable
public class InstanceInputProperties extends HashMap<String, Object> implements RawProperties
{
    public EntityReferenceSet getEntities()
    {
        return (EntityReferenceSet) get("entities");
    }

    public void setEntities(EntityReferenceSet entities)
    {
        put("entities", entities);
    }

    @Override
    public void set(String propertyName, Object value)
    {
        put(propertyName, value);
    }
}

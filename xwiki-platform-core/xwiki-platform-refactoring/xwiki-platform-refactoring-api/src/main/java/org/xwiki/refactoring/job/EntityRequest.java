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
package org.xwiki.refactoring.job;

import java.util.Collection;

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * A generic job request that targets multiple entities.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Unstable
public class EntityRequest extends AbstractRequest
{
    /**
     * @see #getEntityReferences()
     */
    public static final String PROPERTY_ENTITY_REFERENCES = "entityReferences";

    /**
     * @see #isDeep()
     */
    public static final String PROPERTY_DEEP = "deep";

    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @return the collection of entity references that are targeted by this request
     */
    public Collection<EntityReference> getEntityReferences()
    {
        return getProperty(PROPERTY_ENTITY_REFERENCES);
    }

    /**
     * Sets the collection of entity references that are targeted by this request.
     * 
     * @param entityReferences a collection of entity references
     */
    public void setEntityReferences(Collection<EntityReference> entityReferences)
    {
        setProperty(PROPERTY_ENTITY_REFERENCES, entityReferences);
    }

    /**
     * @return {@code true} if the operation should target child entities also (i.e. go deep into the entity hierarchy),
     *         {@code false} otherwise
     */
    public boolean isDeep()
    {
        return getProperty(PROPERTY_DEEP, false);
    }

    /**
     * Sets whether the operation should target child entities also (i.e. go deep into the entity hierarchy) or not.
     * 
     * @param deep {@code true} to include the child entities, {@code false} otherwise
     */
    public void setDeep(boolean deep)
    {
        setProperty(PROPERTY_DEEP, deep);
    }
}

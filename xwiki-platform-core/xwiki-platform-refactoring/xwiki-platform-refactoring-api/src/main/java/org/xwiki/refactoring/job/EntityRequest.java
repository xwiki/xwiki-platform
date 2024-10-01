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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.job.Request;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * A generic job request that targets multiple entities.
 * 
 * @version $Id$
 * @since 7.2M1
 */
public class EntityRequest extends AbstractCheckRightsRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see #getJobType()
     */
    private static final String PROPERTY_JOB_TYPE = "job.type";

    /**
     * @see #getEntityReferences()
     */
    private static final String PROPERTY_ENTITY_REFERENCES = "entityReferences";

    /**
     * @see #getEntityParameters(EntityReference)
     */
    private static final String PROPERTY_ENTITY_PARAMETERS = "entityParameters";

    /**
     * @see #isDeep()
     */
    private static final String PROPERTY_DEEP = "deep";

    /**
     * @see #isWaitForIndexing()
     */
    private static final String PROPERTY_WAIT_FOR_INDEXING = "waitForIndexing";

    /**
     * Default constructor.
     */
    public EntityRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 14.7RC1
     * @since 14.4.4
     * @since 13.10.9
     */
    public EntityRequest(Request request)
    {
        super(request);
    }

    /**
     * @return the type of job that should perform this request; this is useful when different jobs use the same type of
     *         request
     * @deprecated since 9.2RC1, use {@link EntityJobStatus#getJobType()} instead
     */
    @Deprecated
    public String getJobType()
    {
        return getProperty(PROPERTY_JOB_TYPE);
    }

    /**
     * Sets the type of job that should perform this request. This is useful when different jobs use the same type of
     * request.
     * 
     * @param jobType the type of job that should perform this request
     * @deprecated
     */
    @Deprecated
    public void setJobType(String jobType)
    {
        setProperty(PROPERTY_JOB_TYPE, jobType);
    }

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

    /**
     * @param entityReference one of the entity references that are the target of this request
     * @return the custom parameters associated to the specified target entity
     */
    public Map<String, String> getEntityParameters(EntityReference entityReference)
    {
        Map<String, String> entityParameters =
            getProperty(PROPERTY_ENTITY_PARAMETERS, Collections.<EntityReference, Map<String, String>>emptyMap())
                .get(entityReference);
        return entityParameters == null ? Collections.emptyMap() : entityParameters;
    }

    /**
     * Associates custom parameters to a target entity.
     * 
     * @param entityReference one of the target entities
     * @param entityParameters the custom parameters to associate to the specified target entity
     */
    public void setEntityParameters(EntityReference entityReference, Map<String, String> entityParameters)
    {
        Map<EntityReference, Map<String, String>> paramsPerEntity = getProperty(PROPERTY_ENTITY_PARAMETERS);
        if (paramsPerEntity == null) {
            paramsPerEntity = new HashMap<>();
            setProperty(PROPERTY_ENTITY_PARAMETERS, paramsPerEntity);
        }
        paramsPerEntity.put(entityReference, entityParameters);
    }

    /**
     * @return {@code true} if, when links shall be updated, the refactoring shall wait for link indexing to complete.
     * This ensures that accurate information about links is available, this is particularly relevant when multiple
     * documents with links between them are moved.
     *
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     */
    @Unstable
    public boolean isWaitForIndexing()
    {
        return getProperty(PROPERTY_WAIT_FOR_INDEXING, true);
    }

    /**
     * Sets whether the refactoring job should wait for links to be indexed before updating them.
     *
     * @param waitForIndexing if the refactoring job should wait for links to be indexed before updating them
     * @since 16.9.0RC1
     * @since 16.4.5
     * @since 15.10.13
     */
    @Unstable
    public void setWaitForIndexing(boolean waitForIndexing)
    {
        setProperty(PROPERTY_WAIT_FOR_INDEXING, waitForIndexing);
    }
}

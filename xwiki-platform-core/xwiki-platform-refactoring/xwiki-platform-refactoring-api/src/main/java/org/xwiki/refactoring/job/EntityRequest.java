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

import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * A generic job request that targets multiple entities.
 * 
 * @version $Id$
 * @since 7.2M1
 */
public class EntityRequest extends AbstractRequest
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
     * @see #getUserReference()
     */
    private static final String PROPERTY_USER_REFERENCE = "user.reference";

    /**
     * @see #getAuthorReference()
     */
    private static final String PROPERTY_CALLER_REFERENCE = "caller.reference";

    /**
     * @see #isCheckRights()
     */
    private static final String PROPERTY_CHECK_RIGHTS = "checkrights";

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
     * @return {@code true} in case the job should check if the user specified by {@link #getUserReference()} is
     *         authorized to perform the actions implied by this request, {@code false} otherwise
     */
    public boolean isCheckRights()
    {
        return getProperty(PROPERTY_CHECK_RIGHTS, true);
    }

    /**
     * Sets whether the job should check or not if the user specified by {@link #getUserReference()} is authorized to
     * perform the actions implied by this request.
     * 
     * @param checkRights {@code true} to check if {@link #getUserReference()} is authorized to perform this request,
     *            {@code false} to perform this request without checking rights
     */
    public void setCheckRights(boolean checkRights)
    {
        setProperty(PROPERTY_CHECK_RIGHTS, checkRights);
    }

    /**
     * @return the user that should be used to perform this request; this user must be authorized to perform the actions
     *         implied by this request if {@link #isCheckRights()} is {@code true}.
     */
    public DocumentReference getUserReference()
    {
        return getProperty(PROPERTY_USER_REFERENCE);
    }

    /**
     * Sets the user that should be used to perform this request. This user must be authorized to perform the actions
     * implied by this request if {@link #isCheckRights()} is {@code true}.
     * 
     * @param userReference the user reference
     */
    public void setUserReference(DocumentReference userReference)
    {
        setProperty(PROPERTY_USER_REFERENCE, userReference);
    }

    /**
     * @return the author of the script which is performing the request; this user must be authorized to perform the
     * actions implied by this request if {@link #isCheckRights()} is {@code true}.
     * @since 10.10RC1
     * @since 10.8.2
     * @since 9.11.9
     */
    public DocumentReference getAuthorReference()
    {
        return getProperty(PROPERTY_CALLER_REFERENCE);
    }

    /**
     * Sets the author of the script which is performing the request. This user must be authorized to perform the
     * actions implied by this request if {@link #isCheckRights()} is {@code true}.
     *
     * @param authorReference the author reference
     * @since 10.10RC1
     * @since 10.8.2
     * @since 9.11.9
     */
    public void setAuthorReference(DocumentReference authorReference)
    {
        setProperty(PROPERTY_CALLER_REFERENCE, authorReference);
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
}

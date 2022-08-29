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

import java.util.Collections;
import java.util.List;

import org.xwiki.job.Request;
import org.xwiki.model.reference.EntityReference;

/**
 * A job request that can be used to create a collection of entities using a specified template.
 *
 * @version $Id$
 * @since 7.4M2
 */
public class CreateRequest extends EntityRequest
{
    private static final long serialVersionUID = 1L;

    private static final String PROPERTY_TEMPLATE = "template";

    private static final String PROPERTY_SKIPPED_ENTITIES = "skippedEntities";

    /**
     * Default constructor.
     */
    public CreateRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 14.7RC1
     * @since 14.4.4
     * @since 13.10.9
     */
    public CreateRequest(Request request)
    {
        super(request);
    }

    /**
     * @param templateReference the reference of the entity to use as template
     */
    public void setTemplateReference(EntityReference templateReference)
    {
        this.setProperty(PROPERTY_TEMPLATE, templateReference);
    }

    /**
     * @return the reference of the entity to use as template
     */
    public EntityReference getTemplateReference()
    {
        return this.getProperty(PROPERTY_TEMPLATE);
    }

    /**
     * @return the list of entities that are to be skipped by the job's execution
     */
    public List<EntityReference> getSkippedEntities()
    {
        return this.getProperty(PROPERTY_SKIPPED_ENTITIES, Collections.<EntityReference>emptyList());
    }

    /**
     * @param skippedEntities the list of entities that to skip in the job's execution
     */
    public void setSkippedEntities(List<EntityReference> skippedEntities)
    {
        this.setProperty(PROPERTY_SKIPPED_ENTITIES, skippedEntities);
    }
}

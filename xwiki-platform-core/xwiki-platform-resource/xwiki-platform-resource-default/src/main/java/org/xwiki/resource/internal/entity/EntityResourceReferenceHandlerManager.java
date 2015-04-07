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
package org.xwiki.resource.internal.entity;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.internal.AbstractResourceReferenceHandlerManager;

/**
 * For any passed Resource Reference, find the correct {@link org.xwiki.resource.ResourceReferenceHandler} by finding
 * all of them matching Entity Resource Actions and sorting them by priority.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("entity")
@Singleton
public class EntityResourceReferenceHandlerManager extends AbstractResourceReferenceHandlerManager
{
    @Override
    protected boolean matches(ResourceReferenceHandler handler, ResourceReference reference)
    {
        EntityResourceReference entityResourceReference = (EntityResourceReference) reference;
        return handler.getSupportedResourceReferences().contains(entityResourceReference.getAction());
    }

    @Override
    public void handle(ResourceReference reference) throws ResourceReferenceHandlerException
    {
        handle(reference, EntityResourceAction.class);
    }
}

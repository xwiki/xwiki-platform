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

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerManager;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.entity.EntityResourceAction;

/**
 * Handles Entity Resource References.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("bin")
@Singleton
public class EntityResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    @Inject
    private ResourceReferenceHandlerManager<EntityResourceAction> entityResourceReferenceHandlerManager;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        // At this point in time we want that the "bin" type be handled by the legacy XWikiAction code and thus must
        // not have any ResourceReferenceHandler that handles "bin" Resource Type ATM. This will cause the Routing
        // Filter to bypass the new Resource Reference Handler Servlet and thus call the Struts Servlet (and thus call
        // XWikiAction).
        // Also note that since the StandardExtendedURLResourceTypeResolver returns a "bin" Resource Type for all
        // resource types ATM (in order to handl short urls in the "standard" URL Scheme), we will need to find ways
        // to handle all Resource Types properly once we start having this EntityResourceReferenceHandler handle "bin"
        // Resource Types!
        // In the future, modify this to return: Arrays.asList(EntityResourceReference.TYPE);
        return Collections.emptyList();
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        this.entityResourceReferenceHandlerManager.handle(reference);

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }
}

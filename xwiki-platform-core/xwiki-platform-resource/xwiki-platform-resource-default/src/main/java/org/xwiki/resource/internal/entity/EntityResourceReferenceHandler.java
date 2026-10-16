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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceReferenceHandlerManager;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;

/**
 * Handles Entity Resource References by calling the {@link org.xwiki.resource.ResourceReferenceHandler} registered for
 * their {@link EntityResourceAction}.
 * <p>
 * The context is prepared by the {@link EntityResourceContextInitializer} (when there's one) before calling them,
 * since these Handlers replace the legacy {@code XWikiAction}s which always had an initialized and authenticated XWiki
 * Context. This is not done with {@link org.xwiki.resource.annotations.Authenticate} since it would wait for the
 * targeted wiki to be initialized, instead of displaying the initialization progress.
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

    @Inject
    private ComponentManager componentManager;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Collections.singletonList(EntityResourceReference.TYPE);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        if (!initializeContext(reference)) {
            // The request has already been answered (e.g. with the initialization progress).
            return;
        }

        this.entityResourceReferenceHandlerManager.handle(reference);

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    private boolean initializeContext(ResourceReference reference) throws ResourceReferenceHandlerException
    {
        // There's no initializer when there's no wiki (e.g. in some tests or standalone uses of the Resource API).
        if (!(reference instanceof EntityResourceReference entityReference)
            || !this.componentManager.hasComponent(EntityResourceContextInitializer.class)) {
            return true;
        }

        try {
            return this.componentManager.<EntityResourceContextInitializer>getInstance(
                EntityResourceContextInitializer.class).initialize(entityReference);
        } catch (ComponentLookupException e) {
            throw new ResourceReferenceHandlerException("Failed to locate the Entity Resource Context Initializer", e);
        }
    }
}

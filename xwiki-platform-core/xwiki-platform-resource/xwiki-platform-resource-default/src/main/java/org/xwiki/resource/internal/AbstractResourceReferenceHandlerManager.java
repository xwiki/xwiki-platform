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
package org.xwiki.resource.internal;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.observation.ObservationManager;
import org.xwiki.resource.NotFoundResourceHandlerException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceReferenceHandlerManager;

/**
 * Helper to implement {@link ResourceReferenceHandlerManager}.
 *
 * @param <T> the qualifying element to distinguish a Resource Reference (e.g. Resource Type, Entity Resource Action)
 * @version $Id$
 * @since 6.1M2
 */
public abstract class AbstractResourceReferenceHandlerManager<T> implements ResourceReferenceHandlerManager<T>
{
    /**
     * Used to lookup Resource Handler components. We use the Context Component Manager so that Extensions can
     * contribute Resource Handler.
     */
    @Inject
    @Named("context")
    protected ComponentManager contextComponentManager;

    @Inject
    protected ObservationManager observation;

    @Inject
    protected Logger logger;

    protected abstract boolean matches(ResourceReferenceHandler handler, T resourceReferenceQualifier);

    protected abstract T extractResourceReferenceQualifier(ResourceReference reference);

    @Override
    public void handle(ResourceReference reference) throws ResourceReferenceHandlerException
    {
        // Look for a Handler supporting the Resource Type located in the passed Resource Reference object.
        Set<ResourceReferenceHandler> orderedHandlers =
            getMatchingHandlers(extractResourceReferenceQualifier(reference));

        if (!orderedHandlers.isEmpty()) {
            // Create the Handler chain
            ResourceReferenceHandlerChain chain =
                new DefaultResourceReferenceHandlerChain(orderedHandlers, this.observation);

            // Call the first Handler
            chain.handleNext(reference);
        } else {
            // Resource has not been handled since no Handler was found for it!
            throw new NotFoundResourceHandlerException(reference);
        }
    }

    @Override
    public boolean canHandle(T resourceReferenceQualifier)
    {
        boolean result;
        try {
            result = !getMatchingHandlers(resourceReferenceQualifier).isEmpty();
        } catch (ResourceReferenceHandlerException e) {
            this.logger.warn("Failed to list Resource Reference Handers. Error [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            result = false;
        }
        return result;
    }

    protected Set<ResourceReferenceHandler> getMatchingHandlers(T resourceReferenceQualifier)
        throws ResourceReferenceHandlerException
    {
        // Look for a Handler supporting the Resource Type located in the passed Resource Reference object.
        // TODO: Use caching to avoid having to sort all Handlers at every call.
        Set<ResourceReferenceHandler> orderedHandlers = new TreeSet<>();
        for (ResourceReferenceHandler handler : getHandlers(resourceReferenceQualifier.getClass())) {
            if (matches(handler, resourceReferenceQualifier)) {
                orderedHandlers.add(handler);
            }
        }

        return orderedHandlers;
    }

    protected List<ResourceReferenceHandler> getHandlers(Class typeClass) throws ResourceReferenceHandlerException
    {
        try {
            return this.contextComponentManager
                .getInstanceList(new DefaultParameterizedType(null, ResourceReferenceHandler.class, typeClass));
        } catch (ComponentLookupException e) {
            throw new ResourceReferenceHandlerException("Failed to locate Resource Reference Handler components", e);
        }
    }
}

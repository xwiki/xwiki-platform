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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

import org.xwiki.observation.ObservationManager;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.events.ResourceReferenceHandledEvent;
import org.xwiki.resource.events.ResourceReferenceHandlingEvent;

/**
 * Default chain implementation using a Stack.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class DefaultResourceReferenceHandlerChain implements ResourceReferenceHandlerChain
{
    /**
     * Empty chain.
     */
    public static final DefaultResourceReferenceHandlerChain EMPTY =
        new DefaultResourceReferenceHandlerChain(Collections.emptyList(), null);

    /**
     * Contains all remaining Handlers to execute with Handlers on top executing first.
     */
    private final Queue<ResourceReferenceHandler> handlerStack;

    private final ObservationManager observation;

    /**
     * @param orderedHandlers the sorted list of Handlers to execute
     * @param observation used to send event around executed handlers
     */
    public DefaultResourceReferenceHandlerChain(Collection<ResourceReferenceHandler> orderedHandlers,
        ObservationManager observation)
    {
        this.handlerStack = new LinkedList<>(orderedHandlers);
        this.observation = observation;
    }

    @Override
    public void handleNext(ResourceReference reference) throws ResourceReferenceHandlerException
    {
        if (!this.handlerStack.isEmpty()) {
            ResourceReferenceHandler<?> handler = this.handlerStack.poll();

            if (this.observation != null) {
                this.observation.notify(new ResourceReferenceHandlingEvent(reference), handler);
            }

            ResourceReferenceHandlerException exception = null;
            try {
                handler.handle(reference, this);
            } catch (ResourceReferenceHandlerException e) {
                exception = e;
            } finally {
                if (this.observation != null) {
                    this.observation.notify(new ResourceReferenceHandledEvent(reference), handler, exception);
                }
            }

            // Throw the exception if any
            if (exception != null) {
                throw exception;
            }
        }
    }
}

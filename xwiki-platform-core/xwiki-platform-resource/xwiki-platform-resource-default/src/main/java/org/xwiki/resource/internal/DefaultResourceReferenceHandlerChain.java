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
import java.util.Stack;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;

/**
 * Default chain implementation using a Stack.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultResourceReferenceHandlerChain implements ResourceReferenceHandlerChain
{
    /**
     * Contains all remaining Handlers to execute with Handlers on top executing first.
     */
    private Stack<ResourceReferenceHandler> handlerStack;

    /**
     * @param orderedHandlers the sorted list of Handlers to execute
     */
    public DefaultResourceReferenceHandlerChain(Collection<ResourceReferenceHandler> orderedHandlers)
    {
        this.handlerStack = new Stack<>();
        this.handlerStack.addAll(orderedHandlers);
    }

    @Override
    public void handleNext(ResourceReference reference) throws ResourceReferenceHandlerException
    {
        if (!this.handlerStack.isEmpty()) {
            this.handlerStack.pop().handle(reference, this);
        }
    }
}

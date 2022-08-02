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
package org.xwiki.container.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Request;
import org.xwiki.container.RequestInitializer;
import org.xwiki.container.RequestInitializerException;
import org.xwiki.container.RequestInitializerManager;

/**
 * Default implementation for {@link org.xwiki.container.RequestInitializerManager}.
 *
 * @version $Id$
 * @see org.xwiki.container.RequestInitializerManager
 */
@Component
@Singleton
public class DefaultRequestInitializerManager implements RequestInitializerManager
{
    /**
     * The component manager we used to find all components implementing the
     * {@link org.xwiki.container.RequestInitializer} role.
     */
    @Inject
    private ComponentManager componentManager;

    @Override
    public void initializeRequest(Request request) throws RequestInitializerException
    {
        // Find all request interceptors and call them to initialize the Request
        try {
            for (Object interceptor : this.componentManager.getInstanceList(RequestInitializer.class)) {
                ((RequestInitializer) interceptor).initialize(request);
            }
        } catch (ComponentLookupException e) {
            throw new RequestInitializerException("Failed to initialize request", e);
        }
    }
}

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
package org.xwiki.resource.servlet;

import java.lang.reflect.Type;
import java.util.Collections;

import jakarta.servlet.ServletException;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceHandlerManager;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

/**
 * Decides whether an Entity Resource URL (the {@code bin} Resource Type) must be routed to the
 * {@code resourceReferenceHandler} Servlet.
 * <p>
 * The {@code bin} Resource Type is also what short URLs resolve to, so it covers the URLs of the legacy
 * {@code XWikiAction}s and of the Servlets declared in {@code web.xml} (e.g. {@code rest}, {@code webdav}). Only the
 * URLs whose action has a dedicated Handler must be routed to the {@code resourceReferenceHandler} Servlet. The
 * decision must be taken by the {@link RoutingFilter} because the Servlet cannot hand the request back to the rest of
 * the Filter chain.
 * <p>
 * No XWiki Context is available at this point, so only the Handlers registered for the whole farm are found. The ones
 * registered for a wiki or a user are called by {@code XWikiAction} instead.
 *
 * @version $Id$
 */
final class EntityResourceRouter
{
    private final ComponentManager componentManager;

    /**
     * @param componentManager the Root Component Manager, used to look up the components needed to take the decision
     */
    EntityResourceRouter(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * @param extendedURL the URL of the request, without its Resource Type
     * @return {@code true} if the request must be handled by the {@code resourceReferenceHandler} Servlet
     * @throws ServletException if a component needed to take the decision is missing
     */
    boolean isRouted(ExtendedURL extendedURL) throws ServletException
    {
        ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver = getComponent(
            new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class));
        ResourceReferenceHandlerManager<EntityResourceAction> entityResourceReferenceHandlerManager = getComponent(
            new DefaultParameterizedType(null, ResourceReferenceHandlerManager.class, EntityResourceAction.class));

        EntityResourceReference reference;
        try {
            if (!(resourceReferenceResolver.resolve(extendedURL, EntityResourceReference.TYPE,
                Collections.emptyMap()) instanceof EntityResourceReference entityReference)) {
                return false;
            }
            reference = entityReference;
        } catch (Exception e) {
            // Not a valid Entity Resource URL: let the next Filter or Servlet from web.xml decide what to do with it.
            return false;
        }

        return entityResourceReferenceHandlerManager.canHandle(reference.getAction());
    }

    private <T> T getComponent(Type role) throws ServletException
    {
        try {
            return this.componentManager.getInstance(role);
        } catch (ComponentLookupException e) {
            throw new ServletException(String.format("Failed to locate a component of type [%s]", role), e);
        }
    }
}

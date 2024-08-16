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
package com.xpn.xwiki.web;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.internal.web.LegacyAction;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Executed the right right action depending on the XWiki configuration (for example leading to view action by default
 * if enabled, etc.).
 * <p>
 * While the class is much older, the since annotation was moved to 42.0.0 because it implement a completely different
 * API from Java point of view.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class LegacyActionServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private ResourceTypeResolver<ExtendedURL> typeResolver;

    private ResourceReferenceResolver<ExtendedURL> resolver;

    private ComponentManager rootComponentManager;

    @Override
    public void init() throws ServletException
    {
        super.init();

        // Get the Component Manager which has been initialized first in a Servlet Context Listener.
        this.rootComponentManager =
            (ComponentManager) getServletContext().getAttribute(ComponentManager.class.getName());

        try {
            this.typeResolver = this.rootComponentManager
                .getInstance(new DefaultParameterizedType(null, ResourceTypeResolver.class, ExtendedURL.class));
        } catch (ComponentLookupException e) {
            throw new ServletException("Failed to lookup the resource type resolve for ExtendedURL", e);
        }

        try {
            this.resolver = this.rootComponentManager
                .getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class));
        } catch (ComponentLookupException e) {
            throw new ServletException("Failed to lookup the resource reference resolve for ExtendedURL", e);
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String actionName = getActionName(req);
        LegacyAction action;
        if (this.rootComponentManager.hasComponent(LegacyAction.class, actionName)) {
            try {
                action = this.rootComponentManager.getInstance(LegacyAction.class, actionName);
            } catch (ComponentLookupException e) {
                throw new ServletException("Failed to lookup the action with name [" + actionName + "]", e);
            }
        } else {
            try {
                // we change the name just for the logger in case of problem.
                actionName = "unknown";
                // if a component cannot be found with the given action name, we fallback on the default action
                // which is the UnkwownAction. This one is supposed to display an exception, but note that
                // XWikiAction also provides currently a fallback to the EntityResourceReferenceHandler if there
                // is an existing one for the given action.
                action = this.rootComponentManager.getInstance(LegacyAction.class);
            } catch (ComponentLookupException e) {
                throw new ServletException("Failed to lookup the default action", e);
            }
        }
        try {
            action.execute(req, resp);
        } catch (Exception e) {
            throw new ServletException("Failed to execute the action with name [" + actionName + "]", e);
        }
    }

    private String getActionName(HttpServletRequest req) throws ServletException
    {
        String url = req.getRequestURL().toString();
        try {
            ExtendedURL extendedURL = new ExtendedURL(new URL(url), req.getContextPath());

            ResourceType type = this.typeResolver.resolve(extendedURL, Collections.<String, Object>emptyMap());

            EntityResourceReference entityResourceReference = (EntityResourceReference) this.resolver
                .resolve(extendedURL, type, Collections.<String, Object>emptyMap());

            return entityResourceReference.getAction().getActionName();
        } catch (Exception e) {
            throw new ServletException("Failed to extract the Entity Action from URL [" + url + "]", e);
        }
    }
}

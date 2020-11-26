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

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.stability.Unstable;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.internal.web.LegacyAction;

/**
 * Executed the right right action depending on the XWiki configuration (for example leading to view action by default
 * if enabled, etc.).
 * 
 * @version $Id$
 * @since 13.0RC1
 */
@Unstable
@MultipartConfig
public class LegacyActionServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyActionServlet.class);

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

        if (this.rootComponentManager.hasComponent(LegacyAction.class, actionName)) {
            LegacyAction action;
            try {
                action = this.rootComponentManager.getInstance(LegacyAction.class, actionName);
            } catch (ComponentLookupException e) {
                throw new ServletException("Failed to lookup the action with name [" + actionName + "]", e);
            }

            try {
                action.execute(req, resp);
            } catch (Exception e) {
                throw new ServletException("Failed to execute the action with name [" + actionName + "]", e);
            }
        } else {
            resp.setStatus(404);
            resp.getWriter().write("No action could be find for name [" + actionName + "]");
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

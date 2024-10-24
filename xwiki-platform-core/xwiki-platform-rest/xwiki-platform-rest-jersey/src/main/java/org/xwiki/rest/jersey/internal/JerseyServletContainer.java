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
package org.xwiki.rest.jersey.internal;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.rest.XWikiRestComponent;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;

/**
 * Encapsulate the Jersey {@link ServletContainer} to control it's initialization and reload (when a REST component is
 * registered/unregistered).
 * <p>
 * While the class is much older, the since annotation was moved to 42.0.0 because it implement a completely different
 * API from Java point of view.
 * 
 * @version $Id$
 * @since 42.0.0
 */
@Component(roles = JerseyServletContainer.class)
@Singleton
public class JerseyServletContainer extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    @Inject
    @Named("context")
    private ComponentManager contextComponentManager;

    private final Map<ServletContainer, AtomicInteger> containers = new ConcurrentHashMap<>();

    private volatile ServletContainer container;

    @Override
    public void init() throws ServletException
    {
        // Create and initialize the Jersey servlet
        ServletContainer newContainer = new ServletContainer(createResourceConfig());
        try {
            newContainer.init(JakartaServletBridge.toJavax(getServletConfig()));
        } catch (javax.servlet.ServletException e) {
            throw new ServletException(e);
        }

        // Remember the previous container
        ServletContainer previousContainer = this.container;

        // Create the container counter
        this.containers.put(newContainer, new AtomicInteger());

        // Replace the current container
        this.container = newContainer;

        // Destroy the previous container if it's not used anymore
        if (previousContainer != null) {
            AtomicInteger counter = this.containers.get(previousContainer);

            if (counter == null || counter.get() == 0) {
                forgetAndDestroy(previousContainer);
            }
        }
    }

    /**
     * Restart the Jersey container if it was started already.
     * 
     * @throws ServletException when failing to restart the container
     */
    public void restart() throws ServletException
    {
        if (this.container != null) {
            init();
        }
    }

    private ResourceConfig createResourceConfig()
    {
        ResourceConfig resourceConfig = new ResourceConfig();

        for (ComponentDescriptor<XWikiRestComponent> descriptor : this.contextComponentManager
            .getComponentDescriptorList(XWikiRestComponent.class)) {
            resourceConfig.register(descriptor.getImplementation());
        }

        // Inject ContextResolver in charge of providing a Jackson ObjectMapper which behaves like the Restlet one
        resourceConfig.register(RestletJacksonContextResolver.class);

        return resourceConfig;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
    {
        // Remember the current container used for this request
        ServletContainer requestContainer = this.container;

        // Get the counter associated with this container
        AtomicInteger counter = this.containers.get(this.container);

        // Increment the counter
        counter.incrementAndGet();

        try {
            // Execute the request
            this.container.service(JakartaServletBridge.toJavax(req), JakartaServletBridge.toJavax(res));
        } catch (javax.servlet.ServletException e) {
            throw new ServletException(e);
        } finally {
            // Decrement the counter
            counter.decrementAndGet();

            // If the container used for the request is not the current one anymore and the counter is back to 0,
            // forget and destroy it
            if (this.container != requestContainer && counter.get() == 0) {
                forgetAndDestroy(requestContainer);
            }
        }
    }

    private void forgetAndDestroy(ServletContainer container)
    {
        // Forget the container
        if (this.containers.remove(container) != null) {
            // Destroy the container
            container.destroy();
        }
    }

    @Override
    public void destroy()
    {
        forgetAndDestroy(this.container);
    }

    @Override
    public ServletContext getServletContext()
    {
        return JakartaServletBridge.toJakarta(this.container.getServletContext());
    }
}

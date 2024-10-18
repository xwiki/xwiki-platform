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
package org.xwiki.container.servlet.internal;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.container.RequestInitializerManager;
import org.xwiki.container.servlet.ServletApplicationContext;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.container.servlet.ServletSession;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Singleton
public class DefaultServletContainerInitializer implements ServletContainerInitializer
{
    @Inject
    private ApplicationContextListenerManager applicationContextListenerManager;

    @Inject
    private Container container;

    @Inject
    private Execution execution;

    @Inject
    private ComponentManager componentManager;

    @Override
    public void initializeRequest(HttpServletRequest request, HttpServletResponse response)
        throws ServletContainerException
    {
        // 1) Set the request and the response in the Container. From this point forward request initializers can use
        // the Container object to get any data they want from the Request.
        this.container.setRequest(new ServletRequest(request));
        if (response != null) {
            this.container.setResponse(new ServletResponse(response));
        }

        // 2) Create an empty Execution context so that the Container initializers can put things in the
        // execution context when they execute.
        this.execution.setContext(new ExecutionContext());

        // 3) Call the request initializers to populate the Request further.
        try {
            RequestInitializerManager manager = this.componentManager.getInstance(RequestInitializerManager.class);
            manager.initializeRequest(this.container.getRequest());
        } catch (Exception e) {
            throw new ServletContainerException("Failed to initialize request", e);
        }

        // 4) Call Execution Context initializers to perform further Execution Context initializations
        try {
            ExecutionContextManager manager = this.componentManager.getInstance(ExecutionContextManager.class);
            manager.initialize(this.execution.getContext());
        } catch (Exception e) {
            throw new ServletContainerException("Failed to initialize Execution Context", e);
        }
    }

    // Deprecated

    /**
     * @deprecated use the notion of Environment instead
     */
    @Deprecated(since = "3.5M1")
    @Override
    public void initializeApplicationContext(ServletContext servletContext)
    {
        ApplicationContext applicationContext = new ServletApplicationContext(servletContext, this.componentManager);
        this.container.setApplicationContext(applicationContext);
        this.applicationContextListenerManager.initializeApplicationContext(applicationContext);
    }

    @Override
    @Deprecated(since = "42.0.0")
    public void initializeRequest(javax.servlet.http.HttpServletRequest httpServletRequest, Object xwikiContext)
        throws ServletContainerException
    {
        // 1) Create an empty request. From this point forward request initializers can use the
        // Container object to get any data they want from the Request.
        this.container.setRequest(new ServletRequest(httpServletRequest));

        // 2) Create an empty Execution context so that the Container initializers can put things in the
        // execution context when they execute.
        this.execution.setContext(new ExecutionContext());

        // 3) Bridge with old code to play well with new components. Old code relies on the
        // XWikiContext object whereas new code uses the Container component.
        if (xwikiContext != null) {
            ExecutionContext ec = this.execution.getContext();
            String key = "xwikicontext";
            if (ec.hasProperty(key)) {
                ec.setProperty(key, xwikiContext);
            } else {
                ec.newProperty(key).inherited().initial(xwikiContext).declare();
            }
        }

        // 4) Call the request initializers to populate the Request further.
        try {
            RequestInitializerManager manager = this.componentManager.getInstance(RequestInitializerManager.class);
            manager.initializeRequest(this.container.getRequest());
        } catch (Exception e) {
            throw new ServletContainerException("Failed to initialize request", e);
        }

        // 5) Call Execution Context initializers to perform further Execution Context initializations
        try {
            ExecutionContextManager manager = this.componentManager.getInstance(ExecutionContextManager.class);
            manager.initialize(this.execution.getContext());
        } catch (Exception e) {
            throw new ServletContainerException("Failed to initialize Execution Context", e);
        }
    }

    @Override
    @Deprecated(since = "42.0.0")
    public void initializeRequest(javax.servlet.http.HttpServletRequest httpServletRequest)
        throws ServletContainerException
    {
        initializeRequest(httpServletRequest, null);
    }

    @Override
    @Deprecated(since = "42.0.0")
    public void initializeResponse(javax.servlet.http.HttpServletResponse httpServletResponse)
    {
        this.container.setResponse(new ServletResponse(httpServletResponse));
    }

    @Override
    @Deprecated(since = "42.0.0")
    public void initializeSession(javax.servlet.http.HttpServletRequest httpServletRequest)
    {
        this.container.setSession(new ServletSession(httpServletRequest));
    }
}

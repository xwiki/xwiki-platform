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
 *
 */
package org.xwiki.container.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.container.ApplicationContext;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.container.RequestInitializerException;
import org.xwiki.container.RequestInitializerManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

public class DefaultServletContainerInitializer implements ServletContainerInitializer
{
    private ApplicationContextListenerManager applicationContextListenerManager;

    private RequestInitializerManager requestInitializerManager;

    private ExecutionContextManager executionContextManager;

    private Container container;

    private Execution execution;

    public void initializeApplicationContext(ServletContext servletContext)
    {
        ApplicationContext applicationContext = new ServletApplicationContext(servletContext);
        this.container.setApplicationContext(applicationContext);
        applicationContextListenerManager.initializeApplicationContext(applicationContext);
    }

    public void initializeRequest(HttpServletRequest httpServletRequest, Object xwikiContext)
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
            this.execution.getContext().setProperty("xwikicontext", xwikiContext);
        }

        // 4) Call the request initializers to populate the Request.
        // TODO: This is where the URL should be converted to a XWikiURL and the wiki, space,
        // document, skin and possibly other parameters are put in the Execution Context by proper
        // initializers.
        try {
            this.requestInitializerManager.initializeRequest(this.container.getRequest());
        } catch (RequestInitializerException e) {
            throw new ServletContainerException("Failed to initialize request", e);
        }

        // 5) Call Execution Context initializers to perform further Execution Context initializations
        try {
            this.executionContextManager.initialize(this.execution.getContext());
        } catch (ExecutionContextException e) {
            throw new ServletContainerException("Failed to initialize Execution Context", e);
        }
    }

    public void initializeRequest(HttpServletRequest httpServletRequest) throws ServletContainerException
    {
        initializeRequest(httpServletRequest, null);
    }

    public void initializeResponse(HttpServletResponse httpServletResponse)
    {
        this.container.setResponse(new ServletResponse(httpServletResponse));
    }

    public void initializeSession(HttpServletRequest httpServletRequest)
    {
        this.container.setSession(new ServletSession(httpServletRequest));
    }
}

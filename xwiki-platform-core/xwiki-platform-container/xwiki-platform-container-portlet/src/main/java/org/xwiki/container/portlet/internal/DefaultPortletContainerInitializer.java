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
package org.xwiki.container.portlet.internal;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.portlet.PortletContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.container.RequestInitializerException;
import org.xwiki.container.RequestInitializerManager;
import org.xwiki.container.portlet.PortletApplicationContext;
import org.xwiki.container.portlet.PortletContainerException;
import org.xwiki.container.portlet.PortletContainerInitializer;
import org.xwiki.container.portlet.PortletRequest;
import org.xwiki.container.portlet.PortletResponse;
import org.xwiki.container.portlet.PortletSession;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;

@Component
@Singleton
public class DefaultPortletContainerInitializer implements PortletContainerInitializer
{
    @Inject
    private ApplicationContextListenerManager applicationContextListenerManager;

    @Inject
    private RequestInitializerManager requestInitializerManager;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private Container container;

    @Inject
    private Execution execution;

    @Inject
    private ComponentManager componentManager;

    /**
     * @deprecated use the notion of Environment instead
     */
    @Deprecated(since = "3.5M1")
    @Override
    public void initializeApplicationContext(PortletContext portletContext)
    {
        ApplicationContext applicationContext = new PortletApplicationContext(portletContext, this.componentManager);
        this.container.setApplicationContext(applicationContext);
        this.applicationContextListenerManager.initializeApplicationContext(applicationContext);
    }

    @Override
    public void initializeRequest(javax.portlet.PortletRequest portletRequest, Object xwikiContext)
        throws PortletContainerException
    {
        // 1) Create an empty request. From this point forward request initializers can use the
        // Container object to get any data they want from the Request.
        this.container.setRequest(new PortletRequest(portletRequest));

        // 2) Create en empty Execution context so that the Container initializers can put things in the
        // execution context when they execute.
        this.execution.setContext(new ExecutionContext());

        // 3) Bridge with old code to play well with new components. Old code relies on the
        // XWikiContext object whereas new code uses the ExecutionContext found in the Execution component.
        if (xwikiContext != null) {
            ExecutionContext ec = this.execution.getContext();
            String key = "xwikicontext";
            if (ec.hasProperty(key)) {
                ec.setProperty(key, xwikiContext);
            } else {
                ec.newProperty(key).inherited().initial(xwikiContext).declare();
            }
        }

        // 4) Call the request initializers to populate the Request.
        // TODO: This is where the URL should be converted to a XWikiURL and the wiki, space,
        // document, skin and possibly other parameters are put in the Execution Context by proper
        // initializers.
        try {
            this.requestInitializerManager.initializeRequest(this.container.getRequest());
        } catch (RequestInitializerException e) {
            throw new PortletContainerException("Failed to initialize request", e);
        }

        // 5) Call Execution Context initializers to perform further Execution Context initializations
        try {
            this.executionContextManager.initialize(this.execution.getContext());
        } catch (ExecutionContextException e) {
            throw new PortletContainerException("Failed to initialize Execution Context", e);
        }
    }

    @Override
    public void initializeResponse(javax.portlet.PortletResponse portletResponse)
    {
        this.container.setResponse(new PortletResponse(portletResponse));
    }

    @Override
    public void initializeSession(javax.portlet.PortletRequest portletRequest)
    {
        this.container.setSession(new PortletSession(portletRequest));
    }
}

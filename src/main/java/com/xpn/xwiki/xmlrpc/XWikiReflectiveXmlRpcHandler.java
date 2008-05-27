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
package com.xpn.xwiki.xmlrpc;

import java.lang.reflect.Method;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.TypeConverterFactory;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping;
import org.apache.xmlrpc.server.ReflectiveXmlRpcHandler;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestProcessorFactory;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * This is a custom XmlRpcHandler for replacing the standard one in the Apache XMLRPC framework. The
 * trick here is to redefine the execute() method in order to intercept all the XMLRPC method calls.
 * In this way we can wrap method execution in a try/catch/finally in order to properly handle
 * exceptions/cleanup.
 */
public class XWikiReflectiveXmlRpcHandler extends ReflectiveXmlRpcHandler
{
    public XWikiReflectiveXmlRpcHandler(AbstractReflectiveHandlerMapping mapping,
        TypeConverterFactory typeConverterFactory, Class class1, RequestProcessorFactory factory,
        Method[] methods)
    {
        super(mapping, typeConverterFactory, class1, factory, methods);
    }

    /**
     * Here we intercept all the method call and wrap them in a try/catch/finally block.
     * 
     * @see ReflectiveXmlRpcHandler#execute(XmlRpcRequest)
     */
    @Override
    public Object execute(final XmlRpcRequest request) throws XmlRpcException
    {
        XWikiContext context = null;
        
        try {
            // Here we prepare the XWikiXmlRpcContext object and we put it in the config object so
            // that XMLRPC methods can get it. The config object is the same that is passed to the
            // XWikiXmlRpcHandler when it is initialized. So modifications made here are reflected
            // in the config object that is available to XMLRPC methods through the field
            // XWikiXmlRpcHandler.xwikiXmlRpcHttpRequestConfig.
            XWikiXmlRpcHttpRequestConfig config =
                (XWikiXmlRpcHttpRequestConfig) request.getConfig();
            XWikiRequest xwikiRequest = new XWikiXmlRpcRequest(config.getRequest());
            XWikiResponse xwikiResponse =
                new XWikiXmlRpcResponse((XWikiResponse) XWikiUtils.mock(XWikiResponse.class));
            XWikiServletContext xwikiServletContext =
                new XWikiServletContext(config.getServlet().getServletContext());

            context = Utils.prepareContext("", xwikiRequest, xwikiResponse, xwikiServletContext);
            
            XWiki xwiki = XWiki.getXWiki(context);
            XWikiURLFactory urlf =
                xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
            context.setURLFactory(urlf);

            // Here we set the prepared context
            config.setXmlRpcContext(new XWikiXmlRpcContext(context));

            // Initialize new Container subsystem
            initializeContainerComponent(context);
            
            // This performs the actual XMLRPC method invocation using all the logic we don't care
            // of :)
            return super.execute(request);
        } catch (XWikiException e) {
            throw new XmlRpcException(e.getMessage(), e);
        } finally {
            // Cleanup code
            if (context != null) {
                cleanupContainerComponent(context);
            }
        }
    }

    private void initializeContainerComponent(XWikiContext context)
        throws XmlRpcException
    {
        // Initialize the Container fields (request, response, session).
        // Note that this is a bridge between the old core and the component architecture.
        // In the new component architecture we use ThreadLocal to transport the request, 
        // response and session to components which require them.
        ServletContainerInitializer containerInitializer =
            (ServletContainerInitializer) Utils.getComponent(ServletContainerInitializer.ROLE);

        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(),
                context);
            containerInitializer.initializeResponse(context.getResponse().getHttpServletResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new XmlRpcException("Failed to initialize request/response or session", e);
        }            
    }

    private void cleanupContainerComponent(XWikiContext context)
    {
        Container container = (Container) Utils.getComponent(Container.ROLE);
        // We must ensure we clean the ThreadLocal variables located in the Container 
        // component as otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
    }
}

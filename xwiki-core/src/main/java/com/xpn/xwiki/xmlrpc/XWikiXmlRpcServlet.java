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

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestProcessorFactory;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;

/**
 * This is the XMLRPC servlet that is used as a gateway for serving XMLRPC requests.
 */
public class XWikiXmlRpcServlet extends XmlRpcServlet
{
    private static final long serialVersionUID = 3745689092652029366L;

    @Override
    protected PropertyHandlerMapping newPropertyHandlerMapping(URL url) throws IOException,
        XmlRpcException
    {

        PropertyHandlerMapping mapping = new PropertyHandlerMapping()
        {
            /**
             * This is almost a 1-to-1 copy of the implementation of
             * org.apache.xmlrpc.server.PropertyHandlerMapping.newXmlRpcHandler() defined in its
             * superclass AbstractReflectiveHandlerMapping. We had to do this because in the
             * original implementation of the method newXmlRpcHandler in
             * AbstractReflectiveHandlerMapping, the classes RelfectiveXmlRpcHandler and
             * ReflectiveXmlRpcMetaDataHandler are hard-coded.
             */
            @Override
            protected XmlRpcHandler newXmlRpcHandler(Class pClass, Method[] pMethods)
                throws XmlRpcException
            {

                String[][] sig = getSignature(pMethods);
                String help = getMethodHelp(pClass, pMethods);
                RequestProcessorFactory factory =
                    getRequestProcessorFactoryFactory().getRequestProcessorFactory(pClass);
                if (sig == null || help == null) {
                    return new XWikiReflectiveXmlRpcHandler(this,
                        getTypeConverterFactory(),
                        pClass,
                        factory,
                        pMethods);
                }
                return new XWikiReflectiveXmlRpcMetaDataHandler(this,
                    getTypeConverterFactory(),
                    pClass,
                    factory,
                    pMethods,
                    sig,
                    help);
            }

        };

        mapping.setTypeConverterFactory(getXmlRpcServletServer().getTypeConverterFactory());

        RequestProcessorFactoryFactory factory =
            new RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory()
            {
                protected Object getRequestProcessor(Class pClass, XmlRpcRequest pRequest)
                    throws XmlRpcException
                {
                    Object proc = super.getRequestProcessor(pClass, pRequest);

                    if (proc instanceof XWikiXmlRpcHandler) {
                        XWikiXmlRpcHandler handler = (XWikiXmlRpcHandler) proc;
                        HttpServletRequest request =
                            ((XWikiXmlRpcHttpRequestConfig) pRequest.getConfig()).getRequest();

                        handler.init((XWikiXmlRpcHttpRequestConfig) pRequest.getConfig());
                    }

                    return proc;
                }
            };

        mapping.setRequestProcessorFactoryFactory(factory);
        mapping.load(Thread.currentThread().getContextClassLoader(), url);

        return mapping;
    }

    @Override
    protected XmlRpcServletServer newXmlRpcServer(ServletConfig config) throws XmlRpcException
    {
        XmlRpcServletServer server = new XmlRpcServletServer()
        {

            @Override
            protected XmlRpcHttpRequestConfigImpl newConfig(HttpServletRequest request)
            {
                return new XWikiXmlRpcHttpRequestConfig(XWikiXmlRpcServlet.this, request);
            }
        };

        return server;
    }
}

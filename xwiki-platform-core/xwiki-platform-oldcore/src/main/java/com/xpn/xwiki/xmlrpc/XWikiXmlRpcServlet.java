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
import java.net.URL;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory;
import org.apache.xmlrpc.webserver.XmlRpcServlet;

/**
 * This is the XMLRPC Servlet that is used as a gateway for serving XMLRPC requests.
 * 
 * @version $Id$
 */
public class XWikiXmlRpcServlet extends XmlRpcServlet
{
    private static final long serialVersionUID = 3745689092652029366L;

    @Override
    protected PropertyHandlerMapping newPropertyHandlerMapping(URL url) throws IOException, XmlRpcException
    {
        PropertyHandlerMapping mapping = new PropertyHandlerMapping();
        RequestProcessorFactoryFactory factory = new RequestSpecificProcessorFactoryFactory()
        {
            @Override
            protected Object getRequestProcessor(Class class1, XmlRpcRequest request) throws XmlRpcException
            {
                return super.getRequestProcessor(class1, request);
            }
        };

        mapping.setRequestProcessorFactoryFactory(factory);
        mapping.load(Thread.currentThread().getContextClassLoader(), url);
        return mapping;
    }
}

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
import org.apache.xmlrpc.common.TypeConverterFactory;
import org.apache.xmlrpc.server.AbstractReflectiveHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory.RequestProcessorFactory;

/**
 * This is exactly the same of org.apache.xmlrpc.metadataReflectiveXmlRpcMetaDataHandler. We had to
 * re-implement it because it must extend XWikiReflectiveXmlRpcHandler.
 *
 * @see XWikiReflectiveXmlRpcHandler
 */
class XWikiReflectiveXmlRpcMetaDataHandler extends XWikiReflectiveXmlRpcHandler
{
    private String[][] signatures;

    private String methodHelp;

    public XWikiReflectiveXmlRpcMetaDataHandler(AbstractReflectiveHandlerMapping pMapping,
        TypeConverterFactory pTypeConverterFactory, Class pClass,
        RequestProcessorFactory pFactory, Method[] pMethods, String[][] pSignatures,
        String pMethodHelp)
    {
        super(pMapping, pTypeConverterFactory, pClass, pFactory, pMethods);
        signatures = pSignatures;
        methodHelp = pMethodHelp;
    }

    public String[][] getSignatures() throws XmlRpcException
    {
        return signatures;
    }

    public String getMethodHelp() throws XmlRpcException
    {
        return methodHelp;
    }
}

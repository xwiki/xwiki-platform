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

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;

/**
 * This is an helper class for storing the current HTTP request coming from an XMLRPC client
 * interacting with the XMLRPC endpoint servlet
 */
public class XWikiXmlRpcHttpRequestConfig extends XmlRpcHttpRequestConfigImpl
{
    private HttpServletRequest request;

    private HttpServlet servlet;

    private XWikiXmlRpcContext xmlRpcContext;

    public XWikiXmlRpcHttpRequestConfig(HttpServlet servlet, HttpServletRequest request)
    {
        this.servlet = servlet;
        this.request = request;
    }

    public HttpServletRequest getRequest()
    {
        return request;
    }

    public HttpServlet getServlet()
    {
        return servlet;
    }

    public void setXmlRpcContext(XWikiXmlRpcContext xmlRpcContext)
    {
        this.xmlRpcContext = xmlRpcContext;
    }

    public XWikiXmlRpcContext getXmlRpcContext()
    {
        return xmlRpcContext;
    }

}

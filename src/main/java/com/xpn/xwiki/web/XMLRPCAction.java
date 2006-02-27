/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author sdumitriu
 */
package com.xpn.xwiki.web;

import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.xmlrpc.XmlRpcServer;

import com.xpn.xwiki.xmlrpc.ConfluenceRpcHandler;
import com.xpn.xwiki.xmlrpc.XWikiRpcHandler;
import com.xpn.xwiki.xmlrpc.XWikiXMLRPCContext;
import com.xpn.xwiki.xmlrpc.XWikiXMLRPCRequest;
import com.xpn.xwiki.xmlrpc.XWikiXMLRPCResponse;

public class XMLRPCAction extends Action {
    public ActionForward execute(ActionMapping mapping,
            ActionForm form,
            HttpServletRequest request,
            HttpServletResponse response)
    throws Exception, ServletException {
        XmlRpcServer xmlrpcserver = new XmlRpcServer();
        xmlrpcserver.addHandler("wiki", new XWikiRpcHandler(
                                            new XWikiXMLRPCRequest(request),
                                            new XWikiXMLRPCResponse(response),
                                            new XWikiXMLRPCContext(servlet.getServletContext())));
        xmlrpcserver.addHandler("confluence1", new ConfluenceRpcHandler(
                                            new XWikiXMLRPCRequest(request),
                                            new XWikiXMLRPCResponse(response),
                                            new XWikiXMLRPCContext(servlet.getServletContext())));
        try {
            byte[] result = xmlrpcserver.execute( request.getInputStream() );
            response.setContentType( "text/xml; charset=utf-8" );
            response.setContentLength( result.length );
            OutputStream out = response.getOutputStream();
            out.write( result );
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    	return null;
    }
}

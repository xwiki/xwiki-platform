/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 25 nov. 2003
 * Time: 21:20:04
 */


package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiService;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.xmlrpc.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.velocity.VelocityContext;
import org.apache.xmlrpc.XmlRpcServer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>A simple action that handles the display and editing of an
 * wiki page.. </p>
 *
 * <p>The action support an <i>action</i> URL. The action in the URL
 * controls what this action class does. The following values are supported:</p>
 * <ul>
 *    <li>view - view the Wiki Document
 *   <li>edit - edit the Wiki Document
 *   <li>preview - preview the Wiki Document
 *   <li>save - save the Wiki Document
 * </ul>
 * 
 */
public class ViewEditAction extends XWikiAction
{
    private static final Log log = LogFactory.getLog(ViewEditAction.class);
    private static final long UPLOAD_DEFAULT_MAXSIZE = 10000000L;
    private static final long UPLOAD_DEFAULT_SIZETHRESHOLD = 100000L;

    public ViewEditAction() throws Exception {
        super();
    }

    // --------------------------------------------------------- Public Methods
    /**
     * Handle server requests.
     *
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param req The HTTP request we are processing
     * @param resp The HTTP response we are creating
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet exception occurs
     */
    public ActionForward execute(ActionMapping mapping,
                                 ActionForm form,
                                 HttpServletRequest req,
                                 HttpServletResponse resp)
            throws Exception, ServletException
    {
        String action = mapping.getName();

        if (action.equals("xmlrpc")) {
            renderXMLRPC(req, resp);
            return null;
        }

        MonitorPlugin monitor = null;
        try {
            XWikiRequest request = new XWikiServletRequest(req);
            XWikiResponse response = new XWikiServletResponse(resp);
            XWikiContext context = Utils.prepareContext(action, request, response, new XWikiServletContext(servlet.getServletContext()));
            XWikiService xwikiservice = new XWikiService();

            // Add the form to the context
            context.setForm((XWikiForm) form);

            // We should not go further for the Database Status
            // To make sure we don't have more database connections
            if (action.equals("status")) {
                String renderResult = xwikiservice.renderStatus(context);
                String page = Utils.getPage(request, renderResult);
                Utils.parseTemplate(page, !page.equals("direct"), context);
                return null;
            }

            XWiki xwiki = XWiki.getXWiki(context);
            // Any error before this will be treated using a redirection to an error page

            // Start monitoring timer
            monitor = (MonitorPlugin) xwiki.getPlugin("monitor", context);
            if (monitor!=null)
              monitor.startRequest("", action, context.getURL());
            if (monitor!=null)
             monitor.startTimer("request");

            VelocityContext vcontext = null;
            // Prepare velocity context
            vcontext = XWikiVelocityRenderer.prepareContext(context);

            try {
                // Prepare documents and put them in the context
                if (xwiki.prepareDocuments(request, context, vcontext)==false)
                    return null;

                if (monitor!=null)
                 monitor.setWikiPage(context.getDoc().getFullName());

                String renderResult = null;

                // Determine what to do
                if (action.equals("view")) {
                    renderResult = xwikiservice.renderView(context);
                }
                else if ( action.equals("inline")) {
                    renderResult = xwikiservice.renderInline(context);
                }
                else if ( action.equals("edit") ) {
                    renderResult = xwikiservice.renderEdit(context);
                }
                else if ( action.equals("preview")) {
                    renderResult = xwikiservice.renderPreview(context);
                }
                else if (action.equals("save")) {
                    xwikiservice.actionSave(context);
                }
                else if (action.equals("rollback")) {
                    xwikiservice.actionRollback(context);
                }
                else if (action.equals("cancel")) {
                    xwikiservice.actionCancel(context);
                }
                else if (action.equals("delete")) {
                    if (xwikiservice.actionDelete(context))
                        renderResult = xwikiservice.renderDelete(context);
                }
                else if (action.equals("pdf")) {
                    renderResult = xwikiservice.renderPDF(context);
                }
                else if (action.equals("propupdate")) {
                    xwikiservice.actionPropupdate(context);
                }
                else if (action.equals("propadd")) {
                    xwikiservice.actionPropadd(context);
                }
                else if (action.equals("objectadd")) {
                    xwikiservice.actionObjectadd(context);
                }
                else if (action.equals("commentadd")) {
                    xwikiservice.actionCommentadd(context);
                }
                else if (action.equals("objectremove")) {
                    xwikiservice.actionObjectremove(context);
                }
                else if (action.equals("download")) {
                    renderResult = xwikiservice.renderDownload(context);
                }
                else if (action.equals("attach")) {
                    renderResult = xwikiservice.renderAttach(context);
                }
                else if (action.equals("upload")) {
                    xwikiservice.actionUpload(context);
                }
                else if (action.equals("delattachment")) {
                    xwikiservice.actionDelattachment(context);
                }
                else if (action.equals("skin")) {
                    if (xwikiservice.actionSkin(context))
                        renderResult = xwikiservice.renderSkin(context);
                }
                else if (action.equals("login")) {
                    renderResult = xwikiservice.renderLogin(context);
                }
                else if (action.equals("loginerror")) {
                    renderResult = xwikiservice.renderLoginerror(context);
                }
                else if (action.equals("logout")) {
                    xwikiservice.actionLogout(context);
                }

                if (renderResult!=null) {
                    String page = Utils.getPage(request, renderResult);
                    Utils.parseTemplate(page, !page.equals("direct"), context);
                }
                return null;
            } catch (Throwable e) {
                if (!(e instanceof XWikiException)) {
                    e = new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_UNKNOWN,
                            "Uncaught exception", e);
                }

                vcontext.put("exp", e);
                try {
                    XWikiException xex = (XWikiException) e;
                    if (xex.getCode()==XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                        String page = Utils.getPage(request, "accessdenied");
                        Utils.parseTemplate(page, context);
                        return null;
                    } else if (xex.getCode()==XWikiException.ERROR_XWIKI_USER_INACTIVE) {
                        String page = Utils.getPage(request, "userinactive");
                        Utils.parseTemplate(page, context);
                        return null;
                    }

                    if (log.isWarnEnabled()) {
                           log.warn("Uncaught exception: " + e.getMessage(), e);
                    }
                    Utils.parseTemplate(Utils.getPage(request, "exception"), context);
                    return null;
                } catch (Exception e2) {
                    // I hope this never happens
                    e.printStackTrace();
                    e2.printStackTrace();
                    return null;
                }
            } finally {

                // Let's make sure we have flushed content and closed
                try {
                     response.getWriter().flush();
                } catch (Throwable e) {
                }

                if (monitor!=null)
                 monitor.endTimer("request");

                if (monitor!=null)
                 monitor.startTimer("notify");

                // Let's handle the notification and make sure it never fails
                try {
                    xwiki.getNotificationManager().verify(context.getDoc(), action, context);
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                if (monitor!=null)
                 monitor.endTimer("notify");

                // Make sure we cleanup database connections
                // There could be cases where we have some
                if ((context!=null)&&(xwiki!=null)) {
                    xwiki.getStore().cleanUp(context);
                }
            }
        } finally {
            // End request
            if (monitor!=null)
                monitor.endRequest();

            MDC.remove("url");
        }
    }

    public void renderXMLRPC(HttpServletRequest request, HttpServletResponse response) {
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
    }

}


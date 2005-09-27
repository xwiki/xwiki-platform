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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.render.XWikiVelocityRenderer;

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
public abstract class XWikiAction extends Action
{

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
//        String action = mapping.getName();

        MonitorPlugin monitor = null;
        FileUploadPlugin fileupload = null;
        XWikiContext context = null;
        try {
            XWikiRequest request = new XWikiServletRequest(req);
            XWikiResponse response = new XWikiServletResponse(resp);
            context = Utils.prepareContext(mapping.getName(), request, response,
            		new XWikiServletContext(servlet.getServletContext()));

            // Add the form to the context
            context.setForm((XWikiForm) form);
            XWiki xwiki = XWiki.getXWiki(context);
            
            // Parses multipart so that parms in multipart are available for all actions
            fileupload = handleMultipart(req, context);

            XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
            context.setURLFactory(urlf);

            // Any error before this will be treated using a redirection to an error page

            // Start monitoring timer
            monitor = (MonitorPlugin) xwiki.getPlugin("monitor", context);
            if (monitor!=null)
              monitor.startRequest("", mapping.getName(), context.getURL());
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

                if (action(context)) {
                	renderResult = render(context);
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

                    Log log = LogFactory.getLog(XWikiAction.class);
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
                    xwiki.getNotificationManager().verify(context.getDoc(), mapping.getName(), context);
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
            if (fileupload!=null)
                fileupload.cleanFileList(context);
 
            MDC.remove("url");
        }
    }
    
    private FileUploadPlugin handleMultipart(HttpServletRequest request, XWikiContext context)
    {
        FileUploadPlugin fileupload = null;
        try
        {
            if (request instanceof MultipartRequestWrapper)
            {
                fileupload = new FileUploadPlugin("fileupload", "fileupload", context);
                fileupload.loadFileList(context);
                context.put("fileuploadplugin", fileupload);
                MultipartRequestWrapper mpreq = (MultipartRequestWrapper) request;
                List fileItems = fileupload.getFileItems(context);
                for (Iterator iter = fileItems.iterator(); iter.hasNext();)
                {
                    FileItem item = (FileItem) iter.next();
                    if (item.isFormField())
                    {
                        String sName = item.getFieldName();
                        String sValue = item.getString();
                        mpreq.setParameter(sName, sValue);
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return fileupload;
    }

    public String getRealPath(String path) {
        return servlet.getServletContext().getRealPath(path);
    }
        
    // hook
    public boolean action(XWikiContext context) throws XWikiException {
    	return true;
    }
    
    // hook
    public String render(XWikiContext context) throws XWikiException {
    	return null;
    }
    
    protected void handleRevision(XWikiContext context) throws XWikiException {
        String rev = context.getRequest().getParameter("rev");
        if (rev!=null) {
            context.put("rev", rev);
            XWikiDocument doc = (XWikiDocument) context.get("doc");
            XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
            XWikiDocument rdoc = context.getWiki().getDocument(doc, rev, context);
            XWikiDocument rtdoc = context.getWiki().getDocument(tdoc, rev, context);
            context.put("tdoc", rtdoc);
            context.put("cdoc", rdoc);
            context.put("doc", rdoc);
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("doc", new Document(rdoc, context));
            vcontext.put("cdoc", vcontext.get("doc"));
            vcontext.put("tdoc", new Document(rtdoc, context));
        }
    }

    protected void sendRedirect(XWikiResponse response, String page) throws XWikiException {
        try {
            if (page!=null) {
            	response.sendRedirect(page);
            }
        } catch (IOException e) {
            Object[] args = { page };
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_REDIRECT_EXCEPTION,
                    "Exception while sending redirect to page {0}", e, args);
        }
    }
}

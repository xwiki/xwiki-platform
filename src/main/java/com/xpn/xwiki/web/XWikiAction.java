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

package com.xpn.xwiki.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.velocity.VelocityContext;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.ActionExecutionEvent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.render.XWikiVelocityRenderer;

/**
 * <p>
 * Root class for most XWiki actions. It provides a common framework that allows actions to execute
 * just the specific action code, handling the extra activities, such as preparing the context and
 * retrieving the document corresponding to the URL.
 * </p>
 * <p>
 * It defines two methods, {@link #action(XWikiContext)} and {@link #render(XWikiContext)}, that
 * should be overridden by specific actions. {@link #action(XWikiContext)} should contain the
 * processing part of the action. {@link #render(XWikiContext)} should return the name of a template
 * that should be rendered, or manually write to the {@link XWikiResponse response} stream.
 * </p>
 * <p>
 * Serving a request goes through the following phases:
 * </p>
 * <ul>
 * <li>Wrapping the request and response object in XWiki specific wrappers</li>
 * <li>Prepare the request {@link XWikiContext XWiki-specific context}</li>
 * <li>Initialize/retrieve the XWiki object corresponding to the requested wiki</li>
 * <li>Handle file uploads</li>
 * <li>Prepare the velocity context</li>
 * <li>Prepare the document objects corresponding to the requested URL</li>
 * <li>Send action pre-notifications to listeners</li>
 * <li>Run the overridden {@link #action(XWikiContext)}</li>
 * <li>If {@link #action(XWikiContext)} returns true, run the overridden
 * {@link #render(XWikiContext)}</li>
 * <li>If {@link #render(XWikiContext)} returned a string (template name), render the template with
 * that name</li>
 * <li>Send action post-notifications to listeners</li>
 * </ul>
 * <p>
 * During this process, also handle specific errors, like when a document does not exist, or the
 * user does not have the right to perform the current action.
 * </p>
 */
public abstract class XWikiAction extends Action
{
    private static final Log LOG = LogFactory.getLog(XWikiAction.class);

    // --------------------------------------------------------- Public Methods

    /**
     * Handle server requests.
     * 
     * @param mapping The ActionMapping used to select this instance
     * @param form The optional ActionForm bean for this request (if any)
     * @param req The HTTP request we are processing
     * @param resp The HTTP response we are creating
     * @throws IOException if an input/output error occurs
     * @throws ServletException if a servlet exception occurs
     */
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req,
        HttpServletResponse resp) throws Exception, ServletException
    {
        String action = mapping.getName();

        MonitorPlugin monitor = null;
        FileUploadPlugin fileupload = null;
        XWikiContext context = null;
        String docName = "";

        try {
            XWikiRequest request = new XWikiServletRequest(req);
            XWikiResponse response = new XWikiServletResponse(resp);
            context =
                Utils.prepareContext(mapping.getName(), request, response,
                    new XWikiServletContext(servlet.getServletContext()));

            // Add the form to the context
            context.setForm((XWikiForm) form);
            XWiki xwiki = null;
            try {
                xwiki = XWiki.getXWiki(context);
            } catch (XWikiException e) {
                if (e.getCode() == XWikiException.ERROR_XWIKI_DOES_NOT_EXIST) {
                    // redirect
                    String redirect = context.getWiki().Param("xwiki.virtual.redirect");
                    response.sendRedirect(redirect);
                    return null;
                } else {
                    throw e;
                }
            }

            // Start monitoring timer
            monitor = (MonitorPlugin) xwiki.getPlugin("monitor", context);
            if (monitor != null) {
                monitor.startRequest("", mapping.getName(), context.getURL());
                monitor.startTimer("multipart");
            }
            // Parses multipart so that params in multipart are available for all actions
            fileupload = Utils.handleMultipart(req, context);
            if (monitor != null) {
                monitor.endTimer("multipart");
            }

            XWikiURLFactory urlf =
                xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
            context.setURLFactory(urlf);
            String sajax = request.get("ajax");
            boolean ajax = false;
            if (sajax != null && !sajax.trim().equals("") && !sajax.equals("0")) {
                ajax = true;
            }
            context.put("ajax", new Boolean(ajax));

            // Any error before this will be treated using a redirection to an error page

            if (monitor != null) {
                monitor.startTimer("request");
            }

            VelocityContext vcontext = null;
            // Prepare velocity context
            vcontext = XWikiVelocityRenderer.prepareContext(context);

            try {
                // Prepare documents and put them in the context
                if (xwiki.prepareDocuments(request, context, vcontext) == false) {
                    return null;
                }

                if (monitor != null) {
                    monitor.setWikiPage(context.getDoc().getFullName());
                }

                // Let's handle the notification and make sure it never fails
                if (monitor != null) {
                    monitor.startTimer("prenotify");
                }
                try {
                    xwiki.getNotificationManager().preverify(context.getDoc(), mapping.getName(),
                        context);
                } catch (Throwable e) {
                    LOG.error("Exception while pre-notifying", e);
                }
                if (monitor != null) {
                    monitor.endTimer("prenotify");
                }

                String renderResult = null;
                XWikiDocument doc = context.getDoc();
                docName = doc.getFullName();
                if (action(context)) {
                    renderResult = render(context);
                }

                if (renderResult != null) {
                    if ((doc.isNew() && ("view".equals(action) || "delete".equals(action)))
                        && !"recyclebin".equals(request.get("viewer"))) {
                        String page = Utils.getPage(request, "docdoesnotexist");
                        Utils.parseTemplate(page, context);
                    } else {
                        String page = Utils.getPage(request, renderResult);
                        Utils.parseTemplate(page, !page.equals("direct"), context);
                    }
                }
                return null;
            } catch (Throwable e) {
                if (e instanceof IOException) {
                    e =
                        new XWikiException(XWikiException.MODULE_XWIKI_APP,
                            XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                            "Exception while sending response",
                            e);
                }

                if (!(e instanceof XWikiException)) {
                    e =
                        new XWikiException(XWikiException.MODULE_XWIKI_APP,
                            XWikiException.ERROR_XWIKI_UNKNOWN,
                            "Uncaught exception",
                            e);
                }

                try {
                    XWikiException xex = (XWikiException) e;
                    if (xex.getCode() == XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION) {
                        // Connection aborted, simply ignore this.
                        LOG.error("Connection aborted");
                        // We don't write any other message, as the connection is broken, anyway.
                        return null;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                        Utils.parseTemplate(context.getWiki().Param("xwiki.access_exception",
                            "accessdenied"), context);
                        return null;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_USER_INACTIVE) {
                        Utils.parseTemplate(context.getWiki().Param("xwiki.user_exception",
                            "userinactive"), context);
                        return null;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND) {
                        context.put("message", "attachmentdoesnotexist");
                        Utils.parseTemplate(context.getWiki().Param("xwiki.attachment_exception",
                            "attachmentdoesnotexist"), context);
                        return null;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION) {
                        vcontext.put("message", context.getMessageTool().get(
                            "platform.core.invalidUrl"));
                        xwiki.setPhonyDocument(xwiki.getDefaultWeb(context) + "."
                            + xwiki.getDefaultPage(context), context, vcontext);
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        Utils.parseTemplate(context.getWiki().Param(
                            "xwiki.invalid_url_exception", "error"), context);
                        return null;
                    }
                    vcontext.put("exp", e);
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Uncaught exception: " + e.getMessage(), e);
                    }
                    Utils.parseTemplate(Utils.getPage(request, "exception"), context);
                    return null;
                } catch (XWikiException ex) {
                    if (ex.getCode() == XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION) {
                        LOG.error("Connection aborted");
                    }
                } catch (Exception e2) {
                    // I hope this never happens
                    LOG.error("Uncaught exceptions (inner): ", e);
                    LOG.error("Uncaught exceptions (outer): ", e2);
                }
                return null;
            } finally {
                // Let's make sure we have flushed content and closed
                try {
                    response.getWriter().flush();
                } catch (Throwable e) {
                    // This might happen if the connection was closed, for example.
                    // If we can't flush, then there's nothing more we can send to the client.
                }

                if (monitor != null) {
                    monitor.endTimer("request");
                    monitor.startTimer("notify");
                }

                // Let's handle the notification and make sure it never fails
                // This is the old notification mechanism. It is kept here because it is in a
                // deprecation stage. It will be removed later.
                try {
                    xwiki.getNotificationManager().verify(context.getDoc(), mapping.getName(),
                        context);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                // This is the new notification mechanism, implemented as a Plexus Component.
                // For the moment we're sending the XWiki context as the data, but this will be
                // changed in the future, when the whole platform will be written using components
                // and there won't be a need for the context.
                try {
                    ObservationManager om =
                        (ObservationManager) Utils.getComponent(ObservationManager.ROLE, null,
                            context);
                    om.notify(new ActionExecutionEvent(mapping.getName()), context.getDoc(), context);
                } catch (Throwable ex) {
                    LOG.error("Cannot send action notifications for document [" + docName
                        + " using action [" + action + "]", ex);
                }

                if (monitor != null) {
                    monitor.endTimer("notify");
                }

                // Make sure we cleanup database connections
                // There could be cases where we have some
                if ((context != null) && (xwiki != null)) {
                    xwiki.getStore().cleanUp(context);
                }
            }
        } finally {
            // End request
            if (monitor != null)
                monitor.endRequest();
            if (fileupload != null)
                fileupload.cleanFileList(context);

            MDC.remove("url");
        }
    }

    public String getRealPath(String path)
    {
        return servlet.getServletContext().getRealPath(path);
    }

    // hook
    public boolean action(XWikiContext context) throws XWikiException
    {
        return true;
    }

    // hook
    public String render(XWikiContext context) throws XWikiException
    {
        return null;
    }

    protected void handleRevision(XWikiContext context) throws XWikiException
    {
        String rev = context.getRequest().getParameter("rev");
        if (rev != null) {
            context.put("rev", rev);
            XWikiDocument doc = (XWikiDocument) context.get("doc");
            XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
            XWikiDocument rdoc =
                (!doc.getLanguage().equals(tdoc.getLanguage())) ? doc : context.getWiki()
                    .getDocument(doc, rev, context);
            XWikiDocument rtdoc =
                (doc.getLanguage().equals(tdoc.getLanguage())) ? rdoc : context.getWiki()
                    .getDocument(tdoc, rev, context);
            context.put("tdoc", rtdoc);
            context.put("cdoc", rdoc);
            context.put("doc", rdoc);
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("doc", rdoc.newDocument(context));
            vcontext.put("cdoc", vcontext.get("doc"));
            vcontext.put("tdoc", rtdoc.newDocument(context));
        }
    }

    protected void sendRedirect(XWikiResponse response, String page) throws XWikiException
    {
        try {
            if (page != null) {
                response.sendRedirect(page);
            }
        } catch (IOException e) {
            Object[] args = {page};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_REDIRECT_EXCEPTION,
                "Exception while sending redirect to page {0}",
                e,
                args);
        }
    }
}

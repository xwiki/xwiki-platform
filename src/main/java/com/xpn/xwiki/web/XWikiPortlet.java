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
 * @author ludovic
 * @author namphunghai
 * @author davidbrady
 * @author sdumitriu
 */

package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.plugin.charts.actions.ChartingAction;
import com.xpn.xwiki.plugin.charts.actions.PreviewChartAction;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.octo.captcha.module.struts.image.RenderImageCaptchaAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.apache.velocity.VelocityContext;

import javax.portlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class XWikiPortlet extends GenericPortlet {
    protected final Log logger = LogFactory.getLog(getClass());

    private String name = "XWiki Portlet";
    public static final PortletMode CONFIG_PORTLET_MODE = new PortletMode("config");
    public static final String ROOT_SPACE_PARAM_NAME = "rootSpace";

    protected String getTitle(RenderRequest renderRequest) {
        return name;
    }

    protected XWikiContext prepareContext(String action, XWikiRequest request, XWikiResponse response, XWikiEngineContext engine_context) throws XWikiException {
        return Utils.prepareContext(action, request, response, engine_context);
    }

    protected HttpServletRequest processMultipart(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return (request);
        }

        String contentType = request.getContentType();

        if ((contentType != null)
                && contentType.startsWith("multipart/form-data")) {
            return (new MultipartRequestWrapper(request));
        } else {
            return (request);
        }
    }

    protected boolean prepareAction(String action, XWikiRequest request, XWikiResponse response,
                                    XWikiEngineContext engine_context, XWikiContext context) throws XWikiException, IOException {
        XWiki xwiki = XWiki.getXWiki(context);

        FileUploadPlugin fileupload = Utils.handleMultipart(processMultipart(request.getHttpServletRequest()), context);

        XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
        context.setURLFactory(urlf);
        VelocityContext vcontext = XWikiVelocityRenderer.prepareContext(context);
        return xwiki.prepareDocuments(request, context, vcontext);
    }

    protected void cleanUp(XWikiContext context) {
        try {
            FileUploadPlugin fileupload = (FileUploadPlugin) context.get("fileuploadplugin");
            if (fileupload != null)
                fileupload.cleanFileList(context);

            XWiki xwiki = (context != null) ? context.getWiki() : null;
            // Make sure we cleanup database connections
            // There could be cases where we have some
            if ((context != null) && (xwiki != null)) {
                if (xwiki.getStore() != null)
                    xwiki.getStore().cleanUp(context);
            }
        } finally {
            MDC.remove("url");
        }
    }

    protected void handleException(XWikiRequest request, XWikiResponse response, Throwable e, XWikiContext context) {

        if (!(e instanceof XWikiException)) {
            e = new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Uncaught exception", e);
        }

        VelocityContext vcontext = ((VelocityContext) context.get("vcontext"));
        if (vcontext == null) {
            vcontext = new VelocityContext();
            context.put("vcontext", vcontext);
        }
        vcontext.put("exp", e);

        try {
            XWikiException xex = (XWikiException) e;
            if (xex.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                String page = Utils.getPage(request, "accessdenied");
                Utils.parseTemplate(page, context);
                return;
            } else if (xex.getCode() == XWikiException.ERROR_XWIKI_USER_INACTIVE) {
                String page = Utils.getPage(request, "userinactive");
                Utils.parseTemplate(page, context);
                return;
            }
            Utils.parseTemplate(Utils.getPage(request, "exception"), context);
        } catch (Exception e2) {
            // I hope this never happens
            e.printStackTrace();
            e2.printStackTrace();
        }
    }

    protected void doDispatch(RenderRequest aRenderRequest, RenderResponse aRenderResponse) throws PortletException, IOException {
        WindowState windowState = aRenderRequest.getWindowState();
        if (!windowState.equals(WindowState.MINIMIZED) && aRenderRequest.getPortletMode().equals(CONFIG_PORTLET_MODE)) {
            doView(aRenderRequest, aRenderResponse);
        } else {
            super.doDispatch(aRenderRequest, aRenderResponse);
        }
    }

    public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) throws PortletException, IOException {
        WindowState windowState = actionRequest.getWindowState();
        if (!windowState.equals(WindowState.MINIMIZED) && actionRequest.getPortletMode().equals(CONFIG_PORTLET_MODE)) {
            handleConfigForm(actionRequest, actionResponse);
        } else {
            XWikiContext context = null;
            XWikiRequest request = new XWikiPortletRequest(actionRequest);
            XWikiResponse response = new XWikiPortletResponse(actionResponse);
            XWikiEngineContext engine_context = new XWikiPortletContext(actionRequest.getPortletSession().getPortletContext());

            try {
                String action = request.getParameter("action");
                if ((action == null) || (action.equals("")))
                    action = "view";

                context = prepareContext(action, request, response, engine_context);
                if (prepareAction(action, request, response, engine_context, context) == false)
                    return;

//				XWikiService xwikiservice = new XWikiService();


                XWikiForm form = null;

                if (action.equals("save"))
                    form = new EditForm();
                else if (action.equals("lock"))
                    form = new EditForm();
                else if (action.equals("cancel"))
                    form = new EditForm();
                else if (action.equals("rollback"))
                    form = new RollbackForm();
                else if (action.equals("objectadd"))
                    form = new ObjectAddForm();
                else if (action.equals("commentadd"))
                    form = new ObjectAddForm();
                else if (action.equals("objectremove"))
                    form = new ObjectRemoveForm();
                else if (action.equals("propadd"))
                    form = new PropAddForm();

                if (form != null) {
                    form.reset(null, request);
                    context.setForm(form);
                }

                if (action.equals("save")) {
                    (new SaveAction()).action(context);
                } else if (action.equals("rollback")) {
                    (new RollbackAction()).action(context);
                } else if (action.equals("cancel")) {
                    (new CancelAction()).action(context);
                } else if (action.equals("lock")) {
                    (new LockAction()).action(context);
                } else if (action.equals("delete")) {
                    (new DeleteAction()).action(context);
                } else if (action.equals("propupdate")) {
                    (new PropUpdateAction()).action(context);
                } else if (action.equals("propadd")) {
                    (new PropAddAction()).action(context);
                } else if (action.equals("objectadd")) {
                    (new ObjectAddAction()).action(context);
                } else if (action.equals("commentadd")) {
                    (new CommentAddAction()).action(context);
                } else if (action.equals("objectremove")) {
                    (new ObjectRemoveAction()).action(context);
                } else if (action.equals("upload")) {
                    (new UploadAction()).action(context);
                } else if (action.equals("delattachment")) {
                    (new DeleteAttachmentAction()).action(context);
                } else if (action.equals("skin")) {
                    (new SkinAction()).action(context);
                } else if (action.equals("logout")) {
                    (new LogoutAction()).action(context);
                } else if (action.equals("register")) {
                    (new RegisterAction()).action(context);
                } else if (action.equals("inline")) {
                    (new InlineAction()).action(context);
                }
            } catch (Throwable e) {
                handleException(request, response, e, context);
            } finally {
                cleanUp(context);
            }
        }
    }

    private void handleConfigForm(ActionRequest actionRequest, ActionResponse actionResponse) throws PortletException, IOException {
        PortletPreferences preferences = actionRequest.getPreferences();
        String rootSpace = actionRequest.getParameter(ROOT_SPACE_PARAM_NAME);
        preferences.setValue(XWikiPortletRequest.ROOT_SPACE_PREF_NAME, rootSpace);
        actionResponse.setPortletMode(PortletMode.VIEW);
        preferences.store();
        if (logger.isDebugEnabled()) {
            logger.debug("New root space is [" + rootSpace + "]");
        }

    }

    protected void doView(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException, IOException {
        XWikiContext context = null;
        XWikiRequest request = new XWikiPortletRequest(renderRequest);
        XWikiResponse response = new XWikiPortletResponse(renderResponse);
        XWikiEngineContext engine_context = new XWikiPortletContext(renderRequest.getPortletSession().getPortletContext());
        String action = null;

        try {
            action = request.getParameter("action");
            if ((action == null) || (action.equals(""))) {
                action = renderRequest.getPortletMode().equals(CONFIG_PORTLET_MODE)
                        ? "portletConfig"
                        : "view";
            }
            context = prepareContext(action, request, response, engine_context);
            if (prepareAction(action, request, response, engine_context, context) == false)
                return;

            XWikiForm form = null;

            if (action.equals("edit")
                    || action.equals("inline"))
                form = new EditForm();
            else if (action.equals("preview"))
                form = new EditForm();

            if (form != null) {
                form.reset(null, request);
                context.setForm(form);
            }

            String renderResult = null;
            // Determine what to do
            if (action.equals("view")) {
                renderResult = (new ViewAction()).render(context);
            } else if (action.equals("viewrev")) {
                renderResult = (new ViewrevAction()).render(context);
            } else if (action.equals("inline")) {
                renderResult = (new InlineAction()).render(context);
            } else if (action.equals("edit")) {
                renderResult = (new EditAction()).render(context);
            } else if (action.equals("preview")) {
                renderResult = (new PreviewAction()).render(context);
            } else if (action.equals("delete")) {
                renderResult = (new DeleteAction()).render(context);
            } else if (action.equals("download")) {
                renderResult = (new DownloadAction()).render(context);
            } else if (action.equals("downloadrev")) {
                renderResult = (new DownloadRevAction()).render(context);
            } else if (action.equals("viewattachrev")) {
                renderResult = (new ViewAttachRevAction()).render(context);
            } else if (action.equals("dot")) {
                renderResult = (new DotAction()).render(context);
            } else if (action.equals("svg")) {
                renderResult = (new SVGAction()).render(context);
            } else if (action.equals("attach")) {
                renderResult = (new AttachAction()).render(context);
            } else if (action.equals("login")) {
                renderResult = (new LoginAction()).render(context);
            } else if (action.equals("loginsubmit")) {
                renderResult = (new LoginSubmitAction()).render(context);
            } else if (action.equals("loginerror")) {
                renderResult = (new LoginErrorAction()).render(context);
            } else if (action.equals("register")) {
                renderResult = (new RegisterAction()).render(context);
            } else if (action.equals("skin")) {
                renderResult = (new SkinAction()).render(context);
            } else if (action.equals("export")) {
                renderResult = (new ExportAction()).render(context);
            } else if (action.equals("import")) {
                renderResult = (new ImportAction()).render(context);
            } else if (action.equals("portletConfig")) {
                renderResult = "portletConfig";
            } 
            if (renderResult != null) {
                String page = Utils.getPage(request, renderResult);
                Utils.parseTemplate(page, context);
            }
        } catch (Throwable e) {
            if (logger.isWarnEnabled()) {
                logger.warn("oops", e);
            }

            handleException(request, response, e, context);
        } finally {
            // Let's make sure we have flushed content and closed
            try {
                response.getWriter().flush();
            } catch (Throwable e) {
            }

            // / Let's handle the notification and make sure it never fails
            try {
                context.getWiki().getNotificationManager().verify(context.getDoc(), action, context);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            cleanUp(context);
        }
    }

    protected void doEdit(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException, IOException {
        super.doEdit(renderRequest, renderResponse);
    }

}

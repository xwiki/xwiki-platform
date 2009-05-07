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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import org.xwiki.velocity.VelocityManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.MDC;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.apache.velocity.VelocityContext;
import org.xwiki.container.ApplicationContextListenerManager;
import org.xwiki.container.Container;
import org.xwiki.container.portlet.PortletContainerException;
import org.xwiki.container.portlet.PortletContainerInitializer;
import org.xwiki.context.Execution;

import javax.portlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

public class XWikiPortlet extends GenericPortlet
{
    protected final Log logger = LogFactory.getLog(getClass());

    private String name = "XWiki Portlet";
    public static final PortletMode CONFIG_PORTLET_MODE = new PortletMode("config");
    public static final String ROOT_SPACE_PARAM_NAME = "rootSpace";

    protected String getTitle(RenderRequest renderRequest)
    {
        return name;
    }

    protected HttpServletRequest processMultipart(HttpServletRequest request)
    {
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
        XWikiEngineContext engine_context, XWikiContext context) throws XWikiException, IOException
    {
        XWiki xwiki = XWiki.getXWiki(context);

        Utils.handleMultipart(processMultipart(request.getHttpServletRequest()), context);

        XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
        context.setURLFactory(urlf);

        VelocityManager velocityManager =
            (VelocityManager) Utils.getComponent(VelocityManager.class);
        VelocityContext vcontext = velocityManager.getVelocityContext();
        
        return xwiki.prepareDocuments(request, context, vcontext);
    }

    protected void cleanUp(XWikiContext context)
    {
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

    protected void handleException(XWikiRequest request, XWikiResponse response, Throwable e, XWikiContext context)
    {
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
                Utils.parseTemplate("accessdenied", context);
                return;
            } else if (xex.getCode() == XWikiException.ERROR_XWIKI_USER_INACTIVE) {
                Utils.parseTemplate("userinactive", context);
                return;
            }
            Utils.parseTemplate("exception", context);
        } catch (Exception e2) {
            // I hope this never happens
            e.printStackTrace();
            e2.printStackTrace();
        }
    }

    protected void doDispatch(RenderRequest aRenderRequest, RenderResponse aRenderResponse) throws PortletException, IOException
    {
        WindowState windowState = aRenderRequest.getWindowState();
        if (!windowState.equals(WindowState.MINIMIZED) && aRenderRequest.getPortletMode().equals(CONFIG_PORTLET_MODE)) {
            doView(aRenderRequest, aRenderResponse);
        } else {
            super.doDispatch(aRenderRequest, aRenderResponse);
        }
    }

    public void processAction(ActionRequest actionRequest, ActionResponse actionResponse) 
        throws PortletException, IOException
    {
        WindowState windowState = actionRequest.getWindowState();
        if (!windowState.equals(WindowState.MINIMIZED) && actionRequest.getPortletMode().equals(CONFIG_PORTLET_MODE)) {
            handleConfigForm(actionRequest, actionResponse);
        } else {
            XWikiContext context = null;

            try {
                // Initialize the XWiki Context which is the main object used to pass information across
                // classes/methods. It's also wrapping the request, response, and all container objects
                // in general.
                context = initializeXWikiContext(actionRequest, actionResponse);

                // From this line forward all information can be found in the XWiki Context.
                doView(context);
            } catch (XWikiException e) {
                throw new PortletException("Failed to initalize XWiki Context", e);
            } finally {
                if (context != null) {
                    cleanupComponents();
                }
            }
        }
    }
    
    public void processAction(XWikiContext context) 
        throws PortletException, IOException
    {
        try {
            if (prepareAction(context.getAction(), context.getRequest(), context.getResponse(), context.getEngineContext(), context) == false)
                return;

            XWikiForm form = null;

            if (context.getAction().equals("save"))
                form = new EditForm();
            else if (context.getAction().equals("lock"))
                form = new EditForm();
            else if (context.getAction().equals("cancel"))
                form = new EditForm();
            else if (context.getAction().equals("rollback"))
                form = new RollbackForm();
            else if (context.getAction().equals("objectadd"))
                form = new ObjectAddForm();
            else if (context.getAction().equals("commentadd"))
                form = new ObjectAddForm();
            else if (context.getAction().equals("objectremove"))
                form = new ObjectRemoveForm();
            else if (context.getAction().equals("propadd"))
                form = new PropAddForm();
            else if (context.getAction().equals("deleteversions"))
                form = new DeleteVersionsForm();

            if (form != null) {
                form.reset(null, context.getRequest());
                context.setForm(form);
            }

            if (context.getAction().equals("save")) {
                (new SaveAction()).action(context);
            } else if (context.getAction().equals("rollback")) {
                (new RollbackAction()).action(context);
            } else if (context.getAction().equals("cancel")) {
                (new CancelAction()).action(context);
            } else if (context.getAction().equals("lock")) {
                (new LockAction()).action(context);
            } else if (context.getAction().equals("delete")) {
                (new DeleteAction()).action(context);
            } else if (context.getAction().equals("undelete")) {
                (new UndeleteAction()).action(context);
            } else if (context.getAction().equals("propupdate")) {
                (new PropUpdateAction()).action(context);
            } else if (context.getAction().equals("propadd")) {
                (new PropAddAction()).action(context);
            } else if (context.getAction().equals("objectadd")) {
                (new ObjectAddAction()).action(context);
            } else if (context.getAction().equals("commentadd")) {
                (new CommentAddAction()).action(context);
            } else if (context.getAction().equals("objectremove")) {
                (new ObjectRemoveAction()).action(context);
            } else if (context.getAction().equals("upload")) {
                (new UploadAction()).action(context);
            } else if (context.getAction().equals("delattachment")) {
                (new DeleteAttachmentAction()).action(context);
            } else if (context.getAction().equals("skin")) {
                (new SkinAction()).action(context);
            } else if (context.getAction().equals("logout")) {
                (new LogoutAction()).action(context);
            } else if (context.getAction().equals("register")) {
                (new RegisterAction()).action(context);
            } else if (context.getAction().equals("inline")) {
                (new InlineAction()).action(context);
            } else if (context.getAction().equals("deleteversions")) {
                (new DeleteVersionsAction()).action(context);
            }
        } catch (Throwable e) {
            handleException(context.getRequest(), context.getResponse(), e, context);
        } finally {
            cleanUp(context);
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

    protected void doView(RenderRequest renderRequest, RenderResponse renderResponse) 
        throws PortletException, IOException
    {
        XWikiContext context = null;

        try {
            // Initialize the XWiki Context which is the main object used to pass information across
            // classes/methods. It's also wrapping the request, response, and all container objects
            // in general.
            context = initializeXWikiContext(renderRequest, renderResponse);

            // From this line forward all information can be found in the XWiki Context.
            doView(context);
        } catch (XWikiException e) {
            throw new PortletException("Failed to initialize XWiki Context", e);
        } finally {
            if (context != null) {
                cleanupComponents();
            }
        }
    }

    protected void doView(XWikiContext context) 
        throws PortletException, IOException
    {
        try {
            if (prepareAction(context.getAction(), context.getRequest(), context.getResponse(), context.getEngineContext(), context) == false)
                return;

            XWikiForm form = null;

            if (context.getAction().equals("edit")
                    || context.getAction().equals("inline"))
                form = new EditForm();
            else if (context.getAction().equals("preview"))
                form = new EditForm();

            if (form != null) {
                form.reset(null, context.getRequest());
                context.setForm(form);
            }

            String renderResult = null;
            // Determine what to do
            if (context.getAction().equals("view")) {
                renderResult = (new ViewAction()).render(context);
            } else if (context.getAction().equals("viewrev")) {
                renderResult = (new ViewrevAction()).render(context);
            } else if (context.getAction().equals("inline")) {
                renderResult = (new InlineAction()).render(context);
            } else if (context.getAction().equals("edit")) {
                renderResult = (new EditAction()).render(context);
            } else if (context.getAction().equals("preview")) {
                renderResult = (new PreviewAction()).render(context);
            } else if (context.getAction().equals("delete")) {
                renderResult = (new DeleteAction()).render(context);
            } else if (context.getAction().equals("undelete")) {
                renderResult = (new UndeleteAction()).render(context);
            } else if (context.getAction().equals("download")) {
                renderResult = (new DownloadAction()).render(context);
            } else if (context.getAction().equals("downloadrev")) {
                renderResult = (new DownloadRevAction()).render(context);
            } else if (context.getAction().equals("viewattachrev")) {
                renderResult = (new ViewAttachRevAction()).render(context);
            } else if (context.getAction().equals("dot")) {
                renderResult = (new DotAction()).render(context);
            } else if (context.getAction().equals("svg")) {
                renderResult = (new SVGAction()).render(context);
            } else if (context.getAction().equals("attach")) {
                renderResult = (new AttachAction()).render(context);
            } else if (context.getAction().equals("login")) {
                renderResult = (new LoginAction()).render(context);
            } else if (context.getAction().equals("loginsubmit")) {
                renderResult = (new LoginSubmitAction()).render(context);
            } else if (context.getAction().equals("loginerror")) {
                renderResult = (new LoginErrorAction()).render(context);
            } else if (context.getAction().equals("register")) {
                renderResult = (new RegisterAction()).render(context);
            } else if (context.getAction().equals("skin")) {
                renderResult = (new SkinAction()).render(context);
            } else if (context.getAction().equals("export")) {
                renderResult = (new ExportAction()).render(context);
            } else if (context.getAction().equals("import")) {
                renderResult = (new ImportAction()).render(context);
            } else if (context.getAction().equals("portletConfig")) {
                renderResult = "portletConfig";
            } 
            if (renderResult != null) {
                String page = Utils.getPage(context.getRequest(), renderResult);
                Utils.parseTemplate(page, context);
            }
        } catch (Throwable e) {
            if (logger.isWarnEnabled()) {
                logger.warn("oops", e);
            }

            handleException(context.getRequest(), context.getResponse(), e, context);
        } finally {
            // Let's make sure we have flushed content and closed
            try {
                context.getResponse().getWriter().flush();
            } catch (Throwable e) {
            }

            // / Let's handle the notification and make sure it never fails
            try {
                context.getWiki().getNotificationManager().verify(context.getDoc(), context.getAction(), context);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            cleanUp(context);
        }
    }

    protected void doEdit(RenderRequest renderRequest, RenderResponse renderResponse) throws PortletException, IOException {
        super.doEdit(renderRequest, renderResponse);
    }
    
    protected XWikiContext initializeXWikiContext(PortletRequest portletRequest, PortletResponse portletResponse)
        throws XWikiException, PortletException
    {
        XWikiRequest request = new XWikiPortletRequest(portletRequest);
        XWikiResponse response = new XWikiPortletResponse(portletResponse);
        XWikiEngineContext engineContext = new XWikiPortletContext(portletRequest.getPortletSession().getPortletContext());

        String action = request.getParameter("action");
        if ((action == null) || (action.equals(""))) {
            if (RenderRequest.class.isAssignableFrom(portletRequest.getClass())) {
                action = portletRequest.getPortletMode().equals(CONFIG_PORTLET_MODE)
                    ? "portletConfig" : "view";
            } else {
                action = "view";
            }
        }
        
        XWikiContext context = Utils.prepareContext(action, request, response, engineContext);

        // Initialize the Container component which is the new of transporting the Context in the new
        // component architecture.
        initializeContainerComponent(context);

        return context;
    }

    protected void initializeContainerComponent(XWikiContext context)
        throws PortletException
    {
        // Initialize the Container fields (request, response, session).
        // Note that this is a bridge between the old core and the component architecture.
        // In the new component architecture we use ThreadLocal to transport the request, 
        // response and session to components which require them.
        PortletContainerInitializer containerInitializer =
            (PortletContainerInitializer) Utils.getComponent(PortletContainerInitializer.class);

        try {
            containerInitializer.initializeRequest(
                ((XWikiPortletRequest) context.getRequest()).getPortletRequest(), context);
            containerInitializer.initializeResponse(
                ((XWikiPortletResponse) context.getResponse()).getPortletResponse());
            containerInitializer.initializeSession(
                ((XWikiPortletRequest) context.getRequest()).getPortletRequest());
        } catch (PortletContainerException e) {
            throw new PortletException("Failed to initialize request/response or session", e);
        }            
    }    

    protected void cleanupComponents()
    {
        Container container = (Container) Utils.getComponent(Container.class);
        Execution execution = (Execution) Utils.getComponent(Execution.class);

        // We must ensure we clean the ThreadLocal variables located in the Container and Execution
        // components as otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }
    
    /**
     * {@inheritDoc}
     */
    public void destroy()
    {
        Container container = (Container) Utils.getComponent(Container.class);
        ApplicationContextListenerManager applicationContextListenerManager =
            (ApplicationContextListenerManager) Utils.getComponent(ApplicationContextListenerManager.class);
        applicationContextListenerManager.destroyApplicationContext(container.getApplicationContext());
        super.destroy();
    }
}

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
 */
package com.xpn.xwiki.web;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.script.ScriptContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.job.internal.DefaultJobProgress;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.WrappedThreadEventListener;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.resource.NotFoundResourceHandlerException;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.internal.DefaultResourceReferenceHandlerChain;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;

/**
 * <p>
 * Root class for most XWiki actions. It provides a common framework that allows actions to execute just the specific
 * action code, handling the extra activities, such as preparing the context and retrieving the document corresponding
 * to the URL.
 * </p>
 * <p>
 * It defines two methods, {@link #action(XWikiContext)} and {@link #render(XWikiContext)}, that should be overridden by
 * specific actions. {@link #action(XWikiContext)} should contain the processing part of the action.
 * {@link #render(XWikiContext)} should return the name of a template that should be rendered, or manually write to the
 * {@link XWikiResponse response} stream.
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
 * <li>If {@link #action(XWikiContext)} returns true, run the overridden {@link #render(XWikiContext)}</li>
 * <li>If {@link #render(XWikiContext)} returned a string (template name), render the template with that name</li>
 * <li>Send action post-notifications to listeners</li>
 * </ul>
 * <p>
 * During this process, also handle specific errors, like when a document does not exist, or the user does not have the
 * right to perform the current action.
 * </p>
 */
public abstract class XWikiAction extends Action
{
    public static final String ACTION_PROGRESS = "actionprogress";

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiAction.class);

    /**
     * Actions that need to be resolved on the main wiki instead of the current non-existing wiki. This is used to be
     * able to render the skin even on a wiki that doesn't exist.
     */
    private static final List<String> ACTIONS_IGNORED_WHEN_WIKI_DOES_NOT_EXIST =
        Arrays.asList("skin", "ssx", "jsx", "download");

    /**
     * Indicate if the action allow asynchronous display (among which the XWiki initialization).
     */
    protected boolean waitForXWikiInitialization = true;

    /**
     * Indicate if the XWiki.RedirectClass is handled by the action (see handleRedirectObject()).
     */
    protected boolean handleRedirectObject = false;

    private ContextualLocalizationManager localization;

    private JobProgressManager progress;

    private ScriptContextManager scriptContextManager;

    protected ContextualLocalizationManager getLocalization()
    {
        if (this.localization == null) {
            this.localization = Utils.getComponent(ContextualLocalizationManager.class);
        }

        return this.localization;
    }

    protected String localizePlainOrKey(String key, Object... parameters)
    {
        return StringUtils.defaultString(getLocalization().getTranslationPlain(key, parameters), key);
    }

    protected JobProgressManager getProgress()
    {
        if (this.progress == null) {
            this.progress = Utils.getComponent(JobProgressManager.class);
        }

        return this.progress;
    }

    /**
     * @return the current unmodified {@link ScriptContext} instance
     * @since 8.3M1
     */
    protected ScriptContext getCurrentScriptContext()
    {
        if (this.scriptContextManager == null) {
            this.scriptContextManager = Utils.getComponent(ScriptContextManager.class);
        }

        return this.scriptContextManager.getCurrentScriptContext();
    }

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
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest req,
        HttpServletResponse resp) throws Exception
    {
        ActionForward actionForward;
        XWikiContext context = null;

        try {
            // Initialize the XWiki Context which is the main object used to pass information across
            // classes/methods. It's also wrapping the request, response, and all container objects
            // in general.
            context = initializeXWikiContext(mapping, form, req, resp);

            // From this line forward all information can be found in the XWiki Context.
            actionForward = execute(context);
        } finally {
            if (context != null) {
                cleanupComponents();
            }
        }

        return actionForward;
    }

    public ActionForward execute(XWikiContext context) throws Exception
    {
        MonitorPlugin monitor = null;
        FileUploadPlugin fileupload = null;
        DefaultJobProgress actionProgress = null;
        ObservationManager om = Utils.getComponent(ObservationManager.class);
        Execution execution = Utils.getComponent(Execution.class);
        String docName = "";

        boolean debug = StringUtils.equals(context.getRequest().get("debug"), "true");

        String sasync = context.getRequest().get("async");

        try {
            String action = context.getAction();

            // Start progress
            if (debug && om != null && execution != null) {
                actionProgress = new DefaultJobProgress(context.getURL().toExternalForm());
                om.addListener(new WrappedThreadEventListener(actionProgress));

                // Register the action progress in the context
                ExecutionContext econtext = execution.getContext();
                if (econtext != null) {
                    econtext.setProperty(XWikiAction.ACTION_PROGRESS, actionProgress);
                }
            }

            getProgress().pushLevelProgress(2, this);

            getProgress().startStep(this, "Get XWiki instance");

            // Initialize context.getWiki() with the main wiki
            XWiki xwiki;

            // Verify that the requested wiki exists
            try {
                // Don't show init screen if async is forced to false
                xwiki = XWiki.getXWiki(this.waitForXWikiInitialization || StringUtils.equals(sasync, "false"), context);

                // If XWiki is still initializing display initialization template
                if (xwiki == null) {
                    // Display initialization template
                    renderInit(context);

                    // Initialization template has been displayed, stop here.
                    return null;
                }
            } catch (XWikiException e) {
                // If the wiki asked by the user doesn't exist, then we first attempt to use any existing global
                // redirects. If there are none, then we display the specific error template.
                if (e.getCode() == XWikiException.ERROR_XWIKI_DOES_NOT_EXIST) {
                    xwiki = XWiki.getMainXWiki(context);

                    // Initialize the url factory
                    XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
                    context.setURLFactory(urlf);

                    // Initialize the velocity context and its bindings so that it may be used in the velocity templates
                    // that we
                    // are parsing below.
                    VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
                    VelocityContext vcontext = velocityManager.getVelocityContext();

                    if (!sendGlobalRedirect(context.getResponse(), context.getURL().toString(), context)) {
                        // Starting XWiki 5.0M2, 'xwiki.virtual.redirect' was removed. Warn users still using it.
                        if (!StringUtils.isEmpty(context.getWiki().Param("xwiki.virtual.redirect"))) {
                            LOGGER.warn(String.format("%s %s", "'xwiki.virtual.redirect' is no longer supported.",
                                "Please update your configuration and/or see XWIKI-8914 for more details."));
                        }

                        // Display the error template only for actions that are not ignored
                        if (!ACTIONS_IGNORED_WHEN_WIKI_DOES_NOT_EXIST.contains(action)) {

                            // Add localization resources to the context
                            xwiki.prepareResources(context);

                            // Set the main home page in the main space of the main wiki as the current requested entity
                            // since we cannot set the non existing one as it would generate errors obviously...
                            EntityReferenceValueProvider valueProvider =
                                Utils.getComponent(EntityReferenceValueProvider.class);
                            xwiki.setPhonyDocument(new DocumentReference(valueProvider.getDefaultValue(EntityType.WIKI),
                                valueProvider.getDefaultValue(EntityType.SPACE),
                                valueProvider.getDefaultValue(EntityType.DOCUMENT)), context, vcontext);

                            // Parse the error template
                            Utils.parseTemplate(context.getWiki().Param("xwiki.wiki_exception", "wikidoesnotexist"),
                                context);

                            // Error template was displayed, stop here.
                            return null;
                        }

                        // At this point, we allow regular execution of the ignored action because even if the wiki
                        // does not exist, we still need to allow UI resources to be retrieved (from the filesystem
                        // and the main wiki) or our error template will not be rendered properly.

                        // Proceed with serving the main wiki

                    } else {
                        // Global redirect was executed, stop here.
                        return null;
                    }
                } else {
                    LOGGER.error("Uncaught exception during XWiki initialisation:", e);
                    throw e;
                }
            }

            // Send global redirection (if any)
            if (sendGlobalRedirect(context.getResponse(), context.getURL().toString(), context)) {
                return null;
            }

            XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
            context.setURLFactory(urlf);

            // Handle ability to enter space URLs and convert them to page URLs (Nested Documents)
            if (redirectSpaceURLs(action, urlf, xwiki, context)) {
                return null;
            }

            String sajax = context.getRequest().get("ajax");
            boolean ajax = false;
            if (sajax != null && !sajax.trim().equals("") && !sajax.equals("0")) {
                ajax = true;
            }
            context.put("ajax", ajax);

            boolean async = false;
            if (StringUtils.isNotEmpty(sasync)) {
                async = sasync.equals("true");
            } else {
                // By default allow asynchronous rendering for "human oriented" actions which are not executing an ajax
                // request
                async = !ajax && !this.waitForXWikiInitialization;
            }
            Utils.getComponent(AsyncContext.class).setEnabled(async);

            // Any error before this will be treated using a redirection to an error page

            if (monitor != null) {
                monitor.startTimer("request");
            }

            getProgress().startStep(this, "Execute request");

            VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
            VelocityContext vcontext = velocityManager.getVelocityContext();

            getProgress().pushLevelProgress(7, this);

            boolean eventSent = false;
            try {
                getProgress().startStep(this, "Prepare documents and put them in the context");

                // Prepare documents and put them in the context
                if (!xwiki.prepareDocuments(context.getRequest(), context, vcontext)) {
                    return null;
                }

                // Start monitoring timer
                monitor = (MonitorPlugin) xwiki.getPlugin("monitor", context);
                if (monitor != null) {
                    monitor.startRequest("", context.getAction(), context.getURL());
                    monitor.startTimer("multipart");
                }

                getProgress().startStep(this, "Parses multipart");

                // Parses multipart so that params in multipart are available for all actions
                fileupload = Utils.handleMultipart(context.getRequest().getHttpServletRequest(), context);
                if (monitor != null) {
                    monitor.endTimer("multipart");
                }

                if (monitor != null) {
                    monitor.setWikiPage(context.getDoc().getFullName());
                }

                getProgress().startStep(this, "Send [" + context.getAction() + "] action start event");

                // For the moment we're sending the XWiki context as the data, but this will be
                // changed in the future, when the whole platform will be written using components
                // and there won't be a need for the context.
                try {
                    ActionExecutingEvent event = new ActionExecutingEvent(context.getAction());
                    om.notify(event, context.getDoc(), context);
                    eventSent = true;
                    if (event.isCanceled()) {
                        // Action has been canceled
                        // TODO: do something special ?
                        return null;
                    }
                } catch (Throwable ex) {
                    LOGGER.error("Cannot send action notifications for document [" + context.getDoc()
                        + " using action [" + context.getAction() + "]", ex);
                }

                if (monitor != null) {
                    monitor.endTimer("prenotify");
                }

                // Call the Actions

                getProgress().startStep(this, "Search and execute entity resource handler");

                // Call the new Entity Resource Reference Handler.
                ResourceReferenceHandler entityResourceReferenceHandler = Utils.getComponent(
                    new DefaultParameterizedType(null, ResourceReferenceHandler.class, ResourceType.class), "bin");
                EntityResourceReference entityResourceReference =
                    (EntityResourceReference) Utils.getComponent(ResourceReferenceManager.class).getResourceReference();

                // We save the current action set since:
                // - by default the action is set to "view" for Extensions not installed as root and contributing some
                // new Entity Action (see https://jira.xwiki.org/browse/XWIKI-15182).
                // - we want to set back the action in case no ResourceReferenceHandler was found to handle the URL
                // TODO: Remove once https://jira.xwiki.org/browse/XWIKI-14947 is fixed
                String originalAction = context.getAction();
                try {
                    // Force the action in the context because of https://jira.xwiki.org/browse/XWIKI-15182.
                    // TODO: Remove once https://jira.xwiki.org/browse/XWIKI-14947 is fixed
                    context.setAction(entityResourceReference.getAction().getActionName());
                    entityResourceReferenceHandler.handle(entityResourceReference,
                        DefaultResourceReferenceHandlerChain.EMPTY);
                    // Don't let the old actions kick in!
                    return null;
                } catch (NotFoundResourceHandlerException e) {
                    // No Entity Resource Action has been found. Don't do anything and let it go through
                    // so that the old Action system kicks in...
                    // Put back the action, because of https://jira.xwiki.org/browse/XWIKI-15182
                    // TODO: Remove once https://jira.xwiki.org/browse/XWIKI-14947 is fixed
                    context.setAction(originalAction);
                } catch (Throwable e) {
                    // Some real failure, log it since it's a problem but still allow the old Action system a chance
                    // to do something...
                    LOGGER.error("Failed to handle Action for Resource [{}]", entityResourceReference, e);
                }

                getProgress().startStep(this, "Execute action render");

                // Handle the XWiki.RedirectClass object that can be attached to the current document
                boolean hasRedirect = false;
                if (handleRedirectObject) {
                    hasRedirect = handleRedirectObject(context);
                }

                // Then call the old Actions for backward compatibility (and because a lot of them have not been
                // migrated to new Actions yet).
                String renderResult = null;
                XWikiDocument doc = context.getDoc();
                docName = doc.getFullName();
                if (!hasRedirect && action(context)) {
                    renderResult = render(context);
                }

                if (renderResult != null) {
                    if (doc.isNew() && "view".equals(context.getAction())
                        && !"recyclebin".equals(context.getRequest().get("viewer"))
                        && !"children".equals(context.getRequest().get("viewer"))
                        && !"siblings".equals(context.getRequest().get("viewer"))) {
                        String page = Utils.getPage(context.getRequest(), "docdoesnotexist");

                        getProgress().startStep(this, "Execute template [" + page + "]");
                        Utils.parseTemplate(page, context);
                    } else {
                        String page = Utils.getPage(context.getRequest(), renderResult);

                        getProgress().startStep(this, "Execute template [" + page + "]");
                        Utils.parseTemplate(page, !page.equals("direct"), context);
                    }
                }
                return null;
            } catch (Throwable e) {
                if (e instanceof IOException) {
                    e = new XWikiException(XWikiException.MODULE_XWIKI_APP,
                        XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, "Exception while sending response", e);
                }

                if (!(e instanceof XWikiException)) {
                    e = new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_UNKNOWN,
                        "Uncaught exception", e);
                }

                try {
                    XWikiException xex = (XWikiException) e;
                    if (xex.getCode() == XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION) {
                        // Connection aborted from the client side, there's not much we can do on the server side. We
                        // simply ignore it.
                        LOGGER.debug("Connection aborted", e);
                        // We don't write any other message to the response, as the connection is broken, anyway.
                        return null;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                        Utils.parseTemplate(context.getWiki().Param("xwiki.access_exception", "accessdenied"), context);
                        return null;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_USER_INACTIVE) {
                        Utils.parseTemplate(context.getWiki().Param("xwiki.user_exception", "userinactive"), context);
                        return null;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND) {
                        context.put("message", "attachmentdoesnotexist");
                        Utils.parseTemplate(
                            context.getWiki().Param("xwiki.attachment_exception", "attachmentdoesnotexist"), context);
                        return null;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION) {
                        vcontext.put("message", localizePlainOrKey("platform.core.invalidUrl"));
                        xwiki.setPhonyDocument(xwiki.getDefaultSpace(context) + "." + xwiki.getDefaultPage(context),
                            context, vcontext);
                        context.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        Utils.parseTemplate(context.getWiki().Param("xwiki.invalid_url_exception", "error"), context);
                        return null;
                    }
                    vcontext.put("exp", e);
                    if (LOGGER.isWarnEnabled()) {
                        // Don't log "Broken Pipe" exceptions since they're not real errors and we don't want to pollute
                        // the logs with unnecessary stack traces. It just means the client side has cancelled the
                        // connection.
                        if (ExceptionUtils.getRootCauseMessage(e).equals("IOException: Broken pipe")) {
                            return null;
                        }
                        LOGGER.warn("Uncaught exception: " + e.getMessage(), e);
                    }
                    // If the request is an AJAX request, we don't return a whole HTML page, but just the exception
                    // inline.
                    String exceptionTemplate = ajax ? "exceptioninline" : "exception";
                    Utils.parseTemplate(Utils.getPage(context.getRequest(), exceptionTemplate), context);
                    return null;
                } catch (XWikiException ex) {
                    if (ex.getCode() == XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION) {
                        LOGGER.error("Connection aborted");
                    }
                } catch (Exception e2) {
                    // I hope this never happens
                    LOGGER.error("Uncaught exceptions (inner): ", e);
                    LOGGER.error("Uncaught exceptions (outer): ", e2);
                }
                return null;
            } finally {
                // Let's make sure we have flushed content and closed
                try {
                    context.getResponse().getWriter().flush();
                } catch (Throwable e) {
                    // This might happen if the connection was closed, for example.
                    // If we can't flush, then there's nothing more we can send to the client.
                }

                if (monitor != null) {
                    monitor.endTimer("request");
                    monitor.startTimer("notify");
                }

                if (eventSent) {
                    // For the moment we're sending the XWiki context as the data, but this will be
                    // changed in the future, when the whole platform will be written using components
                    // and there won't be a need for the context.
                    try {
                        om.notify(new ActionExecutedEvent(context.getAction()), context.getDoc(), context);
                    } catch (Throwable ex) {
                        LOGGER.error("Cannot send action notifications for document [" + docName + " using action ["
                            + context.getAction() + "]", ex);
                    }
                }

                if (monitor != null) {
                    monitor.endTimer("notify");
                }

                getProgress().startStep(this, "Cleanup database connections");

                // Make sure we cleanup database connections
                // There could be cases where we have some
                xwiki.getStore().cleanUp(context);

                getProgress().popLevelProgress(this);
            }
        } finally {
            // End request
            if (monitor != null) {
                monitor.endRequest();
            }

            // Stop progress
            if (actionProgress != null) {
                getProgress().popLevelProgress(this);

                om.removeListener(actionProgress.getName());
            }

            if (fileupload != null) {
                fileupload.cleanFileList(context);
            }
        }
    }

    private void renderInit(XWikiContext xcontext) throws Exception
    {
        RenderingContext renderingContext = Utils.getComponent(RenderingContext.class);
        MutableRenderingContext mutableRenderingContext =
            renderingContext instanceof MutableRenderingContext ? (MutableRenderingContext) renderingContext : null;

        if (mutableRenderingContext != null) {
            mutableRenderingContext.push(renderingContext.getTransformation(), renderingContext.getXDOM(),
                renderingContext.getDefaultSyntax(), "init.vm", renderingContext.isRestricted(), Syntax.XHTML_1_0);
        }

        xcontext.getResponse().setStatus(202);
        xcontext.getResponse().setContentType("text/html; charset=UTF-8");

        try {
            Utils.getComponent(TemplateManager.class).render("init.vm", xcontext.getResponse().getWriter());
        } finally {
            if (mutableRenderingContext != null) {
                mutableRenderingContext.pop();
            }
        }

        xcontext.getResponse().flushBuffer();

        xcontext.setFinished(true);
    }

    protected XWikiContext initializeXWikiContext(ActionMapping mapping, ActionForm form, HttpServletRequest req,
        HttpServletResponse resp) throws XWikiException, ServletException
    {
        String action = mapping.getName();

        XWikiRequest request = new XWikiServletRequest(req);
        XWikiResponse response = new XWikiServletResponse(resp);
        XWikiContext context =
            Utils.prepareContext(action, request, response, new XWikiServletContext(this.servlet.getServletContext()));

        // This code is already called by struts.
        // However struts will also set all the parameters of the form data
        // directly from the request objects.
        // However because of bug https://jira.xwiki.org/browse/XWIKI-2422
        // We need to perform encoding of windows-1252 chars in ISO mode
        // So we need to make sure this code is called
        // TODO: completely get rid of struts so that we control this part of the code and can reduce drastically the
        // number of calls
        if (form != null) {
            form.reset(mapping, request);
        }

        // Add the form to the context
        context.setForm((XWikiForm) form);

        // Initialize the Container component which is the new way of transporting the Context in the new
        // component architecture.
        initializeContainerComponent(context);

        return context;
    }

    protected void initializeContainerComponent(XWikiContext context) throws ServletException
    {
        // Initialize the Container fields (request, response, session).
        // Note that this is a bridge between the old core and the component architecture.
        // In the new component architecture we use ThreadLocal to transport the request,
        // response and session to components which require them.
        // In the future this Servlet will be replaced by the XWikiPlexusServlet Servlet.
        ServletContainerInitializer containerInitializer = Utils.getComponent(ServletContainerInitializer.class);

        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(), context);
            containerInitializer.initializeResponse(context.getResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new ServletException("Failed to initialize Request/Response or Session", e);
        }
    }

    protected void cleanupComponents()
    {
        Container container = Utils.getComponent(Container.class);
        Execution execution = Utils.getComponent(Execution.class);

        // We must ensure we clean the ThreadLocal variables located in the Container and Execution
        // components as otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }

    public String getRealPath(String path)
    {
        return this.servlet.getServletContext().getRealPath(path);
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

    /**
     * Redirect the user to an other location if the document holds an XWiki.RedirectClass instance (used when a
     * document is moved).
     *
     * @param context the XWiki context
     * @return either or not a redirection have been sent
     * @throws XWikiException if error occurs
     * @since 8.0RC1
     * @since 7.4.2
     */
    protected boolean handleRedirectObject(XWikiContext context) throws XWikiException
    {
        WikiReference wikiReference = context.getWikiReference();

        // Look if the document has a redirect object
        XWikiDocument doc = context.getDoc();
        BaseObject redirectObj =
            doc.getXObject(new DocumentReference("RedirectClass", new SpaceReference("XWiki", wikiReference)));
        if (redirectObj == null) {
            return false;
        }

        // Get the location
        String location = redirectObj.getStringValue("location");
        if (StringUtils.isBlank(location)) {
            return false;
        }

        // Resolve the location to get a reference
        DocumentReferenceResolver<String> resolver = Utils.getComponent(DocumentReferenceResolver.TYPE_STRING);
        EntityReference locationReference = resolver.resolve(location, wikiReference);

        // Get the type of the current target
        ResourceReference resourceReference = Utils.getComponent(ResourceReferenceManager.class).getResourceReference();
        EntityResourceReference entityResource = (EntityResourceReference) resourceReference;
        EntityReference entityReference = entityResource.getEntityReference();

        // If the entity is inside a document, compute the new entity with the new document part.
        if (entityReference.getType().ordinal() > EntityType.DOCUMENT.ordinal()) {
            EntityReference parentDocument = entityReference.extractReference(EntityType.DOCUMENT);
            locationReference = entityReference.replaceParent(parentDocument, locationReference);
        }

        // Get the URL corresponding to the location
        // Note: the anchor part is lost in the process, because it is not sent to the server
        // (see: http://stackoverflow.com/a/4276491)
        String url = context.getWiki().getURL(locationReference, context.getAction(),
            context.getRequest().getQueryString(), null, context);

        // Send the redirection
        try {
            context.getResponse().sendRedirect(url);
        } catch (IOException e) {
            throw new XWikiException("Failed to redirect.", e);
        }

        return true;
    }

    protected void handleRevision(XWikiContext context) throws XWikiException
    {
        String rev = context.getRequest().getParameter("rev");
        if (rev != null) {
            context.put("rev", rev);
            XWikiDocument doc = (XWikiDocument) context.get("doc");
            XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
            XWikiDocument rdoc =
                (!doc.getLocale().equals(tdoc.getLocale())) ? doc : context.getWiki().getDocument(doc, rev, context);
            XWikiDocument rtdoc =
                (doc.getLocale().equals(tdoc.getLocale())) ? rdoc : context.getWiki().getDocument(tdoc, rev, context);
            context.put("tdoc", rtdoc);
            context.put("cdoc", rdoc);
            context.put("doc", rdoc);
        }
    }

    /**
     * Send redirection based on a regexp pattern (if any) set at the main wiki level. To enable this feature you must
     * add xwiki.preferences.redirect=1 to your xwiki.cfg.
     *
     * @param response the servlet response
     * @param url url of the request
     * @param context the XWiki context
     * @return true if a redirection has been sent
     */
    protected boolean sendGlobalRedirect(XWikiResponse response, String url, XWikiContext context) throws Exception
    {
        if ("1".equals(context.getWiki().Param("xwiki.preferences.redirect"))) {
            // Note: This implementation is not performant at all and will slow down the wiki as the number
            // of redirects increases. A better implementation would use a cache of redirects and would use
            // the notification mechanism to update the cache when the XWiki.XWikiPreferences document is
            // modified.
            XWikiDocument globalPreferences = context.getWiki().getDocument("xwiki:XWiki.XWikiPreferences", context);
            Vector<BaseObject> redirects = globalPreferences.getObjects("XWiki.GlobalRedirect");

            if (redirects != null) {
                for (BaseObject redir : redirects) {
                    if (redir != null) {
                        String p = redir.getStringValue("pattern");
                        if (p != null && url.matches(p)) {
                            String dest = redir.getStringValue("destination");
                            response.sendRedirect(url.replaceAll(p, dest));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected void sendRedirect(XWikiResponse response, String url) throws XWikiException
    {
        try {
            if (url != null) {
                response.sendRedirect(response.encodeRedirectURL(url));
            }
        } catch (IOException e) {
            Object[] args = { url };
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_REDIRECT_EXCEPTION,
                "Exception while sending redirect to page {0}", e, args);
        }
    }

    /**
     * Gets the translated version of a document, in the specified language. If the translation does not exist, a new
     * document translation is created. If the requested language does not correspond to a translation (is not defined
     * or is the same as the main document), then the main document is returned.
     *
     * @param doc the main (default, untranslated) document to translate
     * @param language the requested document language
     * @param context the current request context
     * @return the translated document, or the original untranslated document if the requested language is not a
     *         translation
     * @throws XWikiException if the translation cannot be retrieved from the database
     */
    protected XWikiDocument getTranslatedDocument(XWikiDocument doc, String language, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument tdoc;
        if (StringUtils.isBlank(language) || language.equals("default") || language.equals(doc.getDefaultLanguage())) {
            tdoc = doc;
        } else {
            tdoc = doc.getTranslatedDocument(language, context);
            if (tdoc == doc) {
                tdoc = new XWikiDocument(doc.getDocumentReference());
                tdoc.setLanguage(language);
                tdoc.setStore(doc.getStore());
            }
            tdoc.setTranslation(1);
        }
        return tdoc;
    }

    /**
     * Perform CSRF check and redirect to the resubmission page if needed. Throws an exception if the access should be
     * denied, returns false if the check failed and the user will be redirected to a resubmission page.
     *
     * @param context current xwiki context containing the request
     * @return true if the check succeeded, false if resubmission is needed
     * @throws XWikiException if the check fails
     */
    protected boolean csrfTokenCheck(XWikiContext context) throws XWikiException
    {
        CSRFToken csrf = Utils.getComponent(CSRFToken.class);
        try {
            String token = context.getRequest().getParameter("form_token");
            if (!csrf.isTokenValid(token)) {
                sendRedirect(context.getResponse(), csrf.getResubmissionURL());
                return false;
            }
        } catch (XWikiException exception) {
            // too bad
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied, secret token verification failed", exception);
        }
        return true;
    }

    /**
     * In order to let users enter URLs to Spaces we do the following when receiving {@code /A/B} (where A and B are
     * spaces):
     * <ul>
     * <li>check that the action is "view" (we only support this for the view action since otherwise this would break
     * apps written before this concept was introduced in XWiki 7.2M1)</li>
     * <li>if A.B exists then continue</li>
     * <li>if A.B doesn't exist then forward to A.B.WebHome</li>
     * </ul>
     * In order to disable this redirect you should provide the {@code spaceRedirect=false} Query String parameter and
     * value.
     *
     * @since 7.2M1
     */
    private boolean redirectSpaceURLs(String action, XWikiURLFactory urlf, XWiki xwiki, XWikiContext context)
        throws Exception
    {
        if ("view".equals(action) && !"false".equalsIgnoreCase(context.getRequest().getParameter("spaceRedirect"))) {
            DocumentReference reference = xwiki.getDocumentReference(context.getRequest(), context);
            if (!xwiki.exists(reference, context)) {
                String defaultDocumentName = Utils.getComponent(EntityReferenceProvider.class)
                    .getDefaultReference(EntityType.DOCUMENT).getName();
                // Avoid an infinite loop by ensuring we're not on a WebHome already
                if (!reference.getName().equals(defaultDocumentName)) {
                    // Consider the reference as a Space Reference and Construct a new reference to the home of that
                    // Space. Then generate the URL for it and forward to it
                    SpaceReference spaceReference = new SpaceReference(reference.getName(), reference.getParent());
                    EntityReferenceSerializer<String> localSerializer =
                        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
                    // Extract the anchor
                    String anchor = new URL(context.getRequest().getRequestURL().toString()).getRef();
                    URL forwardURL = urlf.createURL(localSerializer.serialize(spaceReference), defaultDocumentName,
                        action, context.getRequest().getQueryString(), anchor,
                        spaceReference.getWikiReference().getName(), context);
                    // Since createURL() contain the webapp context and since RequestDispatcher should not contain it,
                    // we need to remove it!
                    String webappContext = xwiki.getWebAppPath(context);
                    String relativeURL = urlf.getURL(forwardURL, context);
                    relativeURL = '/' + StringUtils.substringAfter(relativeURL, webappContext);
                    context.getRequest().getRequestDispatcher(relativeURL).forward(context.getRequest(),
                        context.getResponse());
                    return true;
                }
            }
        }

        return false;
    }
}

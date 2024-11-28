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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;

import javax.inject.Inject;
import javax.inject.Named;
import javax.script.ScriptContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.ActionExecutedEvent;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.internal.web.DocExistValidator;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.job.internal.DefaultJobProgress;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.localization.LocalizationException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.WrappedThreadEventListener;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.resource.NotFoundResourceHandlerException;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.internal.DefaultResourceReferenceHandlerChain;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.xml.XMLUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.web.LegacyAction;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.redirection.RedirectionFilter;

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
public abstract class XWikiAction implements LegacyAction
{
    public static final String ACTION_PROGRESS = "actionprogress";

    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiAction.class);

    /**
     * Actions that need to be resolved on the main wiki instead of the current non-existing wiki. This is used to be
     * able to render the skin even on a wiki that doesn't exist.
     */
    private static final List<String> ACTIONS_IGNORED_WHEN_WIKI_DOES_NOT_EXIST =
        Arrays.asList("skin", "ssx", "jsx", "download");

    @Inject
    protected ComponentDescriptor<LegacyAction> componentDescriptor;

    @Inject
    protected Container container;

    @Inject
    protected Execution execution;

    @Inject
    protected ObservationManager observation;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    /**
     * Indicate if the action allow asynchronous display (among which the XWiki initialization).
     */
    protected boolean waitForXWikiInitialization = true;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentmixedReferenceResolver;

    @Inject
    private ContextualAuthorizationManager autorization;

    private ContextualLocalizationManager localization;

    private JobProgressManager progress;

    private ScriptContextManager scriptContextManager;

    private EntityNameValidationManager entityNameValidationManager;

    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    /**
     * @return the class of the XWikiForm in charge of parsing the request
     * @since 13.0
     */
    protected Class<? extends XWikiForm> getFormClass()
    {
        return null;
    }

    protected ContextualLocalizationManager getLocalization()
    {
        if (this.localization == null) {
            this.localization = Utils.getComponent(ContextualLocalizationManager.class);
        }

        return this.localization;
    }

    /**
     * @since 12.10.6
     * @since 13.2RC1
     */
    protected DocumentReferenceResolver<String> getCurrentMixedDocumentReferenceResolver()
    {
        return this.currentmixedReferenceResolver;
    }

    /**
     * @since 12.10.6
     * @since 13.2RC1
     */
    protected ContextualAuthorizationManager getContextualAuthorizationManager()
    {
        return this.autorization;
    }

    /**
     * @deprecated use {@link #localizePlainOrReturnKey(String, Object...)} instead. The new API doesn't XML-escape
     *             the translation as it's supposed to be used in a context where the target syntax is plain text.
     */
    @Deprecated(since = "14.10.12,15.5RC1")
    protected String localizePlainOrKey(String key, Object... parameters)
    {
        // TODO: Review all calls to localizePlainOrKey() and once this is done change this method implementation to
        // use:
        //   return localizeOrKey(key, Syntax.PLAIN_1_0, parameters)
        return XMLUtils.escape(Objects.toString(getLocalization().getTranslationPlain(key, parameters), key));
    }

    /**
     * @since 14.10.12
     * @since 15.5RC1
     */
    protected String localizeOrReturnKey(String key, Syntax syntax, Object... parameters)
    {
        String result;
        try {
            result = Objects.toString(getLocalization().getTranslation(key, syntax, parameters), key);
        } catch (LocalizationException e) {
            // Return the key in case of error but log a warning
            LOGGER.warn("Error rendering the translation for key [{}] in syntax [{}]. Using the translation key "
                + "instead. Root cause: [{}]", key, syntax.toIdString(), ExceptionUtils.getRootCauseMessage(e));
            result = key;
        }
        return result;
    }

    /**
     * @since 14.10.12
     * @since 15.5RC1
     */
    protected String localizePlainOrReturnKey(String key, Object... parameters)
    {
        return localizeOrReturnKey(key, Syntax.PLAIN_1_0, parameters);
    }

    protected JobProgressManager getProgress()
    {
        if (this.progress == null) {
            this.progress = Utils.getComponent(JobProgressManager.class);
        }

        return this.progress;
    }

    protected EntityNameValidationManager getEntityNameValidationManager()
    {
        if (this.entityNameValidationManager == null) {
            this.entityNameValidationManager = Utils.getComponent(EntityNameValidationManager.class);
        }
        return this.entityNameValidationManager;
    }

    protected EntityNameValidationConfiguration getEntityNameValidationConfiguration()
    {
        if (this.entityNameValidationConfiguration == null) {
            this.entityNameValidationConfiguration = Utils.getComponent(EntityNameValidationConfiguration.class);
        }

        return this.entityNameValidationConfiguration;
    }

    protected EntityReferenceSerializer<String> getLocalSerializer()
    {
        if (this.localSerializer == null) {
            this.localSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        }
        return this.localSerializer;
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

    @Override
    public void execute(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws Exception
    {
        XWikiContext context = null;

        try {
            // Initialize the XWiki Context which is the main object used to pass information across
            // classes/methods. It's also wrapping the request, response, and all container objects
            // in general.
            context = initializeXWikiContext(servletRequest, servletResponse);

            // From this line forward all information can be found in the XWiki Context.
            execute(context);
        } finally {
            if (context != null) {
                cleanupComponents();
            }
        }
    }

    /**
     * Ensure that the given entity reference is valid according to the configured name strategy. Always returns true if
     * the name strategy is not found.
     *
     * @param entityReference the entity reference name to validate
     * @return {@code true} if the entity reference name is valid according to the name strategy.
     * @since 12.0RC1
     */
    protected boolean isEntityReferenceNameValid(EntityReference entityReference)
    {
        if (this.getEntityNameValidationManager().getEntityReferenceNameStrategy() != null
            && this.getEntityNameValidationConfiguration().useValidation()) {
            if (!this.getEntityNameValidationManager().getEntityReferenceNameStrategy().isValid(entityReference)) {
                Object[] args = {getLocalSerializer().serialize(entityReference)};
                XWikiException invalidNameException = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_APP_DOCUMENT_NAME_INVALID,
                    "Cannot create document {0} because its name does not respect the name strategy of the wiki.", null,
                    args);
                ScriptContext scontext = getCurrentScriptContext();
                scontext.setAttribute("invalidNameReference", entityReference, ScriptContext.ENGINE_SCOPE);
                scontext.setAttribute("createException", invalidNameException, ScriptContext.ENGINE_SCOPE);
                return false;
            }
        }
        return true;
    }

    /**
     * Write an error response to an ajax request.
     *
     * @param httpStatusCode The status code to set on the response.
     * @param message The message that should be displayed.
     * @param context the context.
     */
    protected void writeAjaxErrorResponse(int httpStatusCode, String message, XWikiContext context)
    {
        try {
            context.getResponse().setContentType("text/plain");
            context.getResponse().setStatus(httpStatusCode);
            context.getResponse().setCharacterEncoding(context.getWiki().getEncoding());
            context.getResponse().getWriter().print(message);
        } catch (IOException e) {
            LOGGER.error("Failed to send error response to AJAX save and continue request.", e);
        }
    }

    public void execute(XWikiContext context) throws Exception
    {
        MonitorPlugin monitor = null;
        FileUploadPlugin fileupload = null;
        DefaultJobProgress actionProgress = null;
        String docName = "";

        boolean debug = StringUtils.equals(context.getRequest().get("debug"), "true");

        String sasync = context.getRequest().get("async");

        try {
            String action = context.getAction();

            // Start progress
            if (debug) {
                actionProgress = new DefaultJobProgress(context.getURL().toExternalForm());
                this.observation.addListener(new WrappedThreadEventListener(actionProgress));

                // Register the action progress in the context
                ExecutionContext econtext = this.execution.getContext();
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
                    return;
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
                            EntityReferenceProvider entityReferenceProvider =
                                Utils.getComponent(EntityReferenceProvider.class);
                            DocumentReference phonyDoc =
                                (DocumentReference) entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT);
                            xwiki.setPhonyDocument(phonyDoc, context);

                            // Parse the error template
                            Utils.parseTemplate(context.getWiki().Param("xwiki.wiki_exception", "wikidoesnotexist"),
                                context);

                            // Error template was displayed, stop here.
                            return;
                        }

                        // At this point, we allow regular execution of the ignored action because even if the wiki
                        // does not exist, we still need to allow UI resources to be retrieved (from the filesystem
                        // and the main wiki) or our error template will not be rendered properly.

                        // Proceed with serving the main wiki

                    } else {
                        // Global redirect was executed, stop here.
                        return;
                    }
                } else {
                    LOGGER.error("Uncaught exception during XWiki initialisation:", e);
                    throw e;
                }
            }

            // Send global redirection (if any)
            if (sendGlobalRedirect(context.getResponse(), context.getURL().toString(), context)) {
                return;
            }

            XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
            context.setURLFactory(urlf);

            // Handle ability to enter space URLs and convert them to page URLs (Nested Documents)
            if (redirectSpaceURLs(action, urlf, xwiki, context)) {
                return;
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
                    return;
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
                    this.observation.notify(event, context.getDoc(), context);
                    eventSent = true;
                    if (event.isCanceled()) {
                        // Action has been canceled
                        // TODO: do something special ?
                        return;
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
                ResourceReferenceHandler<ResourceType> entityResourceReferenceHandler = Utils.getComponent(
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
                    return;
                } catch (NotFoundResourceHandlerException e) {
                    // No Entity Resource Action has been found. Don't do anything and let it go through
                    // so that the old Action system kicks in...
                    // Put back the action, because of https://jira.xwiki.org/browse/XWIKI-15182
                    // TODO: Remove once https://jira.xwiki.org/browse/XWIKI-14947 is fixed
                    context.setAction(originalAction);
                }

                getProgress().startStep(this, "Execute action render");

                // Handle the XWiki.RedirectClass object that can be attached to the current document
                boolean hasRedirect = handleRedirect(context);

                // Then call the old Actions for backward compatibility (and because a lot of them have not been
                // migrated to new Actions yet).
                String renderResult = null;
                XWikiDocument doc = context.getDoc();
                docName = doc.getFullName();
                if (!hasRedirect && action(context)) {
                    renderResult = render(context);
                }

                if (renderResult != null) {
                    // check for doc existence
                    if (shouldReturnDocDoesNotExist(doc, context)) {
                        String page = Utils.getPage(context.getRequest(), "docdoesnotexist");

                        getProgress().startStep(this, "Execute template [" + page + "]");
                        Utils.parseTemplate(page, context);
                    } else {
                        String page = Utils.getPage(context.getRequest(), renderResult);

                        getProgress().startStep(this, "Execute template [" + page + "]");
                        Utils.parseTemplate(page, !page.equals("direct"), context);
                    }
                }
                return;
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
                        return;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                        Utils.parseTemplate(context.getWiki().Param("xwiki.access_exception", "accessdenied"), context);
                        return;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_USER_INACTIVE
                        || xex.getCode() == XWikiException.ERROR_XWIKI_USER_DISABLED) {
                        if (xex.getCode() == XWikiException.ERROR_XWIKI_USER_DISABLED) {
                            context.put("cause", "disabled");
                        }
                        // In case of user disabled or inactive, the resources are actually forbidden.
                        context.getResponse().setStatus(HttpServletResponse.SC_FORBIDDEN);
                        Utils.parseTemplate(context.getWiki().Param("xwiki.user_exception", "userinactive"), context);

                        return;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND) {
                        context.put("message", "attachmentdoesnotexist");
                        Utils.parseTemplate(
                            context.getWiki().Param("xwiki.attachment_exception", "attachmentdoesnotexist"), context);
                        return;
                    } else if (xex.getCode() == XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION) {
                        vcontext.put("message", localizePlainOrKey("platform.core.invalidUrl"));
                        xwiki.setPhonyDocument(xwiki.getDefaultSpace(context) + "." + xwiki.getDefaultPage(context),
                            context, vcontext);
                        context.getResponse().setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        Utils.parseTemplate(context.getWiki().Param("xwiki.invalid_url_exception", "error"), context);
                        return;
                    }
                    // Note: We don't use the vcontext variable computed above since apparently the velocity context
                    // can have changed in between. Thus we get it again to be sure we're setting the binding in the
                    // right one.
                    velocityManager.getVelocityContext().put("exp", e);
                    if (LOGGER.isWarnEnabled()) {
                        // Don't log "Broken Pipe" exceptions since they're not real errors and we don't want to pollute
                        // the logs with unnecessary stack traces. It just means the client side has cancelled the
                        // connection.
                        if (ExceptionUtils.getRootCauseMessage(e).equals("IOException: Broken pipe")) {
                            return;
                        }
                        LOGGER.warn("Uncaught exception: " + e.getMessage(), e);
                    }
                    // If the request is an AJAX request, we don't return a whole HTML page, but just the exception
                    // inline.
                    String exceptionTemplate = ajax ? "exceptioninline" : "exception";
                    Utils.parseTemplate(Utils.getPage(context.getRequest(), exceptionTemplate), context);
                    return;
                } catch (XWikiException ex) {
                    if (ex.getCode() == XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION) {
                        LOGGER.error("Connection aborted");
                    }
                } catch (Exception e2) {
                    // I hope this never happens
                    LOGGER.error("Uncaught exceptions (inner): ", e);
                    LOGGER.error("Uncaught exceptions (outer): ", e2);
                }
                return;
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
                        this.observation.notify(new ActionExecutedEvent(context.getAction()), context.getDoc(), context);
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

                this.observation.removeListener(actionProgress.getName());
            }

            if (fileupload != null) {
                fileupload.cleanFileList(context);
            }
        }
    }

    /**
     * Check if the given document exists or not and if it should return a 404 based on the context. A {@link
     * DocExistValidator} with an hint matching the current action is used to check if the document exists. When no
     * {@link DocExistValidator} is found, the response is always {@code false} When a {@link DocExistValidator} is
     * found, the result is delegated to {@link DocExistValidator#docExist(XWikiDocument, XWikiContext)}.
     *
     * @param doc the doc for which to check it exists or not
     * @param context the current context
     * @return {@code true} if we should return a 404
     * @throws ComponentLookupException if an error occurs when instantiating a {@link DocExistValidator}
     */
    private boolean shouldReturnDocDoesNotExist(XWikiDocument doc, XWikiContext context) throws ComponentLookupException
    {
        boolean result = false;
        String action = context.getAction();
        if (this.componentManager.hasComponent(DocExistValidator.class, action)) {
            result = this.componentManager.<DocExistValidator>getInstance(DocExistValidator.class, action)
                .docExist(doc, context);
        }
        return result;
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

    protected XWikiContext initializeXWikiContext(HttpServletRequest servletRequest,
        HttpServletResponse servletResponse)
        throws XWikiException, ServletException, InstantiationException, IllegalAccessException
    {
        XWikiForm form;
        if (getFormClass() != null) {
            form = getFormClass().newInstance();
        } else {
            form = null;
        }

        return initializeXWikiContext(servletRequest, servletResponse, form);
    }

    /**
     * @return the name to put in the {@link XWikiContext}, by default the component role hint is used
     * @since 13.0
     */
    protected String getName()
    {
        return this.componentDescriptor.getRoleHint();
    }

    protected XWikiContext initializeXWikiContext(HttpServletRequest servletRequest,
        HttpServletResponse servletResponse, XWikiForm form) throws XWikiException, ServletException
    {
        String action = getName();

        XWikiRequest request = new XWikiServletRequest(servletRequest);
        XWikiResponse response = new XWikiServletResponse(servletResponse);
        XWikiContext context = Utils.prepareContext(action, request, response,
            new XWikiServletContext(servletRequest.getServletContext()));

        if (form != null) {
            form.reset(request);
        }

        // Add the form to the context
        context.setForm(form);

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
        // We must ensure we clean the ThreadLocal variables located in the Container and Execution
        // components as otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }

    public String getRealPath(String path)
    {
        Request request = this.container.getRequest();

        if (request instanceof ServletRequest) {
            return ((ServletRequest) request).getHttpServletRequest().getServletContext().getRealPath(path);
        }

        return null;
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
     * Indicate if the action support redirection. The default value is {@code false}.
     *
     * @return {@code true} if the action supports redirections, {@code false} otherwise
     * @since 14.0RC1
     */
    protected boolean supportRedirections()
    {
        return false;
    }

    private UserReference getCurrentUserReference(XWikiContext context)
    {
        return this.userReferenceResolver.resolve(context.getUserReference());
    }

    protected void handleRevision(XWikiContext context) throws XWikiException
    {
        String rev = context.getRequest().getParameter("rev");
        if (rev != null) {
            context.put("rev", rev);
            XWikiDocument doc = (XWikiDocument) context.get("doc");
            XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
            // if the doc is deleted and we request a specific language, we have to set the locale so we can retrieve
            // properly the document revision.
            if (rev.startsWith("deleted") && !StringUtils.isEmpty(context.getRequest().getParameter("language"))
                && doc == tdoc) {
                Locale locale = LocaleUtils.toLocale(context.getRequest().getParameter("language"), Locale.ROOT);
                tdoc = new XWikiDocument(tdoc.getDocumentReference(), locale);
            }

            DocumentReference documentReference = doc.getDocumentReference();
            try {
                documentRevisionProvider
                    .checkAccess(Right.VIEW, getCurrentUserReference(context), documentReference, rev);
            } catch (AuthorizationException e) {
                Object[] args = { documentReference, rev, context.getUserReference() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                    "Access to document {0} with revision {1} has been denied to user {2}", e, args);
            }

            XWikiDocument rdoc;
            XWikiDocument rtdoc;
            if (doc.getLocale().equals(tdoc.getLocale())) {
                rdoc = this.documentRevisionProvider.getRevision(doc.getDocumentReferenceWithLocale(), rev);
                rtdoc = rdoc;
            } else {
                rdoc = doc;
                rtdoc = this.documentRevisionProvider.getRevision(tdoc.getDocumentReferenceWithLocale(), rev);
            }
            if (rdoc == null) {
                Object[] args = { doc.getDocumentReferenceWithLocale(), rev };
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION,
                    "Version {1} does not exist while reading document {0}", null, args);
            }

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

    /**
     * Perform a redirect to the given URL.
     * @param response the response to use to perform the redirect
     * @param url the location of the redirect
     * @throws XWikiException in case of IOException when performing the redirect.
     */
    protected void sendRedirect(XWikiResponse response, String url) throws XWikiException
    {
        try {
            if (url != null) {
                response.sendRedirect(response.encodeRedirectURL(url));
            }
        } catch (IOException e) {
            Object[] args = {url};
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
        return csrfTokenCheck(context, false);
    }

    /**
     * Perform CSRF check and redirect to the resubmission page if needed. Throws an exception if the access should be
     * denied, returns false if the check failed and the user will be redirected to a resubmission page.
     *
     * @param context current xwiki context containing the request
     * @param jsonAnswer if true, returns a JSON answer in case of AJAX request: allow to process it properly on client.
     * @return true if the check succeeded, false if resubmission is needed
     * @throws XWikiException if the check fails
     * @since 11.3RC1
     */
    protected boolean csrfTokenCheck(XWikiContext context, boolean jsonAnswer) throws XWikiException
    {
        final boolean isAjaxRequest = Utils.isAjaxRequest(context);
        CSRFToken csrf = Utils.getComponent(CSRFToken.class);
        try {
            String token = context.getRequest().getParameter("form_token");
            if (!csrf.isTokenValid(token)) {
                if (isAjaxRequest) {
                    if (jsonAnswer) {
                        Map<String, String> jsonObject = new LinkedHashMap<>();
                        jsonObject.put("errorType", "CSRF");
                        jsonObject.put("resubmissionURI", csrf.getRequestURI());
                        jsonObject.put("newToken", csrf.getToken());
                        this.answerJSON(context, HttpServletResponse.SC_FORBIDDEN, jsonObject);
                    } else {
                        final String csrfCheckFailedMessage = localizePlainOrKey("core.editors.csrfCheckFailed");
                        writeAjaxErrorResponse(HttpServletResponse.SC_FORBIDDEN, csrfCheckFailedMessage, context);
                    }
                } else {
                    sendRedirect(context.getResponse(), csrf.getResubmissionURL());
                }

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
                    // Extract the anchor
                    String anchor = new URL(context.getRequest().getRequestURL().toString()).getRef();
                    URL forwardURL = urlf.createURL(getLocalSerializer().serialize(spaceReference), defaultDocumentName,
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

    /**
     * Answer to a request with a JSON content.
     * 
     * @param context the current context of the request.
     * @param status the status code to send back.
     * @param answer the content of the JSON answer.
     * @throws XWikiException in case of error during the serialization of the JSON.
     */
    protected void answerJSON(XWikiContext context, int status, Map<String, String> answer) throws XWikiException
    {
        ObjectMapper mapper = new ObjectMapper();

        try {
            String jsonAnswerAsString = mapper.writeValueAsString(answer);
            XWikiResponse response = context.getResponse();
            String encoding = context.getWiki().getEncoding();
            response.setContentType("application/json");
            // Set the content length to the number of bytes, not the
            // string length, so as to handle multi-byte encodings
            response.setContentLength(jsonAnswerAsString.getBytes(encoding).length);
            response.setStatus(status);
            response.setCharacterEncoding(encoding);
            response.getWriter().print(jsonAnswerAsString);
            context.setResponseSent(true);
        } catch (IOException e) {
            throw new XWikiException("Error while sending JSON answer.", e);
        }
    }

    /**
     * Make sure to set the right length (or nothing) in the response.
     * 
     * @param response the response
     * @param length the length to set in the response
     * @since 11.10
     * @since 10.11.10
     * @since 11.3.6
     */
    protected void setContentLength(XWikiResponse response, long length)
    {
        // Set the content length in the response
        response.setContentLengthLong(length);
    }

    /**
     * Helper used resolve the template passed to the action if the current user have access to it.
     * 
     * @param template the template to copy
     * @return the reference of the template if not empty and the current user have access to it
     * @since 12.10.6
     * @since 13.2RC1
     */
    protected DocumentReference resolveTemplate(String template)
    {
        if (StringUtils.isNotBlank(template)) {
            DocumentReference templateReference = getCurrentMixedDocumentReferenceResolver().resolve(template);

            // Make sure the current user have access to the template document before copying it
            if (getContextualAuthorizationManager().hasAccess(Right.VIEW, templateReference)) {
                return templateReference;
            }
        }

        return null;
    }

    /**
     * Helper used by various actions to initialize a document by copying a template to it.
     * 
     * @param document the document to update
     * @param template the template to copy
     * @param context the XWiki context
     * @return true if the document was updated, false otherwise (for example when the current user does not have view
     *         right on the template document)
     * @throws XWikiException when failing to copy the template
     * @since 12.10.6
     * @since 13.2RC1
     */
    protected boolean readFromTemplate(XWikiDocument document, String template, XWikiContext context)
        throws XWikiException
    {
        DocumentReference templateReference = resolveTemplate(template);

        if (templateReference != null) {
            document.readFromTemplate(templateReference, context);

            return true;
        }

        return false;
    }

    /**
     * Loop over the {@link RedirectionFilter} components until one of them perform a redirection. If none of the does,
     * the action continues normally.
     *
     * @param context the current wiki content
     * @return {@code true} if a redirection has been performed, {@code false} otherwise
     * @throws XWikiException in case of error during the execution of a redirection filter
     */
    private boolean handleRedirect(XWikiContext context) throws XWikiException
    {
        // If no redirection are expected, this step is skipped.
        if (this.supportRedirections()) {
            try {
                for (RedirectionFilter filter : this.componentManager.<RedirectionFilter>getInstanceList(
                    RedirectionFilter.class)) {
                    if (filter.redirect(context)) {
                        return true;
                    }
                }
            } catch (ComponentLookupException e) {
                throw new XWikiException("Failed to resolve the redirection filters list", e);
            }
        }
        return false;
    }
}

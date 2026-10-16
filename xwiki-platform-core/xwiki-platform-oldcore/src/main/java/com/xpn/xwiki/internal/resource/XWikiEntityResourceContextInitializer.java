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
package com.xpn.xwiki.internal.resource;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.rendering.internal.transformation.MutableRenderingContext;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.internal.entity.EntityResourceContextInitializer;
import org.xwiki.template.TemplateManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiContextInitializer;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

/**
 * Initializes and authenticates the XWiki Context, and prepares the documents of the request, before calling the
 * Handler of an Entity Resource Action, once XWiki and the targeted wiki are initialized. Until then, it starts their
 * initialization and displays its progress like the legacy {@code XWikiAction}s do, instead of blocking the request
 * until it's finished.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Singleton
public class XWikiEntityResourceContextInitializer implements EntityResourceContextInitializer
{
    private static final String INIT_TEMPLATE = "init.vm";

    private static final String UNKNOWN_ACTION = "unknown";

    private static final String AJAX = "ajax";

    @Inject
    private Container container;

    @Inject
    private Execution execution;

    @Inject
    private Provider<XWikiContextInitializer> xcontextInitializerProvider;

    @Inject
    private Provider<TemplateManager> templateManagerProvider;

    @Inject
    private Provider<RenderingContext> renderingContextProvider;

    @Inject
    private Provider<VelocityManager> velocityManagerProvider;

    @Override
    public boolean initialize(EntityResourceReference reference) throws ResourceReferenceHandlerException
    {
        if (!(this.container.getRequest() instanceof ServletRequest servletRequest)
            || !(this.container.getResponse() instanceof ServletResponse servletResponse)) {
            throw new ResourceReferenceHandlerException(
                String.format("Unsupported request type [%s]", this.container.getRequest()));
        }

        ExecutionContext econtext = this.execution.getContext();
        try {
            if (!isInitialized(servletRequest.getRequest(), servletResponse.getResponse(), econtext)) {
                return false;
            }

            XWikiContext xcontext = this.xcontextInitializerProvider.get().authenticate().initialize(econtext);
            if (!prepareDocuments(xcontext, servletResponse.getResponse())) {
                return false;
            }

            // The Handlers of Entity Resource Actions replace the legacy XWikiActions, which run with the action of
            // the URL set in the XWiki Context: code relying on XWikiContext#getAction() must keep working for them.
            xcontext.setAction(reference.getAction().getActionName());
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException(
                String.format("Failed to initialize the XWiki Context to handle [%s]", reference), e);
        }

        // Nested Handlers requiring authentication must not authenticate the request again.
        if (econtext != null) {
            econtext.setProperty(ResourceReferenceHandlerAuthenticatorListener.AUTHENTICATED_PROPERTY, true);
        }

        return true;
    }

    /**
     * Prepare the documents of the request (current document, its translation, and their Velocity bindings) like
     * {@code XWikiAction} does before calling the Handler of an Entity Resource Action, since Handlers rely on them.
     * The legacy {@code UnknownAction} is the one executing for the actions it doesn't know, so the documents are
     * prepared, and the access to them checked, for its {@value #UNKNOWN_ACTION} action (which requires view right).
     */
    private boolean prepareDocuments(XWikiContext xcontext, HttpServletResponse response) throws Exception
    {
        String ajax = xcontext.getRequest().get(AJAX);
        xcontext.put(AJAX, StringUtils.isNotBlank(ajax) && !"0".equals(ajax));

        xcontext.setAction(UNKNOWN_ACTION);
        try {
            xcontext.getWiki().prepareDocuments(xcontext.getRequest(), xcontext,
                this.velocityManagerProvider.get().getVelocityContext());
        } catch (XWikiException e) {
            if (e.getCode() != XWikiException.ERROR_XWIKI_ACCESS_DENIED) {
                throw e;
            }
            // Guests have already been redirected to the login form when checking their access.
            if (!response.isCommitted()) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
            }
            return false;
        }

        return true;
    }

    /**
     * Same as what {@code XWikiAction} does before executing an action: start the initialization of XWiki and of the
     * targeted wiki if needed, and answer the request if they're not initialized yet.
     */
    private boolean isInitialized(HttpServletRequest request, HttpServletResponse response,
        ExecutionContext econtext) throws Exception
    {
        XWikiContext xcontext = Utils.prepareContext("",
            new XWikiServletRequest(JakartaServletBridge.toJavax(request)),
            new XWikiServletResponse(JakartaServletBridge.toJavax(response)),
            new XWikiServletContext(JakartaServletBridge.toJavax(request.getServletContext())));
        if (econtext != null) {
            xcontext.declareInExecutionContext(econtext);
        }

        XWiki xwiki;
        try {
            // Don't show the initialization progress when the client explicitly asks to wait for the result.
            xwiki = XWiki.getXWiki("false".equals(request.getParameter("async")), xcontext);
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_DOES_NOT_EXIST) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, e.getMessage());
                return false;
            }
            throw e;
        }

        if (xwiki == null) {
            renderInitializationProgress(xcontext);
            return false;
        }

        return true;
    }

    private void renderInitializationProgress(XWikiContext xcontext) throws Exception
    {
        RenderingContext renderingContext = this.renderingContextProvider.get();
        MutableRenderingContext mutableRenderingContext =
            renderingContext instanceof MutableRenderingContext mutableContext ? mutableContext : null;
        if (mutableRenderingContext != null) {
            mutableRenderingContext.push(renderingContext.getTransformation(), renderingContext.getXDOM(),
                renderingContext.getDefaultSyntax(), INIT_TEMPLATE, renderingContext.isRestricted(),
                Syntax.XHTML_1_0);
        }

        xcontext.getResponse().setStatus(HttpServletResponse.SC_ACCEPTED);
        xcontext.getResponse().setContentType("text/html; charset=UTF-8");
        try {
            this.templateManagerProvider.get().render(INIT_TEMPLATE, xcontext.getResponse().getWriter());
        } finally {
            if (mutableRenderingContext != null) {
                mutableRenderingContext.pop();
            }
        }
        xcontext.getResponse().flushBuffer();
        xcontext.setFinished(true);
    }
}

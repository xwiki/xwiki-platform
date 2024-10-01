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
package com.xpn.xwiki.internal;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.context.ExecutionContext;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiContextInitializer;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Default implementation of {@link XWikiContextInitializer}.
 * 
 * @version $Id$
 * @since 9.11RC1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultXWikiContextInitializer implements XWikiContextInitializer
{
    @Inject
    private Container container;

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitResolver;

    /**
     * Used to create a new {@link XWikiContext}.
     */
    @Inject
    private XWikiStubContextProvider contextProvider;

    @Inject
    private Logger logger;

    private boolean authenticate;

    private boolean fallbackOnStub;

    @Override
    public XWikiContextInitializer authenticate()
    {
        this.authenticate = true;

        return this;
    }

    @Override
    public XWikiContextInitializer fallbackOnStub()
    {
        this.fallbackOnStub = true;

        return this;
    }

    @Override
    public XWikiContext initialize(ExecutionContext econtext) throws XWikiException
    {
        Request request = this.container.getRequest();

        XWikiContext xcontext;

        if (request instanceof ServletRequest servletRequest) {
            try {
                HttpServletRequest httpServletRequest = servletRequest.getJakartaHttpServletRequest();
                HttpServletResponse httpServletReponse =
                    ((ServletResponse) this.container.getResponse()).getJakartaHttpServletResponse();

                xcontext = initializeXWikiContext(httpServletRequest, httpServletReponse);

                // Put the XWikiContext in the ExecutionContext in case the authenticator needs it
                if (econtext != null) {
                    xcontext.declareInExecutionContext(econtext);
                }

                if (this.authenticate) {
                    authenticate(xcontext);
                }
            } catch (XWikiException e) {
                if (this.fallbackOnStub) {
                    xcontext = this.contextProvider.createStubContext();
                } else {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INIT,
                        "Failed to initialize XWikiContext", e);
                }
            }
        } else {
            if (this.fallbackOnStub) {
                xcontext = this.contextProvider.createStubContext();
            } else {
                throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INIT,
                    "Unsupported request type [" + request.getClass() + "]");
            }
        }

        // Put the XWikiContext in the ExecutionContext
        if (econtext != null) {
            xcontext.declareInExecutionContext(econtext);
        }

        return xcontext;
    }

    private static XWikiContext initializeXWikiContext(HttpServletRequest request, HttpServletResponse response)
        throws XWikiException
    {
        XWikiServletContext xwikiEngine =
            new XWikiServletContext(JakartaServletBridge.toJavax(request.getServletContext()));
        XWikiServletRequest xwikiRequest = new XWikiServletRequest(JakartaServletBridge.toJavax(request));
        XWikiServletResponse xwikiResponse = new XWikiServletResponse(JakartaServletBridge.toJavax(response));

        // Create the XWiki context.
        XWikiContext context = Utils.prepareContext("", xwikiRequest, xwikiResponse, xwikiEngine);

        // Initialize the XWiki database. XWiki#getXWiki(XWikiContext) calls XWikiContext.setWiki(XWiki).
        XWiki xwiki = XWiki.getXWiki(context);

        // Initialize the URL factory.
        context.setURLFactory(xwiki.getURLFactoryService().createURLFactory(context.getMode(), context));

        // Prepare the localized resources, according to the selected language.
        xwiki.prepareResources(context);

        return context;
    }

    private void authenticate(XWikiContext xcontext) throws XWikiException
    {
        // By default set guest as the user that is sending the request.
        xcontext.setUserReference(null);

        XWikiUser xwikiUser = xcontext.getWiki().checkAuth(xcontext);
        if (xwikiUser != null) {
            SpaceReference defaultUserSpace =
                new SpaceReference(XWiki.SYSTEM_SPACE, new WikiReference(xcontext.getWikiId()));
            DocumentReference userReference = this.explicitResolver.resolve(xwikiUser.getUser(), defaultUserSpace);

            xcontext
                .setUserReference(XWikiRightService.GUEST_USER.equals(userReference.getName()) ? null : userReference);

            this.logger.debug("Authenticated as [{}].", xwikiUser.getUser());
        }
    }

}

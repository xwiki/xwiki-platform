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
package com.xpn.xwiki.internal.context;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.HttpServletUtils;
import org.xwiki.container.servlet.ServletRequest;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Provide various helper to manipulate context request initialization.
 * 
 * @version $Id$
 * @since 11.8RC1
 * @since 10.3.5
 * @since 10.11.10
 */
@Component(roles = RequestInitializer.class)
@Singleton
public class RequestInitializer
{
    @Inject
    private Container container;

    @Inject
    private Logger logger;

    private URL restoreURL(String storedWikiId, Map<String, Serializable> contextStore, XWikiContext xcontext)
    {
        // Find and set the wiki corresponding to the request
        String requestWiki = (String) contextStore.get(XWikiContextContextStore.PROP_REQUEST_WIKI);
        if (requestWiki == null) {
            requestWiki = storedWikiId;
        } else {
            xcontext.setOriginalWikiId(requestWiki);
        }

        // Find the URL to put in the context request
        URL url = (URL) contextStore.get(XWikiContextContextStore.PROP_REQUEST_URL);
        if (url == null) {
            url = (URL) contextStore.get(XWikiContextContextStore.PROP_REQUEST_BASE);
        }

        // Try to deduce missing URL from the request wiki (if provided)
        if (url == null && requestWiki != null) {
            try {
                url = xcontext.getWiki().getServerURL(requestWiki, xcontext);
            } catch (MalformedURLException e) {
                this.logger.warn("Failed to get the URL for stored context wiki [{}]", requestWiki);
            }

            // Assume we always want to behave as a HTTP request when the wiki request is provided
            if (url == null) {
                url = HttpServletUtils.getSourceURL(xcontext.getRequest());
            }
        }

        return url;
    }

    /**
     * Initialize a request and URL factory for the passed wiki.
     * 
     * @param storedWikiId the context wiki
     * @param xcontext the XWiki context
     */
    public void restoreRequest(String storedWikiId, XWikiContext xcontext)
    {
        restoreRequest(storedWikiId, Collections.emptyMap(), xcontext);
    }

    /**
     * Initialize a request and URL factory for the passed wiki.
     * 
     * @param storedWikiId the context wiki
     * @param contextStore the enabled context elements
     * @param xcontext the XWiki context
     */
    public void restoreRequest(String storedWikiId, Map<String, Serializable> contextStore, XWikiContext xcontext)
    {
        URL url = restoreURL(storedWikiId, contextStore, xcontext);

        Map<String, String[]> parameters =
            (Map<String, String[]>) contextStore.get(XWikiContextContextStore.PROP_REQUEST_PARAMETERS);

        boolean daemon;

        String contextPath = null;

        // Fallback on the first request URL
        if (url == null) {
            XWikiRequest request = xcontext.getRequest();

            if (request != null) {
                url = HttpServletUtils.getSourceURL(request);

                contextPath = request.getContextPath();

                if (parameters == null) {
                    parameters = request.getParameterMap();
                }
            }

            // We don't want to take into account the context request URL when generating URLs
            daemon = true;
        } else {
            // Find the request context path
            contextPath = (String) contextStore.get(XWikiContextContextStore.PROP_REQUEST_CONTEXTPATH);

            // We want to take into account the context request URL when generating URLs
            daemon = false;
        }

        // Set the context request
        if (url != null) {
            restoreRequest(url, contextPath, parameters, daemon, xcontext);
        }
    }

    private void restoreRequest(URL url, String contextPath, Map<String, String[]> parameters, boolean daemon,
        XWikiContext xcontext)
    {
        XWikiServletRequestStub stubRequest = new XWikiServletRequestStub(url, contextPath, parameters);
        xcontext.setRequest(stubRequest);
        // Indicate that the URL should be taken into account when generating a URL
        stubRequest.setDaemon(daemon);
        this.container.setRequest(new ServletRequest(stubRequest));

        // Update to create the URL factory
        XWikiURLFactory urlFactory = xcontext.getURLFactory();
        if (urlFactory == null) {
            urlFactory = new XWikiServletURLFactory();
            xcontext.setURLFactory(urlFactory);
        }
        urlFactory.init(xcontext);
    }
}

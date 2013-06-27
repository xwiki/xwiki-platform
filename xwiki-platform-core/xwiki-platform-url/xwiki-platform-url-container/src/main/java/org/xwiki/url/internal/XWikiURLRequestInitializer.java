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
package org.xwiki.url.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.container.Request;
import org.xwiki.container.RequestInitializer;
import org.xwiki.container.RequestInitializerException;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.url.URLCreationException;
import org.xwiki.url.UnsupportedURLException;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.XWikiURLFactory;

/**
 * @version $Id$
 * @since 5.1M1
 */
public class XWikiURLRequestInitializer implements RequestInitializer
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The XWiki URL Factory used to generate a {@link XWikiURL} object out of the HTTP Servlet Request URL.
     * This object is then stored in the XWiki Request and later on stored in the XWiki Execution Context so that it
     * can be accessed in an environment-dependent way in all XWiki Components.
     */
    @Inject
    private XWikiURLFactory<URL, XWikiURL> urlFactory;

    @Override
    public void initialize(Request request) throws RequestInitializerException
    {
        // Only handle Servlet requests ATM.
        if (request instanceof ServletRequest) {
            HttpServletRequest httpServletRequest = ((ServletRequest) request).getHttpServletRequest();
            URL url = getURL(httpServletRequest);
            try {
                XWikiURL xwikiURL = this.urlFactory.createURL(url,
                    Collections.<String, Object> singletonMap("ignorePrefix", httpServletRequest.getContextPath()));
                request.setProperty(Request.XWIKI_URL, xwikiURL);
            } catch (URLCreationException iue) {
                throw new RequestInitializerException(String.format("Failed to extract XWiki URL from [%s]", url), iue);
            } catch (UnsupportedURLException uue) {
                // We got a URL type that we don't support, don't do anything. This can happen for example for REST
                // URLs since we don't parse them as the REST module handles its own URL by itself (delegated to
                // JAX-RS).
                this.logger.debug("Unsupported URL type, ignoring [{}]", url);
            }
        } else {
            this.logger.warn("We currently only support URL extraction for Servlet environments");
        }
    }

    /**
     * Helper method to reconstruct a URL based on the HTTP Servlet Request (since this feature isn't offered by the
     * Servlet specification).
     *
     * @param httpServletRequest the request from which to extract the URL
     * @return the URL as a real URL object
     * @throws RequestInitializerException if the original request isn't a valid URL (shouldn't happen)
     */
    private URL getURL(HttpServletRequest httpServletRequest) throws RequestInitializerException
    {
        URL result;
        StringBuffer url = httpServletRequest.getRequestURL();
        if (!StringUtils.isEmpty(httpServletRequest.getQueryString())) {
            url.append('?');
            url.append(httpServletRequest.getQueryString());
        }
        try {
            result = new URL(url.toString());
        } catch (MalformedURLException e) {
            // Shouldn't happen normally since the Servlet Container should always return valid URLs when
            // getRequestURL() is called...
            throw new RequestInitializerException(
                String.format("Failed to extract XWiki URL because of invalid Request URL [%s]", url.toString()));
        }
        return result;
    }
}

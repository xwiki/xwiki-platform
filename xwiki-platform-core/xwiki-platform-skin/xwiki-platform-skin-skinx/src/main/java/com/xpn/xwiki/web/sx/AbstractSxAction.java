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
package com.xpn.xwiki.web.sx;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.debug.DebugConfiguration;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.sx.SxSource.CachePolicy;

/**
 * Abstract Skin Extension action. Contains the logic to generate the response based on a extension source and a type of
 * extension that implementing classes must provide.
 *
 * @version $Id$
 * @since 1.7M2
 */
public abstract class AbstractSxAction extends XWikiAction
{
    /** How many milliseconds a file should be cached for if it sets CachePolicy to LONG, hardcoded to 30 days. */
    private static final long LONG_CACHE_DURATION = 30 * 24 * 3600 * 1000L;

    /** How many milliseconds a file should be cached for if it sets CachePolicy to SHORT, hardcoded to 1 day. */
    private static final long SHORT_CACHE_DURATION = 1 * 24 * 3600 * 1000L;

    /** What http header parameter is used to specify when a file was last modified. */
    private static final String LAST_MODIFIED_HEADER = "Last-Modified";

    /** What http header parameter is used to specify cache control. */
    private static final String CACHE_CONTROL_HEADER = "Cache-Control";

    /** What http header parameter is used to specify when the cache should expire. */
    private static final String CACHE_EXPIRES_HEADER = "Expires";

    /** The response will be sent to the browser as a byte array in this character set. */
    private static final String RESPONSE_CHARACTER_SET = "UTF-8";

    /** If the user passes this parameter in the URL, we will look for the script in the jar files. */
    private static final String JAR_RESOURCE_REQUEST_PARAMETER = "resource";

    private DebugConfiguration debugConfiguration;

    /** @return the logging object of the concrete subclass. */
    protected abstract Logger getLogger();

    /**
     * This method must be called by render(XWikiContext). Render is in charge of creating the proper source and
     * extension type, and pass it as an argument to this method which will forge the proper response using those.
     *
     * @param sxSource the source of the extension.
     * @param sxType the type of extension
     * @param context the XWiki context when rendering the skin extension.
     * @throws XWikiException when an error occurs when building the response.
     */
    public void renderExtension(SxSource sxSource, Extension sxType, XWikiContext context)
        throws XWikiException
    {
        XWikiResponse response = context.getResponse();

        String extensionContent = sxSource.getContent();

        response.setContentType(sxType.getContentType());

        if (sxSource.getLastModifiedDate() > 0) {
            response.setDateHeader(LAST_MODIFIED_HEADER, sxSource.getLastModifiedDate());
        }

        CachePolicy cachePolicy = sxSource.getCachePolicy();

        if (cachePolicy != CachePolicy.FORBID) {
            response.setHeader(CACHE_CONTROL_HEADER, "public");
        }
        if (cachePolicy == CachePolicy.LONG) {
            // Cache for one month (30 days)
            response.setDateHeader(CACHE_EXPIRES_HEADER, (new Date()).getTime() + LONG_CACHE_DURATION);
        } else if (cachePolicy == CachePolicy.SHORT) {
            // Cache for one day
            response.setDateHeader(CACHE_EXPIRES_HEADER, (new Date()).getTime() + SHORT_CACHE_DURATION);
        } else if (cachePolicy == CachePolicy.FORBID) {
            response.setHeader(CACHE_CONTROL_HEADER, "no-cache, no-store, must-revalidate");
        }

        if (getDebugConfiguration().isMinify()) {
            extensionContent = sxType.getCompressor().compress(extensionContent);
        }

        try {
            response.setContentLength(extensionContent.getBytes(RESPONSE_CHARACTER_SET).length);
            response.getOutputStream().write(extensionContent.getBytes(RESPONSE_CHARACTER_SET));
        } catch (IOException ex) {
            getLogger().warn("Failed to send SX content: [{}]", ex.getMessage());
        }

    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        SxSource sxSource;

        if (context.getRequest().getParameter(JAR_RESOURCE_REQUEST_PARAMETER) != null) {
            sxSource = new SxResourceSource(context.getRequest().getParameter(JAR_RESOURCE_REQUEST_PARAMETER));
        } else {
            if (context.getDoc().isNew()) {
                context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
                return "docdoesnotexist";
            }
            sxSource = new SxDocumentSource(context, getExtensionType());
        }

        try {
            renderExtension(sxSource, getExtensionType(), context);
        } catch (IllegalArgumentException e) {
            // Simply set a 404 status code and return null, so that no unneeded bytes are transfered
            context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
        return null;
    }

    protected DebugConfiguration getDebugConfiguration()
    {
        if (this.debugConfiguration == null) {
            this.debugConfiguration = Utils.getComponent(DebugConfiguration.class);
        }

        return this.debugConfiguration;
    }

    /**
     * Get the type of extension, depends on the type of action.
     *
     * @return a new object which extends Extension.
     */
    public abstract Extension getExtensionType();
}

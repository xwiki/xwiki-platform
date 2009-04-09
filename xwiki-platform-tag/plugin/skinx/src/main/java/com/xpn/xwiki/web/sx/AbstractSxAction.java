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
package com.xpn.xwiki.web.sx;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiAction;
import com.xpn.xwiki.web.XWikiRequest;
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
    protected static final long LONG_CACHE_DURATION = 30 * 24 * 3600 * 1000L;

    protected static final long SHORT_CACHE_DURATION = 1 * 24 * 3600 * 1000L;

    protected abstract Log getLog();

    /**
     * This method must be called by actual SX actions. Those last ones are in charge of creating the proper source and
     * extension type, and pass it as argument of this method which will forge the proper response using those.
     * 
     * @param sxSource the source of the extension.
     * @param sxType the type of extension
     * @param context the XWiki context when rendering the skin extension.
     * @throws XWikiException when an error occurs when building the response.
     */
    public void renderExtension(SxSource sxSource, Extension sxType, XWikiContext context)
        throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        String extensionContent = sxSource.getContent();

        response.setContentType(sxType.getContentType());

        if (sxSource.getLastModifiedDate() > 0) {
            response.setDateHeader("Last-Modified", sxSource.getLastModifiedDate());
        }

        CachePolicy cachePolicy = sxSource.getCachePolicy();

        if (cachePolicy != CachePolicy.FORBID) {
            response.setHeader("Cache-Control", "public");
        }
        if (cachePolicy == CachePolicy.LONG) {
            // Cache for one month (30 days)
            response.setDateHeader("Expires", (new Date()).getTime() + LONG_CACHE_DURATION);
        } else if (cachePolicy == CachePolicy.SHORT) {
            // Cache for one day
            response.setDateHeader("Expires", (new Date()).getTime() + SHORT_CACHE_DURATION);
        } else if (cachePolicy == CachePolicy.FORBID) {
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        }

        if (BooleanUtils.toBoolean(StringUtils.defaultIfEmpty(request.get("minify"), "true"))) {
            extensionContent = sxType.getCompressor().compress(extensionContent);
        }

        try {
            response.setContentLength(extensionContent.getBytes("UTF-8").length);
            response.getOutputStream().write(extensionContent.getBytes("UTF-8"));
        } catch (IOException ex) {
            getLog().warn("Failed to send SX content: " + ex.getMessage());
        }

    }

}

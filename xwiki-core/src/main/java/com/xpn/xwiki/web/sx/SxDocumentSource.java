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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Wiki Document source for Skin Extensions. This is the standard source for Skin Extensions, using an XWiki object of
 * an extension class in a wiki document.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class SxDocumentSource implements SxSource
{
    private XWikiDocument document;

    private XWikiContext context;

    private Extension extension;

    /** Logging helper. */
    private static final Log LOG = LogFactory.getLog(SxDocumentSource.class);

    /**
     * Constructor for this source.
     * 
     * @param context
     * @param type
     */
    public SxDocumentSource(XWikiContext context, Extension extension)
    {
        this.context = context;
        this.document = context.getDoc();
        this.extension = extension;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SxSource#getCachePolicy()
     */
    public CachePolicy getCachePolicy()
    {
        CachePolicy finalCache = CachePolicy.LONG;

        if (document.getObjects(extension.getClassName()) != null) {
            for (BaseObject sxObj : document.getObjects(extension.getClassName())) {
                try {
                    CachePolicy cache =
                        CachePolicy.valueOf(StringUtils.upperCase(StringUtils.defaultIfEmpty(sxObj
                            .getStringValue("cache"), "LONG")));
                    if (cache.compareTo(finalCache) > 0) {
                        finalCache = cache;
                    }
                } catch (Exception ex) {
                    LOG.warn(String.format("SX object [%s#%s] has an invalid cache policy: [%s]", document
                        .getFullName(), sxObj.getStringValue("name"), sxObj.getStringValue("cache")));
                }
            }
        }
        return finalCache;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SxSource#getContent()
     */
    public String getContent()
    {
        StringBuilder resultBuilder = new StringBuilder();

        if (document.getObjects(extension.getClassName()) != null) {
            for (BaseObject sxObj : document.getObjects(extension.getClassName())) {
                String sxContent = sxObj.getLargeStringValue("code");
                int parse = sxObj.getIntValue("parse");
                if (parse == 1) {
                    sxContent = context.getWiki().getRenderingEngine().interpretText(sxContent, document, context);
                }
                // Also add a newline, in case the different object contents don't end with a blank
                // line, and could cause syntax errors when concatenated.
                resultBuilder.append(sxContent + "\n");
            }
        }
        return resultBuilder.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see SxSource#getLastModifiedDate()
     */
    public long getLastModifiedDate()
    {
        return document.getDate().getTime();
    }

}

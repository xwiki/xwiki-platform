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

import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

/**
 * Wiki Document source for Skin Extensions. This is the standard source for Skin Extensions, using an XWiki object of
 * an extension class in a wiki document.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class SxDocumentSource implements SxSource
{
    /** The name of the property in the script extension object which contains the script content. */
    private static final String CONTENT_PROPERTY_NAME = "code";

    /** The name of the property in the script extension object which tells us if the content should be parsed. */
    private static final String PARSE_CONTENT_PROPERTY_NAME = "parse";

    /** The name of the property in the script extension object which contains the cache policy. */
    private static final String CACHE_POLICY_PROPERTY_NAME = "cache";

    /** Logging helper. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SxDocumentSource.class);

    /** The document containing the extension. */
    private XWikiDocument document;

    /** The current XWikiContext. */
    private XWikiContext context;

    /** The type of Extension for getting the right kind of object from the document. */
    private Extension extension;

    /**
     * Constructor for this extension source.
     * 
     * @param context The XWikiContext
     * @param extension The Extension type
     */
    public SxDocumentSource(XWikiContext context, Extension extension)
    {
        this.context = context;
        this.document = context.getDoc();
        this.extension = extension;
    }

    @Override
    public CachePolicy getCachePolicy()
    {
        CachePolicy finalCache = CachePolicy.LONG;

        if (this.document.getObjects(this.extension.getClassName()) != null) {
            for (BaseObject sxObj : this.document.getObjects(this.extension.getClassName())) {
                if (sxObj == null) {
                    continue;
                }
                try {
                    CachePolicy cache =
                        CachePolicy.valueOf(StringUtils.upperCase(StringUtils.defaultIfEmpty(sxObj
                            .getStringValue(CACHE_POLICY_PROPERTY_NAME), "LONG")));
                    if (cache.compareTo(finalCache) > 0) {
                        finalCache = cache;
                    }
                } catch (Exception ex) {
                    LOGGER.warn("SX object [{}#{}] has an invalid cache policy: [{}]",
                        new Object[]{this.document.getFullName(), sxObj.getStringValue("name"),
                            sxObj.getStringValue(CACHE_POLICY_PROPERTY_NAME)});
                }
            }
        }
        return finalCache;
    }

    @Override
    public String getContent()
    {
        StringBuilder resultBuilder = new StringBuilder();

        if (this.document.getObjects(this.extension.getClassName()) != null) {
            for (BaseObject sxObj : this.document.getObjects(this.extension.getClassName())) {
                if (sxObj == null) {
                    continue;
                }
                String sxContent = sxObj.getLargeStringValue(CONTENT_PROPERTY_NAME);
                int parse = sxObj.getIntValue(PARSE_CONTENT_PROPERTY_NAME);
                if (parse == 1) {
                    try {
                        StringWriter writer = new StringWriter();
                        VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
                        VelocityEngine engine = velocityManager.getVelocityEngine();
                        try {
                            VelocityContext vcontext = velocityManager.getVelocityContext();
                            engine.startedUsingMacroNamespace(this.document.getPrefixedFullName());
                            velocityManager.getVelocityEngine().evaluate(vcontext, writer,
                                this.document.getPrefixedFullName(), sxContent);
                            sxContent = writer.toString();
                        } finally {
                            engine.stoppedUsingMacroNamespace(this.document.getPrefixedFullName());
                        }
                    } catch (XWikiVelocityException ex) {
                        LOGGER.warn("Velocity errors while parsing skin extension [{}]: ",
                            this.document.getPrefixedFullName(), ex.getMessage());
                    }
                }
                // Also add a newline, in case the different object contents don't end with a blank
                // line, and could cause syntax errors when concatenated.
                resultBuilder.append(sxContent + "\n");
            }
        }
        return resultBuilder.toString();
    }

    @Override
    public long getLastModifiedDate()
    {
        return this.document.getDate().getTime();
    }

}

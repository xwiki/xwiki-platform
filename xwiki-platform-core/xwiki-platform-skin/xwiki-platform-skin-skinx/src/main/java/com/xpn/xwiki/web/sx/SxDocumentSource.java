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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReferenceFactory;
import org.xwiki.model.reference.ObjectPropertyReference;
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

    /** The name of the property in the script extension object which contains the content type. */
    private static final String CONTENT_TYPE_PROPERTY_NAME = "contentType";

    /** The name of the property in the script extension object which tells us if the content should be parsed. */
    private static final String PARSE_CONTENT_PROPERTY_NAME = "parse";

    /** The name of the property in the script extension object which contains the cache policy. */
    private static final String CACHE_POLICY_PROPERTY_NAME = "cache";

    /** The name of the property in the script extension object which contains the name of the object. */
    private static final String NAME_PROPERTY_NAME = "name";

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
                        new Object[]{this.document.getFullName(), sxObj.getStringValue(NAME_PROPERTY_NAME),
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

        List<BaseObject> objects = this.document.getObjects(this.extension.getClassName());
        if (objects != null) {
            for (BaseObject sxObj : objects) {
                if (sxObj == null) {
                    continue;
                }
                String sxContent = sxObj.getLargeStringValue(CONTENT_PROPERTY_NAME);
                int parse = sxObj.getIntValue(PARSE_CONTENT_PROPERTY_NAME);
                if ("LESS".equals(sxObj.getStringValue(CONTENT_TYPE_PROPERTY_NAME))) {
                    LESSCompiler lessCompiler = Utils.getComponent(LESSCompiler.class);
                    LESSResourceReferenceFactory lessResourceReferenceFactory =
                        Utils.getComponent(LESSResourceReferenceFactory.class);
                    ObjectPropertyReference objectPropertyReference =
                        new ObjectPropertyReference(CONTENT_PROPERTY_NAME, sxObj.getReference());
                    LESSResourceReference lessResourceReference =
                        lessResourceReferenceFactory.createReferenceForXObjectProperty(objectPropertyReference);
                    try {
                        sxContent = lessCompiler.compile(lessResourceReference, true, (parse == 1),
                            CachePolicy.FORBID.equals(getCachePolicy()));
                    } catch (LESSCompilerException e) {
                        // Set the error message in a CSS comment to help the developer understand why its SSX is not
                        // working (it will work only if the CSS minifier is not used).
                        sxContent = String.format("/* LESS errors while parsing skin extension [%s]. */\n/* %s */",
                            sxObj.getStringValue(NAME_PROPERTY_NAME), ExceptionUtils.getRootCauseMessage(e));
                    }
                } else if (parse == 1) {
                    try {
                        StringWriter writer = new StringWriter();
                        VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
                        VelocityContext vcontext = velocityManager.getVelocityContext();
                        velocityManager.getVelocityEngine().evaluate(vcontext, writer,
                            this.document.getPrefixedFullName(), sxContent);
                        sxContent = writer.toString();
                    } catch (XWikiVelocityException ex) {
                        LOGGER.warn("Velocity errors while parsing skin extension [{}] with content [{}]: ",
                            this.document.getPrefixedFullName(), sxContent, ExceptionUtils.getRootCauseMessage(ex));
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

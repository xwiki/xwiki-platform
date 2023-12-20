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
package org.xwiki.component.wiki.internal;

import java.lang.reflect.Type;
import java.time.LocalDateTime;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.cache.CacheControl;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Add support for a wiki content property to execute to {@link AbstractAsyncBaseObjectWikiComponent}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public abstract class AbstractAsyncContentBaseObjectWikiComponent extends AbstractAsyncBaseObjectWikiComponent
{
    @Inject
    protected ContentParser parser;

    @Inject
    protected ComponentManager componentManager;

    @Inject
    protected BlockAsyncRendererExecutor executor;

    @Inject
    @Named("macro")
    protected Transformation macroTransformation;

    @Inject
    protected CacheControl cacheControl;

    protected XDOM xdom;

    protected Syntax syntax;

    protected volatile XDOM preparedXDOM;

    protected volatile LocalDateTime preparedXDOMDate;

    @Override
    protected void initialize(BaseObject baseObject, Type roleType, String roleHint) throws WikiComponentException
    {
        super.initialize(baseObject, roleType, roleHint);

        XWikiDocument ownerDocument = baseObject.getOwnerDocument();

        this.syntax = ownerDocument.getSyntax();
        String content = baseObject.getStringValue(getContentPropertyName());

        // Parse the content
        this.xdom = this.parser.parse(content, this.syntax, ownerDocument.getDocumentReference());
    }

    /**
     * @return the content as {@link XDOM}
     * @since 15.10RC1
     */
    public XDOM getSourceContent()
    {
        return this.xdom;
    }

    /**
     * @return the syntax which was used to parse the content
     * @since 15.10RC1
     */
    public Syntax getSourceSyntax()
    {
        return this.syntax;
    }

    /**
     * Prepare (if not already prepared), cache and return a prepared version of the XDOM.
     * 
     * @return the prepared {@link XDOM}
     * @since 15.10RC1
     */
    public XDOM getPreparedContent()
    {
        // If the block is not prepared yet or if cache reset has been requested, prepare it
        if (this.preparedXDOMDate == null || !this.cacheControl.isCacheReadAllowed(this.preparedXDOMDate)) {
            synchronized (this.xdom) {
                if (this.preparedXDOMDate == null || this.cacheControl.isCacheReadAllowed(this.preparedXDOMDate)) {
                    // Clone the source content in cache the cache reset is forced
                    // TODO: might be better (mainly in term of retained memory) to reload the content from the
                    // document, instead
                    this.preparedXDOM = this.xdom.clone();

                    // Prepare the content
                    this.macroTransformation.prepare(this.preparedXDOM);

                    // Remember when it was prepared
                    this.preparedXDOMDate = LocalDateTime.now();
                }
            }
        }

        return this.preparedXDOM;
    }

    /**
     * @return the name of the property containing the wiki content
     */
    protected abstract String getContentPropertyName();
}

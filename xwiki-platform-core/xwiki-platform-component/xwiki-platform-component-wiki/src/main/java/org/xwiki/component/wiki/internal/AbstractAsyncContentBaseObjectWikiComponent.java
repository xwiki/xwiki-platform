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

import javax.inject.Inject;
import javax.inject.Named;

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

    protected XDOM xdom;

    protected Syntax syntax;

    @Override
    protected void initialize(BaseObject baseObject, Type roleType, String roleHint) throws WikiComponentException
    {
        super.initialize(baseObject, roleType, roleHint);

        XWikiDocument ownerDocument = baseObject.getOwnerDocument();

        this.syntax = ownerDocument.getSyntax();
        String content = baseObject.getStringValue(getContentPropertyName());

        // Parse the content
        this.xdom = this.parser.parse(content, this.syntax, ownerDocument.getDocumentReference());

        // Prepare the content
        this.macroTransformation.prepare(this.xdom);
    }

    /**
     * @return the name of the property containing the wiki content
     */
    protected abstract String getContentPropertyName();
}

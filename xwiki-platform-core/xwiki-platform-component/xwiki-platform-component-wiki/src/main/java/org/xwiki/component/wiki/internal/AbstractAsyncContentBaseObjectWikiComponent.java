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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

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
    protected final ContentParser parser;

    protected final XDOM xdom;

    protected final Syntax syntax;

    protected final ComponentManager componentManager;

    protected final BlockAsyncRendererExecutor executor;

    /**
     * @param baseObject the object containing ui extension setup
     * @param roleType the role Type implemented
     * @param roleHint the role hint for this role implementation
     * @param componentManager The XWiki content manager
     * @throws ComponentLookupException If module dependencies are missing
     * @throws WikiComponentException When failing to parse content
     */
    public AbstractAsyncContentBaseObjectWikiComponent(BaseObject baseObject, Type roleType, String roleHint,
        ComponentManager componentManager) throws ComponentLookupException, WikiComponentException
    {
        super(baseObject, roleType, roleHint);

        this.componentManager = componentManager;
        this.executor = componentManager.getInstance(BlockAsyncRendererExecutor.class);

        XWikiDocument ownerDocument = baseObject.getOwnerDocument();

        this.parser = componentManager.getInstance(ContentParser.class);

        this.syntax = ownerDocument.getSyntax();
        String content = baseObject.getStringValue(getContentPropertyName());
        this.xdom = this.parser.parse(content, syntax, ownerDocument.getDocumentReference());
    }

    /**
     * @return the name of the property containing the wiki content
     */
    protected abstract String getContentPropertyName();
}

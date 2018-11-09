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
package org.xwiki.panels.internal;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import javax.inject.Provider;
import javax.script.ScriptContext;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRenderer;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererDecorator;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererResult;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.internal.AbstractWikiUIExtension;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Provides a bridge between Panels defined in XObjects and {@link UIExtension}.
 *
 * @version $Id$
 * @since 4.3M1
 */
public class PanelWikiUIExtension extends AbstractWikiUIExtension implements BlockAsyncRendererDecorator
{
    private static final String SP_PANELDOC = "paneldoc";

    private final ScriptContextManager scriptContextManager;

    private final Provider<XWikiContext> xcontextProvider;

    /**
     * @param baseObject the object containing panel setup
     * @param id the ID of this UI extension
     * @param componentManager The XWiki content manager
     * @throws ComponentLookupException If module dependencies are missing
     * @throws WikiComponentException When failing to parse content
     */
    public PanelWikiUIExtension(BaseObject baseObject, String id, ComponentManager componentManager)
        throws ComponentLookupException, WikiComponentException
    {
        super(baseObject, UIExtension.class, id, componentManager);

        // TODO: handle scope dynamically, in the meantime it's hardcoded to "global" for backward compatibility
        this.scope = WikiComponentScope.GLOBAL;

        this.scriptContextManager = componentManager.getInstance(ScriptContextManager.class);
        this.xcontextProvider = componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
    }

    @Override
    public String getId()
    {
        return getRoleHint();
    }

    @Override
    public String getExtensionPointId()
    {
        return "platform.panels";
    }

    @Override
    protected BlockAsyncRendererConfiguration configure()
    {
        BlockAsyncRendererConfiguration configuration = super.configure();

        configuration.setDefaultSyntax(this.syntax);

        return configuration;
    }

    private Object before() throws RenderingException
    {
        ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();

        // Remember previous value of "paneldoc"
        Document paneldoc = (Document) scriptContext.getAttribute(SP_PANELDOC, ScriptContext.ENGINE_SCOPE);

        // Make panel document available from the panel context
        try {
            scriptContext.setAttribute(SP_PANELDOC, getPanelDocument(), ScriptContext.ENGINE_SCOPE);
        } catch (XWikiException e) {
            throw new RenderingException("Failed to get panel document", e);
        }

        return paneldoc;
    }

    private void after(Object obj)
    {
        this.scriptContextManager.getCurrentScriptContext().setAttribute(SP_PANELDOC, obj, ScriptContext.ENGINE_SCOPE);
    }

    @Override
    public BlockAsyncRendererResult render(AsyncRenderer renderer, boolean async, boolean cached)
        throws RenderingException
    {
        Object obj = before();

        try {
            return (BlockAsyncRendererResult) renderer.render(async, cached);
        } finally {
            after(obj);
        }
    }

    private Document getPanelDocument() throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document = xcontext.getWiki().getDocument(getDocumentReference(), xcontext);

        return document.newDocument(xcontext);
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Collections.emptyMap();
    }

    @Override
    public WikiComponentScope getScope()
    {
        // TODO: handle scope dynamically, in the meantime it's hardcoded to "global" for backward compatibility
        return WikiComponentScope.GLOBAL;
    }
}

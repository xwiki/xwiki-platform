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
package org.xwiki.uiextension.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.AsyncRenderer;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererDecorator;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererResult;
import org.xwiki.uiextension.UIExtension;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Represents a dynamic component instance of a UI Extension (ie a UI Extension defined in a Wiki page) that we register
 * against the Component Manager.
 *
 * @version $Id$
 * @since 4.2M3
 */
public class WikiUIExtension extends AbstractWikiUIExtension implements BlockAsyncRendererDecorator
{
    /**
     * The key used for the UIX context in the script context.
     */
    public static final String CONTEXT_UIX_KEY = "uix";

    /**
     * The key used for the UIX document in the UIX context.
     */
    public static final String CONTEXT_UIX_DOC_KEY = "doc";

    /**
     * @see #WikiUIExtension
     */
    private final String id;

    /**
     * @see #WikiUIExtension
     */
    private final String extensionPointId;

    private final Provider<XWikiContext> xcontextProvider;

    /**
     * Parameter manager for this extension.
     */
    private WikiUIExtensionParameters parameters;

    /**
     * Default constructor.
     *
     * @param baseObject the object containing panel setup
     * @param roleHint the role hint of the component to create
     * @param id the id of the extension
     * @param extensionPointId ID of the extension point this extension is designed for
     * @param componentManager The XWiki content manager
     * @throws ComponentLookupException If module dependencies are missing
     * @throws WikiComponentException When failing to parse content
     */
    public WikiUIExtension(BaseObject baseObject, String roleHint, String id, String extensionPointId,
        ComponentManager componentManager) throws ComponentLookupException, WikiComponentException
    {
        super(baseObject, UIExtension.class, roleHint, componentManager);

        this.id = id;
        this.extensionPointId = extensionPointId;

        this.xcontextProvider = componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
    }

    /**
     * Set the extension parameters.
     *
     * @param parameters the extension parameters
     */
    public void setParameters(WikiUIExtensionParameters parameters)
    {
        this.parameters = parameters;
    }

    /**
     * Set the scope of the extension.
     *
     * @param scope the scope of the extension
     */
    public void setScope(WikiComponentScope scope)
    {
        this.scope = scope;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    @Override
    public String getExtensionPointId()
    {
        return this.extensionPointId;
    }

    @Override
    public Map<String, String> getParameters()
    {
        if (this.parameters != null) {
            return this.parameters.get();
        } else {
            return Collections.emptyMap();
        }
    }

    private Object before() throws RenderingException
    {
        // Get the document holding the UIX and put it in the UIX context
        XWikiContext xcontext = this.xcontextProvider.get();
        XWikiDocument document;
        try {
            document = xcontext.getWiki().getDocument(getDocumentReference(), xcontext);
        } catch (XWikiException e) {
            throw new RenderingException("Failed to get ui extension document", e);
        }
        Map<String, Object> uixContext = new HashMap<>();
        uixContext.put(WikiUIExtension.CONTEXT_UIX_DOC_KEY, document.newDocument(xcontext));

        // Remember the previous uix context to restore it
        Map<String, Object> previousUIXContext = (Map<String, Object>) xcontext.get(WikiUIExtension.CONTEXT_UIX_KEY);
        // Put the UIX context in the XWiki context
        xcontext.put(WikiUIExtension.CONTEXT_UIX_KEY, uixContext);

        return previousUIXContext;
    }

    private void after(Object uixContext)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Restore previous uid context
        xcontext.put(WikiUIExtension.CONTEXT_UIX_KEY, uixContext);
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
}

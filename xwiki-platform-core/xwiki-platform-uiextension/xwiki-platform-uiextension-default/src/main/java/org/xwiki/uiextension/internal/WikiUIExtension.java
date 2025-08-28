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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.internal.block.BlockAsyncRenderer;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererDecorator;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererResult;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.uiextension.UIExtension;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static javax.script.ScriptContext.ENGINE_SCOPE;

/**
 * Represents a dynamic component instance of a UI Extension (ie a UI Extension defined in a Wiki page) that we register
 * against the Component Manager.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Component(roles = WikiUIExtension.class)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
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
     * The key used to indicate of the UIX is executed in an inline context.
     *
     * @since 14.0RC1
     */
    public static final String CONTEXT_UIX_INLINE_KEY = "inline";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ScriptContextManager scriptContextManager;

    /**
     * @see #WikiUIExtension
     */
    private String id;

    /**
     * @see #WikiUIExtension
     */
    private String extensionPointId;

    /**
     * Parameter manager for this extension.
     */
    private WikiUIExtensionParameters parameters;

    /**
     * @param baseObject the object containing panel setup
     * @param roleHint the role hint of the component to create
     * @param id the id of the extension
     * @param extensionPointId ID of the extension point this extension is designed for
     * @throws WikiComponentException when failing to parse content
     * @since 15.9RC1
     */
    public void initialize(BaseObject baseObject, String roleHint, String id, String extensionPointId)
        throws WikiComponentException
    {
        super.initialize(baseObject, UIExtension.class, roleHint);

        this.id = id;
        this.extensionPointId = extensionPointId;
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

    private PreviousContexts before(boolean inline) throws RenderingException
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
        uixContext.put(CONTEXT_UIX_DOC_KEY, document.newDocument(xcontext));
        uixContext.put(CONTEXT_UIX_INLINE_KEY, inline);

        // Remember the previous uix context to restore it
        @SuppressWarnings("unchecked")
        Map<String, Object> previousUIXContext = (Map<String, Object>) xcontext.get(CONTEXT_UIX_KEY);
        // Put the UIX context in the XWiki context. Note that this is deprecated and using the UIX templates is
        // preferred.
        xcontext.put(CONTEXT_UIX_KEY, uixContext);
        // Put the UIX context in the velocity context "uix" key.
        ScriptContext scriptContext = this.scriptContextManager.getCurrentScriptContext();
        Object previousScriptUIXContext = scriptContext.getAttribute(CONTEXT_UIX_KEY, ENGINE_SCOPE);
        scriptContext.setAttribute(CONTEXT_UIX_KEY, uixContext, ENGINE_SCOPE);
        return new PreviousContexts(previousUIXContext, previousScriptUIXContext);
    }

    private void after(PreviousContexts contexts)
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Restore previous uix context in the XWiki and Velocity contexts.
        xcontext.put(CONTEXT_UIX_KEY, contexts.previousUIXContext);
        this.scriptContextManager.getCurrentScriptContext().setAttribute(CONTEXT_UIX_KEY,
            contexts.previousScriptUIXContext, ENGINE_SCOPE);
    }

    @Override
    public BlockAsyncRendererResult render(BlockAsyncRenderer renderer, boolean async, boolean cached)
        throws RenderingException
    {
        PreviousContexts contexts = before(renderer.isInline());

        try {
            return renderer.render(async, cached);
        } finally {
            after(contexts);
        }
    }

    /**
     * Store the value of the contexts modified during {@link #before(boolean)} to restore them on
     * {@link #after(PreviousContexts)}.
     */
    private final class PreviousContexts
    {
        /**
         * Save the value of "uix" in the {@link XWikiContext} before the initialization, to restore it after the
         * rendering.
         */
        private final Object previousUIXContext;

        /**
         * Save the value of "uix" in the {@link ScriptContext} before the initialization, to restore it after the
         * rendering.
         */
        private final Object previousScriptUIXContext;

        private PreviousContexts(Object previousUIXContext, Object previousScriptUIXContext)
        {
            this.previousUIXContext = previousUIXContext;
            this.previousScriptUIXContext = previousScriptUIXContext;
        }
    }
}

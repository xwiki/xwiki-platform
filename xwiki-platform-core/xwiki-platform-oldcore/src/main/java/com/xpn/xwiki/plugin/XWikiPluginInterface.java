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
package com.xpn.xwiki.plugin;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Deprecated platform extension mechanism allowing three things:
 * <ul>
 * <li>provide custom services usable from other parts of the platform, or as public services in Velocity scripts using
 * a {@link PluginApi} wrapper; this can now be achieved using components for use inside Java, or using
 * {@link org.xwiki.script.service.ScriptService} components for public scriptable APIs</li>
 * <li>alter the request processing by implementing one or more of the hook methods that plug into the rendering engine,
 * such as {@link #endParsing(String, XWikiContext)} or {@link #downloadAttachment(XWikiAttachment, XWikiContext)}; this
 * can now be achieved using {@link org.xwiki.rendering.transformation.Transformation rendering transformations} for
 * changing the rendering result, or {@link org.xwiki.observation.EventListener event listeners} for doing custom
 * actions when different events occur</li>
 * <li>perform extra initialization steps whenever starting the platform, or when loading each virtual wiki; this can be
 * achieved by writing {@link org.xwiki.observation.EventListener event listeners} that wait for
 * {@link org.xwiki.observation.event.ApplicationStartedEvent application events} or
 * {@link org.xwiki.bridge.event.WikiEvent wiki events}</li>
 * </ul>
 * <p>
 * In order for a plugin to be active, it must be registered in {@code xwiki.cfg}.
 * </p>
 * <p>
 * Public scriptable APIs are implemented in a custom {@link PluginApi} wrapper, which is instantiated by calling the
 * {@link #getPluginApi(XWikiPluginInterface, XWikiContext)} method. If a plugin doesn't need a scriptable API, it
 * should return {@code null} instead.
 * </p>
 * <p>
 * The methods of a specific plugin <strong>are called only if the actual class implements that method</strong>, so
 * inherited methods are never called. If a plugin wants to provide an implementation for one of the hook methods, it
 * must provide an implementation for it, even if that implementation only calls a parent method.
 * </p>
 *
 * @version $Id$
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Deprecated
public interface XWikiPluginInterface
{
    /**
     * The name which can be used for accessing this plugin from
     * {@link com.xpn.xwiki.XWiki#getPlugin(String, XWikiContext)}.
     *
     * @return the plugin name, usually a lowercase identifier
     */
    String getName();

    /**
     * Get a public scriptable API that can be used to call methods of the (privileged) plugin.
     *
     * @param plugin the plugin instance to wrap
     * @param context the current request context
     * @return an instance of the corresponding API, or {@code null} if the plugin doesn't want to expose a public API
     */
    Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context);

    /**
     * Global initialization, called when the platform is started and the plugin list is initialized.
     *
     * @param context the current context, should not be stored since it will become mostly unreliable once the initial
     *            request is terminated
     * @throws XWikiException if the plugin fails to initialize
     */
    void init(XWikiContext context) throws XWikiException;

    /**
     * Initialization method called each time a virtual wiki is started, allowing plugins to perform initialization
     * tasks needed in each wiki.
     *
     * @param context the current request context
     */
    void virtualInit(XWikiContext context);

    /**
     * A "flush cache" method usually called by the global {@link com.xpn.xwiki.XWiki#flushCache(XWikiContext)},
     * requesting plugins to clear their cached data. When this method is called, plugins should discard all the
     * information they hold from the wiki and re-read it if needed.
     *
     * @param context the current request context
     */
    void flushCache(XWikiContext context);

    /**
     * Hook method called at the start of the response generation process, before the parsing of the root template
     * begins. This allows a plugin to initialize its per-request variables, or to modify the (velocity) context. This
     * method is called at most once per request. It is not called at all if the response is not rendered using a
     * velocity template, for example during the PDF export or for AJAX requests that don't send back a rendered
     * response.
     *
     * @param context the current request context
     * @see #endParsing(String, XWikiContext) the pair method called at the end of the response generation process
     */
    void beginParsing(XWikiContext context);

    /**
     * Hook method, called before the 1.0 rendering engine starts processing a wiki document, after
     * {@link #beginParsing(XWikiContext)} and before {@link #commonTagsHandler(String, XWikiContext)}. This allows a
     * plugin to initialize its per-rendering variables, or to modify the (velocity) context. This method can be called
     * multiple times per request, once for each time the 1.0 rendering engine is invoked. At this point the content is
     * not yet processed by any of the rendering components.
     *
     * @param context the current request context
     * @see #endRendering(XWikiContext) the pair method called at the end of the content processing
     */
    void beginRendering(XWikiContext context);

    /**
     * Hook method called during the 1.0 rendering process, after {@link #beginRendering(XWikiContext)} and right before
     * {@link #startRenderingHandler(String, XWikiContext)}. At this point the content has already been processed by all
     * of the important rendering components (Velocity, Radeox, Groovy). The content can be altered before returning,
     * and the rendering process will continue on the returned value.
     *
     * @param content the current content being rendered; already processed by Radeox, Velocity and Groovy
     * @param context the current request context
     * @return the processed content, must be the same as the input content if no processing is needed
     */
    String commonTagsHandler(String content, XWikiContext context);

    /**
     * Hook method called during the 1.0 rendering process, after {@link #commonTagsHandler(String, XWikiContext)} and
     * before {@link #outsidePREHandler(String, XWikiContext)}. The content can be altered before returning. This method
     * should be avoided, since the place where the hook is used isn't actually reliable, since at this point the
     * rendering process is almost done.
     *
     * @param content the current content being rendered; already processed by Radeox, Velocity and Groovy
     * @param context the current request context
     * @return the processed content, must be the same as the input content if no processing is needed
     * @see #endRenderingHandler(String, XWikiContext) the pair method called after the content is processed
     * @see #beginRendering(XWikiContext) the method called before the content has been processed by the rendering
     *      engine
     */
    String startRenderingHandler(String content, XWikiContext context);

    /**
     * Hook method called during the 1.0 rendering process, once for each line of content outside {@code pre} blocks,
     * after {@link #startRenderingHandler(String, XWikiContext)} and before
     * {@link #endRenderingHandler(String, XWikiContext)}. The content can be altered before returning.
     *
     * @param line the current line being rendered; already processed by Radeox, Velocity and Groovy
     * @param context the current request context
     * @return the processed line, must be the same as the input content if no processing is needed
     * @see #insidePREHandler(String, XWikiContext) the pair method called for content inside {@code pre} blocks
     */
    String outsidePREHandler(String line, XWikiContext context);

    /**
     * Hook method called during the 1.0 rendering process, once for each line of content inside {@code pre} blocks,
     * after {@link #startRenderingHandler(String, XWikiContext)} and before
     * {@link #endRenderingHandler(String, XWikiContext)}. The content can be altered before returning.
     *
     * @param line the current line being rendered; already processed by Radeox, Velocity and Groovy
     * @param context the current request context
     * @return the processed line, must be the same as the input content if no processing is needed
     * @see #outsidePREHandler(String, XWikiContext) the pair method called for content outside {@code pre} blocks
     */
    String insidePREHandler(String line, XWikiContext context);

    /**
     * Hook method called during the 1.0 rendering process, after {@link #outsidePREHandler(String, XWikiContext)} and
     * right before {@link #endRendering(XWikiContext)}. The content can be altered before returning, and the returned
     * value is the one coming out of the rendering process.
     *
     * @param content the current content being rendered; already processed by Radeox, Velocity and Groovy
     * @param context the current request context
     * @return the processed content, must be the same as the input content if no processing is needed
     * @see #startRenderingHandler(String, XWikiContext) the pair method called before the content is processed
     * @see #endRendering(XWikiContext) the method called after the full rendering process is finished
     */
    String endRenderingHandler(String content, XWikiContext context);

    /**
     * Hook method, called after the 1.0 rendering engine finished processing a wiki document. This allows a plugin to
     * clean up its per-rendering variables, or to modify the (velocity) context. This method can be called multiple
     * times per request, once for each time the 1.0 rendering engine is invoked. At this point the content is processed
     * by all of the rendering components and is ready to be included in the response.
     *
     * @param context the current request context
     * @see #endRenderingHandler(String, XWikiContext) allows to also alter the rendering result
     */
    void endRendering(XWikiContext context);

    /**
     * Hook method called at the end of the response generation process, after the parsing of the root template is
     * finished. This allows a plugin to clean up its per-request variables, to modify the (velocity) context, or to
     * post-process the response. This method is called at most once per request. It is not called at all if the
     * response is not rendered using a velocity template, for example during the PDF export or for AJAX requests that
     * don't send back a rendered response.
     *
     * @param content the full response to send to the client
     * @param context the current request context
     * @return the post-processed content, must be the same as the input content if no processing is needed
     * @see #beginParsing(XWikiContext) the pair method called at the start of the response generation process
     */
    String endParsing(String content, XWikiContext context);

    /**
     * Plugin extension point allowing the plugin to perform modifications to an attachment when the user opens a
     * document attachment. The plugin is passed the original attachment and it has to return the new modified
     * attachment.
     *
     * @param attachment the original attachment
     * @param context the current request context
     * @return the modified attachment
     */
    XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context);
}

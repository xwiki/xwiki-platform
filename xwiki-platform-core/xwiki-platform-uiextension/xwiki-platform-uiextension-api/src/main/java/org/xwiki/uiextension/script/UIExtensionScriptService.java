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
package org.xwiki.uiextension.script;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionFilter;
import org.xwiki.uiextension.UIExtensionManager;
import org.xwiki.uiextension.internal.UIExtensionRenderer;

/**
 * Allows scripts to easily access Interface Extensions APIs.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("uix")
@Singleton
public class UIExtensionScriptService implements ScriptService
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * We use the Context Component Manager to lookup UI Extensions registered as components.
     * The Context Component Manager allows Extensions to be registered for a specific user, for a specific wiki or for
     * a whole farm.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * The default UIExtensionManager.
     */
    @Inject
    private UIExtensionManager uiExtensionManager;

    /**
     * Used to execute and render the UI extensions in a given output syntax.
     */
    @Inject
    private UIExtensionRenderer uiExtensionRenderer;
    
    /**
     * Utility method to split a list of extension names, for example {code}"Panels.Apps,Panels.QuickLinks"{code} to get
     * a List containing those names.
     *
     * @param nameList the list of extension names to split
     * @return a List containing all the names from the given String.
     */
    private String[] parseFilterParameters(String nameList)
    {
        return nameList.replace(" ", "").split(",");
    }

    /**
     * Retrieves all the {@link UIExtension}s for a given Extension Point.
     *
     * @param extensionPointId The ID of the Extension Point to retrieve the {@link UIExtension}s for
     * @return the list of {@link UIExtension} for the given Extension Point
     */
    public List<UIExtension> getExtensions(String extensionPointId)
    {
        return this.uiExtensionManager.get(extensionPointId);
    }

    /**
     * Retrieves the list of {@link UIExtension} for a given Extension Point.
     *
     * Examples:
     * <ul>
     * <li>Get only the {@link UIExtension}s with the given IDs for the Extension Point "platform.example"
     * <pre>$services.uix.getExtensions('platform.example', {'select' : 'id1, id2, id3'})</pre></li>
     * <li>Get all the {@link UIExtension}s for the Extension Point "platform.example" except the
     * {@link UIExtension}s with the IDs "id2" and "id3"
     * <pre>$services.uix.getExtensions('platform.example', {'exclude' : 'id2, id3'})</pre></li>
     * <li>Get all the {@link UIExtension}s for the Extension Point "platform.example" and order them by one of their
     * parameter
     * <pre>$services.uix.getExtensions('platform.example', {'sortByParameter' : 'parameterKey'})</pre></li>
     * <li>Get only the {@link UIExtension}s with the given IDs for the Extension Point "platform.example" and order
     * them by one of their parameter
     * <pre>$services.uix.getExtensions('platform.example',
     * {'select' : 'id1, id2, id3', 'sortByParameter' : 'parameterKey'})</pre></li>
     * </ul>
     *
     * @param extensionPointId The ID of the Extension Point to retrieve the {@link UIExtension}s for
     * @param filters Optional filters to apply before retrieving the list
     * @return the list of {@link UIExtension} for the given Extension Point
     */
    public List<UIExtension> getExtensions(String extensionPointId, Map<String, String> filters)
    {
        List<UIExtension> extensions = getExtensions(extensionPointId);

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String filterHint = entry.getKey();

            try {
                UIExtensionFilter filter =
                    contextComponentManagerProvider.get().getInstance(UIExtensionFilter.class, filterHint);
                extensions = filter.filter(extensions, this.parseFilterParameters(entry.getValue()));
            } catch (ComponentLookupException e) {
                logger.warn("Unable to find a UIExtensionFilter for hint [{}] "
                    + "while getting UIExtensions for extension point [{}]. Root cause is [{}]", filterHint,
                    extensionPointId, ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return extensions;
    }

    /**
     * Executes and renders the specified UI extension in the given output syntax.
     * <p>
     * Contrary to {@code $services.rendering.render($uix.execute(), 'html/5.0')}, the UI extension is also
     * <em>executed</em> with the given output syntax as target syntax. This matters for UI extensions based on
     * templates or wiki pages: the content they produce (e.g. a raw block) is tagged with the target syntax found in
     * the rendering context, which can be different from the syntax used afterwards to render it (e.g. when the page
     * is loaded with {@code ?outputSyntax=plain}), in which case that content is silently dropped.
     * <p>
     * Example: {@code $services.uix.render($uix, 'html/5.0')}
     * <p>
     * Note that a UI extension for which asynchronous execution is enabled is still rendered as a placeholder that is
     * replaced on the client side: forcing the output syntax doesn't make the execution synchronous.
     *
     * @param extension the UI extension to execute and render
     * @param outputSyntax the syntax to execute and render the UI extension in, e.g. {@code html/5.0}
     * @return the result of rendering the given UI extension in the given output syntax, or {@code null} if the UI
     *         extension could not be rendered (in which case a warning is logged)
     * @since 18.8.0RC1
     */
    @Unstable
    public String render(UIExtension extension, Syntax outputSyntax)
    {
        if (extension == null) {
            this.logger.warn("Can't render a null UI extension.");
            return null;
        }

        return render(List.of(extension), outputSyntax, null);
    }

    /**
     * Executes and renders all the UI extensions of the specified extension point in the given output syntax. The
     * results are concatenated, without any separator, in the order in which the UI extensions are returned by
     * {@link #getExtensions(String)}.
     * <p>
     * Example: {@code $services.uix.renderExtensions('org.xwiki.platform.attachment.actions', 'html/5.0')}
     *
     * @param extensionPointId the identifier of the extension point whose UI extensions to execute and render
     * @param outputSyntax the syntax to execute and render the UI extensions in, e.g. {@code html/5.0}
     * @return the concatenated result of rendering the UI extensions of the given extension point, or {@code null} if
     *         they could not be rendered (in which case a warning is logged)
     * @see #render(UIExtension, Syntax)
     * @since 18.8.0RC1
     */
    @Unstable
    public String renderExtensions(String extensionPointId, Syntax outputSyntax)
    {
        return render(getExtensions(extensionPointId), outputSyntax, extensionPointId);
    }

    /**
     * Executes and renders the UI extensions of the specified extension point that match the given filters, in the
     * given output syntax. The results are concatenated, without any separator, in the order in which the UI
     * extensions are returned by {@link #getExtensions(String, Map)}.
     * <p>
     * Example:
     * {@code $services.uix.renderExtensions('org.xwiki.platform.html.head', 'html/5.0', {'sortByParameter': 'order'})}
     *
     * @param extensionPointId the identifier of the extension point whose UI extensions to execute and render
     * @param outputSyntax the syntax to execute and render the UI extensions in, e.g. {@code html/5.0}
     * @param filters the filters to apply before rendering, see {@link #getExtensions(String, Map)}
     * @return the concatenated result of rendering the matching UI extensions, or {@code null} if they could not be
     *         rendered (in which case a warning is logged)
     * @see #render(UIExtension, Syntax)
     * @see #getExtensions(String, Map)
     * @since 18.8.0RC1
     */
    @Unstable
    public String renderExtensions(String extensionPointId, Syntax outputSyntax, Map<String, String> filters)
    {
        List<UIExtension> extensions =
            filters == null ? getExtensions(extensionPointId) : getExtensions(extensionPointId, filters);

        return render(extensions, outputSyntax, extensionPointId);
    }

    private String render(List<UIExtension> extensions, Syntax outputSyntax, String extensionPointId)
    {
        // Mention the extension point in the logs when we have one, to make a bad output syntax easier to locate.
        String origin =
            extensionPointId == null ? "" : String.format(" of the extension point [%s]", extensionPointId);

        if (outputSyntax == null) {
            this.logger.warn("Can't render the UI extensions{} without an output syntax.", origin);
            return null;
        }

        try {
            // All the call sites we know of render standalone content, so we don't execute the UI extensions in an
            // inline context.
            return this.uiExtensionRenderer.render(extensions, outputSyntax, false);
        } catch (RenderingException e) {
            this.logger.warn("Failed to render the UI extensions{} in syntax [{}]. Root cause is [{}]", origin,
                outputSyntax.toIdString(), ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}

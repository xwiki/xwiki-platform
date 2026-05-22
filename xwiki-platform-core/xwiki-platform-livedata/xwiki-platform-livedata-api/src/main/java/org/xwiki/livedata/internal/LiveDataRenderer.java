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
package org.xwiki.livedata.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.skinx.SkinExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Collections.singletonMap;
import static org.xwiki.security.authorization.Right.SCRIPT;

/**
 * Renders a Live Data. Can be called from different context, in particular from the Live Data macro and the Live Data
 * script service.
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
@Component(roles = LiveDataRenderer.class)
@Singleton
public class LiveDataRenderer
{
    /**
     * Used to add default Live Data configuration values.
     */
    @Inject
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigResolver;

    @Inject
    private LiveDataRendererConfiguration liveDataRendererConfiguration;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * The component used to load the JavaScript code of the Live Data widget.
     */
    @Inject
    @Named("jsfx")
    private SkinExtension jsfx;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private RenderingContext renderingContext;

    /**
     * Execute the Live Data and return a {@link Block}.
     *
     * @param parameters the parameters used to render the Live Data
     * @param advancedParameters a map containing the advanced parameters of the Live Data
     * @param restricted when {@code true}, the Live Data is contained in a restricted environment, and its content
     *     will be considered as untrusted
     * @return the Live Data {@link Block}
     * @throws LiveDataException in case of issue when resolving the Live Data configuration or when serializing the
     *     advanced parameters to json
     */
    public Block execute(LiveDataRendererParameters parameters, Map<?, ?> advancedParameters, boolean restricted)
        throws LiveDataException
    {
        try {
            return execute(parameters, new ObjectMapper().writeValueAsString(advancedParameters), restricted);
        } catch (JsonProcessingException e) {
            throw new LiveDataException(
                "Failed to to serialize the advanced parameters [%s] to json.".formatted(advancedParameters),
                e);
        }
    }

    /**
     * Execute the Live Data and return a {@link Block}.
     *
     * @param parameters the parameters used to render the Live Data
     * @param advancedParameters a json string containing the advanced parameters of the Live Data
     * @param restricted when {@code true}, the Live Data is contained in a restricted environment, and its content
     *     will be considered as untrusted
     * @return the Live Data {@link Block}
     * @throws LiveDataException in case of issue when resolving the Live Data configuration
     */
    public Block execute(LiveDataRendererParameters parameters, String advancedParameters, boolean restricted)
        throws LiveDataException
    {
        // Load the JavaScript code of the Live Data element.
        Map<String, Object> skinExtensionParameters = singletonMap("forceSkinAction", Boolean.TRUE);
        this.jsfx.use("uicomponents/widgets/liveData.js", skinExtensionParameters);

        GroupBlock output = new GroupBlock();
        output.setParameter("class", "liveData loading");
        if (parameters.getId() != null) {
            output.setParameter("id", parameters.getId());
        }
        try {
            // Compute the live data configuration based on the macro parameters.
            LiveDataConfiguration liveDataConfig =
                this.liveDataRendererConfiguration.getLiveDataConfiguration(advancedParameters, parameters);
            // Add the default values.
            liveDataConfig = this.defaultLiveDataConfigResolver.resolve(liveDataConfig);
            // Serialize as JSON.
            ObjectMapper objectMapper = new ObjectMapper();
            output.setParameter("data-config", objectMapper.writeValueAsString(liveDataConfig));
            // The content is trusted if the author has script right, or if no advanced configuration is used (i.e.,
            // no macro content), and we are running in a trusted context.

            boolean trustedContent =
                StringUtils.isBlank(advancedParameters) || (this.contextualAuthorizationManager.hasAccess(SCRIPT)
                    && !restricted);
            output.setParameter("data-config-content-trusted", Boolean.toString(trustedContent));
        } catch (Exception e) {
            throw new LiveDataException("Failed to generate live data configuration from macro parameters.", e);
        }
        return output;
    }

    /**
     * Render the Live Data content in the current syntax.
     *
     * @param parameters the parameters used to render the Live Data
     * @param advancedParameters a json string containing the advanced parameters of the Live Data
     * @param restricted when {@code true}, the Live Data is contained in a restricted environment, and its content
     *     will be considered as untrusted
     * @return the Live Data {@link Block}
     * @throws LiveDataException in case of issue when resolving the Live Data configuration or during rendering
     */
    public String render(LiveDataRendererParameters parameters, Map<?, ?> advancedParameters, boolean restricted)
        throws LiveDataException
    {
        String hint = this.renderingContext.getTargetSyntax().toIdString();
        try {
            BlockRenderer renderer = this.componentManagerProvider.get().getInstance(BlockRenderer.class, hint);
            Block block = execute(parameters, advancedParameters, restricted);
            WikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(block, printer);
            return printer.toString();
        } catch (ComponentLookupException e) {
            throw new LiveDataException("Failed to resolve [%s] with hint [%s]".formatted(BlockRenderer.class, hint),
                e);
        }
    }
}

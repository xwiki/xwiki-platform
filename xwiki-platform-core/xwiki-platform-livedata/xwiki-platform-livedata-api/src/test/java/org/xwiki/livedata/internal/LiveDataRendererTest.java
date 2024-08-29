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
import java.util.function.Consumer;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link LiveDataRenderer}.
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
@ComponentTest
class LiveDataRendererTest
{
    private static final String ADVANCED_PARAMETERS_EMPTY = "{}";

    @InjectMockComponents
    private LiveDataRenderer renderer;

    /**
     * Used to add default Live Data configuration values.
     */
    @MockComponent
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigResolver;

    @MockComponent
    private LiveDataRendererConfiguration liveDataRendererConfiguration;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    /**
     * The component used to load the JavaScript code of the Live Data widget.
     */
    @MockComponent
    @Named("jsfx")
    private SkinExtension jsfx;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @MockComponent
    private RenderingContext renderingContext;

    @Test
    void execute() throws Exception
    {
        LiveDataRendererParameters parameters = initParams();
        Block block = this.renderer.execute(parameters, ADVANCED_PARAMETERS_EMPTY, true);
        assertEquals(new GroupBlock(Map.of(
            "class", "liveData loading",
            "data-config", ADVANCED_PARAMETERS_EMPTY,
            "data-config-content-trusted", "false"
        )), block);

        verify(this.jsfx).use("uicomponents/widgets/liveData.js", Map.of("forceSkinAction", true));
    }

    @Test
    void executeWithId() throws Exception
    {
        String liveDataId = "ld-id";
        LiveDataRendererParameters parameters = initParams(params -> params.setId(liveDataId));

        Block block = this.renderer.execute(parameters, ADVANCED_PARAMETERS_EMPTY, true);
        assertEquals(new GroupBlock(Map.of(
            "class", "liveData loading",
            "data-config", ADVANCED_PARAMETERS_EMPTY,
            "data-config-content-trusted", "false",
            "id", liveDataId
        )), block);

        verify(this.jsfx).use("uicomponents/widgets/liveData.js", Map.of("forceSkinAction", true));
    }

    @Test
    void render() throws Exception
    {
        Syntax html50 = Syntax.HTML_5_0;

        ComponentManager componentManager = mock(ComponentManager.class);
        BlockRenderer blockRenderer = mock(BlockRenderer.class);

        initParams();
        when(componentManager.getInstance(BlockRenderer.class, html50.toIdString()))
            .thenReturn(blockRenderer);
        when(this.componentManagerProvider.get()).thenReturn(componentManager);
        when(this.renderingContext.getTargetSyntax()).thenReturn(html50);

        this.renderer.render(new LiveDataRendererParameters(), Map.of(), true);

        verify(blockRenderer).render(eq(new GroupBlock(Map.of(
            "class", "liveData loading",
            "data-config", ADVANCED_PARAMETERS_EMPTY,
            "data-config-content-trusted", "false"
        ))), any(WikiPrinter.class));
    }

    private LiveDataRendererParameters initParams() throws Exception
    {
        return initParams(null);
    }

    private LiveDataRendererParameters initParams(
        Consumer<LiveDataRendererParameters> liveDataRendererParametersConsumer) throws Exception
    {
        LiveDataRendererParameters parameters = new LiveDataRendererParameters();
        if (liveDataRendererParametersConsumer != null) {
            liveDataRendererParametersConsumer.accept(parameters);
        }
        LiveDataConfiguration liveDataConfig = new LiveDataConfiguration();
        when(this.liveDataRendererConfiguration.getLiveDataConfiguration(ADVANCED_PARAMETERS_EMPTY, parameters))
            .thenReturn(liveDataConfig);
        when(this.defaultLiveDataConfigResolver.resolve(liveDataConfig)).thenReturn(liveDataConfig);
        return parameters;
    }
}

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

import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void render() throws Exception
    {
        String advancedParameters = "{}";
        LiveDataRendererParameters parameters = new LiveDataRendererParameters();
        LiveDataConfiguration liveDataConfig = new LiveDataConfiguration();
        when(this.liveDataRendererConfiguration.getLiveDataConfiguration(advancedParameters, parameters))
            .thenReturn(liveDataConfig);
        when(this.defaultLiveDataConfigResolver.resolve(liveDataConfig)).thenReturn(liveDataConfig);

        List<Block> render = this.renderer.render(parameters, advancedParameters, true);
        assertEquals(List.of(new GroupBlock(Map.of(
            "class", "liveData loading",
            "data-config", "{}",
            "data-config-content-trusted", "false"
        ))), render);

        verify(this.jsfx).use("uicomponents/widgets/liveData.js", Map.of("forceSkinAction", true));
    }

    @Test
    void renderWithId() throws Exception
    {
        String advancedParameters = "{}";
        LiveDataRendererParameters parameters = new LiveDataRendererParameters();
        String liveDataId = "ld-id";
        parameters.setId(liveDataId);
        LiveDataConfiguration liveDataConfig = new LiveDataConfiguration();
        when(this.liveDataRendererConfiguration.getLiveDataConfiguration(advancedParameters, parameters))
            .thenReturn(liveDataConfig);
        when(this.defaultLiveDataConfigResolver.resolve(liveDataConfig)).thenReturn(liveDataConfig);

        List<Block> render = this.renderer.render(parameters, advancedParameters, true);
        assertEquals(List.of(new GroupBlock(Map.of(
            "class", "liveData loading",
            "data-config", "{}",
            "data-config-content-trusted", "false",
            "id", liveDataId
        ))), render);

        verify(this.jsfx).use("uicomponents/widgets/liveData.js", Map.of("forceSkinAction", true));
    }
}

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
package org.xwiki.livedata.internal.macro;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.macro.LiveDataMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

/**
 * Display dynamic lists of data.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Named("liveData")
@Singleton
public class LiveDataMacro extends AbstractMacro<LiveDataMacroParameters>
{
    /**
     * Used to add default Live Data configuration values.
     */
    @Inject
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigResolver;

    @Inject
    private LiveDataMacroConfiguration liveDataMacroConfiguration;

    @Inject
    private LiveDataMacroRights liveDataMacroRights;

    /**
     * The component used to load the JavaScript code of the Live Data widget.
     */
    @Inject
    @Named("jsfx")
    private SkinExtension jsfx;

    /**
     * Default constructor.
     */
    public LiveDataMacro()
    {
        super("Live Data", "Display dynamic lists of data.",
            new DefaultContentDescriptor("Advanced Live Data configuration (JSON)", false),
            LiveDataMacroParameters.class);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public List<Block> execute(LiveDataMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Load the JavaScript code of the Live Data widget.
        Map<String, Object> skinExtensionParameters = singletonMap("forceSkinAction", Boolean.TRUE);
        this.jsfx.use("uicomponents/widgets/liveData.js", skinExtensionParameters);

        GroupBlock output = new GroupBlock();
        output.setParameter("class", "liveData loading");
        if (parameters.getId() != null) {
            output.setParameter("id", parameters.getId());
        }
        try {
            // Compute the live data configuration based on the macro parameters.
            LiveDataConfiguration liveDataConfig = this.liveDataMacroConfiguration.getLiveDataConfiguration(content,
                parameters);
            // Add the default values.
            liveDataConfig = this.defaultLiveDataConfigResolver.resolve(liveDataConfig);
            // Serialize as JSON.
            ObjectMapper objectMapper = new ObjectMapper();
            output.setParameter("data-config", objectMapper.writeValueAsString(liveDataConfig));
            // The content is trusted if the author has script right, or if no advanced configuration is used (i.e., 
            // no macro content), and we are running in a trusted context.
            boolean trustedContent =
                StringUtils.isBlank(content) || (this.liveDataMacroRights.authorHasScriptRight()
                    && !context.getTransformationContext().isRestricted());
            output.setParameter("data-config-content-trusted", Boolean.toString(trustedContent));
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to generate live data configuration from macro parameters.", e);
        }
        return singletonList(output);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }
}

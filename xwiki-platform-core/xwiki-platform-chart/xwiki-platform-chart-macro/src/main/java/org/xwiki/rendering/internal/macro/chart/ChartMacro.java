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
package org.xwiki.rendering.internal.macro.chart;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.chart.ChartGenerator;
import org.xwiki.chart.ChartGeneratorException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.internal.macro.chart.source.DataSource;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.chart.ChartMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * A macro for rendering charts.
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("chart")
@Singleton
public class ChartMacro extends AbstractMacro<ChartMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Displays a graphical chart generated from miscellaneous data sources";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "Input data for the chart macro (Ex. for 'inline' source mode)";

    /**
     * Used for building the actual chart.
     */
    @Inject
    private ChartGenerator chartGenerator;

    /**
     * The component manager needed for instantiating the datasource factory.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Used to compute the chart image storage location and URL to access it.
     */
    @Inject
    @Named("tmp")
    private ChartImageWriter imageWriter;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ChartMacro()
    {
        super("Chart", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION, false, Block.LIST_BLOCK_TYPE),
            ChartMacroParameters.class);
        setDefaultCategories(Set.of(DEFAULT_CATEGORY_CONTENT));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(ChartMacroParameters macroParams, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Generate the chart image in a temporary location.
        generateChart(macroParams, content, context);

        String imageLocation = this.imageWriter.getURL(new ImageId(macroParams));
        String title = macroParams.getTitle();
        ResourceReference reference = new ResourceReference(imageLocation, ResourceType.URL);
        ImageBlock imageBlock = new ImageBlock(new ResourceReference(imageLocation, ResourceType.URL), true);
        imageBlock.setParameter("alt", title);
        LinkBlock linkBlock = new LinkBlock(Collections.singletonList(imageBlock), reference, true);
        linkBlock.setParameter("title", title);

        // If the macro is used standalone then we need to wrap it in a paragraph block.
        Block resultBlock;
        if (context.isInline()) {
            resultBlock = linkBlock;
        } else {
            resultBlock = new ParagraphBlock(Collections.singletonList(linkBlock));
        }

        return Collections.singletonList(resultBlock);
    }

    /**
     * Builds the chart image according to the specifications passed in.
     *
     * @param parameters the macro parameters
     * @param content the macro content
     * @param context the macro transformation context, used for example to find out the current document reference
     * @throws MacroExecutionException if an error occurs while generating / saving the chart image
     */
    private void generateChart(ChartMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        String source = computeSource(parameters.getSource(), content);

        DataSource dataSource;
        try {
            dataSource = this.componentManager.getInstance(DataSource.class, source);
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException(String.format("Invalid source parameter [%s]",
                parameters.getSource()), e);
        }

        Map<String, String> sourceParameters = getSourceParameters(parameters, source);

        dataSource.buildDataset(content, sourceParameters, context);

        try {
            this.imageWriter.writeImage(new ImageId(parameters),
                this.chartGenerator.generate(dataSource.getChartModel(), sourceParameters));
        } catch (ChartGeneratorException e) {
            throw new MacroExecutionException("Error while rendering chart", e);
        }
    }

    /**
     * Compute what Data Source to use. If the user has specified one then use it. Otherwise if there's content
     * in the macro default to using the "inline" source and if not default to using the "xdom" source.
     *
     * @param userDefinedSource the user specified source value from the Macro parameter (is null if not specified)
     * @param content the Macro content
     * @return the hint of the {@link DataSource} component to use
     */
    private String computeSource(String userDefinedSource, String content)
    {
        String source = userDefinedSource;
        if (source == null) {
            if (StringUtils.isEmpty(content)) {
                source = "xdom";
            } else {
                source = "inline";
            }
        }
        return source;
    }

    /**
     * TODO There is no way to escape the ';' character.
     *
     * @param chartMacroParameters The macro parameters.
     * @param sourceHint the hint of the Data Source component to use
     * @return A map containing the source parameters.
     */
    private Map<String, String> getSourceParameters(ChartMacroParameters chartMacroParameters, String sourceHint)
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(ChartGenerator.TITLE_PARAM, chartMacroParameters.getTitle());
        parameters.put(ChartGenerator.WIDTH_PARAM, String.valueOf(chartMacroParameters.getWidth()));
        parameters.put(ChartGenerator.HEIGHT_PARAM, String.valueOf(chartMacroParameters.getHeight()));
        parameters.put(ChartGenerator.TYPE_PARAM, chartMacroParameters.getType());
        parameters.put(DataSource.SOURCE_PARAM, sourceHint);
        parameters.put(DataSource.PARAMS_PARAM, chartMacroParameters.getParams());

        String sourceParameters = chartMacroParameters.getParams();

        if (null != sourceParameters) {
            String[] segments = sourceParameters.split(";");
            for (String segment : segments) {
                String[] keyValue = segment.split(":", 2);
                String key = StringUtils.trim(keyValue[0]);
                if (keyValue.length == 2) {
                    parameters.put(key, keyValue[1]);
                } else {
                    parameters.put(key, null);
                }
            }
        }

        return parameters;
    }
}

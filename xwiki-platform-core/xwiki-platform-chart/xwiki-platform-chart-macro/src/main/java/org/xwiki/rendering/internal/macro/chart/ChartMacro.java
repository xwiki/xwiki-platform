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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.chart.ChartGenerator;
import org.xwiki.chart.ChartGeneratorException;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.chart.ChartDataSource;
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
     * Used for getting web URLs from specific filenames.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component manager needed for instantiating the datasource factory.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The web container of the current module.
     */
    @Inject
    private Container container;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ChartMacro()
    {
        super("Chart", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION), ChartMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
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
        String imageLocation =
            this.documentAccessBridge.getURL(null, "charting", null, null) + "/" + generateChart(macroParams, content);
        String title = macroParams.getTitle();
        ResourceReference reference = new ResourceReference(imageLocation, ResourceType.URL);
        ImageBlock imageBlock = new ImageBlock(new ResourceReference(imageLocation, ResourceType.URL), true);
        imageBlock.setParameter("alt", title);
        LinkBlock linkBlock = new LinkBlock(Collections.singletonList((Block) imageBlock), reference, true);
        linkBlock.setParameter("title", title);

        // If the macro is used standalone then we need to wrap it in a paragraph block.
        Block resultBlock;
        if (context.isInline()) {
            resultBlock = linkBlock;
        } else {
            resultBlock = new ParagraphBlock(Collections.singletonList((Block) linkBlock));
        }

        return Collections.singletonList(resultBlock);
    }

    /**
     * Builds the chart image according to the specifications passed in.
     * 
     * @param parameters macro parameters
     * @param content macro content
     * @return the name of the generated image file
     * @throws MacroExecutionException if an error occurs while generating / saving the chart image
     */
    private String generateChart(ChartMacroParameters parameters, String content)
        throws MacroExecutionException
    {
        Map<String, String> paramsMap = parameters.getParametersMap();
        String source = paramsMap.get("source");
        File chartFile;
        try {
            ChartDataSource dataSource = this.componentManager.getInstance(ChartDataSource.class, source);
            byte[] chart =
                this.chartGenerator.generate(dataSource.buildModel(content, paramsMap), paramsMap);
            chartFile = getChartImageFile(parameters);
            FileOutputStream fos = new FileOutputStream(chartFile);
            fos.write(chart);
            fos.close();
        } catch (ComponentLookupException ex) {
            throw new MacroExecutionException("Invalid source parameter.", ex);
        } catch (ChartGeneratorException ex) {
            throw new MacroExecutionException("Error while rendering chart.", ex);
        } catch (Exception ex) {
            throw new MacroExecutionException("Error while saving chart image.", ex);
        }
        return chartFile.getName();
    }

    /**
     * Returns the temporary file into which the chart image will be saved.
     * 
     * @param parameters macro parameters
     * @return the chart image file
     */
    protected File getChartImageFile(ChartMacroParameters parameters)
    {
        File chartsDir = new File(this.container.getApplicationContext().getTemporaryDirectory(), "charts");
        return new File(chartsDir, Math.abs(parameters.hashCode()) + ".png");
    }
}

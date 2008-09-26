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
 *
 */
package com.xpn.xwiki.plugin.charts;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.batik.apps.rasterizer.SVGConverterException;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.plots.PlotFactory;
import com.xpn.xwiki.plugin.charts.source.DataSource;
import com.xpn.xwiki.plugin.charts.source.MainDataSourceFactory;
import com.xpn.xwiki.plugin.svg.SVGPlugin;
import com.xpn.xwiki.web.XWikiResponse;

/**
 * TODO: Document this
 * 
 * @version $Id$
 */
public class ChartingPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    public ChartingPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
        log.info("Charting Plugin - init");

        File dir = context.getWiki().getTempDirectory(context);
        tempDir = new File(dir, "charts");
        try {
            tempDir.mkdirs();
        } catch (Exception e1) {
            log.warn("Could not create charts temporary directory: " + tempDir, e1);
            dir = new File(context.getWiki().Param("xwiki.upload.tempdir"));
            try {
                tempDir = new File(dir, "charts");
            } catch (Exception e2) {
                log.error("Could not create charts temporary directory: " + tempDir, e2);
            }
        }
        ;
    }

    @Override
    public String getName()
    {
        return "charting";
    }

    public Chart generateChart(ChartParams params, XWikiContext context) throws GenerateException
    {
        try {
            // Obtain the corresponding data source and wrap it into a data source object
            DataSource dataSource =
                MainDataSourceFactory.getInstance().create(params.getMap(ChartParams.SOURCE), context);

            String type = params.getString(ChartParams.TYPE);

            Plot plot;
            try {
                String factoryClassName =
                    ChartingPlugin.class.getPackage().getName() + ".plots." + Character.toUpperCase(type.charAt(0))
                        + type.toLowerCase().substring(1) + "PlotFactory";

                Class factoryClass = Class.forName(factoryClassName);
                Method method = factoryClass.getMethod("getInstance", new Class[] {});
                PlotFactory factory = (PlotFactory) method.invoke(null, new Object[] {});

                plot = factory.create(dataSource, params);
            } catch (InvocationTargetException e) {
                throw new GenerateException(e.getTargetException());
            } catch (Throwable e) {
                throw new GenerateException(e);
            }

            ChartCustomizer.customizePlot(plot, params);

            JFreeChart jfchart = new JFreeChart(plot);

            ChartCustomizer.customizeChart(jfchart, params);

            return generatePngChart(jfchart, params, context);
        } catch (IOException ioe) {
            throw new GenerateException(ioe);
        } catch (DataSourceException dse) {
            throw new GenerateException(dse);
        }
    }

    private Chart generateSvgChart(JFreeChart jfchart, ChartParams params, XWikiContext context) throws IOException,
        GenerateException
    {
        // Get a DOMImplementation
        DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
        // Create an instance of org.w3c.dom.Document
        Document document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
        // Create an instance of the SVG Generator
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        // Ask the chart to render into the SVG Graphics2D implementation
        Rectangle2D.Double rect =
            new Rectangle2D.Double(0, 0, params.getInteger(ChartParams.WIDTH).intValue(), params.getInteger(
                ChartParams.HEIGHT).intValue());
        jfchart.draw(svgGenerator, rect);
        boolean useCSS = false;
        StringWriter swriter = new StringWriter();
        svgGenerator.stream(swriter, useCSS);
        String svgText = swriter.toString();

        String pageURL = null;
        SVGPlugin svgPlugin = (SVGPlugin) context.getWiki().getPlugin("svg", context);
        if (svgPlugin == null) {
            throw new GenerateException("SVGPlugin not loaded");
        }

        String imageURL;
        try {
            imageURL =
                svgPlugin.getSVGImageURL(svgText, params.getInteger(ChartParams.HEIGHT).intValue(), params.getInteger(
                    ChartParams.WIDTH).intValue(), context);
        } catch (SVGConverterException sce) {
            throw new GenerateException(sce);
        }

        return new ChartImpl(params, imageURL, pageURL);
    }

    private Chart generatePngChart(JFreeChart jfchart, ChartParams params, XWikiContext context) throws IOException,
        GenerateException
    {

        File file = getTempFile(params.hashCode(), "png");

        ChartUtilities.saveChartAsPNG(file, jfchart, params.getInteger(ChartParams.WIDTH).intValue(), params
            .getInteger(ChartParams.HEIGHT).intValue());

        String imageURL = context.getDoc().getAttachmentURL(file.getName(), "charting", context);
        String pageURL = imageURL;
        return new ChartImpl(params, imageURL, pageURL);
    }

    public void outputFile(String filename, XWikiContext context) throws IOException
    {
        File ofile = getTempFile(filename);
        byte[] bytes = readFile(ofile);
        XWikiResponse response = context.getResponse();
        context.setFinished(true);
        response.setDateHeader("Last-Modified", ofile.lastModified());
        response.setContentLength(bytes.length);
        response.setContentType(context.getEngineContext().getMimeType(filename));
        OutputStream os = response.getOutputStream();
        os.write(bytes);
    }

    public byte[] readFile(File ofile) throws FileNotFoundException, IOException
    {
        FileInputStream fis = new FileInputStream(ofile);
        byte[] result = new byte[(int) ofile.length()];
        fis.read(result);
        return result;
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ChartingPluginApi((ChartingPlugin) plugin, context);
    }

    private File getTempFile(int hashcode, String extension)
    {
        return getTempFile(hashcode + "." + extension);
    }

    private File getTempFile(String filename)
    {
        return new File(tempDir, filename);
    }

    private static Log log = LogFactory.getLog(ChartingPlugin.class);

    private File tempDir;
}

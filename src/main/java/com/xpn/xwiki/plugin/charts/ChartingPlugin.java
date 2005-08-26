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
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
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

public class ChartingPlugin extends XWikiDefaultPlugin implements
		XWikiPluginInterface {

	public ChartingPlugin(String name, String className, XWikiContext context) {
		super(name, className, context);
		init(context);
	}
	
    public void init(XWikiContext context) {
        super.init(context);

        File dir = (File) context.getEngineContext().getAttribute("javax.servlet.context.tempdir");
        tempDir = new File(dir, "charts");
        try {
            tempDir.mkdirs();
        } catch (Exception e) {};

    }

    public String getName() {
        return "charting";
    }

	public Chart generateChart(ChartParams params, XWikiContext context) throws GenerateException {
		try {
			// Obtain the corresponding data source and wrap it into a data source object 
			String source = params.getString(ChartParams.SOURCE);
			DataSource dataSource = MainDataSourceFactory
					.getInstance().create(source, context);

			String type = params.getString(ChartParams.TYPE);
			
			Plot plot;
			try {
				String factoryClassName = ChartingPlugin.class.getPackage().getName()
						+ ".plots." + Character.toUpperCase(type.charAt(0)) +
						type.toLowerCase().substring(1) + "PlotFactory";
				
				Class factoryClass = Class.forName(factoryClassName);
				Method method = factoryClass.getMethod("getInstance", new Class[] {});
				PlotFactory factory = (PlotFactory)method.invoke(null, new Object[] {});
				
				plot = factory.create(dataSource, params);
			} catch (InvocationTargetException e){
				throw new GenerateException(e.getTargetException());
			} catch (Throwable e) {
				throw new GenerateException(e);
			}

	        plot.setDataAreaRatio(params.getInteger(ChartParams.HEIGHT).intValue()/
	        						params.getInteger(ChartParams.WIDTH).intValue());
	        // plot
	        if (params.get(ChartParams.PLOT_BACKGROUND_COLOR) != null) {
	        	plot.setBackgroundPaint(params.getColor(ChartParams.PLOT_BACKGROUND_COLOR));
	        }
	        
	        if (params.get(ChartParams.PLOT_BACKGROUND_ALPHA) != null) {
	        	plot.setBackgroundAlpha(params.getFloat(
	        			ChartParams.PLOT_BACKGROUND_ALPHA).floatValue());	        	
	        }

	        if (params.get(ChartParams.PLOT_FOREGROUND_ALPHA) != null) {
	        	plot.setForegroundAlpha(params.getFloat(
	        			ChartParams.PLOT_FOREGROUND_ALPHA).floatValue());	        	
	        }
	        
	        if (params.get(ChartParams.PLOT_INSERTS) != null) {
	        	plot.setInsets(params.getRectangleInsets(ChartParams.PLOT_INSERTS));	        	
	        }
	        
        	if (params.get(ChartParams.PLOT_OUTLINE_COLOR) != null) {
        		plot.setOutlinePaint(params.getColor(ChartParams.PLOT_OUTLINE_COLOR));
        	}
        	
        	if (params.get(ChartParams.PLOT_OUTLINE_STROKE) != null) {
        		plot.setOutlineStroke(params.getStroke(ChartParams.PLOT_OUTLINE_STROKE));
        	}
        	
        	if (params.get(ChartParams.PLOT_ZOOM) != null) {
        		plot.zoom(params.getDouble(ChartParams.PLOT_ZOOM).doubleValue());
        	}

	        JFreeChart jfchart = new JFreeChart(plot);
	        
	        // title
	        if (params.get(ChartParams.TITLE) != null) {
	        	TextTitle title = new TextTitle(params.getString(ChartParams.TITLE));
	        	jfchart.setTitle(title);
	        	
		        if (params.get(ChartParams.TITLE_FONT) != null) {
		        	title.setFont(params.getFont(ChartParams.TITLE_FONT));
		        } else {
		        	title.setFont(JFreeChart.DEFAULT_TITLE_FONT);
		        }
		        
		        if (params.get(ChartParams.TITLE_POSITION) != null) {
		        	title.setPosition(params.getRectangleEdge(ChartParams.TITLE_POSITION));
		        }
		        
		        if (params.get(ChartParams.TITLE_HORIZONTAL_ALIGNMENT) != null) {
		        	title.setHorizontalAlignment(params.getHorizontalAlignment(
		        			ChartParams.TITLE_HORIZONTAL_ALIGNMENT));
		        }
		        
		        if (params.get(ChartParams.TITLE_VERTICAL_ALIGNMENT) != null) {
		        	title.setVerticalAlignment(params.getVerticalAlignment(
		        			ChartParams.TITLE_VERTICAL_ALIGNMENT));
		        }
		        
		        if (params.get(ChartParams.TITLE_COLOR) != null) {
		        	title.setPaint(params.getColor(ChartParams.TITLE_COLOR));
		        }
		        
		        if (params.get(ChartParams.TITLE_BACKGROUND_COLOR) != null) {
		        	title.setBackgroundPaint(params.getColor(ChartParams.TITLE_BACKGROUND_COLOR));
		        }

		        if (params.get(ChartParams.TITLE_PADDING) != null) {
		        	title.setPadding(params.getRectangleInsets(ChartParams.TITLE_PADDING));
		        }
		        
		        if (params.get(ChartParams.TITLE_URL) != null) {
		        	title.setURLText(params.getString(ChartParams.TITLE_URL));
		        }
	        }
	        
	        // legend
	        LegendTitle legend = jfchart.getLegend();
	        
	        if (params.get(ChartParams.LEGEND_BACKGROUND_COLOR) != null) {
	        	legend.setBackgroundPaint(params.getColor(ChartParams.LEGEND_BACKGROUND_COLOR));
	        }
	        
	        if (params.get(ChartParams.LEGEND_ITEM_FONT) != null) {
	        	legend.setItemFont(params.getFont(ChartParams.LEGEND_ITEM_FONT));
	        }
	        
	        if (params.get(ChartParams.LEGEND_ITEM_LABEL_PADDING) != null) {
	        	legend.setItemLabelPadding(params.getRectangleInsets(
	        			ChartParams.LEGEND_ITEM_LABEL_PADDING));
	        }
	        
	        if (params.get(ChartParams.LEGEND_ITEM_GRAPHIC_ANCHOR) != null) {
	        	legend.setLegendItemGraphicAnchor(params.getRectangleAnchor(
	        			ChartParams.LEGEND_ITEM_GRAPHIC_ANCHOR));
	        }
	        
	        if (params.get(ChartParams.LEGEND_ITEM_GRAPHIC_EDGE) != null) {
	        	legend.setLegendItemGraphicEdge(params.getRectangleEdge(
	        			ChartParams.LEGEND_ITEM_GRAPHIC_EDGE));
	        }
	        
	        if (params.get(ChartParams.LEGEND_ITEM_GRAPHIC_LOCATION) != null) {
	        	legend.setLegendItemGraphicAnchor(params.getRectangleAnchor(
	        			ChartParams.LEGEND_ITEM_GRAPHIC_LOCATION));
	        }
	        
	        if (params.get(ChartParams.LEGEND_ITEM_GRAPHIC_PADDING) != null) {
	        	legend.setLegendItemGraphicPadding(params.getRectangleInsets(
	        			ChartParams.LEGEND_ITEM_GRAPHIC_PADDING));
	        }

	        // anti-alias
	        if (params.get(ChartParams.ANTI_ALIAS) != null) {
	        	jfchart.setAntiAlias(params.getBoolean(ChartParams.ANTI_ALIAS).booleanValue());
	        }
	        // background color
	        if (params.get(ChartParams.BACKGROUND_COLOR) != null) {
	        	jfchart.setBackgroundPaint(params.getColor(ChartParams.BACKGROUND_COLOR));
	        }

	        // border
	        if (params.get(ChartParams.BORDER_VISIBLE) != null && 
	        		params.getBoolean(ChartParams.BORDER_VISIBLE).booleanValue()) {
	        	jfchart.setBorderVisible(true);
	        	if (params.get(ChartParams.BORDER_COLOR) != null) {
	        		jfchart.setBorderPaint(params.getColor(ChartParams.BORDER_COLOR));
	        	}
	        	if (params.get(ChartParams.BORDER_STROKE) != null) {
	        		jfchart.setBorderStroke(params.getStroke(ChartParams.BORDER_STROKE));
	        	}
	        }

			return generatePngChart(jfchart, params, context);
		} catch (IOException ioe) {
			throw new GenerateException(ioe);
		} catch (DataSourceException dse) {
			throw new GenerateException(dse);		
		}
	}
	
	private Chart generateSvgChart(JFreeChart jfchart,
			ChartParams params, XWikiContext context)
			throws IOException, GenerateException {
		// Get a DOMImplementation
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		// Create an instance of org.w3c.dom.Document
		Document document = domImpl.createDocument("http://www.w3.org/2000/svg", "svg", null);
		// Create an instance of the SVG Generator
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
		// Ask the chart to render into the SVG Graphics2D implementation
		Rectangle2D.Double rect = new Rectangle2D.Double(0, 0,
				params.getInteger(ChartParams.WIDTH).intValue(),
				params.getInteger(ChartParams.HEIGHT).intValue());
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
            imageURL = svgPlugin.getSVGImageURL(svgText,
	    			params.getInteger(ChartParams.HEIGHT).intValue(),
	    			params.getInteger(ChartParams.WIDTH).intValue(), context);
	    } catch (SVGConverterException sce) {
	    	throw new GenerateException(sce);
	    }
		
		return new ChartImpl(params, imageURL, pageURL);
	}
	
	private Chart generatePngChart(JFreeChart jfchart,
			ChartParams params, XWikiContext context) throws IOException, GenerateException {
		
		File file = getTempFile(params.hashCode(), "png");
		
		ChartUtilities.saveChartAsPNG(file, jfchart,
				params.getInteger(ChartParams.WIDTH).intValue(),
				params.getInteger(ChartParams.HEIGHT).intValue());
		
        String imageURL = context.getDoc().getAttachmentURL(
        		file.getName(), "charting", context);
		String pageURL = imageURL; // TODO: generate this
		return new ChartImpl(params, imageURL, pageURL);
	}
	
    public void outputFile(String filename, XWikiContext context) throws IOException {
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
    
    public byte[] readFile(File ofile) throws FileNotFoundException, IOException {
        FileInputStream fis = new FileInputStream(ofile);
        byte[] result = new byte[(int)ofile.length()];
        fis.read(result);
        return result;
    }
    
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new ChartingPluginApi((ChartingPlugin) plugin, context);
    }

    private File getTempFile(int hashcode, String extension) {
        return getTempFile(hashcode + "." + extension);
    }
    
    private File getTempFile(String filename) {
        return new File(tempDir, filename);
    }

    private File tempDir;
}

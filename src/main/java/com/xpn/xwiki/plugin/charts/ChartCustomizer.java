package com.xpn.xwiki.plugin.charts;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;

import com.xpn.xwiki.plugin.charts.params.ChartParams;

public class ChartCustomizer {
    public static void customizePlot(Plot plot, ChartParams params) {
        if (params.get(ChartParams.HEIGHT) != null && params.get(ChartParams.WIDTH) != null) {
	        plot.setDataAreaRatio(params.getInteger(ChartParams.HEIGHT).intValue()/
					params.getInteger(ChartParams.WIDTH).intValue());
        }
		
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
    }

    public static void customizeAxis(Axis axis, ChartParams params, String prefix) {
    	
    	if (params.get(prefix+ChartParams.AXIS_VISIBLE_SUFIX) != null &&
    			params.getBoolean(prefix+ChartParams.AXIS_VISIBLE_SUFIX).booleanValue() == false) {
    		axis.setVisible(false);

    	} else {
    	   	if (params.get(prefix+ChartParams.AXIS_LINE_VISIBLE_SUFFIX) != null) {
	    		if (params.getBoolean(prefix+ChartParams.AXIS_LINE_VISIBLE_SUFFIX).booleanValue()) {
	    			axis.setAxisLineVisible(true);
	    			
	    	    	if (params.get(prefix+ChartParams.AXIS_LINE_COLOR_SUFFIX) != null) {
	    	    		axis.setAxisLinePaint(params.getColor(prefix+ChartParams.AXIS_LINE_COLOR_SUFFIX));
	    	    	}
	    	    	
	    	    	if (params.get(prefix+ChartParams.AXIS_LINE_STROKE_SUFFIX) != null) {
	    	    		axis.setAxisLineStroke(params.getStroke(prefix+ChartParams.AXIS_LINE_STROKE_SUFFIX));
	    	    	}
	    		} else {
	    			axis.setAxisLineVisible(false);
	    		}
	    	}
	    	
	    	if (params.get(prefix+ChartParams.AXIS_LABEL_SUFFIX) != null) {
	    		axis.setLabel(params.getString(prefix+ChartParams.AXIS_LABEL_SUFFIX));
	
		    	if (params.get(prefix+ChartParams.AXIS_LABEL_FONT_SUFFIX) != null) {
		    		axis.setLabelFont(params.getFont(prefix+ChartParams.AXIS_LABEL_FONT_SUFFIX));
		    	}
	
		    	if (params.get(prefix+ChartParams.AXIS_LABEL_COLOR_SUFFIX) != null) {
		    		axis.setLabelPaint(params.getColor(prefix+ChartParams.AXIS_LABEL_COLOR_SUFFIX));
		    	}
		    	
		    	if (params.get(prefix+ChartParams.AXIS_LABEL_INSERTS_SUFFIX) != null) {
		    		axis.setLabelInsets(params.getRectangleInsets(prefix+ChartParams.AXIS_LABEL_INSERTS_SUFFIX));
		    	}
	    	}
	    	
	    	if (params.get(prefix+ChartParams.AXIS_TICK_LABEL_VISIBLE_SUFFIX) != null) {
	    		if (params.getBoolean(prefix+ChartParams.AXIS_TICK_LABEL_VISIBLE_SUFFIX).booleanValue()) {
	    			axis.setTickLabelsVisible(true);
	
		    	if (params.get(prefix+ChartParams.AXIS_TICK_LABEL_FONT_SUFFIX) != null) {
		    		axis.setTickLabelFont(params.getFont(prefix+ChartParams.AXIS_TICK_LABEL_FONT_SUFFIX));
		    	}
	
		    	if (params.get(prefix+ChartParams.AXIS_TICK_LABEL_COLOR_SUFFIX) != null) {
		    		axis.setTickLabelPaint(params.getColor(prefix+ChartParams.AXIS_TICK_LABEL_COLOR_SUFFIX));
		    	}
		    	
		    	if (params.get(prefix+ChartParams.AXIS_TICK_LABEL_INSERTS_SUFFIX) != null) {
		    		axis.setTickLabelInsets(params.getRectangleInsets(prefix+ChartParams.AXIS_TICK_LABEL_INSERTS_SUFFIX));
		    	}
	
	    		} else {
	    			axis.setTickLabelsVisible(false);    			
	    		}
	    	}
	    	
	    	if (params.get(prefix+ChartParams.AXIS_TICK_MARK_VISIBLE_SUFFIX) != null) {
	    		if (params.getBoolean(prefix+ChartParams.AXIS_TICK_MARK_VISIBLE_SUFFIX).booleanValue()) {
	    			axis.setTickMarksVisible(true);
	    			
	    			if (params.get(prefix+ChartParams.AXIS_TICK_MARK_INSIDE_LENGTH_SUFFIX) != null) {
	    				axis.setTickMarkInsideLength(params.getFloat(prefix+ChartParams
	    						.AXIS_TICK_MARK_INSIDE_LENGTH_SUFFIX).floatValue());
	    			}
	    			
	    			if (params.get(prefix+ChartParams.AXIS_TICK_MARK_OUTSIDE_LENGTH_SUFFIX) != null) {
	    				axis.setTickMarkOutsideLength(params.getFloat(prefix+ChartParams
	    						.AXIS_TICK_MARK_OUTSIDE_LENGTH_SUFFIX).floatValue());
	    			}
	    			
	    			if (params.get(prefix+ChartParams.AXIS_TICK_MARK_COLOR_SUFFIX) != null) {
	    				axis.setTickMarkPaint(params.getColor(prefix+ChartParams
	    						.AXIS_TICK_MARK_COLOR_SUFFIX));
	    			}
	    			
	    			if (params.get(prefix+ChartParams.AXIS_TICK_MARK_COLOR_SUFFIX) != null) {
	    				axis.setTickMarkStroke(params.getStroke(prefix+ChartParams
	    						.AXIS_TICK_MARK_STROKE_SUFFIX));
	    			}
	    			
		    	} else {
					axis.setTickMarksVisible(false);
		    	}
	    	}
    	}
    }

    public static void customizeCategoryAxis(CategoryAxis axis, ChartParams params, String prefix) {
    	customizeAxis(axis, params, prefix);
    }
    
    public static void customizeValueAxis(ValueAxis axis, ChartParams params, String prefix) {
    	customizeAxis(axis, params, prefix);
    	
    }
    
    public static void customizeChart(JFreeChart jfchart, ChartParams params) {
        // title
        if (params.get(ChartParams.TITLE_PREFIX+ChartParams.TITLE_SUFFIX) != null) {
        	TextTitle title = new TextTitle(params.getString(
        			ChartParams.TITLE_PREFIX+ChartParams.TITLE_SUFFIX));
        	
        	customizeTitle(title, params, ChartParams.TITLE_PREFIX);
        	
        	jfchart.setTitle(title);
        }
        
        // subtitle
        if (params.get(ChartParams.SUBTITLE_PREFIX+ChartParams.TITLE_SUFFIX) != null) {
        	TextTitle subtitle = new TextTitle(params.getString(
        			ChartParams.SUBTITLE_PREFIX+ChartParams.TITLE_SUFFIX));
        	
        	customizeTitle(subtitle, params, ChartParams.SUBTITLE_PREFIX);
        	
        	jfchart.addSubtitle(subtitle);
        }
        
        // legend
        LegendTitle legend = jfchart.getLegend();
        
        customizeLegend(legend, params);
        
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
    }
    
    public static void customizeTitle(TextTitle title, ChartParams params, String prefix) {
        if (params.get(prefix+ChartParams.TITLE_FONT_SUFFIX) != null) {
        	title.setFont(params.getFont(prefix+ChartParams.TITLE_FONT_SUFFIX));
        } else {
        	title.setFont(JFreeChart.DEFAULT_TITLE_FONT);
        }
        
        if (params.get(prefix+ChartParams.TITLE_POSITION_SUFFIX) != null) {
        	title.setPosition(params.getRectangleEdge(prefix+ChartParams.TITLE_POSITION_SUFFIX));
        }
        
        if (params.get(prefix+ChartParams.TITLE_HORIZONTAL_ALIGNMENT_SUFFIX) != null) {
        	title.setHorizontalAlignment(params.getHorizontalAlignment(
        			prefix+ChartParams.TITLE_HORIZONTAL_ALIGNMENT_SUFFIX));
        }
        
        if (params.get(prefix+ChartParams.TITLE_VERTICAL_ALIGNMENT_SUFFIX) != null) {
        	title.setVerticalAlignment(params.getVerticalAlignment(
        			prefix+ChartParams.TITLE_VERTICAL_ALIGNMENT_SUFFIX));
        }
        
        if (params.get(prefix+ChartParams.TITLE_COLOR_SUFFIX) != null) {
        	title.setPaint(params.getColor(prefix+ChartParams.TITLE_COLOR_SUFFIX));
        }
        
        if (params.get(prefix+ChartParams.TITLE_BACKGROUND_COLOR_SUFFIX) != null) {
        	title.setBackgroundPaint(params.getColor(prefix+ChartParams.TITLE_BACKGROUND_COLOR_SUFFIX));
        }

        if (params.get(prefix+ChartParams.TITLE_PADDING_SUFFIX) != null) {
        	title.setPadding(params.getRectangleInsets(prefix+ChartParams.TITLE_PADDING_SUFFIX));
        }
        
        if (params.get(prefix+ChartParams.TITLE_URL_SUFFIX) != null) {
        	title.setURLText(params.getString(prefix+ChartParams.TITLE_URL_SUFFIX));
        }
    }
    
    public static void customizeLegend(LegendTitle legend, ChartParams params) {
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
    }
}

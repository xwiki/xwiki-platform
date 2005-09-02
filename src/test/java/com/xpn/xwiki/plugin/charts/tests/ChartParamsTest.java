package com.xpn.xwiki.plugin.charts.tests;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.jfree.ui.RectangleInsets;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.params.ColorChartParam;
import com.xpn.xwiki.plugin.charts.params.DefaultChartParams2;
import com.xpn.xwiki.plugin.charts.params.FontChartParam;
import com.xpn.xwiki.plugin.charts.params.RectangleInsetsChartParam;
import com.xpn.xwiki.plugin.charts.params.StrokeChartParam;

public class ChartParamsTest extends TestCase {

	public static void main(String[] args) {
		junit.textui.TestRunner.run(ChartParamsTest.class);
	}

	public ChartParamsTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
	}

	protected void tearDown() throws Exception {
	}

	public void testGetColorParam1() throws ParamException {
		params.addParam(new ColorChartParam("color1"));
		params.set("color1", "#C08080");
		Color color = params.getColor("color1");
		Assert.assertEquals(192, color.getRed());
		Assert.assertEquals(128, color.getGreen());
		Assert.assertEquals(128, color.getBlue());
	}

	public void testGetColorParam2() throws ParamException {
		params.addParam(new ColorChartParam("color1"));
		params.set("color1", "blue");
		Color color = params.getColor("color1");
		Assert.assertEquals(0, color.getRed());
		Assert.assertEquals(0, color.getGreen());
		Assert.assertEquals(255, color.getBlue());
	}

	public void testGetStrokeParam0() throws ParamException {
		String name = "stroke0";
		params.addParam(new StrokeChartParam(name));
		params.set(name, "");
		params.getStroke(name);
	}
	
	public void testGetStrokeParam1() throws ParamException {
		String name = "stroke1";
		params.addParam(new StrokeChartParam(name));
		params.set(name, "width:0.57");
		BasicStroke stroke = (BasicStroke)params.getStroke(name);
		Assert.assertEquals(0.57, stroke.getLineWidth(), 0.0001);	
	}
	
	public void testGetStrokeParam3() throws ParamException {
		String name = "stroke3";
		params.addParam(new StrokeChartParam(name));
		params.set(name, "width:0.57;cap:butt;join:bevel");
		BasicStroke stroke = (BasicStroke)params.getStroke(name);
		Assert.assertEquals(0.57, stroke.getLineWidth(), 0.0001);
		Assert.assertEquals(BasicStroke.CAP_BUTT, stroke.getEndCap());
		Assert.assertEquals(BasicStroke.JOIN_BEVEL, stroke.getLineJoin());		
	}

	public void testGetStrokeParam4() throws ParamException {
		String name = "stroke4";
		params.addParam(new StrokeChartParam(name));
		params.set(name, "width:0.57;cap:butt;join:bevel;miterlimit:0.94");
		BasicStroke stroke = (BasicStroke)params.getStroke(name);
		Assert.assertEquals(0.57, stroke.getLineWidth(), 0.0001);
		Assert.assertEquals(BasicStroke.CAP_BUTT, stroke.getEndCap());
		Assert.assertEquals(BasicStroke.JOIN_BEVEL, stroke.getLineJoin());		
		Assert.assertEquals(0.94, stroke.getMiterLimit(), 0.001);		
	}

	public void testGetStrokeParam6() throws ParamException {
		String name = "stroke6";
		params.addParam(new StrokeChartParam(name));
		params.set(name, "width:0.57;cap:butt;join:bevel;miterlimit:0.94;dash:0.1,0.3,0.5;dash_phase:0.3");
		BasicStroke stroke = (BasicStroke)params.getStroke(name);
		Assert.assertEquals(0.57, stroke.getLineWidth(), 0.0001);
		Assert.assertEquals(BasicStroke.CAP_BUTT, stroke.getEndCap());
		Assert.assertEquals(BasicStroke.JOIN_BEVEL, stroke.getLineJoin());		
		Assert.assertEquals(0.94, stroke.getMiterLimit(), 0.001);
		Assert.assertEquals(3, stroke.getDashArray().length);		
		Assert.assertEquals(0.1, stroke.getDashArray()[0], 0.001);
		Assert.assertEquals(0.3, stroke.getDashArray()[1], 0.001);		
		Assert.assertEquals(0.5, stroke.getDashArray()[2], 0.001);		
		Assert.assertEquals(0.3, stroke.getDashPhase(), 0.001);
	}
	
	public void testFontChartParam() throws ParamException {
		String name = "font";
		params.addParam(new FontChartParam(name));
		params.set(name, "name:Times New Roman;style:bold;size:32");
		Font font = (Font)params.getFont(name);
		Assert.assertEquals("Times New Roman", font.getName());
		Assert.assertEquals(Font.BOLD, font.getStyle());
		Assert.assertEquals(32, font.getSize());		
	}
	
	public void testRectangleInsetsChartParam() throws ParamException {
		String name = "padding";
		params.addParam(new RectangleInsetsChartParam(name));
		params.set(name, "top:10;left:20;bottom:30;right:40");
		RectangleInsets padding = (RectangleInsets)params.getRectangleInsets(name);
		Assert.assertEquals(10, padding.getTop(), 0.0001);
		Assert.assertEquals(20, padding.getLeft(), 0.0001);
		Assert.assertEquals(30, padding.getBottom(), 0.0001);
		Assert.assertEquals(40, padding.getRight(), 0.0001);
	}

	public void testRectangleInsetsChartParam2() throws ParamException {
		String name = "padding";
		params.addParam(new RectangleInsetsChartParam(name));
		params.set(name, "right:10;bottom:20;left:30;top:40");
		RectangleInsets padding = (RectangleInsets)params.getRectangleInsets(name);
		Assert.assertEquals(10, padding.getRight(), 0.0001);
		Assert.assertEquals(20, padding.getBottom(), 0.0001);
		Assert.assertEquals(30, padding.getLeft(), 0.0001);
		Assert.assertEquals(40, padding.getTop(), 0.0001);
	}
	
//	public void testToString() throws ParamException {
//		params.addParam(new RectangleInsetsChartParam("padding"));
//		params.set("padding", "right:10;bottom:20;left:30;top:40");
//		System.out.println(params.toString());
//		Map map = new LinkedHashMap();
//		map.put("type", "bar");
//		System.out.println(RadeoxHelper.buildMacro("chart", map));
//	}

//	public void testFont() throws ParamException {
//		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment(); 
//		Font[] fonts = env.getAllFonts();
//		for (int i = 0; i<fonts.length; i++) {
//			System.out.println(fonts[i].getName());
//		}
//		String[] families = env.getAvailableFontFamilyNames();
//		for (int i = 0; i<families.length; i++) {
//			System.out.println(families[i]);
//		}		
//	}
	
	public void testDefaultParams() throws ParamException {
		ChartParams params = DefaultChartParams2.getInstance();
	}
	
	public void testSource() throws ParamException {
		ChartParams params = new ChartParams();
		params.set("source", "type:object;doc:Main.WebHome;class:XWiki.TableDataSource;number:1");
		Map map = params.getMap("source");
		Assert.assertEquals("object", (String)map.get("type"));
		Assert.assertEquals("Main.WebHome", (String)map.get("doc"));
		Assert.assertEquals("XWiki.TableDataSource", (String)map.get("class"));
		Assert.assertEquals("1", (String)map.get("number"));
	}
	
	private ChartParams params = new ChartParams();
}

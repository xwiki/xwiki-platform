package com.xpn.xwiki.plugin.charts.mocks;

import java.io.IOException;

import org.apache.batik.apps.rasterizer.SVGConverterException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.svg.SVGPlugin;

public class MockSVGPlugin extends SVGPlugin {
	public MockSVGPlugin(String name, String className, XWikiContext context) {
		super(name, className, context);
	}
	
	public String getSVGImageURL(String content, int height, int width, XWikiContext context)
			throws IOException, SVGConverterException {
		return "http://www.example.com/xwiki/svg/blah";
	}
}

package com.xpn.xwiki.plugin.charts;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;

/**
 * Represents a SVG chart as returned by the ChartingPlugin.
 */
public class ChartImpl implements Chart {

	public ChartImpl(ChartParams params, String imageURL, String pageURL) {
		this.params = params;
		this.imageURL= imageURL;
		this.pageURL= pageURL;
	}
	
	public String getTitle() {
		return params.getString("title");
	}
	
	public void setTitle(String title) {
		try {
			params.set("title", title);
		} catch (ParamException e) {}
	}
	
	public String getPageURL() {
		return pageURL;
	}
	
	public void setPageURL(String pageURL) {
		this.pageURL = pageURL;
	}
	
	public String getImageURL() {
		return imageURL;
	}

	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}

	public ChartParams getParameters() {
		return params;
	}

	private String pageURL;
	private String imageURL;
	private ChartParams params;
}

package com.xpn.xwiki.plugin.charts;

import com.xpn.xwiki.plugin.charts.params.ChartParams;

public interface Chart {
	/**
	 * Each chart should have a title.
	 * @return The chart title.
	 */
	public String getTitle();

	/**
	 * The location of the generated image.
	 * @return
	 */
	public String getImageURL();

	public String getPageURL();

	public ChartParams getParameters();

}
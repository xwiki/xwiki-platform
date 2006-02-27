/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author sdumitriu
 */
package com.xpn.xwiki.plugin.charts.mocks;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.charts.Chart;
import com.xpn.xwiki.plugin.charts.ChartImpl;
import com.xpn.xwiki.plugin.charts.ChartingPlugin;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;

public class MockChartingPlugin extends ChartingPlugin {
	public MockChartingPlugin(String name, String className, XWikiContext context) {
		super(name, className, context);
	}

	public Chart generateChart(ChartParams params, XWikiContext context) throws GenerateException {
		return new ChartImpl(params, "http://hahahahahhaha", "http://gsfdgdfg");
	}
}

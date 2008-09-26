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

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;

/**
 * Represents a SVG chart as returned by the ChartingPlugin.
 * 
 * @version $Id$
 */
public class ChartImpl implements Chart
{

    public ChartImpl(ChartParams params, String imageURL, String pageURL)
    {
        this.params = params;
        this.imageURL = imageURL;
        this.pageURL = pageURL;
    }

    public String getTitle()
    {
        return params.getString("title");
    }

    public void setTitle(String title)
    {
        try {
            params.set("title", title);
        } catch (ParamException e) {
        }
    }

    public String getPageURL()
    {
        return pageURL;
    }

    public void setPageURL(String pageURL)
    {
        this.pageURL = pageURL;
    }

    public String getImageURL()
    {
        return imageURL;
    }

    public void setImageURL(String imageURL)
    {
        this.imageURL = imageURL;
    }

    public ChartParams getParameters()
    {
        return params;
    }

    private String pageURL;

    private String imageURL;

    private ChartParams params;
}

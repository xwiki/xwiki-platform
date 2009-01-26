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
package com.xpn.xwiki.wysiwyg.client.plugin.color;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A popup panel which allows you to pick a color from a color palette by clicking on that color.
 * 
 * @version $Id$
 */
public class ColorPicker extends PopupPanel implements ClickListener
{
    /**
     * The colors present on the default color palette.
     */
    public static final String[][] COLORS = {
        {"#000000", "#444444", "#666666", "#999999", "#CCCCCC", "#EEEEEE", "#F3F3F3", "#FFFFFF"},
        {"#FF0000", "#FF9900", "#FFFF00", "#00FF00", "#00FFFF", "#0000FF", "#9900FF", "#FF00FF"},
        {"#F4CCCC", "#FCE5CD", "#FFF2CC", "#D9EAD3", "#D0E0E3", "#CFE2F3", "#D9D2E9", "#EAD1DC"},
        {"#EA9999", "#F9CB9C", "#FFE599", "#B6D7A8", "#A2C4C9", "#9FC5E8", "#B4A7D6", "#D5A6BD"},
        {"#E06666", "#F6B26B", "#FFD966", "#93C47D", "#76A5AF", "#6FA8DC", "#8E7CC3", "#C27BA0"},
        {"#CC0000", "#E69138", "#F1C232", "#6AA84F", "#45818E", "#3D85C6", "#674EA7", "#A64D79"},
        {"#990000", "#B45F06", "#BF9000", "#38761D", "#134F5C", "#0B5394", "#351C75", "#741B47"},
        {"#660000", "#783F04", "#7F6000", "#274E13", "#0C343D", "#073763", "#20124D", "#4C1130"}};

    /**
     * The color palette used for picking the color.
     */
    private final ColorPalette palette;

    /**
     * Creates a new color picker that uses the default color palette.
     */
    public ColorPicker()
    {
        super(true, false);

        addStyleName("xColorPicker");

        palette = new ColorPalette(COLORS);
        palette.addClickListener(this);

        setWidget(palette);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == palette) {
            hide();
        }
    }

    /**
     * @return the selected color
     */
    public String getColor()
    {
        return palette.getSelectedColor();
    }

    /**
     * Sets the color that should appear as selected on the color palette.
     * 
     * @param color the color to be selected
     */
    public void setColor(String color)
    {
        palette.setSelectedColor(convertRGBToHex(color).toUpperCase());
    }

    /**
     * @param rgbColor the RGB code of a color
     * @return the hex code of the specified color
     */
    public static native String convertRGBToHex(String rgbColor)
    /*-{
        var re = new RegExp("rgb\\s*\\(\\s*([0-9]+).*,\\s*([0-9]+).*,\\s*([0-9]+).*\\)", "gi");
        
        var rgb = rgbColor.replace(re, "$1,$2,$3").split(',');
        if (rgb.length == 3) {
            var r = parseInt(rgb[0]).toString(16);
            var g = parseInt(rgb[1]).toString(16);
            var b = parseInt(rgb[2]).toString(16);
        
            r = r.length == 1 ? '0' + r : r;
            g = g.length == 1 ? '0' + g : g;
            b = b.length == 1 ? '0' + b : b;
        
            return "#" + r + g + b;
        }
        
        return rgbColor;
    }-*/;

    /**
     * @param hexColor the hex code of a color
     * @return the RGB code of the specified color
     */
    public static native String convertHexToRGB(String hexColor)
    /*-{
        if (hexColor.indexOf('#') != -1) {
            hexColor = hexColor.replace(new RegExp('[^0-9A-F]', 'gi'), '');
        
            var r = parseInt(hexColor.substring(0, 2), 16);
            var g = parseInt(hexColor.substring(2, 4), 16);
            var b = parseInt(hexColor.substring(4, 6), 16);
        
            return "rgb(" + r + "," + g + "," + b + ")";
        }
        
        return hexColor;
    }-*/;
}

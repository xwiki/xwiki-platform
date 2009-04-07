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
        {"#660000", "#783F04", "#7F6000", "#274E13", "#0C343D", "#073763", "#20124D", "#4C1130"}
    };

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
        palette.setSelectedColor(convertToHex(color).toUpperCase());
    }

    /**
     * Converts the specified color code to hex code (as in {@code #F0F0EE}). The following input formats are supported:
     * <ul>
     * <li>IE's decimal integer format: 8242323</li>
     * <li>RGB CSS expression: {@code rgb(255,125,75)}</li>
     * </ul>
     * .
     * 
     * @param color a color code, in one of the supported formats
     * @return the hex code of the specified color
     */
    public static String convertToHex(String color)
    {
        try {
            // The color is a decimal integer (IE specific).
            String hex = Integer.toHexString(Integer.parseInt(color));
            char[] padding = new char[Math.max(0, 6 - hex.length())];
            for (int i = 0; i < padding.length; i++) {
                padding[i] = '0';
            }
            hex = String.valueOf(padding) + hex;
            return '#' + hex.substring(4) + hex.substring(2, 4) + hex.substring(0, 2);
        } catch (NumberFormatException e) {
            String rgbRegExp = "^rgb\\s*\\(\\s*([0-9]+).*,\\s*([0-9]+).*,\\s*([0-9]+).*\\)$";
            String[] rgb = color.toLowerCase().replaceAll(rgbRegExp, "$1,$2,$3").split(",");
            if (rgb.length == 3) {
                // The color is a RGB CSS expression.
                StringBuffer hex = new StringBuffer("#");
                for (int i = 0; i < rgb.length; i++) {
                    String channel = Integer.toHexString(Integer.parseInt(rgb[i]));
                    if (channel.length() == 1) {
                        channel = '0' + channel;
                    }
                    hex.append(channel);
                }
                return hex.toString();
            } else {
                // Either already hex color or unknown format. Leave it as it is.
                return color;
            }
        }
    }
}

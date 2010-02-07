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
package org.xwiki.gwt.wysiwyg.client.plugin.color;

import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * A popup panel which allows you to pick a color from a color palette by clicking on that color.
 * 
 * @version $Id$
 */
public class ColorPicker extends PopupPanel implements SelectionHandler<String>
{
    /**
     * Creates a new color picker that uses the given color palette.
     * 
     * @param palette the color palette to be used by this color picker
     */
    public ColorPicker(ColorPalette palette)
    {
        super(true, false);

        addStyleName("xColorPicker");

        palette.addSelectionHandler(this);

        setWidget(palette);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SelectionHandler#onSelection(SelectionEvent)
     */
    public void onSelection(SelectionEvent<String> event)
    {
        if (event.getSource() == getWidget()) {
            hide();
        }
    }

    /**
     * @return the color palette used by this color picker
     */
    protected ColorPalette getColorPalette()
    {
        return (ColorPalette) getWidget();
    }

    /**
     * @return the selected color
     */
    public String getColor()
    {
        return getColorPalette().getSelectedColor();
    }

    /**
     * Sets the color that should appear as selected on the color palette.
     * 
     * @param color the color to be selected
     */
    public void setColor(String color)
    {
        getColorPalette().setSelectedColor(convertToHex(color));
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
        if (color == null) {
            return null;
        }
        try {
            // The color is a decimal integer (IE specific).
            String hex = Integer.toHexString(Integer.parseInt(color));
            char[] padding = new char[Math.max(0, 6 - hex.length())];
            for (int i = 0; i < padding.length; i++) {
                padding[i] = '0';
            }
            hex = String.valueOf(padding) + hex;
            // We have to reverse the order of the channels because IE gives as BGR instead of RGB.
            hex = '#' + hex.substring(4) + hex.substring(2, 4) + hex.substring(0, 2);
            return hex.toUpperCase();
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
                return hex.toString().toUpperCase();
            } else {
                // Either already hex color or unknown format. Leave it as it is.
                return color;
            }
        }
    }
}

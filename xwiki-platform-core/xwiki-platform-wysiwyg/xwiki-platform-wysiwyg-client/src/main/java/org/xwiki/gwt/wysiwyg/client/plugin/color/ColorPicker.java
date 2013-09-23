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
        getColorPalette().setSelectedColor(color);
    }
}

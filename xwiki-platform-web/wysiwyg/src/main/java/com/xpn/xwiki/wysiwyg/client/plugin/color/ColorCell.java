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

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.xpn.xwiki.wysiwyg.client.dom.Style;

/**
 * Defines a cell in the {@link ColorPalette} that can be selected by being clicked.
 * 
 * @version $Id$
 */
public class ColorCell extends FlowPanel
{
    /**
     * The CSS class name used when the color cell is neither selected nor hovered.
     */
    public static final String NORMAL_STYLE_NAME = "colorCell";

    /**
     * The CSS class name used when the color cell is selected.
     */
    public static final String SELECTED_STYLE_NAME = "colorCell-selected";

    /**
     * The CSS class name used when the color cell is hovered.
     */
    public static final String HOVERED_STYLE_NAME = "colorCell-hover";

    /**
     * This cell's color code.
     */
    private final String color;

    /**
     * Flag indicating if this cell was clicked, thus selected.
     */
    private boolean selected;

    /**
     * Creates a new color cell using the specified color code.
     * 
     * @param color the code of the color to fill the new cell
     */
    public ColorCell(String color)
    {
        super();

        this.color = color;
        getElement().getStyle().setProperty(Style.BACKGROUND_COLOR, color);
        addStyleName(NORMAL_STYLE_NAME);

        sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT);
    }

    /**
     * @return {@link #color}
     */
    public String getColor()
    {
        return color;
    }

    /**
     * @return {@link #selected}
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * Sets the selected state of this color cell.
     * 
     * @param selected {@code true} to mark this cell as selected, {@code false} otherwise
     */
    public void setSelected(boolean selected)
    {
        this.selected = selected;
        if (selected) {
            removeStyleName(NORMAL_STYLE_NAME);
            removeStyleName(HOVERED_STYLE_NAME);
            addStyleName(SELECTED_STYLE_NAME);
        } else {
            removeStyleName(SELECTED_STYLE_NAME);
            addStyleName(NORMAL_STYLE_NAME);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FlowPanel#onBrowserEvent(Event)
     */
    public void onBrowserEvent(Event event)
    {
        if (event.getTypeInt() == Event.ONMOUSEOVER) {
            addStyleName(HOVERED_STYLE_NAME);
        } else if (event.getTypeInt() == Event.ONMOUSEOUT) {
            removeStyleName(HOVERED_STYLE_NAME);
        }
        super.onBrowserEvent(event);
    }
}

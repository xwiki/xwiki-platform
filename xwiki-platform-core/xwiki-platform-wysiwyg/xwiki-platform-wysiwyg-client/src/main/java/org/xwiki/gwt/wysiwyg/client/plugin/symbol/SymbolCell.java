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
package org.xwiki.gwt.wysiwyg.client.plugin.symbol;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;

/**
 * Defines a cell in the {@link SymbolPalette} that can be selected by being clicked.
 * 
 * @version $Id$
 */
public class SymbolCell extends FlowPanel
{
    /**
     * The CSS class name used when the symbol cell is not hovered.
     */
    public static final String NORMAL_STYLE_NAME = "symbolCell";

    /**
     * The CSS class name used when the symbol cell is hovered.
     */
    public static final String HOVERED_STYLE_NAME = "symbolCell-hover";

    /**
     * This cell's symbol.
     */
    private final String symbol;

    /**
     * Flag indicating if this cell was clicked, thus selected.
     */
    private boolean selected;

    /**
     * Creates a new symbol cell using the specified symbol.
     * 
     * @param symbol the symbol to put inside the new cell
     */
    public SymbolCell(String symbol)
    {
        super();

        this.symbol = symbol;
        getElement().setInnerHTML(symbol);
        addStyleName(NORMAL_STYLE_NAME);

        sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT);
    }

    /**
     * @return {@link #symbol}
     */
    public String getSymbol()
    {
        return symbol;
    }

    /**
     * @return {@link #selected}
     */
    public boolean isSelected()
    {
        return selected;
    }

    /**
     * Sets the selected state of this symbol cell.
     * 
     * @param selected {@code true} to mark this cell as selected, {@code false} otherwise
     */
    public void setSelected(boolean selected)
    {
        this.selected = selected;
        if (selected) {
            removeStyleName(HOVERED_STYLE_NAME);
        }
    }

    @Override
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

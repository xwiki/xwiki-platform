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
package com.xpn.xwiki.wysiwyg.client.plugin.symbol;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class SymbolCell extends FlowPanel
{
    private final String symbol;

    private boolean selected;

    public SymbolCell(String symbol)
    {
        super();

        this.symbol = symbol;
        getElement().setInnerHTML(symbol);
        addStyleName("symbolCell");
        sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT);
    }

    public String getSymbol()
    {
        if (selected) {
            selected = false;
            return symbol;
        } else {
            return null;
        }
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
        if (selected) {
            removeStyleName("symbolCell-hover");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Widget#onBrowserEvent(Event)
     */
    public void onBrowserEvent(Event event)
    {
        if (event.getTypeInt() == Event.ONMOUSEOVER) {
            addStyleName("symbolCell-hover");
        } else if (event.getTypeInt() == Event.ONMOUSEOUT) {
            removeStyleName("symbolCell-hover");
        }
        super.onBrowserEvent(event);
    }
}

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

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FlowPanel;

public class ColorCell extends FlowPanel
{
    private final String color;

    private boolean selected = false;

    public ColorCell(String color)
    {
        super();

        this.color = color;
        DOM.setStyleAttribute(getElement(), "backgroundColor", color);
        addStyleName("colorCell");

        sinkEvents(Event.ONMOUSEOVER | Event.ONMOUSEOUT);
    }

    public String getColor()
    {
        return color;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
        if (selected) {
            removeStyleName("colorCell");
            removeStyleName("colorCell-hover");
            addStyleName("colorCell-selected");
        } else {
            removeStyleName("colorCell-selected");
            addStyleName("colorCell");
        }
    }

    public void onBrowserEvent(Event event)
    {
        if (DOM.eventGetType(event) == Event.ONMOUSEOVER) {
            addStyleName("colorCell-hover");
        } else if (DOM.eventGetType(event) == Event.ONMOUSEOUT) {
            removeStyleName("colorCell-hover");
        }
        super.onBrowserEvent(event);
    }
}

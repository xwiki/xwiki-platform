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
package org.xwiki.gwt.user.client.ui;

import java.util.Iterator;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.IndexedPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Generic tool bar panel accepting any kind of widget.
 * 
 * @version $Id$
 */
public class ToolBar extends Composite implements HasWidgets, IndexedPanel
{
    /**
     * The panel holding the tool bar widgets.
     */
    protected FlowPanel panel;

    /**
     * This needs to be put at the end of the {@link #panel} because we're using HTML divs on which we set the float CSS
     * property.
     */
    protected FlowPanel clearFloats;

    /**
     * Creates an empty tool bar that waits for widgets to be added.
     */
    public ToolBar()
    {
        panel = new FlowPanel();
        panel.addStyleName("xToolbar");
        initWidget(panel);

        clearFloats = new FlowPanel();
        clearFloats.addStyleName("clearfloats");
        panel.add(clearFloats);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasWidgets#add(Widget)
     */
    public void add(Widget w)
    {
        clearFloats.removeFromParent();
        if ("div".equals(w.getElement().getTagName().toLowerCase())) {
            panel.add(w);
        } else {
            FlowPanel toolBarItem = new FlowPanel();
            toolBarItem.add(w);
            panel.add(toolBarItem);
        }
        panel.add(clearFloats);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasWidgets#clear()
     */
    public void clear()
    {
        panel.clear();
        panel.add(clearFloats);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasWidgets#iterator()
     */
    public Iterator<Widget> iterator()
    {
        return panel.iterator();
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasWidgets#remove(Widget)
     */
    public boolean remove(Widget w)
    {
        if (w != clearFloats) {
            return panel.remove(w);
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see IndexedPanel#getWidget(int)
     */
    public Widget getWidget(int index)
    {
        return panel.getWidget(index);
    }

    /**
     * {@inheritDoc}
     * 
     * @see IndexedPanel#getWidgetCount()
     */
    public int getWidgetCount()
    {
        return panel.getWidgetCount();
    }

    /**
     * {@inheritDoc}
     * 
     * @see IndexedPanel#getWidgetIndex(Widget)
     */
    public int getWidgetIndex(Widget child)
    {
        return panel.getWidgetIndex(child);
    }

    /**
     * {@inheritDoc}
     * 
     * @see IndexedPanel#remove(int)
     */
    public boolean remove(int index)
    {
        if (getWidget(index) != clearFloats) {
            return panel.remove(index);
        } else {
            return false;
        }
    }
}

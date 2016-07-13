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

import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Used to indicate that a specified widget is loading.
 * 
 * @version $Id$
 */
public class LoadingPanel extends FocusPanel
{
    /**
     * Creates a new loading panel.
     */
    public LoadingPanel()
    {
        addStyleName("loading");
        getElement().getStyle().setPosition(Position.ABSOLUTE);
    }

    /**
     * @return {@code true} is the widget is loading, {@code false} otherwise
     */
    public boolean isLoading()
    {
        // NOTE: We use getParentElement() instead of getParentNode() because in IE the parent of an orphan node is
        // sometimes a document fragment, so we have to check if the parent is an element node.
        return getElement().getParentElement() != null;
    }

    /**
     * Shows the loading animation.
     * 
     * @param widget the widget whose loading state will be indicated
     */
    public void startLoading(Widget widget)
    {
        if (widget.isAttached()) {
            stopLoading();

            getElement().getStyle().setWidth(widget.getOffsetWidth(), Unit.PX);
            getElement().getStyle().setHeight(widget.getOffsetHeight(), Unit.PX);
            widget.getElement().getParentNode().insertBefore(getElement(), widget.getElement());
        }
    }

    /**
     * Hides the loading animation.
     */
    public void stopLoading()
    {
        if (isLoading()) {
            getElement().getParentNode().removeChild(getElement());
        }
    }
}

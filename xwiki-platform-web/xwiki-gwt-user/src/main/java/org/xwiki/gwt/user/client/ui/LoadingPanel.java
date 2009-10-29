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

import org.xwiki.gwt.dom.client.Style;

import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Used to indicate that a specified widget is loading.
 * 
 * @version $Id: LoadingPanel.java 23255 2009-09-03 10:16:02Z mflorea $
 */
public class LoadingPanel extends FocusPanel
{
    /**
     * Creates a new loading panel.
     */
    public LoadingPanel()
    {
        addStyleName("loading");
        getElement().getStyle().setProperty(Style.POSITION, Style.Position.ABSOLUTE);
    }

    /**
     * @return {@code true} is the widget is loading, {@code false} otherwise
     */
    public boolean isLoading()
    {
        // NOTE: We don't test the parent but the next sibling because in IE the parent of an orphan node is sometimes a
        // document fragment, thus not null.
        return getElement().getNextSibling() != null;
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

            getElement().getStyle().setPropertyPx(Style.WIDTH, widget.getOffsetWidth());
            getElement().getStyle().setPropertyPx(Style.HEIGHT, widget.getOffsetHeight());
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

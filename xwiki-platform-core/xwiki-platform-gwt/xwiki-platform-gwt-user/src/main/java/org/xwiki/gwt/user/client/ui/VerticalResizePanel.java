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

import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Panel that allows one of the contained widgets to be resized so that it vertically fills all the space left in this
 * container after its siblings are drawn. The desired container size will be considered to be its current size so it
 * needs to be correctly sized from CSS or otherwise before any refresh is triggered.
 * <p>
 * The layout is always automatically done on attaching this container, and it can also be requested manually by calling
 * {@link #refreshHeights()}. {@link #refreshHeights()} must <em>always</em> be called when the children of this
 * container are modified (their content, their size, or visibility), children are added or removed or when the size of
 * this widget is changed.
 * 
 * @version $Id$
 */
public class VerticalResizePanel extends FlowPanel
{
    /**
     * The widget that will be expanded expand to vertically fill all remaining height after all its siblings have been
     * displayed.
     */
    private Widget expandingWidget;

    /**
     * Flag to store whether the expanding widget needs a reload every time a new size is set.
     */
    private boolean needsReload;

    /**
     * @param expandingWidget the widget that will be expanded expand to vertically fill all remaining height after all
     *            its siblings have been displayed
     * @param needsReload {@code true} if the set widget will be reloaded on each size change, {@code false} otherwise.
     *            Set this to {@code true} if the expanding widget needs to recompute its layout any time its size
     *            changes.
     */
    public void setExpandingWidget(Widget expandingWidget, boolean needsReload)
    {
        this.expandingWidget = expandingWidget;
        this.needsReload = needsReload;
    }

    /**
     * Recomputes the heights of the child widgets, setting the appropriate height for the expanded widget.
     */
    public void refreshHeights()
    {
        // nothing to do if this container is not attached
        // FIXME: This function should normally be hooked to the moment when this element becomes
        // visible (no display:none on him or one of its ancestors) but since there is no way to detect this, for the
        // moment it is "safe" to only not resize it when it's not visible i.e. if it's attached when it's not visible,
        // old sizes will be kept. The sizes measurement is a "good enough" (although not covering all cases) way to
        // detect if this element is visible when this function is called, the cases when the size of the panel is
        // intentionally set to 0 width and 0 height are negligible.
        boolean isInvisible = getOffsetHeight() == 0 && getOffsetWidth() == 0;
        if (!isAttached() || isInvisible) {
            return;
        }
        String pixelUnit = "px";

        // get the difference of size from the desired height of the container and the actual height

        // hack to get the inner size of this container: overflow hidden, so that no matter the content, we will never
        // get scroll bars to shrink the clientHeight. We need client height (and not offset height) because we need to
        // check the space inside the container.
        String oldOverflow = getElement().getStyle().getOverflow();
        getElement().getStyle().setOverflow(Overflow.HIDDEN);
        int desiredHeight = getElement().getClientHeight();

        // hack to get the actual height: shrink the container to 0px height with scroll and get the scroll height
        int oldHeight = getElement().getOffsetHeight();
        setHeight("0px");
        getElement().getStyle().setOverflow(Overflow.AUTO);
        int actualHeight = getElement().getScrollHeight();

        int deltaHeight = desiredHeight - actualHeight;
        if (expandingWidget != null) {
            int oldExpandingWidgetHeight = expandingWidget.getElement().getClientHeight();
            expandingWidget.setHeight((oldExpandingWidgetHeight + deltaHeight) + pixelUnit);
            if (needsReload) {
                int index = getWidgetIndex(expandingWidget);
                expandingWidget.removeFromParent();
                insert(expandingWidget, index);
            }
        }

        // restore the container setup
        setHeight(oldHeight + pixelUnit);
        getElement().getStyle().setProperty("overflow", oldOverflow);
    }

    @Override
    protected void onLoad()
    {
        super.onLoad();
        refreshHeights();
    }
}

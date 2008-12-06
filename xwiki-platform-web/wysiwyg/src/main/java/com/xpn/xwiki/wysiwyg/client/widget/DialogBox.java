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
package com.xpn.xwiki.wysiwyg.client.widget;

import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.client.GlassPanel;
import com.xpn.xwiki.wysiwyg.client.dom.Style;
import com.xpn.xwiki.wysiwyg.client.editor.Images;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.DragAdaptor;
import com.xpn.xwiki.wysiwyg.client.util.DragListener;

/**
 * Generic dialog box with optimized dragging.
 * 
 * @version $Id$
 */
public class DialogBox extends PopupPanel implements HasHTML, DragListener, ClickListener
{
    /**
     * The style name used when the dialog is dragged.
     */
    public static final String DRAGGING_STYLE = "xDialogBox-dragging";

    /**
     * The title bar.
     */
    private final FlowPanel titleBar;

    /**
     * The title of this dialog. It appears on the title bar.
     */
    private final HTML caption;

    /**
     * The close button on the title bar.
     */
    private final PushButton closeButton;

    /**
     * Temporary panel that replaces dialog's body while the dialog box is moved. It is used for optimized dragging.
     */
    private final Panel panelTemp;

    /**
     * The outer-most container. It contains the title bar and the contents of the dialog box.
     */
    private final FlowPanel mainPanel;

    /**
     * Used when the dialog is in modal state to prevent clicking outside of the dialog.
     */
    private final GlassPanel glassPanel;

    /**
     * The contents of the dialog.
     */
    private Widget child;

    /**
     * The horizontal coordinate of the point where the drag started.
     */
    private int dragStartX;

    /**
     * The vertical coordinate of the point where the drag started.
     */
    private int dragStartY;

    /**
     * Creates a new dialog that doesn't auto hide and is not modal.
     */
    public DialogBox()
    {
        this(false);
    }

    /**
     * Creates a new dialog that is not modal.
     * 
     * @param autoHide Whether or not the dialog should auto hide when the user clicks outside of it.
     */
    public DialogBox(boolean autoHide)
    {
        this(autoHide, false);
    }

    /**
     * Creates a new dialog.
     * 
     * @param autoHide Whether or not the dialog should auto hide when the user clicks outside of it.
     * @param modal Specifies if the dialog box can loose focus.
     */
    public DialogBox(boolean autoHide, boolean modal)
    {
        // We use our own modal mechanism, based on glass panel.
        super(autoHide, false);

        caption = new HTML();
        caption.addStyleName("xCaption");
        (new DragAdaptor(caption)).addDragListener(this);

        closeButton = new PushButton(Images.INSTANCE.close().createImage(), this);
        closeButton.setTitle(Strings.INSTANCE.close());

        titleBar = new FlowPanel();
        titleBar.addStyleName("xTitleBar");
        titleBar.add(caption);
        titleBar.add(closeButton);

        // Temporary panel that replaces dialog's body while the dialog box is moved.
        // Used for optimized dragging.
        panelTemp = new SimplePanel();

        mainPanel = new FlowPanel();
        mainPanel.add(titleBar);

        int zIndex = 100;
        if (modal) {
            glassPanel = new GlassPanel(false);
            glassPanel.getElement().getStyle().setProperty(Style.Z_INDEX, String.valueOf(zIndex++));
        } else {
            glassPanel = null;
        }

        addStyleName("xDialogBox");
        getElement().getStyle().setProperty(Style.Z_INDEX, String.valueOf(zIndex));
        super.setWidget(mainPanel);
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasHTML#getHTML()
     */
    public String getHTML()
    {
        return caption.getHTML();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.HasText#getText()
     */
    public String getText()
    {
        return caption.getText();
    }

    /**
     * {@inheritDoc}
     * 
     * @see SimplePanel#getWidget()
     */
    public Widget getWidget()
    {
        return child;
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasHTML#setHTML(String)
     */
    public void setHTML(String html)
    {
        caption.setHTML(html);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.google.gwt.user.client.ui.HasText#setText(String)
     */
    public void setText(String text)
    {
        caption.setText(text);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupPanel#setWidget(Widget)
     */
    public void setWidget(Widget widget)
    {
        if (child != null) {
            mainPanel.remove(child);
        }
        if (widget != null) {
            mainPanel.add(widget);
        }
        child = widget;
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupPanel#onEventPreview(Event)
     */
    public boolean onEventPreview(Event event)
    {
        // We need to preventDefault() on mouseDown events (outside of the DialogBox content) to keep text from being
        // selected when it is dragged.
        if (event.getTypeInt() == Event.ONMOUSEDOWN) {
            if (caption.getElement().isOrHasChild(event.getTarget())) {
                event.preventDefault();
            }
        }
        return super.onEventPreview(event);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DragListener#onDragStart(Widget, int, int)
     */
    public void onDragStart(Widget sender, int x, int y)
    {
        dragStartX = x;
        dragStartY = y;
        panelTemp.setPixelSize(child.getOffsetWidth(), child.getOffsetHeight());
        mainPanel.remove(child);
        mainPanel.add(panelTemp);
        addStyleName(DRAGGING_STYLE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see DragListener#onDrag(Widget, int, int)
     */
    public void onDrag(Widget sender, int x, int y)
    {
        if (sender == caption) {
            int absX = x + getAbsoluteLeft();
            int absY = y + getAbsoluteTop();
            setPopupPosition(absX - dragStartX, absY - dragStartY);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DragListener#onDragEnd(Widget, int, int)
     */
    public void onDragEnd(Widget sender, int x, int y)
    {
        mainPanel.remove(panelTemp);
        mainPanel.add(child);
        removeStyleName(DRAGGING_STYLE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupPanel#show()
     */
    public void show()
    {
        if (glassPanel != null) {
            RootPanel.get().add(glassPanel, 0, 0);
        }
        super.show();
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupPanel#hide()
     */
    public void hide()
    {
        if (glassPanel != null) {
            glassPanel.removeFromParent();
        }
        super.hide();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == closeButton) {
            hide();
        }
    }
}

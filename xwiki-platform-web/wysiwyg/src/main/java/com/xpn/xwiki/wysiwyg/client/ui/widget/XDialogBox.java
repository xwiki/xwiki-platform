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
package com.xpn.xwiki.wysiwyg.client.ui.widget;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.widgetideas.client.GlassPanel;
import com.xpn.xwiki.wysiwyg.client.ui.Images;

public class XDialogBox extends PopupPanel implements HasHTML, MouseListener
{
    private final FlowPanel titleBar;

    private final HTML caption;

    private final PushButton closeButton;

    private Widget child;

    private final Panel panelTemp;

    private boolean dragging;

    private boolean startDragging;

    private int dragStartX, dragStartY;

    private final FlowPanel mainPanel;

    protected GlassPanel glassPanel;

    public XDialogBox()
    {
        this(false);
    }

    public XDialogBox(boolean autoHide)
    {
        this(autoHide, true);
    }

    public XDialogBox(boolean autoHide, boolean modal)
    {
        super(autoHide, modal);

        caption = new HTML();
        caption.addStyleName("xCaption");
        caption.addMouseListener(this);

        closeButton = new PushButton(Images.INSTANCE.close().createImage());
        closeButton.addClickListener(new ClickListener()
        {
            public void onClick(Widget sender)
            {
                hide();
            }
        });

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
        glassPanel = new GlassPanel(false);
        glassPanel.getElement().getStyle().setProperty("zIndex", String.valueOf(zIndex));

        addStyleName("xDialogBox");
        getElement().getStyle().setProperty("zIndex", String.valueOf(zIndex + 1));
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
     * @see HasText#getText()
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
     * @see HasText#setText(String)
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
     * @see MouseListener#onMouseDown(Widget, int, int)
     */
    public void onMouseDown(Widget sender, int x, int y)
    {
        dragging = true;
        startDragging = true;
        DOM.setCapture(caption.getElement());
        dragStartX = x;
        dragStartY = y;
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseEnter(Widget)
     */
    public void onMouseEnter(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseLeave(Widget)
     */
    public void onMouseLeave(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseMove(Widget, int, int)
     */
    public void onMouseMove(Widget sender, int x, int y)
    {
        onStartDragging();
        if (dragging) {
            int absX = x + getAbsoluteLeft();
            int absY = y + getAbsoluteTop();
            setPopupPosition(absX - dragStartX, absY - dragStartY);
            mainPanel.remove(child);
            mainPanel.add(panelTemp);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseUp(Widget, int, int)
     */
    public void onMouseUp(Widget sender, int x, int y)
    {
        dragging = false;
        DOM.releaseCapture(caption.getElement());
        mainPanel.remove(panelTemp);
        mainPanel.add(child);
        this.removeStyleName("xDialogBox-dragging");
    }

    private void onStartDragging()
    {
        if (startDragging) {
            panelTemp.setPixelSize(child.getOffsetWidth(), child.getOffsetHeight());
            startDragging = false;
            this.addStyleName("xDialogBox-dragging");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupPanel#show()
     */
    public void show()
    {
        RootPanel.get().add(glassPanel, 0, 0);
        super.show();
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupPanel#hide()
     */
    public void hide()
    {
        super.hide();
        glassPanel.removeFromParent();
    }
}

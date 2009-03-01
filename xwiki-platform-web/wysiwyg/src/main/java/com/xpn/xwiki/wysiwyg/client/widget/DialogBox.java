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
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.RootPanel;
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
public class DialogBox extends PopupPanel implements DragListener, ClickListener
{
    /**
     * The style name used when the dialog is dragged.
     */
    public static final String DRAGGING_STYLE = "dragging";

    /**
     * The image on the left of the caption bar.
     */
    private Image icon;

    /**
     * The text on the caption bar.
     */
    private final Label caption;

    /**
     * The close icon on the right of the caption bar.
     */
    private final Image closeIcon;

    /**
     * The bar at the top of the dialog holding the icon, the title and close button.
     */
    private final FlowPanel captionBar;

    /**
     * The content of the dialog.
     */
    private FlowPanel content;

    /**
     * Temporary panel that replaces dialog's content while the dialog box is moved. It is used for optimized dragging.
     */
    private final FlowPanel contentPlaceHolder;

    /**
     * The dialog box containing the caption bar, the content and the content place holder.
     */
    private final FlowPanel box;

    /**
     * Used when the dialog is in modal state to prevent clicking outside of the dialog.
     */
    private final GlassPanel glassPanel;

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

        caption = new Label();
        caption.addStyleName("xDialogCaption");
        (new DragAdaptor(caption)).addDragListener(this);

        closeIcon = Images.INSTANCE.close().createImage();
        closeIcon.setTitle(Strings.INSTANCE.close());
        closeIcon.addStyleName("xDialogCloseIcon");
        closeIcon.addClickListener(this);

        captionBar = new FlowPanel();
        captionBar.addStyleName("xDialogCaptionBar");
        captionBar.add(caption);
        captionBar.add(closeIcon);

        content = new FlowPanel();
        content.addStyleName("xDialogContent");

        contentPlaceHolder = new FlowPanel();
        contentPlaceHolder.setVisible(false);

        box = new FlowPanel();
        box.setStylePrimaryName("xDialogBox");
        box.add(captionBar);
        box.add(content);
        box.add(contentPlaceHolder);

        int zIndex = 100;
        if (modal) {
            glassPanel = new GlassPanel(false);
            glassPanel.getElement().getStyle().setProperty(Style.Z_INDEX, String.valueOf(zIndex++));
        } else {
            glassPanel = null;
        }

        getElement().getStyle().setProperty(Style.Z_INDEX, String.valueOf(zIndex));
        super.setWidget(box);
    }

    /**
     * @return the text of the caption bar
     */
    public String getCaption()
    {
        return caption.getText();
    }

    /**
     * Sets the text of the caption bar.
     * 
     * @param caption the string to be placed on the caption bar
     */
    public void setCaption(String caption)
    {
        this.caption.setText(caption);
    }

    /**
     * @return the image on the left of the caption bar
     */
    public Image getIcon()
    {
        return icon;
    }

    /**
     * Sets the icon on left of the caption bar.
     * 
     * @param icon the image to placed on the left of the caption bar
     */
    public void setIcon(Image icon)
    {
        if (this.icon != null) {
            this.icon.removeFromParent();
        }
        this.icon = icon;
        icon.addStyleName("xDialogIcon");
        captionBar.insert(icon, 0);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupPanel#getWidget()
     */
    public Widget getWidget()
    {
        if (content.getWidgetCount() > 0) {
            return content.getWidget(0);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupPanel#setWidget(Widget)
     */
    public void setWidget(Widget widget)
    {
        content.clear();
        content.add(widget);
    }

    /**
     * {@inheritDoc}
     * 
     * @see PopupPanel#onEventPreview(Event)
     */
    public boolean onEventPreview(Event event)
    {
        // We need to preventDefault() on mouseDown events (outside of the dialog's content) to keep text from being
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
        contentPlaceHolder.setPixelSize(content.getOffsetWidth(), content.getOffsetHeight());
        content.setVisible(false);
        contentPlaceHolder.setVisible(true);
        box.addStyleDependentName(DRAGGING_STYLE);
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
        contentPlaceHolder.setVisible(false);
        content.setVisible(true);
        box.removeStyleDependentName(DRAGGING_STYLE);
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
        if (sender == closeIcon) {
            hide();
        }
    }
}

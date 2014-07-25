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

import org.xwiki.gwt.user.client.DragAdaptor;
import org.xwiki.gwt.user.client.DragListener;
import org.xwiki.gwt.user.client.Images;
import org.xwiki.gwt.user.client.Strings;

import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Generic dialog box with optimized dragging.
 * 
 * @version $Id$
 */
public class DialogBox extends PopupPanel implements DragListener, ClickHandler
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
        // We use only the glass panel to make the dialog box modal because otherwise the RichTextArea would not be
        // usable on a modal dialog box. If we make the popup panel modal it will stop all the events whose target is
        // not a child of the panel. Events triggered inside the rich text area are from a different document than the
        // one holding the panel and thus their target is not a child of the panel.
        super(autoHide, false);
        setGlassEnabled(modal);

        caption = new Label();
        caption.addStyleName("xDialogCaption");
        (new DragAdaptor(caption)).addDragListener(this);

        closeIcon = new Image(Images.INSTANCE.close());
        closeIcon.setTitle(Strings.INSTANCE.close());
        closeIcon.addStyleName("xDialogCloseIcon");
        closeIcon.addClickHandler(this);

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

    @Override
    public Widget getWidget()
    {
        if (content.getWidgetCount() > 0) {
            return content.getWidget(0);
        } else {
            return null;
        }
    }

    @Override
    public void setWidget(Widget widget)
    {
        content.clear();
        content.add(widget);
    }

    @Override
    protected void onPreviewNativeEvent(NativePreviewEvent event)
    {
        // We need to preventDefault() on mouseDown events (outside of the
        // DialogBox content) to keep text from being selected when it
        // is dragged.
        NativeEvent nativeEvent = event.getNativeEvent();

        if (!event.isCanceled() && (event.getTypeInt() == Event.ONMOUSEDOWN) && isCaptionEvent(nativeEvent)) {
            nativeEvent.preventDefault();
        }

        super.onPreviewNativeEvent(event);
    }

    /**
     * @param event a native event
     * @return {@code true} if the target of the given event is the caption bar or one of its descendants, {@code false}
     *         otherwise
     */
    private boolean isCaptionEvent(NativeEvent event)
    {
        EventTarget target = event.getEventTarget();
        if (Element.is(target)) {
            return captionBar.getElement().isOrHasChild(Element.as(target));
        }
        return false;
    }

    @Override
    public void onDragStart(Widget sender, int x, int y)
    {
        dragStartX = x;
        dragStartY = y;
        contentPlaceHolder.setPixelSize(content.getOffsetWidth(), content.getOffsetHeight());
        content.setVisible(false);
        contentPlaceHolder.setVisible(true);
        box.addStyleDependentName(DRAGGING_STYLE);
    }

    @Override
    public void onDrag(Widget sender, int x, int y)
    {
        if (sender == caption) {
            int absX = x + getAbsoluteLeft();
            int absY = y + getAbsoluteTop();
            setPopupPosition(absX - dragStartX, absY - dragStartY);
        }
    }

    @Override
    public void onDragEnd(Widget sender, int x, int y)
    {
        contentPlaceHolder.setVisible(false);
        content.setVisible(true);
        box.removeStyleDependentName(DRAGGING_STYLE);
    }

    @Override
    public void onClick(ClickEvent event)
    {
        if (event.getSource() == closeIcon) {
            hide();
        }
    }
}

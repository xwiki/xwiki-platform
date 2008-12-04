package com.xpn.xwiki.wysiwyg.client.plugin.image.ui;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.plugin.image.ImageConfig;

/**
 * Widget to display an image in the image chooser panel, with an image preview and an insert button.
 * 
 * @version $Id$
 */
public class ImagePreviewWidget extends Composite implements SourcesClickEvents
{
    /**
     * Variable holding the resize parameters of the image, so that the image thumbnail is resized on the server.
     */
    private final String resizeParameters = "width=110&height=90";

    /**
     * The image to generate preview for.
     */
    private ImageConfig image;

    /**
     * Listeners collection to be able to fire the insert button click further.
     */
    private ClickListenerCollection clickListeners;

    /**
     * Builds an image preview for the passed image.
     * 
     * @param img the image to build a preview for
     */
    public ImagePreviewWidget(ImageConfig img)
    {
        this.image = img;
        clickListeners = new ClickListenerCollection();
        Image htmlImage = new Image(img.getImageURL() + "?" + resizeParameters);
        htmlImage.setTitle(image.getImageFileName());
        htmlImage.addClickListener(new ClickListener()
        {
            public void onClick(Widget sender)
            {
                clickListeners.fireClick(ImagePreviewWidget.this);
            }
        });
        Panel mainPanel = new FlowPanel();
        mainPanel.addStyleName("xImagePreview");
        mainPanel.add(htmlImage);

        initWidget(mainPanel);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesClickEvents#addClickListener(ClickListener)
     */
    public void addClickListener(ClickListener listener)
    {
        clickListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesClickEvents#removeClickListener(ClickListener)
     */
    public void removeClickListener(ClickListener listener)
    {
        clickListeners.remove(listener);
    }

    /**
     * Toggles selected style on this image preview widget.
     * 
     * @param selected true if this image must be set as selected, false otherwise.
     */
    public void setSelected(boolean selected)
    {
        String selectedStyle = "xImagePreviewSelected";
        if (selected) {
            this.addStyleName(selectedStyle);
        } else {
            this.removeStyleName(selectedStyle);
        }
    }

    /**
     * @return the image for which the preview is generated.
     */
    public ImageConfig getImage()
    {
        return image;
    }
}

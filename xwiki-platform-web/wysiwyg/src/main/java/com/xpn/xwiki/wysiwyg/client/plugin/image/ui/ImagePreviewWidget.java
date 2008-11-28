package com.xpn.xwiki.wysiwyg.client.plugin.image.ui;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;

/**
 * Widget to display an image in the image chooser panel, with an image preview and an insert button.
 * 
 * @version $Id$
 */
public class ImagePreviewWidget extends Composite implements SourcesClickEvents, HasImage
{
    /**
     * The displayed image URL.
     */
    private String imageURL;

    /**
     * The displayed image file name.
     */
    private String imageName;

    /**
     * Listeners collection to be able to fire the insert button click further.
     */
    private ClickListenerCollection clickListeners;

    /**
     * Builds an image preview for the passed image.
     * 
     * @param url the URL of the image to preview.
     * @param filename the filename of the image to preview.
     */
    public ImagePreviewWidget(String url, String filename)
    {
        this.imageURL = url;
        this.imageName = filename;
        clickListeners = new ClickListenerCollection();
        Image image = new Image(url);

        Button insertImageButton = new Button(Strings.INSTANCE.fileInsertImageButton());
        insertImageButton.setStyleName("xInsertImageButton");
        insertImageButton.addClickListener(new ClickListener()
        {
            public void onClick(Widget sender)
            {
                clickListeners.fireClick(ImagePreviewWidget.this);
            }
        });
        Panel mainPanel = new FlowPanel();
        mainPanel.addStyleName("xImagePreview");
        mainPanel.add(image);
        mainPanel.add(insertImageButton);

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
     * {@inheritDoc}
     * 
     * @see HasImage#getImageFileName()
     */
    public String getImageFileName()
    {
        return imageName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasImage#getImageURL()
     */
    public String getImageURL()
    {
        return imageURL;
    }
}

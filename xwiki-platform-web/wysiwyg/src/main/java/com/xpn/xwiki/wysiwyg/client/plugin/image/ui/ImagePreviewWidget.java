package com.xpn.xwiki.wysiwyg.client.plugin.image.ui;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.util.Attachment;
import com.xpn.xwiki.wysiwyg.client.widget.AttachmentPreviewWidget;

/**
 * Widget to display an image preview.
 * 
 * @version $Id$
 */
public class ImagePreviewWidget extends AttachmentPreviewWidget
{
    /**
     * Variable holding the resize parameters of the image, so that the image thumbnail is resized on the server.
     */
    private static final String RESIZE_PARAMETERS = "width=135";

    /**
     * Builds an image preview from the passed attachment information.
     * 
     * @param attach the attached image to build a preview for
     */
    public ImagePreviewWidget(Attachment attach)
    {
        super(attach);
    }

    /**
     * {@inheritDoc}. Overwrite to create a thumbnail preview of the image.
     */
    @Override
    protected Widget getUI()
    {
        Image htmlImage = new Image(getAttachment().getDownloadUrl() + "?" + RESIZE_PARAMETERS);
        htmlImage.setTitle(getAttachment().getFilename());
        FlowPanel previewPanel = new FlowPanel();
        previewPanel.addStyleName("xImagePreview");
        previewPanel.add(htmlImage);
        return previewPanel;
    }
}

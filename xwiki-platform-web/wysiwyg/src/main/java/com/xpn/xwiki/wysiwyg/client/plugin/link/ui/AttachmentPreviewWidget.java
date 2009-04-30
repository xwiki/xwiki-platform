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
package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.plugin.link.Attachment;

/**
 * Widget to create an attachment preview. Can be subclasses with various implementations to generate the UI of this
 * widget.
 * 
 * @version $Id$
 */
public class AttachmentPreviewWidget extends Composite
{
    /**
     * The attachment to generate preview for.
     */
    protected Attachment attachment;

    /**
     * Builds an attachment preview for the passed attachment.
     * 
     * @param attach the attachment configuration to build the preview for
     */
    public AttachmentPreviewWidget(Attachment attach)
    {
        this.attachment = attach;
        initWidget(getUI());
    }

    /**
     * @return the UI main widget, used to initialize this widget. To be overriden by subclasses to generate specific UI
     *         for the type of preview they're doing: e.g. an image should actually print an HTML with the image.
     */
    protected Widget getUI()
    {
        Label attachmentLabel = new Label(attachment.getFilename());
        attachmentLabel.addStyleName("xAttachPreview");
        return attachmentLabel;
    }

    /**
     * @return the attachment for which the preview is generated.
     */
    public Attachment getAttachment()
    {
        return attachment;
    }
}

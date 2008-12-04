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
package com.xpn.xwiki.wysiwyg.client.plugin.image.ui;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;

/**
 * Form to upload a file to a specified URL. It allows the user to select a file to upload a file to the specified URL.
 * 
 * @version $Id$
 */
public class FileUploadForm extends FormPanel implements FormHandler
{
    /**
     * The file upload input used for the new images upload.
     */
    private FileUpload fileUploadInput;

    /**
     * Builds a file upload form, using the passed URL as the action URL and the passed field name as the name of the
     * file upload input.
     * 
     * @param uploadURL action URL of this form
     * @param fileInputName the name of the file input contained by this form.
     */
    public FileUploadForm(String uploadURL, String fileInputName)
    {
        super();
        addStyleName("xUploadForm");
        setAction(uploadURL);
        setEncoding(FormPanel.ENCODING_MULTIPART);
        setMethod(FormPanel.METHOD_POST);
        FlowPanel panel = new FlowPanel();
        setWidget(panel);

        panel.add(new Label(Strings.INSTANCE.fileUploadLabel()));

        fileUploadInput = new FileUpload();
        fileUploadInput.setName(fileInputName);
        panel.add(fileUploadInput);

        panel.add(new Button(Strings.INSTANCE.fileUploadSubmitLabel(), new ClickListener()
        {
            public void onClick(Widget sender)
            {
                submit();
            }
        }));

        addFormHandler(this);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see FormHandler#onSubmit(FormSubmitEvent)
     */
    public void onSubmit(FormSubmitEvent event)
    {
        if (fileUploadInput.getFilename().trim().length() == 0) {
            Window.alert(Strings.INSTANCE.fileUploadNoPathError());
            event.setCancelled(true);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FormHandler#onSubmitComplete(FormSubmitCompleteEvent)
     */
    public void onSubmitComplete(FormSubmitCompleteEvent event)
    {
        // nothing here
    }    
}

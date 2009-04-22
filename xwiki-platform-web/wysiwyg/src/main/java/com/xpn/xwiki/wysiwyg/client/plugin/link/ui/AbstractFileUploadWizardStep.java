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

import java.util.EnumSet;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Wizard step to handle the upload of a file: display the file input and upload on finish. <br/>
 * FIXME: move this class in a generic package, since it's not specific to links (should be used for images upload too)
 * 
 * @version $Id$
 */
public abstract class AbstractFileUploadWizardStep implements WizardStep
{
    /**
     * Main panel of this wizard step, to be used for the {@link #display()}.
     */
    private final Panel mainPanel = new FlowPanel();

    /**
     * The file upload form.
     */
    private final FormPanel fileUploadForm = new FormPanel();

    /**
     * The file input in the file upload form.
     */
    private final FileUpload fileUploadInput = new FileUpload();

    /**
     * Default constructor.
     */
    public AbstractFileUploadWizardStep()
    {
        mainPanel.addStyleName("xUploadPanel");
        fileUploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        fileUploadForm.setMethod(FormPanel.METHOD_POST);
        // set the url on submit time, just before upload

        Label fileLabel = new Label(Strings.INSTANCE.fileUploadLabel());
        fileUploadInput.setName(getFileUploadInputName());
        FlowPanel formPanel = new FlowPanel();

        formPanel.add(fileLabel);
        formPanel.add(fileUploadInput);

        fileUploadForm.setWidget(formPanel);

        mainPanel.add(fileUploadForm);
    }

    /**
     * {@inheritDoc}
     */
    public Widget display()
    {
        return mainPanel;
    }

    /**
     * {@inheritDoc}
     */
    public String getDirectionName(NavigationDirection direction)
    {
        if (direction == NavigationDirection.NEXT) {
            return Strings.INSTANCE.fileUploadSubmitLabel();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.fileUploadTitle();
    }

    /**
     * {@inheritDoc}
     */
    public EnumSet<NavigationDirection> getValidDirections()
    {
        return EnumSet.of(NavigationDirection.CANCEL, NavigationDirection.PREVIOUS, NavigationDirection.NEXT);
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(AsyncCallback<Boolean> async)
    {
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        // compute and set the upload URL
        fileUploadForm.setAction(getUploadURL());
        // add a form handler dependent on the success async
        fileUploadForm.addFormHandler(new FormHandler()
        {
            public void onSubmit(FormSubmitEvent event)
            {
                // nothing before form submit
            }

            public void onSubmitComplete(FormSubmitCompleteEvent event)
            {
                AbstractFileUploadWizardStep.this.onSubmitComplete(event, async);
                // and remove itself from the list after all was done
                fileUploadForm.removeFormHandler(this);
            }
        });
        // validate the form field
        if (fileUploadInput.getFilename().trim().length() == 0) {
            Window.alert(Strings.INSTANCE.fileUploadNoPathError());
            async.onSuccess(false);
            return;
        }
        // otherwise continue with submit
        fileUploadForm.submit();
    }

    /**
     * Builds the form upload URL for this form, to be called before form submission.
     * 
     * @return the url to set as the action of the file upload form
     */
    protected abstract String getUploadURL();

    /**
     * @return the {@code name} attribute of the {@link #fileUploadInput}, to be returned by subclasses implementing
     *         {@link #getUploadURL()} to set the file upload form data.
     */
    protected abstract String getFileUploadInputName();

    /**
     * Handles the submit completion in asynchronous mode, to pass the result of processing the result in the received
     * callback.
     * 
     * @param event the original {@link FormSubmitCompleteEvent}
     * @param async the callback used to send back the response of form event processing
     * @see {@link #onSubmit}
     */
    protected void onSubmitComplete(FormSubmitCompleteEvent event, AsyncCallback<Boolean> async)
    {
        // nothing for now, should parse form result and set filename, fileId and file URL, but it cannot be obtained
        // from the rest response now: application/json is not recognized by browser and application/xml is changed by
        // IE in XHTML, so info cannot be obtained
        async.onSuccess(true);
    }

    /**
     * @return the fileUploadInput
     */
    public FileUpload getFileUploadInput()
    {
        return fileUploadInput;
    }
}

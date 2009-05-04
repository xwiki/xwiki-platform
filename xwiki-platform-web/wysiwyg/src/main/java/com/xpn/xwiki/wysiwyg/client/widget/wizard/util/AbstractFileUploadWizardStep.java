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
package com.xpn.xwiki.wysiwyg.client.widget.wizard.util;

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
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.util.Attachment;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.WizardStep;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.NavigationListener.NavigationDirection;

/**
 * Wizard step to handle the upload of a file to a wiki page: display the file input and upload on finish. <br/>
 * 
 * @version $Id: AbstractFileUploadWizardStep.java 18936 2009-04-22 12:10:16Z lucaa $
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
     * Builds the form upload URL for this form, to be called before form submission.
     * 
     * @return the url to set as the action of the file upload form
     */
    protected String getUploadURL()
    {
        // use a regular post to the upload action ftm, since REST is throwing an exception and messes up the document
        // in some cases
        // FIXME: un-hardcode this and make it work with multiwiki
        StringBuffer uploadURL = new StringBuffer();
        uploadURL.append("../../upload/");
        uploadURL.append(getSpace());
        uploadURL.append('/');
        uploadURL.append(getPage());

        return uploadURL.toString();
    }

    /**
     * @return the {@code name} attribute of the {@link #fileUploadInput}, to be returned by subclasses implementing
     *         {@link #getUploadURL()} to set the file upload form data.
     */
    protected String getFileUploadInputName()
    {
        return "filepath";
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
     * Handles the submit completion in asynchronous mode, to pass the result of processing the result in the received
     * callback.
     * 
     * @param event the original {@link FormSubmitCompleteEvent}
     * @param async the callback used to send back the response of form event processing
     * @see {@link #onSubmit}
     */
    protected void onSubmitComplete(FormSubmitCompleteEvent event, final AsyncCallback<Boolean> async)
    {
        // create the link reference
        WysiwygService.Singleton.getInstance().getAttachment(getWiki(), getSpace(), getPage(), extractFileName(),
            new AsyncCallback<Attachment>()
            {
                public void onSuccess(Attachment result)
                {
                    if (result == null) {
                        // there was a problem with the attachment, call it a failure
                        Window.alert(Strings.INSTANCE.fileUploadSubmitError());
                        async.onSuccess(false);
                    } else {
                        onAttachmentUploaded(result);
                        async.onSuccess(true);
                    }
                }

                public void onFailure(Throwable caught)
                {
                    async.onFailure(caught);
                }
            });
    }

    /**
     * Notifies the successful completion of a file upload, to be overridden by subclasses to provide specific behavior.
     * 
     * @param attach the successfully uploaded attachment
     */
    protected abstract void onAttachmentUploaded(Attachment attach);

    /**
     * @return the wiki of the document to upload this file to, or null if the default wiki should be used.
     */
    public abstract String getWiki();

    /**
     * @return the space of the document to upload this file to, or null if the default space should be used.
     */
    public abstract String getSpace();

    /**
     * @return the document to upload this file to, or null if the default document should be used.
     */
    public abstract String getPage();

    /**
     * @return the filename set in the file upload field.
     */
    protected String extractFileName()
    {
        // not correct, since it strips \ out of unix filenames, but consistent with UploadAction behaviour, which we
        // need to match to get the correct information about uploaded file
        String fname = getFileUploadInput().getFilename();
        fname = StringUtils.substringAfterLast(fname, "/");
        fname = StringUtils.substringAfterLast(fname, "\\");
        return fname;
    }

    /**
     * @return the fileUploadInput
     */
    public FileUpload getFileUploadInput()
    {
        return fileUploadInput;
    }
}

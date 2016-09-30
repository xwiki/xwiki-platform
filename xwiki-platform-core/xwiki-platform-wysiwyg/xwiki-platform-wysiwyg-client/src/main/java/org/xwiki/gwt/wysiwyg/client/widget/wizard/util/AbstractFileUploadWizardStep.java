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
package org.xwiki.gwt.wysiwyg.client.widget.wizard.util;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.AbstractInteractiveWizardStep;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;

/**
 * Wizard step to handle the upload of a file to a wiki page: display the file input and upload on finish.
 * 
 * @version $Id$
 */
public abstract class AbstractFileUploadWizardStep extends AbstractInteractiveWizardStep
{
    /**
     * The style of the fields under error.
     */
    protected static final String FIELD_ERROR_STYLE = "xErrorField";

    /**
     * The style used on the field label.
     */
    protected static final String FIELD_LABEL_STYLE = "xInfoLabel";

    /**
     * The style used on the field hint.
     */
    protected static final String FIELD_HINT_STYLE = "xHelpLabel";

    /**
     * The file upload form.
     */
    private final FormPanel fileUploadForm = new FormPanel();

    /**
     * The file input in the file upload form.
     */
    private final FileUpload fileUploadInput = new FileUpload();

    /**
     * The error label for the file input.
     */
    private final Label fileErrorLabel = new Label();

    /**
     * The help label for the file input.
     */
    private final Label fileHelpLabel = new Label();

    /**
     * The service used to access the uploaded attachments.
     */
    private final WikiServiceAsync wikiService;

    /**
     * Creates a new file upload wizard step that uses the given service to get information about the uploaded files.
     * 
     * @param wikiService the service used to access the uploaded attachments
     */
    public AbstractFileUploadWizardStep(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;

        setStepTitle(Strings.INSTANCE.fileUploadTitle());

        display().addStyleName("xUploadPanel");
        fileUploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        fileUploadForm.setMethod(FormPanel.METHOD_POST);
        // set the url on submit time, just before upload

        Panel fileLabel = new FlowPanel();
        fileLabel.setStyleName(FIELD_LABEL_STYLE);
        fileLabel.add(new InlineLabel(getFileLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName("xMandatory");
        fileLabel.add(mandatoryLabel);
        fileUploadInput.setName(getFileUploadInputName());
        FlowPanel formPanel = new FlowPanel();

        formPanel.add(fileLabel);
        fileHelpLabel.setStyleName(FIELD_HINT_STYLE);
        fileHelpLabel.setVisible(false);
        formPanel.add(fileHelpLabel);

        fileErrorLabel.addStyleName("xErrorMsg");
        fileErrorLabel.setVisible(false);

        formPanel.add(fileErrorLabel);
        formPanel.add(fileUploadInput);

        fileUploadForm.setWidget(formPanel);

        display().add(fileUploadForm);
    }

    /**
     * @return the label of the file input
     */
    protected String getFileLabel()
    {
        return Strings.INSTANCE.fileUploadLabel();
    }

    /**
     * @return the help label for the file input
     */
    public String getFileHelpLabel()
    {
        return fileHelpLabel.getText();
    }

    /**
     * Sets the help label for the file input.
     * 
     * @param fileHelpLabelText the new help label text for the file input
     */
    public void setFileHelpLabel(String fileHelpLabelText)
    {
        fileHelpLabel.setVisible(!StringUtils.isEmpty(fileHelpLabelText));
        fileHelpLabel.setText(fileHelpLabelText);
    }

    /**
     * Requests the upload URL from the server.
     * 
     * @param cb the object to be notified when the upload URL is received
     */
    protected void getUploadURL(AsyncCallback<String> cb)
    {
        wikiService.getUploadURL(getTargetPageReference(), cb);
    }

    /**
     * @return the {@code name} attribute of the {@link #fileUploadInput}, to be returned by subclasses implementing
     *         {@link #getUploadURL(AsyncCallback)} to set the file upload form data
     */
    protected String getFileUploadInputName()
    {
        return "filepath";
    }

    @Override
    public void init(Object data, AsyncCallback< ? > cb)
    {
        hideError();
        cb.onSuccess(null);
    }

    @Override
    public void onCancel()
    {
        // ignore
    }

    @Override
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        getUploadURL(new AsyncCallback<String>()
        {
            @Override
            public void onFailure(Throwable caught)
            {
                async.onFailure(caught);
            }

            @Override
            public void onSuccess(String result)
            {
                submitTo(result, async);
            }
        });
    }

    /**
     * Submits the upload form the the specified upload URL and notifies the given call-back when the response is
     * received.
     * 
     * @param uploadURL where to submit the upload form
     * @param async the object to be notifies when the response is received
     */
    private void submitTo(String uploadURL, final AsyncCallback<Boolean> async)
    {
        // Set the upload URL.
        fileUploadForm.setAction(uploadURL);
        // Handle the submit complete event on the file upload form.
        // Note: The registrations array is just a hack to be able to remove the handler from within the handler itself
        // (otherwise we get the "local variable may not have been initialized" compiler error).
        final HandlerRegistration[] registrations = new HandlerRegistration[1];
        registrations[0] = fileUploadForm.addSubmitCompleteHandler(new SubmitCompleteHandler()
        {
            public void onSubmitComplete(SubmitCompleteEvent event)
            {
                AbstractFileUploadWizardStep.this.onSubmitComplete(event, async);
                // Stop handling the submit complete event on the file upload form.
                registrations[0].removeHandler();
            }
        });

        hideError();
        // validate the form field
        if (fileUploadInput.getFilename().trim().length() == 0) {
            displayError(Strings.INSTANCE.fileUploadNoPathError());
            async.onSuccess(false);
            return;
        }
        // Otherwise continue with submit.
        fileUploadForm.submit();
    }

    /**
     * Handles the submit completion in asynchronous mode, to pass the result of processing the result in the received
     * callback.
     * 
     * @param event the original {@link SubmitCompleteEvent}
     * @param async the callback used to send back the response of form event processing
     * @see #onSubmit
     */
    protected void onSubmitComplete(SubmitCompleteEvent event, final AsyncCallback<Boolean> async)
    {
        // Create the link reference.
        wikiService.getAttachment(getAttachmentReference(extractFileName()), new AsyncCallback<Attachment>()
        {
            public void onSuccess(Attachment result)
            {
                if (result == null) {
                    // there was a problem with the attachment, call it a failure
                    displayError(Strings.INSTANCE.fileUploadSubmitError());
                    async.onSuccess(false);
                } else {
                    onAttachmentUploaded(result, async);
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
     * @param async the call-back used to indicate the completion of this method. It's required that the sub-classes
     *            invoke async.onSuccess(true); method once they are done with processing the attachment. Without this
     *            method being invoked, the submission of this wizard step will not complete.
     */
    protected abstract void onAttachmentUploaded(Attachment attach, AsyncCallback<Boolean> async);

    /**
     * @return a reference to the page where the attachment will be uploaded
     */
    protected abstract WikiPageReference getTargetPageReference();

    /**
     * @param fileName the name of the file used to upload the attachment
     * @return a reference to the specified attachment
     */
    private AttachmentReference getAttachmentReference(String fileName)
    {
        WikiPageReference targetPageReference = getTargetPageReference();
        return new AttachmentReference(fileName, targetPageReference);
    }

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
    protected FileUpload getFileUploadInput()
    {
        return fileUploadInput;
    }

    /**
     * Displays the error message and markers for this dialog.
     * 
     * @param errorMessage the error message to display.
     */
    protected void displayError(String errorMessage)
    {
        fileErrorLabel.setText(errorMessage);
        fileErrorLabel.setVisible(true);
        fileUploadInput.addStyleName(FIELD_ERROR_STYLE);
    }

    /**
     * Hides the error message and markers for this dialog.
     */
    protected void hideError()
    {
        fileErrorLabel.setVisible(false);
        fileUploadInput.removeStyleName(FIELD_ERROR_STYLE);
    }
}

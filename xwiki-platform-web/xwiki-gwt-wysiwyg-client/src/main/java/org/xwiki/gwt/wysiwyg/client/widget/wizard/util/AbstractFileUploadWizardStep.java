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

import java.util.EnumSet;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.user.client.ui.wizard.NavigationListener.NavigationDirection;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;

/**
 * Wizard step to handle the upload of a file to a wiki page: display the file input and upload on finish. <br/>
 * 
 * @version $Id$
 */
public abstract class AbstractFileUploadWizardStep implements WizardStep
{
    /**
     * The style of the fields under error.
     */
    protected static final String FIELD_ERROR_STYLE = "xErrorField";

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
     * The error label for the file input.
     */
    private final Label fileErrorLabel = new Label();

    /**
     * The service used to access the uploaded attachments.
     */
    private WikiServiceAsync wikiService;

    /**
     * Default constructor.
     */
    public AbstractFileUploadWizardStep()
    {
        mainPanel.addStyleName("xUploadPanel");
        fileUploadForm.setEncoding(FormPanel.ENCODING_MULTIPART);
        fileUploadForm.setMethod(FormPanel.METHOD_POST);
        // set the url on submit time, just before upload

        Panel fileLabel = new FlowPanel();
        fileLabel.setStyleName("xInfoLabel");
        fileLabel.add(new InlineLabel(getFileLabel()));
        InlineLabel mandatoryLabel = new InlineLabel(Strings.INSTANCE.mandatory());
        mandatoryLabel.addStyleName("xMandatory");
        fileLabel.add(mandatoryLabel);
        fileUploadInput.setName(getFileUploadInputName());
        // FIXME: this should be set from CSS, but it's not possible on all browsers
        fileUploadInput.getElement().setAttribute("size", "50");
        FlowPanel formPanel = new FlowPanel();

        formPanel.add(fileLabel);
        if (!StringUtils.isEmpty(getFileHelpLabel())) {
            Label fileHelpLabel = new Label(getFileHelpLabel());
            fileHelpLabel.setStyleName("xHelpLabel");
            formPanel.add(fileHelpLabel);
        }

        fileErrorLabel.addStyleName("xErrorMsg");
        fileErrorLabel.setVisible(false);

        formPanel.add(fileErrorLabel);
        formPanel.add(fileUploadInput);

        fileUploadForm.setWidget(formPanel);

        mainPanel.add(fileUploadForm);
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
    protected abstract String getFileHelpLabel();

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
        uploadURL.append(URL.encodeComponent(getSpace()));
        uploadURL.append('/');
        uploadURL.append(URL.encodeComponent(getPage()));

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
    public void init(Object data, AsyncCallback< ? > cb)
    {
        hideError();
        cb.onSuccess(null);
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
     * 
     * @see WizardStep#onCancel()
     */
    public void onCancel()
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see WizardStep#onSubmit(AsyncCallback)
     */
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        // Compute and set the upload URL.
        fileUploadForm.setAction(getUploadURL());
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
     * @see {@link #onSubmit}
     */
    protected void onSubmitComplete(SubmitCompleteEvent event, final AsyncCallback<Boolean> async)
    {
        // create the link reference
        wikiService.getAttachment(getWiki(), getSpace(), getPage(), extractFileName(), new AsyncCallback<Attachment>()
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
     * @return the wiki of the document to upload this file to, or null if the default wiki should be used.
     */
    protected abstract String getWiki();

    /**
     * @return the space of the document to upload this file to, or null if the default space should be used.
     */
    protected abstract String getSpace();

    /**
     * @return the document to upload this file to, or null if the default document should be used.
     */
    protected abstract String getPage();

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
     * Method for retrieving the main UI (panel) by sub-classes so that they can customize it.
     * 
     * @return the main ui panel.
     */
    protected Panel getMainPanel()
    {
        return this.mainPanel;
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

    /**
     * Inject the wiki service.
     * 
     * @param wikiService the service used to access the uploaded attachments
     */
    public void setWikiService(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }
}

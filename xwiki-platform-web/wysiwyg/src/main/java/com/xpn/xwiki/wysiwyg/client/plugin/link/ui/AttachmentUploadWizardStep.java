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

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * Specific file upload wizard step to handle upload of a new file attachment in order to create a link to it.
 * 
 * @version $Id$
 */
public class AttachmentUploadWizardStep extends AbstractFileUploadWizardStep
{
    /**
     * The configuration data about the link handled by this wizard step.
     */
    private LinkConfig linkData;

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getUploadURL()
    {
        // use a regular post to the upload action ftm, since REST is throwing an exception and messes up the document
        // in some cases
        // FIXME: un-hardcode this and make it work with multiwiki
        StringBuffer uploadURL = new StringBuffer();
        uploadURL.append("../../upload/");
        uploadURL.append(linkData.getSpace());
        uploadURL.append('/');
        uploadURL.append(linkData.getPage());

        return uploadURL.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getFileUploadInputName()
    {
        return "filepath";
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        return LinkWizardSteps.WIKIPAGECONFIG.toString();
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return linkData;
    }

    /**
     * {@inheritDoc}
     */
    public void init(Object data, AsyncCallback< ? > cb)
    {
        linkData = (LinkConfig) data;
        cb.onSuccess(null);
    }

    /**
     * {@inheritDoc}.
     */
    @Override
    protected void onSubmitComplete(FormSubmitCompleteEvent event, final AsyncCallback<Boolean> async)
    {
        // create the link reference
        WysiwygService.Singleton.getInstance().getAttachmentLink(linkData.getWiki(), linkData.getSpace(),
            linkData.getPage(), extractFileName(), new AsyncCallback<LinkConfig>()
            {
                public void onSuccess(LinkConfig result)
                {
                    if (result == null) {
                        // there was a problem with the attachment, call it a failure
                        Window.alert(Strings.INSTANCE.fileUploadSubmitError());
                        async.onSuccess(false);
                    } else {
                        linkData.setReference("attach:" + ((LinkConfig) result).getReference());
                        linkData.setUrl(((LinkConfig) result).getUrl());
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
}

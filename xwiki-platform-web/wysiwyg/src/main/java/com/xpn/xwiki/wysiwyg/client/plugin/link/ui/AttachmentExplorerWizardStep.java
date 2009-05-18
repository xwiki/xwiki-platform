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
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.WysiwygService;
import com.xpn.xwiki.wysiwyg.client.util.Attachment;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * Specialized {@link AbstractExplorerWizardStep} to select an attachment to a wiki page.
 * 
 * @version $Id$
 */
public class AttachmentExplorerWizardStep extends AbstractExplorerWizardStep
{
    /**
     * The currently edited resource (currently edited page).
     */
    private ResourceName editedResource;

    /**
     * Creates an attachment selection wizard step for the specified resource to be edited.
     * 
     * @param editedResource the currently edited resource
     */
    public AttachmentExplorerWizardStep(ResourceName editedResource)
    {
        // make this smaller, to fit the toggling bar for the AttachmentSelectorWizardStep
        // FIXME: so wrong to have this kind of setting here: this WS should be usable with or without the aggregating
        // step. Also having size information added in more than one single place is very very bad.
        super(false, true, true, editedResource.toString() + "#Attachments", 455, 280);
        this.editedResource = editedResource;
    }

    /**
     * {@inheritDoc}
     */
    protected void initializeSelection()
    {
        String reference = getData().getReference();
        if (!StringUtils.isEmpty(reference)) {
            // resolve the reference of the edited link, relative to the edited resource
            ResourceName r = new ResourceName();
            r.fromString(reference, true);
            getExplorer().setValue(r.resolveRelativeTo(editedResource).toString());
        }
        // else leave the selection where it was the last time
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        if (getExplorer().isNewAttachment()) {
            // if a new attachment will be uploaded, invalidate the explorer cache so that the new attachment shows up
            // in the tree when it will be loaded next. Even if the upload dialog could be canceled and then this is
            // useless, there is no further point where we could access the explorer to invalidate it.
            invalidateExplorerData();
            return LinkWizardSteps.ATTACHUPLOAD.toString();
        }
        return LinkWizardSteps.WIKIPAGECONFIG.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.linkSelectAttachmentTitle();
    }

    /**
     * {@inheritDoc}
     */
    public void onCancel(AsyncCallback<Boolean> async)
    {
        // nothing to do here, just return
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        // get selected file, get its URL and add it
        String attachment = getExplorer().getSelectedAttachment();
        if (StringUtils.isEmpty(attachment) && !getExplorer().isNewAttachment()) {
            Window.alert(Strings.INSTANCE.linkNoAttachmentSelectedError());
            async.onSuccess(false);
        } else if (getExplorer().isNewAttachment()) {
            // prepare the link config for the upload attachment step
            getData().setWiki(getExplorer().getSelectedWiki());
            getData().setSpace(getExplorer().getSelectedSpace());
            getData().setPage(getExplorer().getSelectedPage());
            async.onSuccess(true);
        } else {
            WysiwygService.Singleton.getInstance().getAttachment(getExplorer().getSelectedWiki(),
                getExplorer().getSelectedSpace(), getExplorer().getSelectedPage(),
                getExplorer().getSelectedAttachment(), new AsyncCallback<Attachment>()
                {
                    public void onSuccess(Attachment result)
                    {
                        if (result == null) {
                            // there was a problem with getting the attachment, call it a failure.
                            Window.alert(Strings.INSTANCE.fileUploadSubmitError());
                            async.onSuccess(false);
                        } else {
                            getData().setReference("attach:" + result.getReference());
                            getData().setUrl(result.getDownloadUrl());
                            async.onSuccess(true);
                        }
                    }

                    public void onFailure(Throwable caught)
                    {
                        async.onFailure(caught);
                    }
                });
        }
    }

}

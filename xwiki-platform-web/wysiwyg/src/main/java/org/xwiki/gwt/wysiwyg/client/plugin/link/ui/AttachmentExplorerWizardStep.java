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
package org.xwiki.gwt.wysiwyg.client.plugin.link.ui;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceName;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Specialized {@link AbstractExplorerWizardStep} to select an attachment to a wiki page.
 * 
 * @version $Id$
 */
public class AttachmentExplorerWizardStep extends AbstractExplorerWizardStep
{
    /**
     * The attachment prefix to use for attached files.
     */
    private static final String ATTACH_PREFIX = "attach:";

    /**
     * The currently edited resource (currently edited page).
     */
    private ResourceName editedResource;

    /**
     * The service used to retrieve the attachments.
     */
    private WikiServiceAsync wikiService;

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
    protected void initializeSelection(AsyncCallback< ? > initCallback)
    {
        String reference = getData().getReference();
        if (!StringUtils.isEmpty(reference)) {
            ResourceName r = new ResourceName(reference, true);
            getExplorer().setValue(r.toString());
        }
        // else leave the selection where it was the last time
        super.initializeSelection(initCallback);
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
            return LinkWizardSteps.ATTACHMENT_UPLOAD.toString();
        }
        return LinkWizardSteps.WIKI_PAGE_CONFIG.toString();
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
    public void onCancel()
    {
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(final AsyncCallback<Boolean> async)
    {
        // reset error display
        hideError();
        // get selected file, get its URL and add it
        String attachment = getExplorer().getSelectedAttachment();
        if (StringUtils.isEmpty(attachment) && !getExplorer().isNewAttachment()) {
            displayError(Strings.INSTANCE.linkNoAttachmentSelectedError());
            async.onSuccess(false);
        } else if (StringUtils.isEmpty(getData().getReference())
            || !getData().getReference().equals(ATTACH_PREFIX + getExplorer().getValue())) {
            // commit changes only if reference was changed
            if (getExplorer().isNewAttachment()) {
                // prepare the link config for the upload attachment step
                getData().setWiki(getExplorer().getSelectedWiki());
                getData().setSpace(getExplorer().getSelectedSpace());
                getData().setPage(getExplorer().getSelectedPage());
                async.onSuccess(true);
            } else {
                // FIXME: move the reference setting logic in a controller, along with the async fetching
                wikiService.getAttachment(getExplorer().getSelectedWiki(), getExplorer().getSelectedSpace(),
                    getExplorer().getSelectedPage(), getExplorer().getSelectedAttachment(),
                    new AsyncCallback<Attachment>()
                    {
                        public void onSuccess(Attachment result)
                        {
                            if (result == null) {
                                // there was a problem with getting the attachment, call it a failure.
                                displayError(Strings.INSTANCE.fileGetSubmitError());
                                async.onSuccess(false);
                            } else {
                                ResourceName ref = new ResourceName(result.getReference(), true);
                                getData().setReference(ATTACH_PREFIX + ref.getRelativeTo(editedResource).toString());
                                getData().setUrl(result.getURL());
                                async.onSuccess(true);
                            }
                        }

                        public void onFailure(Throwable caught)
                        {
                            async.onFailure(caught);
                        }
                    });
            }
        } else {
            async.onSuccess(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getHelpLabelText()
    {
        return Strings.INSTANCE.linkSelectAttachmentHelpLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDefaultErrorText()
    {
        return Strings.INSTANCE.linkNoAttachmentSelectedError();
    }

    /**
     * Injects the wiki service.
     * 
     * @param wikiService the service used to retrieve the attachments
     */
    public void setWikiService(WikiServiceAsync wikiService)
    {
        this.wikiService = wikiService;
    }
}

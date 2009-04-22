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
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * Specialized {@link AbstractSelectorWizardStep} to select an attachment to a wiki page (existing or new).
 * 
 * @version $Id$
 */
public class AttachmentSelectorWizardStep extends AbstractSelectorWizardStep
{
    /**
     * The default selection on the tree.
     */
    private String defaultSelection;

    /**
     * Creates an attachment selection wizard step with the specified default selection. The selection will be used to
     * position the attachment selection tree on the resource named by it, unless specified otherwise by the
     * initialization data.
     * 
     * @param defaultSelection the default selection of the wiki explorer
     */
    public AttachmentSelectorWizardStep(String defaultSelection)
    {
        super(false, true, true, defaultSelection);
        this.defaultSelection = defaultSelection;
    }

    /**
     * {@inheritDoc}
     */
    protected void initializeExplorerSelection()
    {
        String reference = getLinkData().getReference();
        if (!StringUtils.isEmpty(reference)) {
            if (reference.startsWith("attach")) {
                reference = reference.substring(7);
            }
            getExplorer().setValue(reference);
        } else {
            // set the default selection back, every time for attachments
            if (!StringUtils.isEmpty(defaultSelection)) {
                // alter the defaultSelection to have an #Attachments anchor at the end, so that the attachments section
                // is selected from the defaultResource
                String attachmentsAnchor = "#Attachments";
                getExplorer().setValue(
                    defaultSelection + (!defaultSelection.endsWith(attachmentsAnchor) ? attachmentsAnchor : ""));
            }
        }
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
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        // get selected file, get its URL and add it
        String attachment = getExplorer().getSelectedAttachment();
        if (StringUtils.isEmpty(attachment) && !getExplorer().isNewAttachment()) {
            Window.alert(Strings.INSTANCE.linkNoAttachmentSelectedError());
            async.onSuccess(false);
        } else if (getExplorer().isNewAttachment()) {
            // prepare the link config for the uplaod attachment step
            getLinkData().setWiki(getExplorer().getSelectedWiki());
            getLinkData().setSpace(getExplorer().getSelectedSpace());
            getLinkData().setPage(getExplorer().getSelectedPage());
            async.onSuccess(true);
        } else {
            String attachmentRef = "attach:" + getExplorer().getValue();
            String attachmentURL = getExplorer().getSelectedResourceURL();
            getLinkData().setReference(attachmentRef);
            getLinkData().setUrl(attachmentURL);
            async.onSuccess(true);
        }
    }

}

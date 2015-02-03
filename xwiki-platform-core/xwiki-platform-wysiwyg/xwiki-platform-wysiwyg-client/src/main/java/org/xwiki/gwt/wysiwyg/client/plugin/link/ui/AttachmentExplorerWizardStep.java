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

import org.xwiki.gwt.user.client.Config;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.AttachmentReference;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Specialized {@link AbstractExplorerWizardStep} to select an attachment to a wiki page.
 * 
 * @version $Id$
 */
public class AttachmentExplorerWizardStep extends AbstractExplorerWizardStep
{
    /**
     * Creates a new attachment selection wizard step that allows the user to select an attachment from a tree.
     *
     * @param config the configuration object
     */
    public AttachmentExplorerWizardStep(Config config)
    {
        // Reduce the size to fit the toggling bar of the attachment selector step which aggregates this step.
        // FIXME: This wizard step should be usable w/o the aggregating step. Also having size information added in more
        // than one single place is bad.
        super(config.getParameter("attachmentTreeURL"), 455, 280);

        setStepTitle(Strings.INSTANCE.linkSelectAttachmentTitle());
        setHelpLabelText(Strings.INSTANCE.linkSelectAttachmentHelpLabel());
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        AttachmentReference attachmentReference =
            new AttachmentReference(getData().getDestination().getEntityReference());
        return StringUtils.isEmpty(attachmentReference.getFileName()) ? LinkWizardStep.ATTACHMENT_UPLOAD.toString()
            : LinkWizardStep.LINK_CONFIG.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void onSubmit(AsyncCallback<Boolean> async)
    {
        hideError();

        AttachmentReference attachmentReference = new AttachmentReference();
        attachmentReference.getWikiPageReference().setWikiName(getExplorer().getSelectedWiki());
        attachmentReference.getWikiPageReference().setSpaceName(getExplorer().getSelectedSpace());
        attachmentReference.getWikiPageReference().setPageName(getExplorer().getSelectedPage());
        attachmentReference.setFileName(getExplorer().getSelectedAttachment());

        if (getExplorer().isNewAttachment()) {
            getData().getDestination().setEntityReference(attachmentReference.getEntityReference());
            // Invalidate the explorer cache so that the new attachment shows up in the tree when the tree is reloaded.
            getExplorer().invalidateCache();
            async.onSuccess(true);
        } else if (StringUtils.isEmpty(attachmentReference.getFileName())) {
            displayError(Strings.INSTANCE.linkNoAttachmentSelectedError());
            async.onSuccess(false);
        } else if (getData().getDestination().getEntityReference().equals(attachmentReference.getEntityReference())) {
            async.onSuccess(true);
        } else {
            updateLinkConfig(attachmentReference.getEntityReference());
            async.onSuccess(true);
        }
    }
}

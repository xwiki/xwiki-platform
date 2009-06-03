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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import com.xpn.xwiki.wysiwyg.client.util.Attachment;
import com.xpn.xwiki.wysiwyg.client.util.ResourceName;
import com.xpn.xwiki.wysiwyg.client.widget.wizard.util.AbstractFileUploadWizardStep;

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
     * The resource currently edited by this wizard step, i.e. the currently edited wiki document.
     */
    private ResourceName editedResource;

    /**
     * Builds an attachment upload wizard step for the currently edited resource.
     * 
     * @param editedResource the resource currently edited by this wizard step, i.e. the currently edited wiki document
     */
    public AttachmentUploadWizardStep(ResourceName editedResource)
    {
        super();
        this.editedResource = editedResource;
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
     * {@inheritDoc}
     */
    @Override
    public String getPage()
    {
        return linkData.getPage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSpace()
    {
        return linkData.getSpace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWiki()
    {
        return linkData.getWiki();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onAttachmentUploaded(Attachment attach)
    {
        // commit the attachment data in the link config
        // commit relative reference
        ResourceName ref = new ResourceName(attach.getReference(), true);
        // FIXME: move the reference setting logic in a controller        
        linkData.setReference("attach:" + ref.getRelativeTo(editedResource).toString());
        linkData.setUrl(attach.getDownloadUrl());
    }
}

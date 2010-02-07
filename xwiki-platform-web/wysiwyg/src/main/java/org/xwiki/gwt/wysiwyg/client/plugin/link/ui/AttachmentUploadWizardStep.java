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

import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.link.LinkConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.link.ui.LinkWizard.LinkWizardSteps;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractFileUploadWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceName;

import com.google.gwt.user.client.rpc.AsyncCallback;

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
        return LinkWizardSteps.WIKI_PAGE_CONFIG.toString();
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
    public void init(final Object data, final AsyncCallback< ? > cb)
    {
        super.init(data, new AsyncCallback<Object>()
        {
            public void onSuccess(Object result)
            {
                linkData = (LinkConfig) data;
                cb.onSuccess(null);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPage()
    {
        return linkData.getPage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSpace()
    {
        return linkData.getSpace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getWiki()
    {
        return linkData.getWiki();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getFileHelpLabel()
    {
        return Strings.INSTANCE.linkAttachmentUploadHelpLabel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onAttachmentUploaded(Attachment attach, AsyncCallback<Boolean> async)
    {
        // commit the attachment data in the link config
        // commit relative reference
        ResourceName ref = new ResourceName(attach.getReference(), true);
        // FIXME: move the reference setting logic in a controller
        linkData.setReference("attach:" + ref.getRelativeTo(editedResource).toString());
        linkData.setUrl(attach.getURL());
        async.onSuccess(true);
    }
}

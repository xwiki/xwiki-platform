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
package org.xwiki.gwt.wysiwyg.client.plugin.image.ui;

import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ImageConfig;
import org.xwiki.gwt.wysiwyg.client.plugin.image.ui.ImageWizard.ImageWizardSteps;
import org.xwiki.gwt.wysiwyg.client.widget.wizard.util.AbstractFileUploadWizardStep;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.ResourceName;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Specialized wizard step to upload an image.
 * 
 * @version $Id$
 */
public class ImageUploadWizardStep extends AbstractFileUploadWizardStep
{
    /**
     * The image to configure with this {@code WizardStep}.
     */
    private ImageConfig imageData;

    /**
     * The resource currently edited by this wizard step, i.e. the currently edited wiki document.
     */
    private ResourceName editedResource;

    /**
     * Builds an image upload wizard step for the currently edited resource.
     * 
     * @param editedResource the resource currently edited by this wizard step, i.e. the currently edited wiki document
     */
    public ImageUploadWizardStep(ResourceName editedResource)
    {
        super();
        this.editedResource = editedResource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getPage()
    {
        return imageData.getPage();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSpace()
    {
        return imageData.getSpace();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getWiki()
    {
        return imageData.getWiki();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onAttachmentUploaded(Attachment attach, AsyncCallback<Boolean> async)
    {
        // upload is done successfully, commit the data in the image config
        imageData.setImageURL(attach.getURL());
        ResourceName ref = new ResourceName(attach.getReference(), true);
        imageData.setReference(ref.getRelativeTo(editedResource).toString());
        async.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getFileHelpLabel()
    {
        return Strings.INSTANCE.imageUploadHelpLabel();
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        return ImageWizardSteps.IMAGE_CONFIG.toString();
    }

    /**
     * {@inheritDoc}
     */
    public Object getResult()
    {
        return imageData;
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
                imageData = (ImageConfig) data;
                cb.onSuccess(null);
            }

            public void onFailure(Throwable caught)
            {
                cb.onFailure(caught);
            }
        });
    }
}

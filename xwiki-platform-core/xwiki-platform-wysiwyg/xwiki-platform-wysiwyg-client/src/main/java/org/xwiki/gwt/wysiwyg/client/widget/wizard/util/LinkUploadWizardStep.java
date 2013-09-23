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

import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiServiceAsync;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Generic wizard step that can be used by wizards that create links to attachments.
 * 
 * @param <C> the type of configuration data associated with a link
 * @version $Id$
 */
public class LinkUploadWizardStep<C extends EntityConfig> extends AbstractFileUploadWizardStep
{
    /**
     * The link to the uploaded file.
     */
    private EntityLink<C> entityLink;

    /**
     * Creates a new upload wizard step that uses the given service to access the attachments.
     * 
     * @param wikiService the service used to access the attachments
     */
    public LinkUploadWizardStep(WikiServiceAsync wikiService)
    {
        super(wikiService);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractFileUploadWizardStep#init(Object, AsyncCallback)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void init(final Object data, final AsyncCallback< ? > cb)
    {
        entityLink = (EntityLink<C>) data;
        super.init(data, cb);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractFileUploadWizardStep#getTargetPageReference()
     */
    @Override
    protected WikiPageReference getTargetPageReference()
    {
        return new WikiPageReference(entityLink.getDestination().getEntityReference());
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractFileUploadWizardStep#onAttachmentUploaded(Attachment, AsyncCallback)
     */
    @Override
    protected void onAttachmentUploaded(Attachment attachment, final AsyncCallback<Boolean> callback)
    {
        entityLink.getDestination().setEntityReference(attachment.getReference().clone());
        callback.onSuccess(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractFileUploadWizardStep#getResult()
     */
    public Object getResult()
    {
        return entityLink;
    }
}

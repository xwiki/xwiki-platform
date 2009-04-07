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
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.util.StringUtils;

/**
 * Specialized {@link AbstractSelectorWizardStep} to select an attachment to a wiki page (existing or new).
 * 
 * @version $Id$
 */
public class AttachmentSelectorWizardStep extends AbstractSelectorWizardStep
{
    /**
     * Default constructor.
     */
    public AttachmentSelectorWizardStep()
    {
        // don't show "Add attachment" for the moment
        super(false, true, false);
    }

    /**
     * {@inheritDoc}
     */
    public void init(Object data, AsyncCallback< ? > cb)
    {
        // if the data reference has a "attach" prefix, strip it away, to make the reference valid in the tree
        String reference = ((LinkConfig) data).getReference();
        if (!StringUtils.isEmpty(reference) && reference.startsWith("attach")) {
            reference = reference.substring(7);
            ((LinkConfig) data).setReference(reference);
        }
        super.init(data, cb);
    }

    /**
     * {@inheritDoc}
     */
    public String getNextStep()
    {
        return "wikipageconfig";
    }

    /**
     * {@inheritDoc}
     */
    public String getStepTitle()
    {
        return Strings.INSTANCE.selectAttachmentTitle();
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
        if (StringUtils.isEmpty(attachment)) {
            Window.alert(Strings.INSTANCE.linkNoAttachmentSelectedError());
            async.onSuccess(false);
        } else {
            String attachmentRef = "attach:" + getExplorer().getValue();
            String attachmentURL = getExplorer().getSelectedResourceURL();
            getLinkData().setReference(attachmentRef);
            getLinkData().setUrl(attachmentURL);
            async.onSuccess(true);
        }
    }

}

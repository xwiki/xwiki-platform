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
package com.xpn.xwiki.web;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * <p>
 * Action for previewing document changes. It prepares a temporarily changed document which is placed in the context,
 * without actually saving anything. The response is normally rendered by the {@code preview.vm} template.
 * </p>
 * <p>
 * This action also works like a request dispatcher, an early work-around for the fact that in HTML a form can only have
 * one destination URL. Thus, the form had to be submitted to one action which would further dispatch the request to
 * other actions, based on the clicked form button. Since preview is the safest method of the possible form actions, it
 * was chosen as the dispatcher. Currently this functionality is deprecated and maintained only for backwards
 * compatibility with older skins, since a cleaner dispatcher was implemented in {@link ActionFilter}.
 * </p>
 *
 * @version $Id$
 */
public class PreviewAction extends EditAction
{
    /**
     * Default constructor.
     */
    public PreviewAction()
    {
        this.waitForXWikiInitialization = true;
    }

    @Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return EditForm.class;
    }

    /**
     * Check if a certain action was selected by the user. This is needed in older skins, which don't make use of the
     * {@link ActionFilter}'s dispatcher functionality, but rely on detecting the submit button that was clicked.
     *
     * @param action the request parameter value that should be tested
     * @return {@code true} if the value is a non-empty string, {@code false} otherwise
     */
    private boolean isActionSelected(String action)
    {
        return StringUtils.isNotEmpty(action);
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String formactionsave = request.getParameter("formactionsave");
        String formactioncancel = request.getParameter("formactioncancel");
        String formactionsac = request.getParameter("formactionsac");

        if (isActionSelected(formactionsave)) {
            SaveAction sa = new SaveAction();
            if (sa.action(context)) {
                sa.render(context);
            }
            return false;
        }

        if (isActionSelected(formactioncancel)) {
            CancelAction ca = new CancelAction();
            if (ca.action(context)) {
                ca.render(context);
            }
            return false;
        }

        if (isActionSelected(formactionsac)) {
            SaveAndContinueAction saca = new SaveAndContinueAction();
            if (saca.action(context)) {
                saca.render(context);
            }
            return false;
        }
        return true;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiDocument editedDocument = prepareEditedDocument(context);

        // The current user editing the document should be displayed as author and creator (if the edited document is
        // new) when the edited document is previewed.
        editedDocument.setAuthorReference(context.getUserReference());
        if (editedDocument.isNew()) {
            editedDocument.setCreatorReference(context.getUserReference());
        }

        // Make sure the current user doesn't use the programming rights of the previous content author (by editing a
        // document saved with programming rights, changing it and then previewing it). Also make sure the code
        // requiring programming rights is executed in preview mode if the current user has programming rights.
        editedDocument.setContentAuthorReference(context.getUserReference());

        // Reconfirm edit (captcha) when jcaptcha is not correct.
        Boolean reCheckCaptcha = (Boolean) context.get("recheckcaptcha");
        return reCheckCaptcha != null && reCheckCaptcha ? "captcha" : "preview";
    }
}

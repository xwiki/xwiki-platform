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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.web.LegacyAction;

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
@Component
@Named("preview")
@Singleton
public class PreviewAction extends EditAction
{
    @Inject
    @Named("save")
    private LegacyAction saveAction;

    @Inject
    @Named("cancel")
    private LegacyAction cancelAction;

    @Inject
    @Named("saveandcontinue")
    private LegacyAction saveandcontinueAction;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    /**
     * Default constructor.
     */
    public PreviewAction()
    {
        this.waitForXWikiInitialization = true;
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
            if (((XWikiAction) this.saveAction).action(context)) {
                ((XWikiAction) this.saveAction).render(context);
            }
            return false;
        }

        if (isActionSelected(formactioncancel)) {
            if (((XWikiAction) this.cancelAction).action(context)) {
                ((XWikiAction) this.cancelAction).render(context);
            }
            return false;
        }

        if (isActionSelected(formactionsac)) {
            if (((XWikiAction) this.saveandcontinueAction).action(context)) {
                ((XWikiAction) this.saveandcontinueAction).render(context);
            }
            return false;
        }
        // CSRF prevention
        if (!csrfTokenCheck(context, true)) {
            return false;
        }
        return true;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiDocument editedDocument = prepareEditedDocument(context);
        DocumentAuthors editedDocumentAuthors = editedDocument.getAuthors();

        // The current user editing the document should be displayed as author and creator (if the edited document is
        // new) when the edited document is previewed.
        UserReference currentUserReference = this.currentUserResolver.resolve(CurrentUserReference.INSTANCE);
        editedDocumentAuthors.setOriginalMetadataAuthor(currentUserReference);
        if (editedDocument.isNew()) {
            editedDocumentAuthors.setCreator(currentUserReference);
        }

        // Make sure the meta data of the edited document (e.g. a text area property) is executed with the rights of the
        // effective author specified by the preview request. This also means that the request effective author (e.g.
        // the currently authenticated user) can't execute code on behalf of the previous metadata author of the edited
        // document (e.g. by modifying a text area property and previewing the changes without saving, i.e. without
        // updating the medata author).
        context.getRequest().getEffectiveAuthor().ifPresent(editedDocumentAuthors::setEffectiveMetadataAuthor);
        if (editedDocument.isContentDirty()) {
            // The request effective author has modified the content of the edited document (without saving) so we must
            // execute this content using the rights of the request effective author. This is needed to prevent
            // privilege escalation from the previous content author to the request effective author.
            editedDocumentAuthors.setContentAuthor(editedDocumentAuthors.getEffectiveMetadataAuthor());
        }

        if ("1".equals(context.getRequest().getParameter("diff"))
            && StringUtils.isNotEmpty(context.getRequest().getParameter("version"))) {
            return "previewdiff";
        } else {
            return "preview";
        }
    }
}

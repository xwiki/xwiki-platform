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
package org.xwiki.refactoring.internal.job;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.refactoring.job.RefactoringJobs;

import com.google.common.base.Objects;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A job that can replace the occurrences or an user reference with another user reference.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Named(RefactoringJobs.REPLACE_USER)
public class ReplaceUserJob extends AbstractReplaceUserJob
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    protected void update(DocumentReference documentReference)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        try {
            update(xcontext.getWiki().getDocument(documentReference, xcontext));
        } catch (XWikiException e) {
            this.logger.error("Failed to update document [{}]. Root cause is [{}].", documentReference,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void update(XWikiDocument document) throws XWikiException
    {
        List<String> updatedFields = new ArrayList<>();
        if (getRequest().isReplaceDocumentAuthor()
            && Objects.equal(document.getAuthorReference(), getRequest().getOldUserReference())) {
            document.setAuthorReference(getRequest().getNewUserReference());
            updatedFields.add("author");
        }
        if (getRequest().isReplaceDocumentContentAuthor()
            && Objects.equal(document.getContentAuthorReference(), getRequest().getOldUserReference())) {
            document.setContentAuthorReference(getRequest().getNewUserReference());
            updatedFields.add("contentAuthor");
        }
        if (getRequest().isReplaceDocumentCreator()
            && Objects.equal(document.getCreatorReference(), getRequest().getOldUserReference())) {
            document.setCreatorReference(getRequest().getNewUserReference());
            updatedFields.add("creator");
        }
        if (updatedFields.size() > 0) {
            XWikiContext xcontext = this.xcontextProvider.get();
            String comment = String.format("Replaced user [%s] in fields %s to user [%s]",
                getRequest().getOldUserReference(), updatedFields, getRequest().getNewUserReference());
            xcontext.getWiki().saveDocument(document, comment, xcontext);
        }
    }
}

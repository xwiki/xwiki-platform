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

import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

/**
 * @deprecated use {@link EditAction} with {@code editor=inline} in the query string instead since 3.2
 */
@Component
@Named("inline")
@Singleton
@Deprecated
public class InlineAction extends XWikiAction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(InlineAction.class);

    @Override
    protected Class<? extends XWikiForm> getFormClass()
    {
        return EditForm.class;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = context.getDoc();

        synchronized (doc) {
            XWikiForm form = context.getForm();

            XWikiDocument cdoc = (XWikiDocument) context.get("cdoc");
            if (cdoc == null) {
                cdoc = doc;
            }

            EditForm peform = (EditForm) form;

            XWikiDocument doc2 = doc.clone();
            context.put("doc", doc2);

            String parent = peform.getParent();
            if (parent != null) {
                doc2.setParent(parent);
            }
            String creator = peform.getCreator();
            if (creator != null) {
                doc2.setCreator(creator);
            }
            String defaultLanguage = peform.getDefaultLanguage();
            if ((defaultLanguage != null) && !defaultLanguage.equals("")) {
                doc2.setDefaultLanguage(defaultLanguage);
            }
            if (doc2.getDefaultLanguage().equals("")) {
                doc2.setDefaultLanguage(context.getWiki().getLanguagePreference(context));
            }
            try {
                readFromTemplate(doc2, peform.getTemplate(), context);
            } catch (XWikiException e) {
                if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                    return "docalreadyexists";
                }
            }

            if (doc == cdoc) {
                context.put("cdoc", doc2);
            } else {
                XWikiDocument cdoc2 = cdoc.clone();
                readFromTemplate(cdoc2, peform.getTemplate(), context);
                context.put("cdoc", cdoc2);
            }

            doc2.readFromForm((EditForm) form, context);

            // Set the current user as creator, author and contentAuthor when the edited document is newly created
            // to avoid using XWikiGuest instead (because those fields were not previously initialized).
            // This is needed for the script right, as guest doesn't have it and this would block the execution of
            // scripts in newly created documents even if the user creating the document has the right.
            if (doc2.isNew()) {
                doc2.setCreatorReference(context.getUserReference());
                doc2.setAuthorReference(context.getUserReference());
                doc2.setContentAuthorReference(context.getUserReference());
            }

            /* Setup a lock */
            try {
                XWikiLock lock = doc.getLock(context);
                if ((lock == null) || (lock.getUserName().equals(context.getUser())) || (peform.isLockForce())) {
                    doc.setLock(context.getUser(), context);
                }
            } catch (Exception e) {
                // Lock should never make XWiki fail
                // But we should log any related information
                LOGGER.error("Exception while setting up lock", e);
            }
        }

        // Make sure object property fields are displayed in edit mode.
        // See XWikiDocument#display(String, BaseObject, XWikiContext)
        context.put("display", "edit");

        return "inline";
    }
}

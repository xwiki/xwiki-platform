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

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

/**
 * @deprecated use {@link EditAction} with {@code editor=inline} in the query string instead since 3.2
 */
@Deprecated
public class InlineAction extends XWikiAction
{
    private static final Logger LOGGER = LoggerFactory.getLogger(InlineAction.class);

    @Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return EditForm.class;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = context.getDoc();

        synchronized (doc) {
            XWikiForm form = context.getForm();
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            Document vdoc = (Document) vcontext.get("doc");
            Document vcdoc = (Document) vcontext.get("cdoc");
            EditForm peform = (EditForm) form;

            XWikiDocument doc2 = doc.clone();
            Document vdoc2 = doc2.newDocument(context);
            context.put("doc", doc2);
            vcontext.put("doc", vdoc2);

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
                doc2.readFromTemplate(peform, context);
            } catch (XWikiException e) {
                if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
                    return "docalreadyexists";
                }
            }

            if (vdoc == vcdoc) {
                vcontext.put("cdoc", vdoc2);
            } else {
                XWikiDocument cdoc = vcdoc.getDocument();
                XWikiDocument cdoc2 = cdoc.clone();
                vcontext.put("cdoc", cdoc2.newDocument(context));
                cdoc2.readFromTemplate(peform, context);
            }

            doc2.readFromForm((EditForm) form, context);

            /* Setup a lock */
            try {
                XWikiLock lock = doc.getLock(context);
                if ((lock == null) || (lock.getUserName().equals(context.getUser())) || (peform.isLockForce())) {
                    doc.setLock(context.getUser(), context);
                }
            } catch (Exception e) {
                e.printStackTrace();
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

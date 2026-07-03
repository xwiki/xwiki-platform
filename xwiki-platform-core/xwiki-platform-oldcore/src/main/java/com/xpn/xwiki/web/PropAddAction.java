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
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.util.Util;

@Component
@Named("propadd")
@Singleton
public class PropAddAction extends XWikiAction
{
    @Override
    protected Class<? extends XWikiForm> getFormClass()
    {
        return PropAddForm.class;
    }

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        // CSRF prevention
        if (!csrfTokenCheck(context)) {
            return false;
        }

        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        // We need to clone this document first, since a cached storage would return the same object for the
        // following requests, so concurrent request might get a partially modified object, or worse, if an error
        // occurs during the save, the cached object will not reflect the actual document at all.
        doc = doc.clone();

        XWikiForm form = context.getForm();

        String propName = ((PropAddForm) form).getPropName();

        if (!Util.isValidXMLElementName(propName)) {
            writeAjaxErrorResponse(HttpServletResponse.SC_BAD_REQUEST,
                localizePlainOrKey("action.addClassProperty.error.invalidName"), context);
            return false;
        }

        String propType = ((PropAddForm) form).getPropType();
        BaseClass bclass = doc.getXClass();
        bclass.setName(doc.getFullName());
        if (bclass.get(propName) != null) {
            String localizedMessage =
                localizePlainOrReturnKey("action.addClassProperty.error.alreadyExists", propName);
            writeAjaxErrorResponse(HttpServletResponse.SC_BAD_REQUEST, localizedMessage, context);
            return false;
        } else {
            MetaClass mclass = xwiki.getMetaclass();
            PropertyMetaClass pmclass = (PropertyMetaClass) mclass.get(propType);
            if (pmclass != null) {
                PropertyClass pclass = (PropertyClass) pmclass.newObject(context);
                pclass.setObject(bclass);
                pclass.setName(propName);
                pclass.setPrettyName(propName);
                bclass.put(propName, pclass);
                doc.setAuthorReference(context.getUserReference());
                if (doc.isNew()) {
                    doc.setCreatorReference(context.getUserReference());
                }
                doc.setMetaDataDirty(true);

                String comment = localizePlainOrKey("core.comment.addClassProperty");

                // Make sure the user is allowed to make this modification
                context.getWiki().checkSavingDocument(context.getUserReference(), doc, comment, true, context);

                xwiki.saveDocument(doc, comment, true, context);
            }
        }
        // forward to edit
        String redirect = Utils.getRedirect("edit", "editor=class", context);
        sendRedirect(response, redirect);
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        return "exception";
    }
}

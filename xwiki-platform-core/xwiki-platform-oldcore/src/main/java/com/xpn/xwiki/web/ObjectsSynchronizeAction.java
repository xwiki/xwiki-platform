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

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Synchronizes the objects in a document with their current classes, by removing any deprecated properties.
 *
 * @version $Id$
 * @since 2.4M2
 */
@Component
@Named("objectsync")
@Singleton
public class ObjectsSynchronizeAction extends XWikiAction
{
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        XWikiRequest request = context.getRequest();

        String className = request.getParameter("classname");
        String objectNumber = request.getParameter("object");

        if (className != null && objectNumber != null) {
            try {
                BaseObject object = doc.getObject(className, Integer.valueOf(objectNumber));
                synchronizeObject(object, context);
            } catch (Exception ex) {
                // Wrong parameters, non-existing object
                return true;
            }
        } else {
            for (List<BaseObject> classObjects : doc.getXObjects().values()) {
                for (BaseObject object : classObjects) {
                    if (object != null) {
                        synchronizeObject(object, context);
                    }
                }
            }
        }

        // Set the new author
        doc.setAuthorReference(context.getUserReference());

        xwiki.saveDocument(doc, localizePlainOrKey("core.model.xobject.synchronizeObjects.versionSummary"), true,
            context);

        if (Utils.isAjaxRequest(context)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.setContentLength(0);
        } else {
            // forward to edit
            String redirect = Utils.getRedirect("edit", "editor=object", context);
            sendRedirect(response, redirect);
        }
        return false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
        context.put("message", "core.model.xobject.synchronizeObjects.error.missingObject");
        return "exception";
    }

    /**
     * Remove deprecated fields (properties deleted from the XClass) from an object.
     *
     * @param object the object to synchronize
     * @param context the current request context
     */
    private void synchronizeObject(BaseObject object, XWikiContext context)
    {
        for (BaseProperty property : object.getXClass(context).getDeprecatedObjectProperties(object)) {
            object.removeField(property.getName());
        }
    }
}

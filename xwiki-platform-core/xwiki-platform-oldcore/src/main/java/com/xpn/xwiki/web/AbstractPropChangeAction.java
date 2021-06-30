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

import javax.servlet.http.HttpServletResponse;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Base class for manipulating property definitions: disable, enable, delete. The property to alter is specified in the
 * {@code propname} request parameter, and the class is the one defined in the requested document.
 *
 * @version $Id$
 * @since 2.4M2
 */
public abstract class AbstractPropChangeAction extends XWikiAction
{
    @Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return PropChangeForm.class;
    }

    /**
     * Tries to change the specified property, and redirect back to the class editor (or the specified {@code xredirect}
     * location). If the property does not exist, forward to the exception page.
     *
     * @param context the current request context
     * @return {@code false} if the operation succeeded and the response is finished, {@code true} if the response must
     *         be rendered by {@link #render(XWikiContext)}
     * @throws XWikiException if saving the document fails
     */
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        PropChangeForm form = (PropChangeForm) context.getForm();
        String propertyName = form.getPropertyName();
        BaseClass xclass = doc.getXClass();

        if (propertyName != null && xclass.get(propertyName) != null) {
            // CSRF prevention
            if (!csrfTokenCheck(context)) {
                return false;
            }
            changePropertyDefinition(xclass, propertyName, context);
        } else {
            return true;
        }

        if (Utils.isAjaxRequest(context)) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            response.setContentLength(0);
        } else {
            String redirect = Utils.getRedirect("edit", "editor=class", context);
            sendRedirect(response, redirect);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        context.getResponse().setStatus(HttpServletResponse.SC_NOT_FOUND);
        context.put("message", "core.model.xclass.classProperty.error.missingProperty");
        return "exception";
    }

    /**
     * The method which does the actual modification of the property definition.
     *
     * @param xclass the affected class
     * @param propertyName the property to change
     * @param context the current request context
     * @throws XWikiException if a storage error occurs
     */
    public abstract void changePropertyDefinition(BaseClass xclass, String propertyName, XWikiContext context)
        throws XWikiException;
}

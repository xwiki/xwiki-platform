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

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class ObjectAddAction extends XWikiAction
{
    private static final String[] EMPTY_PROPERTY = new String[] { "" };

    /**
     * A pattern that matches the {@code xobjectNumber} request parameter which is used to pass the number of the added
     * object to the redirect URL.
     */
    private static final Pattern XOBJECT_NUMBER_PARAMETER = Pattern.compile("(\\?|&)xobjectNumber=?(&|#|$)");

    /**
     * Used to resolve XClass references.
     */
    private EntityReferenceResolver<String> relativeResolver = Utils.getComponent(EntityReferenceResolver.TYPE_STRING,
        "relative");

    @Override
    protected Class<? extends XWikiForm> getFomClass()
    {
        return ObjectAddForm.class;
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
        DocumentReference userReference = context.getUserReference();
        XWikiDocument doc = context.getDoc();
        ObjectAddForm oform = (ObjectAddForm) context.getForm();

        String className = oform.getClassName();
        EntityReference classReference = this.relativeResolver.resolve(className, EntityType.DOCUMENT);
        BaseObject object = doc.newXObject(classReference, context);

        BaseClass baseclass = object.getXClass(context);
        // The request parameter names that correspond to object fields must NOT specify the object number because the
        // object number is not known before the object is added. The following is a good parameter name:
        // Space.Class_property. As a consequence we use only the class name to extract the object from the request.
        Map<String, String[]> objmap = oform.getObject(className);
        // We need to have a string in the map for each field for the object to be correctly created.
        // Otherwise, queries using the missing properties will fail to return this object.
        @SuppressWarnings("unchecked")
        Collection<PropertyClass> fields = baseclass.getFieldList();
        for (PropertyClass property : fields) {
            String name = property.getName();
            if (objmap.get(name) == null) {
                objmap.put(name, EMPTY_PROPERTY);
            }
        }

        // Load the object properties that are defined in the request.
        baseclass.fromMap(objmap, object);

        doc.setAuthorReference(userReference);
        if (doc.isNew()) {
            doc.setCreatorReference(userReference);
        }
        xwiki.saveDocument(doc, localizePlainOrKey("core.comment.addObject"), true, context);

        // If this is an ajax request, no need to redirect.
        if (Utils.isAjaxRequest(context)) {
            context.getResponse().setStatus(HttpServletResponse.SC_NO_CONTENT);
            return false;
        }

        // forward to edit
        String redirect = Utils.getRedirect("edit", "editor=object", "xcontinue", "xredirect");
        // If the redirect URL contains the xobjectNumber parameter then inject the number of the added object as its
        // value so that the target page knows which object was added.
        redirect =
            XOBJECT_NUMBER_PARAMETER.matcher(redirect).replaceFirst("$1xobjectNumber=" + object.getNumber() + "$2");
        sendRedirect(response, redirect);

        return false;
    }
}

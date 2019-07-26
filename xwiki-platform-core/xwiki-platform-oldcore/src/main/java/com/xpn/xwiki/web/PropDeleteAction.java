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

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Action for removing a property definition from the current class. The property to remove is specified in the
 * {@code propname} request parameter, and the class is the one defined in the requested document.
 *
 * @version $Id$
 * @since 2.4M2
 */
public class PropDeleteAction extends AbstractPropChangeAction
{
    @Override
    public void changePropertyDefinition(BaseClass xclass, String propertyName, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = context.getDoc();

        // We need to clone this document first, since a cached storage would return the same object for the
        // following requests, so concurrent request might get a partially modified object, or worse, if an error
        // occurs during the save, the cached object will not reflect the actual document at all.
        doc = doc.clone();

        xclass.removeField(propertyName);

        String comment = localizePlainOrKey("core.model.xclass.deleteClassProperty.versionSummary", propertyName);

        // Make sure the user is allowed to make this modification
        context.getWiki().checkSavingDocument(context.getUserReference(), doc, comment, true, context);

        xwiki.saveDocument(doc, comment, true, context);
    }
}

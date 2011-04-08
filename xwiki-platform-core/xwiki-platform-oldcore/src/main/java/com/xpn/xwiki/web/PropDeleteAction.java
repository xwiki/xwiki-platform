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
 *
 */
package com.xpn.xwiki.web;

import java.util.Collections;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Action for removing a property definition from the current class. The property to remove is specified in the {@code
 * propname} request parameter, and the class is the one defined in the requested document.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public class PropDeleteAction extends AbstractPropChangeAction
{
    /**
     * {@inheritDoc}
     */
    @Override
    public void changePropertyDefinition(BaseClass xclass, String propertyName, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiDocument doc = context.getDoc();

        xclass.removeField(propertyName);
        xwiki.saveDocument(doc, context.getMessageTool().get("core.model.xclass.deleteClassProperty.versionSummary",
            Collections.singletonList(propertyName)), true, context);
    }
}

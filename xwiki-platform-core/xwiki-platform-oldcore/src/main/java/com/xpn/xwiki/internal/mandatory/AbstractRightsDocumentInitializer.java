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
package com.xpn.xwiki.internal.mandatory;

import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * XWiki.XWikiPreferences class.
 *
 * @version $Id$
 * @since 4.3M1
 */
public abstract class AbstractRightsDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * @param pageName the document name of the rights class
     */
    public AbstractRightsDocumentInitializer(String pageName)
    {
        super(new LocalDocumentReference(XWiki.SYSTEM_SPACE, pageName));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addGroupsField("groups", "Groups");
        xclass.addLevelsField("levels", "Levels");
        xclass.addUsersField("users", "Users");
        xclass.addBooleanField("allow", "Allow/Deny", "allow", Boolean.TRUE);
    }
}

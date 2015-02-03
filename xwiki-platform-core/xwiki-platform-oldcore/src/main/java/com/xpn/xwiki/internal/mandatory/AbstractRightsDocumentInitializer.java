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

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.objects.classes.LevelsClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.UsersClass;

/**
 * XWiki.XWikiPreferences class.
 *
 * @version $Id$
 * @since 4.3M1
 */
public abstract class AbstractRightsDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * @param pageName the document name of the rights class
     */
    public AbstractRightsDocumentInitializer(String pageName)
    {
        super(new EntityReference(pageName, EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE)));
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        BaseClass bclass = document.getXClass();

        PropertyInterface groupsProp = bclass.get("groups");
        if ((groupsProp != null) && !(groupsProp instanceof GroupsClass)) {
            bclass.removeField("groups");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addGroupsField("groups", "Groups");

        PropertyInterface levelsProp = bclass.get("levels");
        if ((levelsProp != null) && !(levelsProp instanceof LevelsClass)) {
            bclass.removeField("levels");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addLevelsField("levels", "Levels");

        PropertyInterface usersProp = bclass.get("users");
        if ((usersProp != null) && !(usersProp instanceof UsersClass)) {
            bclass.removeField("users");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addUsersField("users", "Users");

        PropertyInterface allowProp = bclass.get("allow");
        if ((allowProp != null) && (allowProp instanceof NumberClass)) {
            bclass.removeField("allow");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addBooleanField("allow", "Allow/Deny", "allow");
        BooleanClass afield = (BooleanClass) bclass.get("allow");
        if (afield.getDefaultValue() != 1) {
            afield.setDefaultValue(1);
            needsUpdate = true;
        }

        return needsUpdate;
    }
}

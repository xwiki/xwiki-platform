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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update XWiki.SheetClass document with all required informations.
 *
 * @version $Id$
 * @deprecated since 3.1M2 edit mode class should be used for this purpose, not the sheet class
 * @since 4.3M1
 */
@Component
@Named("XWiki.SheetClass")
@Singleton
@Deprecated
public class SheetClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Default constructor.
     */
    public SheetClassDocumentInitializer()
    {
        super(new LocalDocumentReference(XWiki.SYSTEM_SPACE, "SheetClass"));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        // Note: Ideally we don't want a special field in the sheet class but XWiki classes must have at
        // least one field or they're not saved. Thus we are introducing a "defaultEditMode" which will
        // tell what edit mode to use. If empty it'll default to "inline".
        xclass.addTextField("defaultEditMode", "Default Edit Mode", 15);
    }
}

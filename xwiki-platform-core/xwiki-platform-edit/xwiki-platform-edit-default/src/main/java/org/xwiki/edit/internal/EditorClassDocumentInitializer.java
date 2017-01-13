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
package org.xwiki.edit.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass.EditorType;

/**
 * Update the {@code XWiki.EditorClass} document with all the required information.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Named("XWiki.EditorClass")
@Singleton
public class EditorClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The local reference of the editor class.
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "EditorClass");

    /**
     * Default constructor.
     */
    public EditorClassDocumentInitializer()
    {
        super(REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField("dataType", "Data Type", 30);
        xclass.addTextField("roleHint", "Role Hint", 30);
        xclass.addTextAreaField("code", "Code", 40, 20, EditorType.TEXT);
        xclass.addTextField("icon", "Icon", 30);
        xclass.addTextField("category", "Category", 30);
        xclass.addStaticListField("scope", "Scope", "wiki=Current Wiki|user=Current User|global=Global");
    }
}

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
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update XWiki.XWikiSkinFileOverrideClass document with all required properties. This XClass is used to override a skin
 * file by attaching an instance of it to the skin document.
 * 
 * @version $Id$
 * @since 7.0RC1
 */
@Component
@Named("XWiki.XWikiSkinFileOverrideClass")
@Singleton
public class XWikiSkinFileOverrideClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The reference to the mandatory document.
     */
    public static final EntityReference DOCUMENT_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "XWikiSkinFileOverrideClass");

    /**
     * The name of the property storing the path of the file to overwrite.
     */
    public static final String PROPERTY_PATH = "path";

    /**
     * The name of the property holding the content.
     */
    public static final String PROPERTY_CONTENT = "content";

    /**
     * Default constructor.
     */
    public XWikiSkinFileOverrideClassDocumentInitializer()
    {
        super(DOCUMENT_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(PROPERTY_PATH, "Path", 255);
        xclass.addTemplateField(PROPERTY_CONTENT, "Content");
    }
}

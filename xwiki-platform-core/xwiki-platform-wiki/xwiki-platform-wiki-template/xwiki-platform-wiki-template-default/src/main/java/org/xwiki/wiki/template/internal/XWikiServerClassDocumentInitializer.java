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
package org.xwiki.wiki.template.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update the XWiki.XWikiServerClass document with all required information.
 *
 * @since 5.3M1
 */
@Component
@Named("XWiki.XWikiServerClassTemplate")
@Singleton
public class XWikiServerClassDocumentInitializer extends
        org.xwiki.wiki.descriptor.internal.XWikiServerClassDocumentInitializer
{
    /**
     * Name of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_ISWIKITEMPLATE = "iswikitemplate";

    /**
     * Pretty name of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_ISWIKITEMPLATE = "Template";

    /**
     * Display type of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDDT_ISWIKITEMPLATE = FIELDDT_SECURE;

    /**
     * Default value of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final Boolean DEFAULT_ISWIKITEMPLATE = Boolean.FALSE;

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        // Add missing class fields
        BaseClass baseClass = document.getXClass();

        needsUpdate |= baseClass.addBooleanField(FIELD_ISWIKITEMPLATE, FIELDPN_ISWIKITEMPLATE, FIELDDT_ISWIKITEMPLATE);
        needsUpdate |= updateBooleanClassDefaultValue(baseClass, FIELD_ISWIKITEMPLATE, DEFAULT_ISWIKITEMPLATE);

        return needsUpdate;
    }
}

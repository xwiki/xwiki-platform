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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Update XWiki.XWikiUsers document with all required informations.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("XWiki.XWikiUsers")
@Singleton
public class XWikiUsersDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the field containing the user email.
     */
    private static final String EMAIL_FIELD = "email";

    /**
     * The name of the field containing the time zone.
     */
    private static final String TIMEZONE_FIELD = "timezone";

    /**
     * Used to bind a class to a document sheet.
     */
    @Inject
    @Named("class")
    private SheetBinder classSheetBinder;

    /**
     * Default constructor.
     */
    public XWikiUsersDocumentInitializer()
    {
        super(new LocalDocumentReference(XWiki.SYSTEM_SPACE, "XWikiUsers"));
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField("first_name", "First Name", 30);
        xclass.addTextField("last_name", "Last Name", 30);
        xclass.addEmailField(EMAIL_FIELD, "e-Mail", 30);
        xclass.addPasswordField("password", "Password", 10);
        xclass.addPasswordField("validkey", "Validation Key", 10);
        xclass.addBooleanField("active", "Active", "active");
        xclass.addTextField("default_language", "Default Language", 30);
        xclass.addTextField("company", "Company", 30);
        xclass.addTextField("blog", "Blog", 60);
        xclass.addTextField("blogfeed", "Blog Feed", 60);
        xclass.addTextAreaField("comment", "Comment", 40, 5);
        xclass.addStaticListField("imtype", "IM Type", "---|AIM|Yahoo|Jabber|MSN|Skype|ICQ");
        xclass.addTextField("imaccount", "imaccount", 30);
        xclass.addStaticListField("editor", "Default Editor", "---|Text|Wysiwyg");
        xclass.addStaticListField("usertype", "User type", "Simple|Advanced");
        xclass.addBooleanField("accessibility", "Enable extra accessibility features", "yesno");
        xclass.addBooleanField("displayHiddenDocuments", "Display Hidden Documents", "yesno");
        xclass.addTimezoneField(TIMEZONE_FIELD, "Time Zone", 30);

        // New fields for the XWiki 1.0 skin
        xclass.addPageField("skin", "skin", 30);
        xclass.addTextField("avatar", "Avatar", 30);
        xclass.addTextField("phone", "Phone", 30);
        xclass.addTextAreaField("address", "Address", 40, 3);

        xclass.addBooleanField("extensionConflictSetup", "Enable extension conflict setup", "yesno");
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = super.updateDocument(document);

        // Use XWikiUserSheet to display documents having XWikiUsers objects if no other class sheet is specified.
        if (this.classSheetBinder.getSheets(document).isEmpty()) {
            String wikiName = document.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, XWiki.SYSTEM_SPACE, "XWikiUserSheet");
            needsUpdate |= this.classSheetBinder.bind(document, sheet);
        }

        return needsUpdate;
    }
}

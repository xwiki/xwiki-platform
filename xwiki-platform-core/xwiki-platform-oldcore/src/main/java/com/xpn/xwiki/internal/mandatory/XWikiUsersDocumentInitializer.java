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
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.EmailClass;
import com.xpn.xwiki.objects.classes.TimezoneClass;

/**
 * Update XWiki.XWikiUsers document with all required informations.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("XWiki.XWikiUsers")
@Singleton
public class XWikiUsersDocumentInitializer extends AbstractMandatoryDocumentInitializer
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
        super(XWiki.SYSTEM_SPACE, "XWikiUsers");
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        BaseClass bclass = document.getXClass();

        // Force the class document to use the 2.1 syntax default syntax, the same syntax used in the custom displayer.
        if (!Syntax.XWIKI_2_1.equals(document.getSyntax())) {
            document.setSyntax(Syntax.XWIKI_2_1);
            needsUpdate = true;
        }

        needsUpdate |= bclass.addTextField("first_name", "First Name", 30);
        needsUpdate |= bclass.addTextField("last_name", "Last Name", 30);

        // Email. Handle upgrade since in the past we were using a standard text field with a custom displayer and
        // we're now using an Email Class. Thus we need to remove any existing field to upgrade it.
        if (!(bclass.getField(EMAIL_FIELD) instanceof EmailClass)) {
            bclass.removeField(EMAIL_FIELD);
            bclass.addEmailField(EMAIL_FIELD, "e-Mail", 30);
            needsUpdate = true;
        }
        needsUpdate |= bclass.addPasswordField("password", "Password", 10);
        needsUpdate |= bclass.addPasswordField("validkey", "Validation Key", 10);
        needsUpdate |= bclass.addBooleanField("active", "Active", "active");
        needsUpdate |= bclass.addTextField("default_language", "Default Language", 30);
        needsUpdate |= bclass.addTextField("company", "Company", 30);
        needsUpdate |= bclass.addTextField("blog", "Blog", 60);
        needsUpdate |= bclass.addTextField("blogfeed", "Blog Feed", 60);
        needsUpdate |= bclass.addTextAreaField("comment", "Comment", 40, 5);
        needsUpdate |= bclass.addStaticListField("imtype", "IM Type", "---|AIM|Yahoo|Jabber|MSN|Skype|ICQ");
        needsUpdate |= bclass.addTextField("imaccount", "imaccount", 30);
        needsUpdate |= bclass.addStaticListField("editor", "Default Editor", "---|Text|Wysiwyg");
        needsUpdate |= bclass.addStaticListField("usertype", "User type", "Simple|Advanced");
        needsUpdate |= bclass.addBooleanField("accessibility", "Enable extra accessibility features", "yesno");
        needsUpdate |= bclass.addBooleanField("displayHiddenDocuments", "Display Hidden Documents", "yesno");

        // Timezone. Handle upgrade since in the past we were using a standard text field with a custom displayer and
        // we're now using a Timezone Class. Thus we need to remove any existing field to upgrade it.
        if (!(bclass.getField(TIMEZONE_FIELD) instanceof TimezoneClass)) {
            bclass.removeField(TIMEZONE_FIELD);
            bclass.addTimezoneField(TIMEZONE_FIELD, "Time Zone", 30);
            needsUpdate = true;
        }

        // New fields for the XWiki 1.0 skin
        needsUpdate |= bclass.addTextField("skin", "skin", 30);
        needsUpdate |= bclass.addTextField("avatar", "Avatar", 30);
        needsUpdate |= bclass.addTextField("phone", "Phone", 30);
        needsUpdate |= bclass.addTextAreaField("address", "Address", 40, 3);
        needsUpdate |= setClassDocumentFields(document, "XWiki User Class");

        // Use XWikiUserSheet to display documents having XWikiUsers objects if no other class sheet is specified.
        if (this.classSheetBinder.getSheets(document).isEmpty()) {
            String wikiName = document.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, XWiki.SYSTEM_SPACE, "XWikiUserSheet");
            needsUpdate |= this.classSheetBinder.bind(document, sheet);
        }

        return needsUpdate;
    }
}

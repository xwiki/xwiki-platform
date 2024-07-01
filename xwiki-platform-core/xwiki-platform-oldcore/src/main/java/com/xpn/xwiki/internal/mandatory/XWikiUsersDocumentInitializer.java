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
     * Local reference of the XWikiUsers class document.
     */
    public static final LocalDocumentReference XWIKI_USERS_DOCUMENT_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "XWikiUsers");

    /**
     * The local reference of the XWikiUsers class document as String.
     * 
     * @since 12.7RC1
     * @since 12.6.1
     */
    public static final String CLASS_REFERENCE_STRING =
        XWiki.SYSTEM_SPACE + '.' + XWIKI_USERS_DOCUMENT_REFERENCE.getName();

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
        super(XWIKI_USERS_DOCUMENT_REFERENCE);
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
        xclass.addTextField("company", "Company", 30);
        xclass.addTextField("blog", "Blog", 60);
        xclass.addTextField("blogfeed", "Blog Feed", 60);
        xclass.addTextAreaField("comment", "Comment", 40, 5);
        xclass.addStaticListField("imtype", "IM Type", "AIM|Yahoo|Jabber|MSN|Skype|ICQ");
        xclass.addTextField("imaccount", "imaccount", 30);
        xclass.addStaticListField("editor", "Default Editor", "Text|Wysiwyg");
        xclass.addStaticListField("usertype", "User type", "Simple|Advanced", "Simple");
        xclass.addStaticListField("underline", "Underline links", "OnlyInlineLinks|Yes|No", "OnlyInlineLinks");
        xclass.addBooleanField("displayHiddenDocuments", "Display Hidden Documents", "yesno");
        xclass.addTimezoneField(TIMEZONE_FIELD, "Time Zone", 30);

        // New fields for the XWiki 1.0 skin
        xclass.addPageField("skin", "skin", 30);
        xclass.addTextField("avatar", "Avatar", 30);
        xclass.addTextField("phone", "Phone", 30);
        xclass.addTextAreaField("address", "Address", 40, 3);

        xclass.addBooleanField("extensionConflictSetup", "Enable extension conflict setup", "yesno");
        xclass.addBooleanField("email_checked", "Email address verified");

        // Shortcut preferences (22) added in 16.2.0-rc1
        // Edit modes (7)
        xclass.addTextField("shortcut_view_edit", "Shortcut for default edit mode", 30);
        xclass.addTextField("shortcut_view_wiki", "Shortcut for wiki edit mode", 30);
        xclass.addTextField("shortcut_view_wysiwyg", "Shortcut for wysiwyg edit mode", 30);
        xclass.addTextField("shortcut_view_inline", "Shortcut for inline edit mode", 30);
        xclass.addTextField("shortcut_view_rights", "Shortcut for rights edit mode", 30);
        xclass.addTextField("shortcut_view_objects", "Shortcut for object edit mode", 30);
        xclass.addTextField("shortcut_view_class", "Shortcut for class edit mode", 30);
        // Extra info views (5)
        xclass.addTextField("shortcut_view_comments", "Shortcut to view page comments", 30);
        xclass.addTextField("shortcut_view_annotations", "Shortcut to view page annotations", 30);
        xclass.addTextField("shortcut_view_attachments", "Shortcut to view page attachments", 30);
        xclass.addTextField("shortcut_view_history", "Shortcut to view page history", 30);
        xclass.addTextField("shortcut_view_information", "Shortcut to view page information", 30);
        // Alternative views (1)
        xclass.addTextField("shortcut_view_code", "Shortcut to view page code", 30);
        // Page actions (2)
        xclass.addTextField("shortcut_view_delete", "Shortcut to delete the page", 30);
        xclass.addTextField("shortcut_view_rename", "Shortcut to rename the page", 30);
        // Editor actions (5)
        xclass.addTextField("shortcut_edit_preview", "Shortcut to see preview from the editor", 30);
        xclass.addTextField("shortcut_edit_backtoedit", "Shortcut to go back to the editor from preview", 30);
        xclass.addTextField("shortcut_edit_cancel", "Shortcut to cancel edition", 30);
        xclass.addTextField("shortcut_edit_save", "Shortcut to save and continue in the editor", 30);
        xclass.addTextField("shortcut_edit_saveandview", "Shortcut to save and view from the editor", 30);
        // Developer shortcuts (2)
        xclass.addTextField("shortcut_developer_usertype", "Shortcut to switch the current user type", 30);
        xclass.addTextField("shortcut_developer_display_hidden_docs", "Shortcut to switch display of hidden documents", 30);
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

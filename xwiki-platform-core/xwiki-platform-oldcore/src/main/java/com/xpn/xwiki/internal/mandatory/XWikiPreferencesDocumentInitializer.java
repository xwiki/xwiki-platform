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
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObjectReference;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass.ContentType;
import com.xpn.xwiki.objects.meta.PasswordMetaClass;

/**
 * Update XWiki.XWikiPreferences document with all required informations.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named(XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE_STRING)
@Singleton
public class XWikiPreferencesDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the initialized document.
     *
     * @since 9.4RC1
     */
    public static final String NAME = "XWikiPreferences";

    /**
     * The local reference of the initialized document as String.
     *
     * @since 9.4RC1
     */
    public static final String LOCAL_REFERENCE_STRING = XWiki.SYSTEM_SPACE + '.' + NAME;

    /**
     * The local reference of the initialized document as String.
     *
     * @since 9.4RC1
     */
    public static final LocalDocumentReference LOCAL_REFERENCE = new LocalDocumentReference(XWiki.SYSTEM_SPACE, NAME);

    /**
     * A regex to match any object reference with initialized class.
     *
     * @since 9.4RC1
     */
    public static final RegexEntityReference OBJECT_REFERENCE = BaseObjectReference.any(LOCAL_REFERENCE_STRING);

    /**
     * The name of the field containing the time zone.
     */
    private static final String TIMEZONE_FIELD = "timezone";

    private static final LocalDocumentReference SHEET_REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "AdminSheet");

    /**
     * Used to bind a class to a document sheet.
     */
    @Inject
    @Named("class")
    protected SheetBinder classSheetBinder;

    /**
     * Default constructor.
     */
    public XWikiPreferencesDocumentInitializer()
    {
        super(LOCAL_REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.setCustomMapping("internal");

        xclass.addTextField("parent", "Parent Space", 30);
        xclass.addBooleanField("multilingual", "Multi-Lingual", "yesno");
        xclass.addTextField("default_language", "Default Language", 5);
        xclass.addBooleanField("authenticate_edit", "Authenticated Edit", "yesno");
        xclass.addBooleanField("authenticate_view", "Authenticated View", "yesno");
        xclass.addBooleanField("auth_active_check", "Authentication Active Check", "yesno");

        xclass.addPageField("skin", "Skin", 30);
        xclass.addDBListField("colorTheme", "Color theme",
            "select doc.fullName, doc.title from XWikiDocument as doc, BaseObject as theme "
                + "where doc.fullName=theme.name and (theme.className='ColorThemes.ColorThemeClass' "
                + "or theme.className='FlamingoThemesCode.ThemeClass') "
                + "and doc.fullName<>'ColorThemes.ColorThemeTemplate' "
                + "and doc.fullName<>'FlamingoThemesCode.ThemeTemplate'");
        xclass.addDBListField("iconTheme", "Icon theme",
            "select doc.fullName, propName.value from XWikiDocument as doc, BaseObject as theme, "
                + "StringProperty propName "
                + "where doc.fullName=theme.name and theme.className='IconThemesCode.IconThemeClass' "
                + "and doc.fullName<>'IconThemesCode.IconThemeTemplate' "
                + "and theme.id = propName.id and propName.name = 'name'");
        xclass.addTextField("stylesheet", "Default Stylesheet", 30);
        xclass.addTextField("stylesheets", "Alternative Stylesheet", 60);
        xclass.addBooleanField("accessibility", "Enable extra accessibility features", "yesno");

        xclass.addStaticListField("editor", "Default Editor", "Text|Wysiwyg");

        xclass.addTextField("webcopyright", "Copyright", 30);
        xclass.addTextField("title", "Title", 30);
        xclass.addTextField("version", "Version", 30);
        xclass.addTextAreaField("meta", "HTTP Meta Info", 60, 8, ContentType.PURE_TEXT);
        xclass.addTextField("dateformat", "Date Format", 30);

        // mail
        xclass.addBooleanField("use_email_verification", "Use eMail Verification", "yesno");
        xclass.addTextField("admin_email", "Admin eMail", 30);
        xclass.addTextField("smtp_server", "SMTP Server", 30);
        xclass.addTextField("smtp_port", "SMTP Port", 5);
        xclass.addTextField("smtp_server_username", "Server username (optional)", 30);
        xclass.addTextField("smtp_server_password", "Server password (optional)", 30);
        xclass.addTextAreaField("javamail_extra_props", "Additional JavaMail properties", 60, 6, ContentType.PURE_TEXT);
        xclass.addTextAreaField("validation_email_content", "Validation eMail Content", 72, 10, ContentType.PURE_TEXT);
        xclass.addTextAreaField("confirmation_email_content", "Confirmation eMail Content", 72, 10,
            ContentType.PURE_TEXT);
        xclass.addTextAreaField("invitation_email_content", "Invitation eMail Content", 72, 10, ContentType.PURE_TEXT);
        xclass.addBooleanField("obfuscateEmailAddresses", "Obfuscate Email Addresses", "yesno");

        xclass.addStaticListField("registration_anonymous", "Anonymous", "Image|Text");
        xclass.addStaticListField("registration_registered", "Registered", "Image|Text");
        xclass.addStaticListField("edit_anonymous", "Anonymous", "Image|Text");
        xclass.addStaticListField("edit_registered", "Registered", "Image|Text");
        xclass.addStaticListField("comment_anonymous", "Anonymous", "Image|Text");
        xclass.addStaticListField("comment_registered", "Registered", "Image|Text");

        xclass.addNumberField("upload_maxsize", "Maximum Upload Size", 5, "long");

        // Captcha for guest comments
        xclass.addBooleanField("guest_comment_requires_captcha", "Enable CAPTCHA in comments for unregistered users",
            "select");

        // Document editing
        xclass.addTextField("core.defaultDocumentSyntax", "Default document syntax", 60);
        xclass.addBooleanField("xwiki.title.mandatory", "Make document title field mandatory", "yesno");

        // for tags
        xclass.addBooleanField("tags", "Activate the tagging", "yesno");

        // for backlinks
        xclass.addBooleanField("backlinks", "Activate the backlinks", "yesno");

        // New fields for the XWiki 1.0 skin
        xclass.addTextField("leftPanels", "Panels displayed on the left", 60);
        xclass.addTextField("rightPanels", "Panels displayed on the right", 60);
        xclass.addBooleanField("showLeftPanels", "Display the left panel column", "yesno");
        xclass.addBooleanField("showRightPanels", "Display the right panel column", "yesno");
        xclass.addStaticListField("leftPanelsWidth", "Width of the left panel column", "Small|Medium|Large");
        xclass.addStaticListField("rightPanelsWidth", "Width of the right panel column", "Small|Medium|Large");
        xclass.addTextField("languages", "Supported languages", 30);
        xclass.addPageField("documentBundles", "Internationalization Document Bundles", 60);
        xclass.addTimezoneField(TIMEZONE_FIELD, "Time Zone", 30);

        // Only used by LDAP authentication service

        xclass.addBooleanField("ldap", "Ldap", "yesno");
        xclass.addTextField("ldap_server", "Ldap server adress", 60);
        xclass.addTextField("ldap_port", "Ldap server port", 60);
        xclass.addTextField("ldap_bind_DN", "Ldap login matching", 60);
        xclass.addPasswordField("ldap_bind_pass", "Ldap password matching", 60, PasswordMetaClass.CLEAR);
        xclass.addBooleanField("ldap_validate_password", "Validate Ldap user/password", "yesno");
        xclass.addTextField("ldap_user_group", "Ldap group filter", 60);
        xclass.addTextField("ldap_exclude_group", "Ldap group to exclude", 60);
        xclass.addTextField("ldap_base_DN", "Ldap base DN", 60);
        xclass.addTextField("ldap_UID_attr", "Ldap UID attribute name", 60);
        xclass.addTextAreaField("ldap_fields_mapping", "Ldap user fields mapping", 60, 1, ContentType.PURE_TEXT);
        xclass.addBooleanField("ldap_update_user", "Update user from LDAP", "yesno");
        xclass.addBooleanField("ldap_update_photo", "Update user photo from LDAP", "yesno");
        xclass.addTextField("ldap_photo_attachment_name", "Attachment name to save LDAP photo", 30);
        xclass.addTextField("ldap_photo_attribute", "Ldap photo attribute name", 60);
        xclass.addTextAreaField("ldap_group_mapping", "Ldap groups mapping", 60, 5, ContentType.PURE_TEXT);
        xclass.addTextField("ldap_groupcache_expiration", "LDAP groups members cache", 60);
        xclass.addStaticListField("ldap_mode_group_sync", "LDAP groups sync mode", "always|create");
        xclass.addBooleanField("ldap_trylocal", "Try local login", "yesno");

        xclass.addBooleanField("showannotations", "Show document annotations", "yesno");
        xclass.addBooleanField("showcomments", "Show document comments", "yesno");
        xclass.addBooleanField("showattachments", "Show document attachments", "yesno");
        xclass.addBooleanField("showhistory", "Show document history", "yesno");
        xclass.addBooleanField("showinformation", "Show document information", "yesno");
        xclass.addBooleanField("editcomment", "Enable version summary", "yesno");
        xclass.addBooleanField("editcomment_mandatory", "Make version summary mandatory", "yesno");
        xclass.addBooleanField("minoredit", "Enable minor edits", "yesno");
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = super.updateDocument(document);

        // This one should not be in the prefs
        BaseClass xclass = document.getXClass();
        PropertyInterface baseskinProp = xclass.get("baseskin");
        if (baseskinProp != null) {
            xclass.removeField("baseskin");
            needsUpdate = true;
        }

        return needsUpdate;
    }

    @Override
    protected boolean updateDocumentSheet(XWikiDocument document)
    {
        // Use AdminSheet to display documents having XWikiPreferences objects.
        return this.classSheetBinder.bind(document, SHEET_REFERENCE);
    }
}

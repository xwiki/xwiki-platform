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
package com.xpn.xwiki.internal.classes;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;

import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * XWiki.XWikiPreferences class.
 * 
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("XWiki.XWikiPreferences")
@Singleton
public class XWikiPreferencesXClassProvider extends AbstractXClassProvider
{
    @Override
    protected void initialize(BaseClass bclass) throws InitializationException
    {
        bclass.setCustomMapping("internal");

        bclass.addTextField("parent", "Parent Space", 30);
        bclass.addBooleanField("multilingual", "Multi-Lingual", "yesno");
        bclass.addTextField("default_language", "Default Language", 5);
        bclass.addBooleanField("authenticate_edit", "Authenticated Edit", "yesno");
        bclass.addBooleanField("authenticate_view", "Authenticated View", "yesno");
        bclass.addBooleanField("auth_active_check", "Authentication Active Check", "yesno");

        bclass.addTextField("skin", "Skin", 30);

        bclass.addDBListField("colorTheme", "Color theme",
            "select doc.fullName, doc.title from XWikiDocument as doc, BaseObject as theme "
                + "where doc.fullName=theme.name and theme.className='ColorThemes.ColorThemeClass' "
                + "and doc.fullName<>'ColorThemes.ColorThemeTemplate'");

        // This one should not be in the prefs
        PropertyInterface baseskinProp = bclass.get("baseskin");
        if (baseskinProp != null) {
            bclass.removeField("baseskin");
        }

        bclass.addTextField("stylesheet", "Default Stylesheet", 30);
        bclass.addTextField("stylesheets", "Alternative Stylesheet", 60);
        bclass.addBooleanField("accessibility", "Enable extra accessibility features", "yesno");

        bclass.addStaticListField("editor", "Default Editor", "---|Text|Wysiwyg");

        bclass.addTextField("webcopyright", "Copyright", 30);
        bclass.addTextField("title", "Title", 30);
        bclass.addTextField("version", "Version", 30);
        bclass.addTextAreaField("meta", "HTTP Meta Info", 60, 8);
        bclass.addTextField("dateformat", "Date Format", 30);

        // mail
        bclass.addBooleanField("use_email_verification", "Use eMail Verification", "yesno");
        bclass.addTextField("admin_email", "Admin eMail", 30);
        bclass.addTextField("smtp_server", "SMTP Server", 30);
        bclass.addTextField("smtp_port", "SMTP Port", 5);
        bclass.addTextField("smtp_server_username", "Server username (optional)", 30);
        bclass.addTextField("smtp_server_password", "Server password (optional)", 30);
        bclass.addTextAreaField("javamail_extra_props", "Additional JavaMail properties", 60, 6);
        bclass.addTextAreaField("validation_email_content", "Validation eMail Content", 72, 10);
        bclass.addTextAreaField("confirmation_email_content", "Confirmation eMail Content", 72, 10);
        bclass.addTextAreaField("invitation_email_content", "Invitation eMail Content", 72, 10);

        bclass.addStaticListField("registration_anonymous", "Anonymous", "---|Image|Text");
        bclass.addStaticListField("registration_registered", "Registered", "---|Image|Text");
        bclass.addStaticListField("edit_anonymous", "Anonymous", "---|Image|Text");
        bclass.addStaticListField("edit_registered", "Registered", "---|Image|Text");
        bclass.addStaticListField("comment_anonymous", "Anonymous", "---|Image|Text");
        bclass.addStaticListField("comment_registered", "Registered", "---|Image|Text");

        bclass.addNumberField("upload_maxsize", "Maximum Upload Size", 5, "long");

        // Captcha for guest comments

        bclass.addBooleanField("guest_comment_requires_captcha", "Enable CAPTCHA in Comments for Unregistered Users",
            "select");

        // Document editing
        bclass.addTextField("core.defaultDocumentSyntax", "Default document syntax", 60);
        bclass.addBooleanField("xwiki.title.mandatory", "Make document title field mandatory", "yesno");

        // for tags
        bclass.addBooleanField("tags", "Activate the tagging", "yesno");

        // for backlinks
        bclass.addBooleanField("backlinks", "Activate the backlinks", "yesno");

        // New fields for the XWiki 1.0 skin
        bclass.addTextField("leftPanels", "Panels displayed on the left", 60);
        bclass.addTextField("rightPanels", "Panels displayed on the right", 60);
        bclass.addBooleanField("showLeftPanels", "Display the left panel column", "yesno");
        bclass.addBooleanField("showRightPanels", "Display the right panel column", "yesno");
        bclass.addTextField("languages", "Supported languages", 30);
        bclass.addTextField("documentBundles", "Internationalization Document Bundles", 60);

        // Only used by LDAP authentication service

        bclass.addBooleanField("ldap", "Ldap", "yesno");
        bclass.addTextField("ldap_server", "Ldap server adress", 60);
        bclass.addTextField("ldap_port", "Ldap server port", 60);
        bclass.addTextField("ldap_bind_DN", "Ldap login matching", 60);
        bclass.addTextField("ldap_bind_pass", "Ldap password matching", 60);
        bclass.addBooleanField("ldap_validate_password", "Validate Ldap user/password", "yesno");
        bclass.addTextField("ldap_user_group", "Ldap group filter", 60);
        bclass.addTextField("ldap_exclude_group", "Ldap group to exclude", 60);
        bclass.addTextField("ldap_base_DN", "Ldap base DN", 60);
        bclass.addTextField("ldap_UID_attr", "Ldap UID attribute name", 60);
        bclass.addTextField("ldap_fields_mapping", "Ldap user fiels mapping", 60);
        bclass.addBooleanField("ldap_update_user", "Update user from LDAP", "yesno");
        bclass.addTextAreaField("ldap_group_mapping", "Ldap groups mapping", 60, 5);
        bclass.addTextField("ldap_groupcache_expiration", "LDAP groups members cache", 60);
        bclass.addStaticListField("ldap_mode_group_sync", "LDAP groups sync mode", "|always|create");
        bclass.addBooleanField("ldap_trylocal", "Try local login", "yesno");

        bclass.addBooleanField("showannotations", "Show document annotations", "yesno");
        bclass.addBooleanField("showcomments", "Show document comments", "yesno");
        bclass.addBooleanField("showattachments", "Show document attachments", "yesno");
        bclass.addBooleanField("showhistory", "Show document history", "yesno");
        bclass.addBooleanField("showinformation", "Show document information", "yesno");
        bclass.addBooleanField("editcomment", "Enable version summary", "yesno");
        bclass.addBooleanField("editcomment_mandatory", "Make version summary mandatory", "yesno");
        bclass.addBooleanField("minoredit", "Enable minor edits", "yesno");
    }
}

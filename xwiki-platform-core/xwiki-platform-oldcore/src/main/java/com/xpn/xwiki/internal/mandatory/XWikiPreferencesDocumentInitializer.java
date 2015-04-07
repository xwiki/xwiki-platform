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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Update XWiki.XWikiPreferences document with all required informations.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Component
@Named("XWiki.XWikiPreferences")
@Singleton
public class XWikiPreferencesDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * Used to bind a class to a document sheet.
     */
    @Inject
    @Named("class")
    protected SheetBinder classSheetBinder;

    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    /**
     * Default constructor.
     */
    public XWikiPreferencesDocumentInitializer()
    {
        super(XWiki.SYSTEM_SPACE, "XWikiPreferences");
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        BaseClass bclass = document.getXClass();

        if (!"internal".equals(bclass.getCustomMapping())) {
            needsUpdate = true;
            bclass.setCustomMapping("internal");
        }

        XWikiContext xcontext = this.xcontextProvider.get();

        needsUpdate |= bclass.addTextField("parent", "Parent Space", 30);
        needsUpdate |= bclass.addBooleanField("multilingual", "Multi-Lingual", "yesno");
        needsUpdate |= bclass.addTextField("default_language", "Default Language", 5);
        needsUpdate |= bclass.addBooleanField("authenticate_edit", "Authenticated Edit", "yesno");
        needsUpdate |= bclass.addBooleanField("authenticate_view", "Authenticated View", "yesno");
        needsUpdate |= bclass.addBooleanField("auth_active_check", "Authentication Active Check", "yesno");

        needsUpdate |= bclass.addTextField("skin", "Skin", 30);
        needsUpdate |=
            bclass.addDBListField("colorTheme", "Color theme",
                "select doc.fullName, doc.title from XWikiDocument as doc, BaseObject as theme "
                    + "where doc.fullName=theme.name and (theme.className='ColorThemes.ColorThemeClass' "
                    + "or theme.className='FlamingoThemesCode.ThemeClass') "
                    + "and doc.fullName<>'ColorThemes.ColorThemeTemplate' "
                    + "and doc.fullName<>'FlamingoThemesCode.ThemeTemplate'");
        // This one should not be in the prefs
        PropertyInterface baseskinProp = bclass.get("baseskin");
        if (baseskinProp != null) {
            bclass.removeField("baseskin");
            needsUpdate = true;
        }
        needsUpdate |=
            bclass.addDBListField("iconTheme", "Icon theme",
                "select doc.fullName, propName.value from XWikiDocument as doc, BaseObject as theme, "
                    + "StringProperty propName "
                    + "where doc.fullName=theme.name and theme.className='IconThemesCode.IconThemeClass' "
                    + "and doc.fullName<>'IconThemesCode.IconThemeTemplate' "
                    + "and theme.id = propName.id and propName.name = 'name'");
        needsUpdate |= bclass.addTextField("stylesheet", "Default Stylesheet", 30);
        needsUpdate |= bclass.addTextField("stylesheets", "Alternative Stylesheet", 60);
        needsUpdate |= bclass.addBooleanField("accessibility", "Enable extra accessibility features", "yesno");

        needsUpdate |= bclass.addStaticListField("editor", "Default Editor", "---|Text|Wysiwyg");

        needsUpdate |= bclass.addTextField("webcopyright", "Copyright", 30);
        needsUpdate |= bclass.addTextField("title", "Title", 30);
        needsUpdate |= bclass.addTextField("version", "Version", 30);
        needsUpdate |= bclass.addTextAreaField("meta", "HTTP Meta Info", 60, 8);
        needsUpdate |= bclass.addTextField("dateformat", "Date Format", 30);

        // mail
        needsUpdate |= bclass.addBooleanField("use_email_verification", "Use eMail Verification", "yesno");
        needsUpdate |= bclass.addTextField("admin_email", "Admin eMail", 30);
        needsUpdate |= bclass.addTextField("smtp_server", "SMTP Server", 30);
        needsUpdate |= bclass.addTextField("smtp_port", "SMTP Port", 5);
        needsUpdate |= bclass.addTextField("smtp_server_username", "Server username (optional)", 30);
        needsUpdate |= bclass.addTextField("smtp_server_password", "Server password (optional)", 30);
        needsUpdate |= bclass.addTextAreaField("javamail_extra_props", "Additional JavaMail properties", 60, 6);
        needsUpdate |= bclass.addTextAreaField("validation_email_content", "Validation eMail Content", 72, 10);
        needsUpdate |= bclass.addTextAreaField("confirmation_email_content", "Confirmation eMail Content", 72, 10);
        needsUpdate |= bclass.addTextAreaField("invitation_email_content", "Invitation eMail Content", 72, 10);
        needsUpdate |= bclass.addBooleanField("obfuscateEmailAddresses", "Obfuscate Email Addresses", "yesno");

        needsUpdate |= bclass.addStaticListField("registration_anonymous", "Anonymous", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("registration_registered", "Registered", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("edit_anonymous", "Anonymous", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("edit_registered", "Registered", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("comment_anonymous", "Anonymous", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("comment_registered", "Registered", "---|Image|Text");

        needsUpdate |= bclass.addNumberField("upload_maxsize", "Maximum Upload Size", 5, "long");

        // Captcha for guest comments
        needsUpdate |=
            bclass.addBooleanField("guest_comment_requires_captcha",
                "Enable CAPTCHA in Comments for Unregistered Users", "select");

        // Document editing
        needsUpdate |= bclass.addTextField("core.defaultDocumentSyntax", "Default document syntax", 60);
        needsUpdate |= bclass.addBooleanField("xwiki.title.mandatory", "Make document title field mandatory", "yesno");

        // for tags
        needsUpdate |= bclass.addBooleanField("tags", "Activate the tagging", "yesno");

        // for backlinks
        needsUpdate |= bclass.addBooleanField("backlinks", "Activate the backlinks", "yesno");

        // New fields for the XWiki 1.0 skin
        needsUpdate |= bclass.addTextField("leftPanels", "Panels displayed on the left", 60);
        needsUpdate |= bclass.addTextField("rightPanels", "Panels displayed on the right", 60);
        needsUpdate |= bclass.addBooleanField("showLeftPanels", "Display the left panel column", "yesno");
        needsUpdate |= bclass.addBooleanField("showRightPanels", "Display the right panel column", "yesno");
        needsUpdate |= bclass.addStaticListField("leftPanelsWidth", "Width of the left panel column",
            "---|Small|Medium|Large");
        needsUpdate |= bclass.addStaticListField("rightPanelsWidth", "Width of the right panel column",
            "---|Small|Medium|Large");
        needsUpdate |= bclass.addTextField("languages", "Supported languages", 30);
        needsUpdate |= bclass.addTextField("documentBundles", "Internationalization Document Bundles", 60);
        needsUpdate |= bclass.addTextField("timezone", "Timezone", 30);
        PropertyClass timezoneProperty = (PropertyClass) bclass.get("timezone");
        if (!timezoneProperty.isCustomDisplayed(xcontext)) {
            StringBuilder builder = new StringBuilder();
            builder.append("{{velocity}}\n");
            builder.append("#if ($xcontext.action == 'inline' || $xcontext.action == 'edit')\n");
            builder.append("  {{html}}\n");
            builder.append("    #if($xwiki.jodatime)\n");
            builder.append("      <select id='$prefix$name' name='$prefix$name'>\n");
            builder.append("        <option value=\"\" #if($value == $tz)selected=\"selected\"#end>"
                + "$services.localization.render('XWiki.XWikiPreferences_timezone_default')</option>\n");
            builder.append("        #foreach($tz in $xwiki.jodatime.getServerTimezone().getAvailableIDs())\n");
            builder.append("          <option value=\"$tz\" #if($value == $tz)selected=\"selected\"#end>"
                + "$tz</option>\n");
            builder.append("        #end\n");
            builder.append("      </select>\n");
            builder.append("    #else\n");
            builder.append("      <input id='$prefix$name' name='$prefix$name' type=\"text\" value=\"$!value\"/>\n");
            builder.append("    #end\n");
            builder.append("  {{/html}}\n");
            builder.append("#else\n");
            builder.append("  $!value\n");
            builder.append("#end\n");
            builder.append("{{/velocity}}\n");
            timezoneProperty.setCustomDisplay(builder.toString());
            needsUpdate = true;
        }

        // Only used by LDAP authentication service

        needsUpdate |= bclass.addBooleanField("ldap", "Ldap", "yesno");
        needsUpdate |= bclass.addTextField("ldap_server", "Ldap server adress", 60);
        needsUpdate |= bclass.addTextField("ldap_port", "Ldap server port", 60);
        needsUpdate |= bclass.addTextField("ldap_bind_DN", "Ldap login matching", 60);
        needsUpdate |= bclass.addTextField("ldap_bind_pass", "Ldap password matching", 60);
        needsUpdate |= bclass.addBooleanField("ldap_validate_password", "Validate Ldap user/password", "yesno");
        needsUpdate |= bclass.addTextField("ldap_user_group", "Ldap group filter", 60);
        needsUpdate |= bclass.addTextField("ldap_exclude_group", "Ldap group to exclude", 60);
        needsUpdate |= bclass.addTextField("ldap_base_DN", "Ldap base DN", 60);
        needsUpdate |= bclass.addTextField("ldap_UID_attr", "Ldap UID attribute name", 60);
        needsUpdate |= bclass.addTextField("ldap_fields_mapping", "Ldap user fiels mapping", 60);
        needsUpdate |= bclass.addBooleanField("ldap_update_user", "Update user from LDAP", "yesno");
        needsUpdate |= bclass.addTextAreaField("ldap_group_mapping", "Ldap groups mapping", 60, 5);
        needsUpdate |= bclass.addTextField("ldap_groupcache_expiration", "LDAP groups members cache", 60);
        needsUpdate |= bclass.addStaticListField("ldap_mode_group_sync", "LDAP groups sync mode", "|always|create");
        needsUpdate |= bclass.addBooleanField("ldap_trylocal", "Try local login", "yesno");

        if (((BooleanClass) bclass.get("showLeftPanels")).getDisplayType().equals("checkbox")) {
            ((BooleanClass) bclass.get("showLeftPanels")).setDisplayType("yesno");
            ((BooleanClass) bclass.get("showRightPanels")).setDisplayType("yesno");
            needsUpdate = true;
        }

        needsUpdate |= bclass.addBooleanField("showannotations", "Show document annotations", "yesno");
        needsUpdate |= bclass.addBooleanField("showcomments", "Show document comments", "yesno");
        needsUpdate |= bclass.addBooleanField("showattachments", "Show document attachments", "yesno");
        needsUpdate |= bclass.addBooleanField("showhistory", "Show document history", "yesno");
        needsUpdate |= bclass.addBooleanField("showinformation", "Show document information", "yesno");
        needsUpdate |= bclass.addBooleanField("editcomment", "Enable version summary", "yesno");
        needsUpdate |= bclass.addBooleanField("editcomment_mandatory", "Make version summary mandatory", "yesno");
        needsUpdate |= bclass.addBooleanField("minoredit", "Enable minor edits", "yesno");

        boolean withoutDocumentSheets = this.documentSheetBinder.getSheets(document).isEmpty();
        if (withoutDocumentSheets) {
            // Bind a document sheet to prevent the default class sheet from being used.
            this.documentSheetBinder.bind(document, document.getDocumentReference());
        }
        needsUpdate |= setClassDocumentFields(document, "XWiki Preferences");
        if (withoutDocumentSheets) {
            // Unbind the document sheet we bound earlier.
            this.documentSheetBinder.unbind(document, document.getDocumentReference());
        }

        // Use AdminSheet to display documents having XWikiPreferences objects if no other class sheet is specified.
        if (this.classSheetBinder.getSheets(document).isEmpty()) {
            String wikiName = document.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, XWiki.SYSTEM_SPACE, "AdminSheet");
            needsUpdate |= this.classSheetBinder.bind(document, sheet);
        }

        return needsUpdate;
    }
}

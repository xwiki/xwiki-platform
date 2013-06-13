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
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.sheet.SheetBinder;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.EmailClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

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
    private static final String FIEDL_EMAIL = "email";

    /**
     * Used to bind a class to a document sheet.
     */
    @Inject
    @Named("class")
    private SheetBinder classSheetBinder;

    /**
     * Used to access current XWikiContext.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

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

        XWikiContext xcontext = xcontextProvider.get();

        needsUpdate |= bclass.addTextField("first_name", "First Name", 30);
        needsUpdate |= bclass.addTextField("last_name", "Last Name", 30);
        if (!(bclass.getField(FIEDL_EMAIL) instanceof EmailClass)) {
            bclass.removeField(FIEDL_EMAIL);
            bclass.addEmailField(FIEDL_EMAIL, "e-Mail", 30);
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

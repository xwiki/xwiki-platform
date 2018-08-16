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
package org.xwiki.wiki.internal.descriptor.document;

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
 * Update the XWiki.XWikiServerClass document with all required information.
 * 
 * @version $Id$
 * @since 5.0M2
 */
@Component
@Named("XWiki.XWikiServerClass")
@Singleton
public class XWikiServerClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the mandatory document.
     */
    public static final String DOCUMENT_NAME = "XWikiServerClass";

    /**
     * Reference to the server class.
     */
    public static final LocalDocumentReference SERVER_CLASS =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, DOCUMENT_NAME);

    /**
     * Default list separators of XWiki.XWikiServerClass fields.
     */
    public static final String DEFAULT_FIELDS_SEPARATOR = "|";

    /**
     * Name of field <code>prettyname</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_WIKIPRETTYNAME = "wikiprettyname";

    /**
     * Pretty name of field <code>prettyname</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_WIKIPRETTYNAME = "Wiki pretty name";

    /**
     * Name of field <code>owner</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_OWNER = "owner";

    /**
     * Pretty name of field <code>owner</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_OWNER = "Owner";

    /**
     * Name of field <code>description</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_DESCRIPTION = "description";

    /**
     * Pretty name of field <code>description</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_DESCRIPTION = "Description";

    /**
     * Name of field <code>server</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_SERVER = "server";

    /**
     * Pretty name of field <code>server</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_SERVER = "Server";

    /**
     * Name of field <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_VISIBILITY = "visibility";

    /**
     * First possible values for <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDL_VISIBILITY_PUBLIC = "public";

    /**
     * Second possible values for <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDL_VISIBILITY_PRIVATE = "private";

    /**
     * List of possible values for <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDL_VISIBILITY =
        FIELDL_VISIBILITY_PUBLIC + DEFAULT_FIELDS_SEPARATOR + FIELDL_VISIBILITY_PRIVATE + DEFAULT_FIELDS_SEPARATOR;

    /**
     * Pretty name of field <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_VISIBILITY = "Visibility";

    /**
     * Name of field <code>state</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_STATE = "state";

    /**
     * First possible values for <code>state</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDL_STATE_ACTIVE = "active";

    /**
     * Second possible values for <code>state</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDL_STATE_INACTIVE = "inactive";

    /**
     * Third possible values for <code>state</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDL_STATE_LOCKED = "locked";

    /**
     * List of possible values for <code>state</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDL_STATE = FIELDL_STATE_ACTIVE + DEFAULT_FIELDS_SEPARATOR + FIELDL_STATE_INACTIVE
        + DEFAULT_FIELDS_SEPARATOR + FIELDL_STATE_LOCKED;

    /**
     * Pretty name of field <code>state</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_STATE = "State";

    /**
     * Name of field <code>language</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_LANGUAGE = "language";

    /**
     * List of possible values for <code>language</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDL_LANGUAGE = "en|fr";

    /**
     * Pretty name of field <code>language</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_LANGUAGE = "Language";

    /**
     * Name of field <code>secure</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_SECURE = "secure";

    /**
     * Pretty name of field <code>secure</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_SECURE = "Secure";

    /**
     * Form type of field <code>secure</code> for the XWiki class XWiki.XWikiServerClass.
     * 
     * @since 10.7RC1
     */
    public static final String FIELDFT_SECURE = "select";

    /**
     * Display type of field <code>secure</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDDT_SECURE = "";

    /**
     * Default value of field <code>secure</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final Boolean DEFAULT_SECURE = null;

    /**
     * Name of field <code>port</code> for the XWiki class XWiki.XWikiServerClass.
     * 
     * @since 10.7RC1
     */
    public static final String FIELD_PORT = "port";

    /**
     * Pretty name of field <code>port</code> for the XWiki class XWiki.XWikiServerClass.
     * 
     * @since 10.7RC1
     */
    public static final String FIELDPN_PORT = "Port";

    /**
     * Display type of field <code>port</code> for the XWiki class XWiki.XWikiServerClass.
     * 
     * @since 10.7RC1
     */
    public static final String FIELDT_PORT = "integer";

    /**
     * Name of field <code>homepage</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_HOMEPAGE = "homepage";

    /**
     * Pretty name of field <code>homepage</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_HOMEPAGE = "Home page";

    /**
     * Used to bind a class to a document sheet.
     */
    @Inject
    @Named("class")
    private SheetBinder classSheetBinder;

    /**
     * Default constructor.
     */
    public XWikiServerClassDocumentInitializer()
    {
        super(SERVER_CLASS);
    }

    @Override
    public boolean isMainWikiOnly()
    {
        // Initialize it only for the main wiki.
        return true;
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(FIELD_WIKIPRETTYNAME, FIELDPN_WIKIPRETTYNAME, 30);
        xclass.addUsersField(FIELD_OWNER, FIELDPN_OWNER, false);
        xclass.addTextAreaField(FIELD_DESCRIPTION, FIELDPN_DESCRIPTION, 40, 5);
        xclass.addTextField(FIELD_SERVER, FIELDPN_SERVER, 30);
        xclass.addStaticListField(FIELD_VISIBILITY, FIELDPN_VISIBILITY, FIELDL_VISIBILITY);
        xclass.addStaticListField(FIELD_STATE, FIELDPN_STATE, FIELDL_STATE);
        xclass.addStaticListField(FIELD_LANGUAGE, FIELDPN_LANGUAGE, FIELDL_LANGUAGE);
        xclass.addBooleanField(FIELD_SECURE, FIELDPN_SECURE, FIELDFT_SECURE, FIELDDT_SECURE, DEFAULT_SECURE);
        xclass.addNumberField(FIELD_SECURE, FIELDPN_SECURE, 4, FIELDT_PORT);
        xclass.addTextField(FIELD_HOMEPAGE, FIELDPN_HOMEPAGE, 30);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = super.updateDocument(document);

        // Use XWikiServerClassSheet to display documents having XWikiServerClass objects if no other class sheet is
        // specified.
        if (this.classSheetBinder.getSheets(document).isEmpty()) {
            String wikiName = document.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, XWiki.SYSTEM_SPACE, "XWikiServerClassSheet");
            needsUpdate |= this.classSheetBinder.bind(document, sheet);
        }

        return needsUpdate;
    }
}

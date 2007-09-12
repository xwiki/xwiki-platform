/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors.
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

package com.xpn.xwiki.plugin.multiwiki.doc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.objects.classes.AbstractSuperClass;
import com.xpn.xwiki.doc.objects.classes.ISuperDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.UsersClass;
import com.xpn.xwiki.plugin.multiwiki.WikiManagerException;

public class XWikiServerClass extends AbstractSuperClass
{
    /**
     * Space of class document.
     */
    private static final String CLASS_SPACE = "XWiki";
    /**
     * Prefix of class document.
     */
    private static final String CLASS_PREFIX = "XWikiServer";

    // ///

    /**
     * Name of field <code>owner</code>.
     */
    public static final String FIELD_owner = "owner";
    /**
     * Pretty name of field <code>owner</code>.
     */
    public static final String FIELDPN_owner = "Owner";

    /**
     * Name of field <code>description</code>.
     */
    public static final String FIELD_description = "description";
    /**
     * Pretty name of field <code>description</code>.
     */
    public static final String FIELDPN_description = "Description";

    /**
     * Name of field <code>server</code>.
     */
    public static final String FIELD_server = "server";
    /**
     * Pretty name of field <code>server</code>.
     */
    public static final String FIELDPN_server = "Server";

    /**
     * Name of field <code>visibility</code>.
     */
    public static final String FIELD_visibility = "visibility";
    public static final String FIELDL_visibility_public = "public";
    public static final String FIELDL_visibility_private = "private";
    public static final String FIELDL_visibility_template = "template";
    /**
     * List of possible values for <code>visibility</code>.
     */
    public static final String FIELDL_visibility =
        FIELDL_visibility_public + "|" + FIELDL_visibility_private + "|"
            + FIELDL_visibility_template;
    /**
     * Pretty name of field <code>visibility</code>.
     */
    public static final String FIELDPN_visibility = "Visibility";

    /**
     * Name of field <code>state</code>.
     */
    public static final String FIELD_state = "state";
    public static final String FIELDL_state_active = "active";
    public static final String FIELDL_state_inactive = "inactive";
    public static final String FIELDL_state_locked = "locked";
    /**
     * List of possible values for <code>state</code>.
     */
    public static final String FIELDL_state =
        FIELDL_state_active + "|" + FIELDL_state_inactive + "|" + FIELDL_state_locked;
    /**
     * Pretty name of field <code>state</code>.
     */
    public static final String FIELDPN_state = "State";

    /**
     * Name of field <code>language</code>.
     */
    public static final String FIELD_language = "language";
    /**
     * List of possible values for <code>language</code>.
     */
    public static final String FIELDL_language = "en|fr";
    /**
     * Pretty name of field <code>language</code>.
     */
    public static final String FIELDPN_language = "Language";

    // ///

    private static XWikiServerClass instance = null;

    /**
     * Return unique instance of XWikiServerClass and update documents for this context.
     * 
     * @param context Context.
     * @return XWikiServerClass Instance of XWikiApplicationClass.
     * @throws XWikiException
     */
    public static XWikiServerClass getInstance(XWikiContext context) throws XWikiException
    {
        synchronized (XWikiServerClass.class) {
            if (instance == null)
                instance = new XWikiServerClass();
        }

        instance.check(context);

        return instance;
    }

    /**
     * Default constructor for XWikiServerClass.
     */
    private XWikiServerClass()
    {
        super(CLASS_SPACE, CLASS_PREFIX, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.xpn.xwiki.util.AbstractSuperClass#updateBaseClass(com.xpn.xwiki.objects.classes.BaseClass)
     */
    protected boolean updateBaseClass(BaseClass baseClass)
    {
        boolean needsUpdate = super.updateBaseClass(baseClass);

        baseClass.setName(getClassFullName());

        needsUpdate |= baseClass.addUsersField(FIELD_owner, FIELDPN_owner);
        // TODO : move into addUserField with "multiselect" parameter
        UsersClass userclass = (UsersClass)baseClass.getField(FIELD_owner);
        userclass.setMultiSelect(false);
        
        needsUpdate |= baseClass.addTextAreaField(FIELD_description, FIELDPN_description, 40, 5);
        needsUpdate |= baseClass.addTextField(FIELD_server, FIELDPN_server, 30);
        needsUpdate |=
            baseClass.addStaticListField(FIELD_visibility, FIELDPN_visibility, FIELDL_visibility);
        needsUpdate |= baseClass.addStaticListField(FIELD_state, FIELDPN_state, FIELDL_state);
        needsUpdate |=
            baseClass.addStaticListField(FIELD_language, FIELDPN_language, FIELDL_language);

        return needsUpdate;
    }

    private XWikiDocument getWikiServerDocument(String wikiName, XWikiContext context,
        boolean validate) throws XWikiException
    {
        XWikiDocument doc = getItemDocument(wikiName, context);

        if (validate && doc.isNew())
            throw new WikiManagerException(WikiManagerException.ERROR_MULTIWIKI_SERVER_DOES_NOT_EXIST,
                wikiName + " wiki server does not exist");

        return doc;
    }

    private XWikiDocument getWikiTemplateServerDocument(String wikiName, XWikiContext context,
        boolean validate) throws XWikiException
    {
        XWikiDocument doc = getItemDocument(wikiName, context);

        if (validate) {
            if (doc.isNew())
                throw new WikiManagerException(WikiManagerException.ERROR_MULTIWIKI_SERVER_DOES_NOT_EXIST,
                    wikiName + " wiki server does not exist");
            if (!doc.getStringValue(FIELD_visibility).equals(FIELDL_visibility_template))
                throw new WikiManagerException(WikiManagerException.ERROR_MULTIWIKI_SERVER_DOES_NOT_EXIST,
                    wikiName + " wiki server template does not exist");
        }

        return doc;
    }

    public XWikiServer getWikiServer(String wikiName, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return (XWikiServer)newSuperDocument(getWikiServerDocument(wikiName, context, validate), context);
    }

    public XWikiServer getWikiTemplateServer(String wikiName, XWikiContext context,
        boolean validate) throws XWikiException
    {
        return (XWikiServer)newSuperDocument(getWikiTemplateServerDocument(wikiName, context, validate),
            context);
    }
    
    public ISuperDocument newSuperDocument(XWikiDocument doc, XWikiContext context)
    {
        return (ISuperDocument)doc.newDocument(XWikiServer.class.getName(), context);
    }
}

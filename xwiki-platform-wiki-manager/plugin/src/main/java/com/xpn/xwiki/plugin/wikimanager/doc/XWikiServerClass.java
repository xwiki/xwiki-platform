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

package com.xpn.xwiki.plugin.wikimanager.doc;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.AbstractSuperClass;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerException;

/**
 * {@link com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass}
 * implementation for XWiki.XWikiServerClass class.
 * 
 * @version $Id: $
 * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass
 * @see AbstractSuperClass
 */
public class XWikiServerClass extends AbstractSuperClass
{
    /**
     * Default list separators of XWiki.XWikiServerClass fields.
     */
    public static final String DEFAULT_FIELDS = "|";

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
     * First possible values for <code>visibility</code> for the XWiki class
     * XWiki.XWikiServerClass.
     */
    public static final String FIELDL_VISIBILITY_PUBLIC = "public";

    /**
     * Second possible values for <code>visibility</code> for the XWiki class
     * XWiki.XWikiServerClass.
     */
    public static final String FIELDL_VISIBILITY_PRIVATE = "private";

    /**
     * Third possible values for <code>visibility</code> for the XWiki class
     * XWiki.XWikiServerClass.
     */
    public static final String FIELDL_VISIBILITY_TEMPLATE = "template";

    /**
     * List of possible values for <code>visibility</code> for the XWiki class
     * XWiki.XWikiServerClass.
     */
    public static final String FIELDL_VISIBILITY =
        FIELDL_VISIBILITY_PUBLIC + DEFAULT_FIELDS + FIELDL_VISIBILITY_PRIVATE + DEFAULT_FIELDS
            + FIELDL_VISIBILITY_TEMPLATE;

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
    public static final String FIELDL_STATE =
        FIELDL_STATE_ACTIVE + DEFAULT_FIELDS + FIELDL_STATE_INACTIVE + DEFAULT_FIELDS
            + FIELDL_STATE_LOCKED;

    /**
     * Pretty name of field <code>state</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_STATE = "State";

    /**
     * Name of field <code>language</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_LANGUAGE = "language";

    /**
     * List of possible values for <code>language</code> for the XWiki class
     * XWiki.XWikiServerClass.
     */
    public static final String FIELDL_LANGUAGE = "en|fr";

    /**
     * Pretty name of field <code>language</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_LANGUAGE = "Language";

    /**
     * Name of field <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_SECURE = "secure";

    /**
     * Pretty name of field <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_SECURE = "Secure";

    /**
     * Display type of field <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDDT_SECURE = "checkbox";

    /**
     * Name of field <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_HOMEPAGE = "homepage";

    /**
     * Pretty name of field <code>visibility</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_HOMEPAGE = "Home page";

    // ///

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
     * Unique instance of XWikiServerClass.
     */
    private static XWikiServerClass instance;

    /**
     * Default constructor for XWikiServerClass.
     */
    protected XWikiServerClass()
    {
        super(CLASS_SPACE, CLASS_PREFIX, false);
    }

    /**
     * Return unique instance of XWikiServerClass and update documents for this context.
     * 
     * @param context Context.
     * @return XWikiServerClass Instance of XWikiApplicationClass.
     * @throws XWikiException error when checking for class, class template and class sheet.
     */
    public static XWikiServerClass getInstance(XWikiContext context) throws XWikiException
    {
        synchronized (XWikiServerClass.class) {
            if (instance == null) {
                instance = new XWikiServerClass();
            }
        }

        instance.check(context);

        return instance;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.AbstractSuperClass#updateBaseClass(com.xpn.xwiki.objects.classes.BaseClass)
     */
    protected boolean updateBaseClass(BaseClass baseClass)
    {
        boolean needsUpdate = super.updateBaseClass(baseClass);

        baseClass.setName(getClassFullName());

        needsUpdate |= baseClass.addUsersField(FIELD_OWNER, FIELDPN_OWNER, false);
        needsUpdate |= baseClass.addTextAreaField(FIELD_DESCRIPTION, FIELDPN_DESCRIPTION, 40, 5);
        needsUpdate |= baseClass.addTextField(FIELD_SERVER, FIELDPN_SERVER, 30);
        needsUpdate |=
            baseClass.addStaticListField(FIELD_VISIBILITY, FIELDPN_VISIBILITY, FIELDL_VISIBILITY);
        needsUpdate |= baseClass.addStaticListField(FIELD_STATE, FIELDPN_STATE, FIELDL_STATE);
        needsUpdate |=
            baseClass.addStaticListField(FIELD_LANGUAGE, FIELDPN_LANGUAGE, FIELDL_LANGUAGE);
        needsUpdate |= baseClass.addBooleanField(FIELD_SECURE, FIELDPN_SECURE, FIELDDT_SECURE);
        needsUpdate |= baseClass.addTextField(FIELD_HOMEPAGE, FIELDPN_HOMEPAGE, 30);

        return needsUpdate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.AbstractSuperClass#updateClassTemplateDocument(com.xpn.xwiki.doc.XWikiDocument)
     */
    protected boolean updateClassTemplateDocument(XWikiDocument doc)
    {
        boolean needsUpdate = false;

        if ("WikiManager.WebHome".equals(doc.getParent())) {
            doc.setParent(getClassSpacePrefix() + "Manager.WebHome");
            needsUpdate = true;
        }

        return needsUpdate;
    }

    /**
     * Get wiki descriptor {@link XWikiDocument}.
     * 
     * @param wikiName the name of the wiki.
     * @param context the XWiki context.
     * @param validate indicate if it return new {@link XWikiDocument} or throw exception if wiki
     *            descriptor does not exist.
     * @return the {@link XWikiDocument} representing wiki descriptor.
     * @throws XWikiException error when searching for wiki descriptor document.
     */
    private XWikiDocument getWikiServerDocument(String wikiName, XWikiContext context,
        boolean validate) throws XWikiException
    {
        XWikiDocument doc = getItemDocument(wikiName, context);

        if (validate && doc.isNew()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS,
                wikiName + " wiki descriptor document does not exist");
        }

        return doc;
    }

    /**
     * Get wiki descriptor {@link XWikiDocument} with "visibility" field to "template".
     * 
     * @param wikiName the name of the wiki.
     * @param context the XWiki context.
     * @param validate indicate if it return new {@link XWikiDocument} or throw exception if wiki
     *            descriptor does not exist.
     * @return the {@link XWikiDocument} representing wiki descriptor.
     * @throws XWikiException error when searching for wiki descriptor document.
     */
    private XWikiDocument getWikiTemplateServerDocument(String wikiName, XWikiContext context,
        boolean validate) throws XWikiException
    {
        XWikiDocument doc = getItemDocument(wikiName, context);

        if (validate) {
            if (doc.isNew()
                || !doc.getStringValue(FIELD_VISIBILITY).equals(FIELDL_VISIBILITY_TEMPLATE)) {
                throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS,
                    wikiName + " wiki template descriptor document does not exist");
            }
        }

        return doc;
    }

    /**
     * Get wiki descriptor {@link XWikiServer}.
     * 
     * @param wikiName the name of the wiki.
     * @param context the XWiki context.
     * @param validate indicate if it return new {@link XWikiServer} or throw exception if wiki
     *            descriptor does not exist.
     * @return the {@link XWikiServer} representing wiki descriptor.
     * @throws XWikiException error when searching for wiki descriptor document.
     */
    public XWikiServer getWikiServer(String wikiName, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return (XWikiServer) newSuperDocument(getWikiServerDocument(wikiName, context, validate),
            context);
    }

    /**
     * Get wiki template descriptor {@link XWikiServer}.
     * 
     * @param wikiName the name of the wiki.
     * @param context the XWiki context.
     * @param validate indicate if it return new {@link XWikiServer} or throw exception if wiki
     *            descriptor does not exist.
     * @return the {@link XWikiServer} representing wiki descriptor.
     * @throws XWikiException error when searching for wiki descriptor document.
     */
    public XWikiServer getWikiTemplateServer(String wikiName, XWikiContext context,
        boolean validate) throws XWikiException
    {
        return (XWikiServer) newSuperDocument(getWikiTemplateServerDocument(wikiName, context,
            validate), context);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.AbstractSuperClass#newSuperDocument(com.xpn.xwiki.doc.XWikiDocument, com.xpn.xwiki.XWikiContext)
     */
    public SuperDocument newSuperDocument(XWikiDocument doc, XWikiContext context)
    {
        return (SuperDocument) doc.newDocument(XWikiServer.class.getName(), context);
    }
}

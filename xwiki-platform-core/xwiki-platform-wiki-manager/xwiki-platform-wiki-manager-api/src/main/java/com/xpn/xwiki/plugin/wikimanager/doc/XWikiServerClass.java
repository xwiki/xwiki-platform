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

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.AbstractXClassManager;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XObjectDocumentDoesNotExistException;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerException;
import com.xpn.xwiki.plugin.wikimanager.WikiManagerMessageTool;

/**
 * {@link com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XClassManager} implementation for
 * XWiki.XWikiServerClass class.
 * 
 * @version $Id$
 * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XClassManager
 */
public class XWikiServerClass extends AbstractXClassManager<XWikiServer>
{
    /**
     * Default list separators of XWiki.XWikiServerClass fields.
     */
    public static final String DEFAULT_FIELDS = "|";

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
        FIELDL_VISIBILITY_PUBLIC + DEFAULT_FIELDS + FIELDL_VISIBILITY_PRIVATE + DEFAULT_FIELDS;

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
        FIELDL_STATE_ACTIVE + DEFAULT_FIELDS + FIELDL_STATE_INACTIVE + DEFAULT_FIELDS + FIELDL_STATE_LOCKED;

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
     * Display type of field <code>secure</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDDT_SECURE = "checkbox";

    /**
     * Default value of field <code>secure</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final Boolean DEFAULT_SECURE = Boolean.FALSE;

    /**
     * Name of field <code>homepage</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_HOMEPAGE = "homepage";

    /**
     * Pretty name of field <code>homepage</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_HOMEPAGE = "Home page";

    /**
     * The full name of the default home page of a newly created wiki.
     */
    public static final String DEFAULT_HOMEPAGE = "Main.WebHome";

    /**
     * Name of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELD_ISWIKITEMPLATE = "iswikitemplate";

    /**
     * Pretty name of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDPN_ISWIKITEMPLATE = "Template";

    /**
     * Display type of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final String FIELDDT_ISWIKITEMPLATE = FIELDDT_SECURE;

    /**
     * Default value of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final Boolean DEFAULT_ISWIKITEMPLATE = Boolean.FALSE;

    /**
     * Default value of field <code>iswikitemplate</code> for the XWiki class XWiki.XWikiServerClass.
     */
    public static final int FIELDDV_ISWIKITEMPLATE_INT = 0;

    /**
     * The full name of the default parent of a newly created document.
     */
    public static final String DEFAULT_PAGE_PARENT = "WikiManager.WebHome";

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

    @Override
    public boolean forceValidDocumentName()
    {
        // All wiki descriptors are of the form <code>XWiki.XWikiServer%</code>
        return true;
    }

    @Override
    protected void check(XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        try {
            context.setDatabase(context.getMainXWiki());

            super.check(context);
        } finally {
            context.setDatabase(database);
        }
    }

    @Override
    protected boolean updateBaseClass(BaseClass baseClass)
    {
        boolean needsUpdate = super.updateBaseClass(baseClass);

        baseClass.setName(getClassFullName());

        needsUpdate |= baseClass.addTextField(FIELD_WIKIPRETTYNAME, FIELDPN_WIKIPRETTYNAME, 30);
        needsUpdate |= baseClass.addUsersField(FIELD_OWNER, FIELDPN_OWNER, false);
        needsUpdate |= baseClass.addTextAreaField(FIELD_DESCRIPTION, FIELDPN_DESCRIPTION, 40, 5);
        needsUpdate |= baseClass.addTextField(FIELD_SERVER, FIELDPN_SERVER, 30);
        needsUpdate |= baseClass.addStaticListField(FIELD_VISIBILITY, FIELDPN_VISIBILITY, FIELDL_VISIBILITY);
        needsUpdate |= baseClass.addStaticListField(FIELD_STATE, FIELDPN_STATE, FIELDL_STATE);
        needsUpdate |= baseClass.addStaticListField(FIELD_LANGUAGE, FIELDPN_LANGUAGE, FIELDL_LANGUAGE);
        needsUpdate |= baseClass.addBooleanField(FIELD_SECURE, FIELDPN_SECURE, FIELDDT_SECURE);
        needsUpdate |= updateBooleanClassDefaultValue(baseClass, FIELD_SECURE, DEFAULT_SECURE);
        needsUpdate |= baseClass.addTextField(FIELD_HOMEPAGE, FIELDPN_HOMEPAGE, 30);
        needsUpdate |= baseClass.addBooleanField(FIELD_ISWIKITEMPLATE, FIELDPN_ISWIKITEMPLATE, FIELDDT_ISWIKITEMPLATE);
        needsUpdate |= updateBooleanClassDefaultValue(baseClass, FIELD_ISWIKITEMPLATE, DEFAULT_ISWIKITEMPLATE);

        return needsUpdate;
    }

    @Override
    protected boolean updateClassTemplateDocument(XWikiDocument doc)
    {
        boolean needsUpdate = false;

        if (!DEFAULT_PAGE_PARENT.equals(doc.getParent())) {
            doc.setParent(DEFAULT_PAGE_PARENT);
            needsUpdate = true;
        }

        needsUpdate |= updateDocStringValue(doc, FIELD_HOMEPAGE, DEFAULT_HOMEPAGE);

        needsUpdate |= updateDocBooleanValue(doc, FIELD_SECURE, DEFAULT_SECURE);

        needsUpdate |= updateDocBooleanValue(doc, FIELD_ISWIKITEMPLATE, DEFAULT_ISWIKITEMPLATE);

        return needsUpdate;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Make sure it return main wiki documents.
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.AbstractXClassManager#searchXObjectDocumentsByFields(java.lang.Object[][],
     *      com.xpn.xwiki.XWikiContext)
     * @since 1.5
     */
    @Override
    public List<XWikiServer> searchXObjectDocumentsByFields(Object[][] fieldDescriptors, XWikiContext context)
        throws XWikiException
    {
        String database = context.getDatabase();
        try {
            context.setDatabase(context.getMainXWiki());

            return super.searchXObjectDocumentsByFields(fieldDescriptors, context);
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * Get wiki alias {@link XWikiServer}.
     * 
     * @param wikiName the name of the wiki.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param validate indicate if it return new {@link XWikiServer} or throw exception if wiki descriptor does not
     *            exist.
     * @param context the XWiki context.
     * @return the {@link XWikiServer} representing wiki descriptor.
     * @throws XWikiException error when searching for wiki descriptor document.
     */
    public XWikiServer getWikiAlias(String wikiName, int objectId, boolean validate, XWikiContext context)
        throws XWikiException
    {
        try {
            return getXObjectDocument(wikiName, objectId, validate, context);
        } catch (XObjectDocumentDoesNotExistException e) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS,
                WikiManagerMessageTool.getDefault(context).get(WikiManagerMessageTool.ERROR_WIKIALIASDOESNOTEXISTS,
                    wikiName), e);
        }
    }

    /**
     * Get wiki template alias {@link XWikiServer}.
     * 
     * @param wikiName the name of the wiki.
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context.
     * @param validate indicate if it return new {@link XWikiServer} or throw exception if wiki descriptor does not
     *            exist.
     * @return the {@link XWikiServer} representing wiki descriptor.
     * @throws XWikiException error when searching for wiki descriptor document.
     */
    public XWikiServer getWikiTemplateAlias(String wikiName, int objectId, boolean validate, XWikiContext context)
        throws XWikiException
    {
        XWikiServer wiki = getWikiAlias(wikiName, objectId, validate, context);

        if (validate && !wiki.isWikiTemplate()) {
            throw new WikiManagerException(WikiManagerException.ERROR_WM_WIKIDOESNOTEXISTS,
                WikiManagerMessageTool.getDefault(context).get(
                    WikiManagerMessageTool.ERROR_WIKITEMPLATEALIASDOESNOTEXISTS, wikiName));
        }

        return wiki;
    }

    @Override
    public XWikiServer getXObjectDocument(String itemName, int objectId, boolean validate, XWikiContext context)
        throws XWikiException
    {
        String wiki = context.getDatabase();

        try {
            context.setDatabase(context.getMainXWiki());

            return super.getXObjectDocument(itemName, objectId, validate, context);
        } finally {
            context.setDatabase(wiki);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Override abstract method using XWikiApplication as
     * {@link com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XObjectDocument}.
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.AbstractXClassManager#newXObjectDocument(com.xpn.xwiki.doc.XWikiDocument,
     *      int, com.xpn.xwiki.XWikiContext)
     */
    @Override
    public XWikiServer newXObjectDocument(XWikiDocument doc, int objId, XWikiContext context) throws XWikiException
    {
        return new XWikiServer(doc, objId, context);
    }
}

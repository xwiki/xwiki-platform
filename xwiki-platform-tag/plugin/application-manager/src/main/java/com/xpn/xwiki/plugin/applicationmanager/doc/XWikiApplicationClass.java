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

package com.xpn.xwiki.plugin.applicationmanager.doc;

import java.util.List;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.objects.classes.AbstractSuperClass;
import com.xpn.xwiki.doc.objects.classes.SuperDocument;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.applicationmanager.ApplicationManagerException;

public class XWikiApplicationClass extends AbstractSuperClass
{
    /**
     * Space of class document.
     */
    private static final String CLASS_SPACE_PREFIX = "XApp";
    /**
     * Prefix of class document.
     */
    private static final String CLASS_PREFIX = "XWikiApplication";

    // ///

    /**
     * Name of field <code>appname</code>.
     */
    public static final String FIELD_appname = "appname";
    /**
     * Pretty name of field <code>appname</code>.
     */
    public static final String FIELDPN_appname = "Application Name";

    /**
     * Name of field <code>description</code>.
     */
    public static final String FIELD_description = "description";
    /**
     * Pretty name of field <code>description</code>.
     */
    public static final String FIELDPN_description = "Description";

    /**
     * Name of field <code>version</code>.
     */
    public static final String FIELD_appversion = "appversion";
    /**
     * Pretty name of field <code>version</code>.
     */
    public static final String FIELDPN_appversion = "Application Version";

    /**
     * Name of field <code>dependencies</code>.
     */
    public static final String FIELD_dependencies = "dependencies";
    /**
     * Pretty name of field <code>dependencies</code>.
     */
    public static final String FIELDPN_dependencies = "Dependencies";

    /**
     * Name of field <code>applications</code>.
     */
    public static final String FIELD_applications = "applications";
    /**
     * Pretty name of field <code>applications</code>.
     */
    public static final String FIELDPN_applications = "Applications";

    /**
     * Name of field <code>documents</code>.
     */
    public static final String FIELD_documents = "documents";
    /**
     * Pretty name of field <code>documents</code>.
     */
    public static final String FIELDPN_documents = "Documents";

    /**
     * Name of field <code>docstoinclude</code>.
     */
    public static final String FIELD_docstoinclude = "docstoinclude";
    /**
     * Pretty name of field <code>docstoinclude</code>.
     */
    public static final String FIELDPN_docstoinclude = "Documents to include";

    /**
     * Name of field <code>docstolink</code>.
     */
    public static final String FIELD_docstolink = "docstolink";
    /**
     * Pretty name of field <code>docstolink</code>.
     */
    public static final String FIELDPN_docstolink = "Documents to link";
    
    /**
     * Name of field <code>translationdocs</code>.
     */
    public static final String FIELD_translationdocs = "translationdocs";
    /**
     * Pretty name of field <code>translationdocs</code>.
     */
    public static final String FIELDPN_translationdocs = "Translations documents";

    // ///

    /**
     * Unique instance of XWikiApplicationClass;
     */
    private static XWikiApplicationClass instance = null;

    /**
     * Return unique instance of XWikiApplicationClass and update documents for this context.
     * 
     * @param context Context.
     * @return XWikiApplicationClass Instance of XWikiApplicationClass.
     * @throws XWikiException
     */
    public static XWikiApplicationClass getInstance(XWikiContext context) throws XWikiException
    {
        synchronized (XWikiApplicationClass.class) {
            if (instance == null)
                instance = new XWikiApplicationClass();
        }

        instance.check(context);

        return instance;
    }

    /**
     * Default constructor for XWikiApplicationClass.
     */
    private XWikiApplicationClass()
    {
        super(CLASS_SPACE_PREFIX, CLASS_PREFIX);
    }

    protected boolean updateBaseClass(BaseClass baseClass)
    {
        boolean needsUpdate = super.updateBaseClass(baseClass);

        needsUpdate |= baseClass.addTextField(FIELD_appname, FIELDPN_appname, 30);
        needsUpdate |= baseClass.addTextAreaField(FIELD_description, FIELDPN_description, 40, 5);
        needsUpdate |= baseClass.addTextField(FIELD_appversion, FIELDPN_appversion, 30);
        
        StaticListClass slc;
        needsUpdate |= baseClass.addStaticListField(FIELD_dependencies, FIELDPN_dependencies, 80, true, "", "input");

        // TODO : move into BaseClass.addStaticListField with "separators' parameter when/if
        // http://jira.xwiki.org/jira/browse/XWIKI-1683 is applied in XWiki Core and when this
        // starts depending on that version where it's applied.
        slc = (StaticListClass)baseClass.getField(FIELD_dependencies);
        slc.setSeparators("|");
        slc.setSeparator("|");

        needsUpdate |= baseClass.addStaticListField(FIELD_applications, FIELDPN_applications, 80, true, "", "input");

        // TODO : move into BaseClass.addStaticListField with "separators' parameter when/if
        // http://jira.xwiki.org/jira/browse/XWIKI-1683 is applied in XWiki Core and when this
        // starts depending on that version where it's applied.
        slc = (StaticListClass)baseClass.getField(FIELD_applications);
        slc.setSeparators("|");
        slc.setSeparator("|");

        needsUpdate |= baseClass.addStaticListField(FIELD_documents, FIELDPN_documents, 80, true, "", "input");

        // TODO : move into BaseClass.addStaticListField with "separators' parameter when/if
        // http://jira.xwiki.org/jira/browse/XWIKI-1683 is applied in XWiki Core and when this
        // starts depending on that version where it's applied.
        slc = (StaticListClass)baseClass.getField(FIELD_documents);
        slc.setSeparators("|");
        slc.setSeparator("|");

        needsUpdate |= baseClass.addStaticListField(FIELD_docstoinclude, FIELDPN_docstoinclude, 80, true, "", "input");

        // TODO : move into BaseClass.addStaticListField with "separators' parameter when/if
        // http://jira.xwiki.org/jira/browse/XWIKI-1683 is applied in XWiki Core and when this
        // starts depending on that version where it's applied.
        slc = (StaticListClass)baseClass.getField(FIELD_docstoinclude);
        slc.setSeparators("|");
        slc.setSeparator("|");

        needsUpdate |= baseClass.addStaticListField(FIELD_docstolink, FIELDPN_docstolink, 80, true, "", "input");

        // TODO : move into BaseClass.addStaticListField with "separators' parameter when/if
        // http://jira.xwiki.org/jira/browse/XWIKI-1683 is applied in XWiki Core and when this
        // starts depending on that version where it's applied.
        slc = (StaticListClass)baseClass.getField(FIELD_docstolink);
        slc.setSeparators("|");
        slc.setSeparator("|");

        needsUpdate |= baseClass.addStaticListField(FIELD_translationdocs, FIELDPN_translationdocs, 80, true, "", "input");

        // TODO : move into BaseClass.addStaticListField with "separators' parameter when/if
        // http://jira.xwiki.org/jira/browse/XWIKI-1683 is applied in XWiki Core and when this
        // starts depending on that version where it's applied.
        slc = (StaticListClass)baseClass.getField(FIELD_translationdocs);
        slc.setSeparators("|");
        slc.setSeparator("|");

        return needsUpdate;
    }

    private XWikiDocument getApplicationDocument(String appName, XWikiContext context,
        boolean validate) throws XWikiException
    {
        XWiki xwiki = context.getWiki();

        List listApp = searchItemDocumentsByField(FIELD_appname, appName, StringProperty.class.getSimpleName(), context);

        if (listApp.size() == 0) {
            if (validate)
                throw new ApplicationManagerException(ApplicationManagerException.ERROR_APPLICATIONMANAGER_DOES_NOT_EXIST,
                    appName + " application does not exist");
            else
                return xwiki.getDocument(getItemDocumentDefaultFullName(appName, context),
                    context);
        }

        return (XWikiDocument) listApp.get(0);
    }

    public XWikiApplication getApplication(String appName, XWikiContext context, boolean validate)
        throws XWikiException
    {
        return (XWikiApplication)newSuperDocument(getApplicationDocument(appName, context, validate), context);
    }
    
    public SuperDocument newSuperDocument(XWikiDocument doc, XWikiContext context)
    {
        return (SuperDocument)doc.newDocument(XWikiApplication.class.getName(), context);
    }
}

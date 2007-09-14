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

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultSuperDocument;
import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiApplication extends DefaultSuperDocument
{
    public XWikiApplication(XWikiDocument xdoc, XWikiContext context) throws XWikiException
    {
        super(XWikiApplicationClass.getInstance(context), xdoc, context);
    }

    public void reload(XWikiContext context) throws XWikiException
    {
        super.reload(context);

        if (getAppVersion().length() == 0)
            setAppVersion("1.0");
    }

    // ///

    public String getAppName()
    {
        return getStringValue(XWikiApplicationClass.FIELD_appname);
    }

    public void setAppName(String appname)
    {
        setStringValue(XWikiApplicationClass.FIELD_appname, appname);
    }

    public String getDescription()
    {
        return getStringValue(XWikiApplicationClass.FIELD_description);
    }

    public void setDescription(String description)
    {
        setStringValue(XWikiApplicationClass.FIELD_description, description);
    }

    public String getAppVersion()
    {
        return getStringValue(XWikiApplicationClass.FIELD_appversion);
    }

    public void setAppVersion(String appversion)
    {
        setStringValue(XWikiApplicationClass.FIELD_appversion, appversion);
    }

    public List getDependencies()
    {
        return getListValue(XWikiApplicationClass.FIELD_dependencies);
    }

    public void setDependencies(List dependencies)
    {
        setListValue(XWikiApplicationClass.FIELD_dependencies, dependencies);
    }

    public List getApplications()
    {
        return getListValue(XWikiApplicationClass.FIELD_applications);
    }

    public void setApplications(List applications)
    {
        setListValue(XWikiApplicationClass.FIELD_applications, applications);
    }

    public List getDocuments()
    {
        return getListValue(XWikiApplicationClass.FIELD_documents);
    }

    public void setDocuments(List documents)
    {
        setListValue(XWikiApplicationClass.FIELD_documents, documents);
    }

    public List getDocsToInclude()
    {
        return getListValue(XWikiApplicationClass.FIELD_docstoinclude);
    }

    public void setDocsToInclude(List docstoinclude)
    {
        setListValue(XWikiApplicationClass.FIELD_docstoinclude, docstoinclude);
    }

    public List getDocsToLink()
    {
        return getListValue(XWikiApplicationClass.FIELD_docstolink);
    }

    public void setDocsToLink(List docstolink)
    {
        setListValue(XWikiApplicationClass.FIELD_docstolink, docstolink);
    }
    
    public List getTranslationDocs()
    {
        return getListValue(XWikiApplicationClass.FIELD_translationdocs);
    }

    public void setTranslationDocs(List translationdocs)
    {
        setListValue(XWikiApplicationClass.FIELD_translationdocs, translationdocs);
    }

    // ///

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return getAppName() + "-" + getAppVersion();
    }

    // ///

    public Set getXWikiApplicationSet(boolean recurse, XWikiContext context)
        throws XWikiException
    {
        Set applicationSet = new HashSet();
        
        List applications = getApplications();
        for (Iterator it = applications.iterator(); it.hasNext();) {
            XWikiApplication app = ((XWikiApplicationClass)sclass).getApplication((String)it.next(), context, true);
            applicationSet.add(app);
            
            if (recurse)
                applicationSet.addAll(app.getXWikiApplicationSet(recurse, context));
            
        }
        
        return applicationSet;
    }

    private Set getDocsNameSet(String type, boolean recurse, XWikiContext context)
        throws XWikiException
    {
        return getDocsNameSet(type, recurse, false, context);
    }

    private Set getDocsNameSet(String type, boolean recurse, boolean includeAppDoc,
        XWikiContext context) throws XWikiException
    {
        Set set = new HashSet();

        if (includeAppDoc)
            set.add(getFullName());

        List list = getListValue(type);

        if (list.contains("*"))
            set.add("*");
        else {
            set.addAll(list);

            if (recurse) {
                for (Iterator it = getXWikiApplicationSet(true, context).iterator(); it.hasNext();) {
                    XWikiApplication app = (XWikiApplication) it.next();

                    if (includeAppDoc)
                        set.add(app.getDocument().getFullName());
                    
                    set.addAll(app.getListValue(type));
                }
            }
        }

        return set;
    }

    public Set getDocumentsNames(boolean recurse, boolean includeAppDoc, XWikiContext context)
        throws XWikiException
    {
        return getDocsNameSet(XWikiApplicationClass.FIELD_documents, recurse, includeAppDoc,
            context);
    }

    public Set getDocsNameToInclude(boolean recurse, XWikiContext context) throws XWikiException
    {
        return getDocsNameSet(XWikiApplicationClass.FIELD_docstoinclude, recurse, context);
    }

    public static Set getDocsNameToInclude(List applications, boolean recurse,
        XWikiContext context) throws XWikiException
    {
        Set docsToInclude = new HashSet();

        for (Iterator it = applications.iterator(); it.hasNext();) {
            docsToInclude.addAll(((XWikiApplication) it.next()).getDocsNameToInclude(recurse,
                context));
        }

        return docsToInclude;
    }

    public Set getDocsNameToLink(boolean recurse, XWikiContext context) throws XWikiException
    {
        return getDocsNameSet(XWikiApplicationClass.FIELD_docstolink, recurse, context);
    }
}

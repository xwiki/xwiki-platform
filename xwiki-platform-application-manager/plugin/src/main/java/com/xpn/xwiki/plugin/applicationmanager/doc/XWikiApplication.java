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

package com.xpn.xwiki.plugin.applicationmanager.doc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultSuperDocument;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * This class manage an XWiki document containing XWiki.XWikiApplicationClass object. It add some
 * specifics methods, getters and setters for this type of object and fields.
 * 
 * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultSuperDocument
 */
public class XWikiApplication extends DefaultSuperDocument
{
    /**
     * Pattern to match that indicate if a document name contains SQL "like" matching string.
     */
    static private final Pattern extDocNamePattern = Pattern.compile("^\\[(.*)\\]$");

    /**
     * Create new XWikiApplication managing provided XWikiDocument.
     * 
     * @param xdoc the encapsulated XWikiDocument
     * @param context the XWiki context
     * @throws XWikiException
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultSuperDocument#DefaultSuperDocument(SuperClass,
     *      XWikiDocument, XWikiContext)
     */
    public XWikiApplication(XWikiDocument xdoc, XWikiContext context) throws XWikiException
    {
        super(XWikiApplicationClass.getInstance(context), xdoc, context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Add some specifics action for Application Manager descriptors document.
     * <ul>
     * <li> Initialize application version to "1.0".
     * <li> Set descriptor document default parent to Application Manager home page :
     * XAppManager.WebHome.
     * </ul>
     * 
     * @see com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultSuperDocument#reload(com.xpn.xwiki.XWikiContext)
     */
    public void reload(XWikiContext context) throws XWikiException
    {
        super.reload(context);

        if (getAppVersion().length() == 0)
            setAppVersion("1.0");

        if (super.isNew())
            // set Application Manager home as default parent for all applications descriptors
            setParent(sclass.getClassSpacePrefix() + "Manager.WebHome");
    }

    // ///

    /**
     * @return the name of the application
     * @see #setAppName(String)
     */
    public String getAppName()
    {
        return getStringValue(XWikiApplicationClass.FIELD_appname);
    }

    /**
     * Modify the name of the application.
     * 
     * @param appname the new name of the application.
     * @see #getAppName()
     */
    public void setAppName(String appname)
    {
        setStringValue(XWikiApplicationClass.FIELD_appname, appname);
    }

    /**
     * @return the description of the application.
     * @see #setDescription(String)
     */
    public String getDescription()
    {
        return getStringValue(XWikiApplicationClass.FIELD_description);
    }

    /**
     * Modify the description of the application.
     * 
     * @param description the new description of the application.
     * @see #getDescription()
     */
    public void setDescription(String description)
    {
        setStringValue(XWikiApplicationClass.FIELD_description, description);
    }

    /**
     * @return the version of the application.
     * @see #setAppVersion(String)
     */
    public String getAppVersion()
    {
        return getStringValue(XWikiApplicationClass.FIELD_appversion);
    }

    /**
     * Modify the version of the application.
     * 
     * @param appversion
     * @see #getAppVersion()
     */
    public void setAppVersion(String appversion)
    {
        setStringValue(XWikiApplicationClass.FIELD_appversion, appversion);
    }

    /**
     * @return the list of plugins on which application depends.
     * @see #setDependencies(List)
     */
    public List getDependencies()
    {
        return getListValue(XWikiApplicationClass.FIELD_dependencies);
    }

    /**
     * Modify the list of plugins on which application depends.
     * 
     * @param dependencies the new list of plugins on which application depends.
     * @see #getDependencies()
     */
    public void setDependencies(List dependencies)
    {
        setListValue(XWikiApplicationClass.FIELD_dependencies, dependencies);
    }

    /**
     * @return the list of other applications on which current application depends.
     */
    public List getApplications()
    {
        return getListValue(XWikiApplicationClass.FIELD_applications);
    }

    /**
     * Modify the list of other applications on which current application depends.
     * 
     * @param applications the new list of other applications on which current application depends.
     * @see #getApplications()
     */
    public void setApplications(List applications)
    {
        setListValue(XWikiApplicationClass.FIELD_applications, applications);
    }

    /**
     * @return the list of documents application contains.
     * @see #setDocuments(List)
     */
    public List getDocuments()
    {
        return getListValue(XWikiApplicationClass.FIELD_documents);
    }

    /**
     * Modify the list of documents application contains.
     * 
     * @param documents the new list of documents application contains.
     * @see #getDocuments()
     */
    public void setDocuments(List documents)
    {
        setListValue(XWikiApplicationClass.FIELD_documents, documents);
    }

    /**
     * @return the list of document application contains that will be included in place of copy from
     *         wiki template.
     * @see #setDocsToInclude(List)
     */
    public List getDocsToInclude()
    {
        return getListValue(XWikiApplicationClass.FIELD_docstoinclude);
    }

    /**
     * Modify the list of document application contains that will be included in place of copy from
     * wiki template.
     * 
     * @param docstoinclude the new list of document application contains that will be included in
     *            place of copy from a wiki template.
     * @see #getDocsToInclude()
     */
    public void setDocsToInclude(List docstoinclude)
    {
        setListValue(XWikiApplicationClass.FIELD_docstoinclude, docstoinclude);
    }

    /**
     * @return the list of document application contains that will be linked in place of copy from a
     *         wiki template.
     * @see #setDocsToLink(List)
     */
    public List getDocsToLink()
    {
        return getListValue(XWikiApplicationClass.FIELD_docstolink);
    }

    /**
     * Modify the list of document application contains that will be included in place of copy from
     * wiki template.
     * 
     * @param docstolink the new list of document application contains that will be linked in place
     *            of copy from a wiki template.
     * @see #getDocsToLink()
     */
    public void setDocsToLink(List docstolink)
    {
        setListValue(XWikiApplicationClass.FIELD_docstolink, docstolink);
    }

    /**
     * @return the list of documents containing translations strings.
     *         <p>
     *         Theses documents are added to XWiki.XWikiPreferences "documentBundles" field at
     *         application installation.
     */
    public List getTranslationDocs()
    {
        return getListValue(XWikiApplicationClass.FIELD_translationdocs);
    }

    /**
     * Modify the list of documents containing translations strings.
     * <p>
     * Theses documents are added to XWiki.XWikiPreferences "documentBundles" field at application
     * installation.
     * 
     * @param translationdocs the new list of documents containing translations strings. Theses
     *            documents are added to XWiki.XWikiPreferences "documentBundles" field at
     *            application installation.
     */
    public void setTranslationDocs(List translationdocs)
    {
        setListValue(XWikiApplicationClass.FIELD_translationdocs, translationdocs);
    }

    // ///

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return getAppName() + "-" + getAppVersion();
    }

    // ///

    /**
     * Get set of XWikiApplication containing all applications on which current application depend.
     * 
     * @param recurse if true it add recursively all applications dependencies, if false return only
     *            direct dependencies.
     * @param context the XWiki context.
     * @return the set list of XWikiApplication.
     * @throws XWikiException
     * @see #getApplications()
     */
    public Set getXWikiApplicationSet(boolean recurse, XWikiContext context)
        throws XWikiException
    {
        Set applicationSet = new HashSet();

        List applications = getApplications();
        for (Iterator it = applications.iterator(); it.hasNext();) {
            XWikiApplication app =
                ((XWikiApplicationClass) sclass)
                    .getApplication((String) it.next(), context, true);
            applicationSet.add(app);

            if (recurse)
                applicationSet.addAll(app.getXWikiApplicationSet(recurse, context));

        }

        return applicationSet;
    }

    /**
     * Insert in <code>docsNames</code> all documents names <code>docsNamesToResolve</code>
     * contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param docsNames the collection to complete with resolved documents names.
     * @param docsNamesToResolve the documents names to resolve.
     * @param context the XWiki context.
     * @throws XWikiException
     * @see #extDocNamePattern
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocumentsNames(String, XWikiContext)
     */
    private static void resolveDocumentsNames(Collection docsNames,
        Collection docsNamesToResolve, XWikiContext context) throws XWikiException
    {
        Matcher matcher;

        StringBuffer where = new StringBuffer();
        for (Iterator it = docsNamesToResolve.iterator(); it.hasNext();) {
            String docName = (String) it.next();

            matcher = extDocNamePattern.matcher(docName);
            if (matcher.matches()) {
                if (where.length() > 0)
                    where.append(" or ");
                where.append("doc.fullName like '").append(matcher.group(1)).append("'");
            } else
                docsNames.add(docName);
        }

        if (where.length() > 0)
            docsNames.addAll(context.getWiki().getStore().searchDocumentsNames("where " + where,
                context));
    }

    /**
     * Insert in <code>docsNames</code> all documents names of type <code>type</code>
     * application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param docsNames the collection to complete with resolved documents names.
     * @param type the XWikiApplicationClass field where to find documents names list :
     *            {@link XWikiApplicationClass#FIELD_documents},
     *            {@link XWikiApplicationClass#FIELD_docstoinclude},
     *            {@link XWikiApplicationClass#FIELD_docstolink}.
     * @param includeAppDesc if true application descriptor document names is add to
     *            <code>docsNames</code>.
     * @param context the XWiki context.
     * @throws XWikiException
     * @see XWikiApplicationClass#FIELD_documents
     * @see XWikiApplicationClass#FIELD_docstoinclude
     * @see XWikiApplicationClass#FIELD_docstolink
     * @see #resolveDocumentsNames(Collection, Collection, XWikiContext)
     */
    private void resolveDocumentsNames(Collection docsNames, String type, boolean includeAppDesc,
        XWikiContext context) throws XWikiException
    {
        if (includeAppDesc)
            docsNames.add(getFullName());

        resolveDocumentsNames(docsNames, getListValue(type), context);
    }

    /**
     * Insert in <code>docsNames</code> all documents names of type
     * <code>type</code> <code>applications</code> XWikiApplication list contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param docsNames the collection to complete with resolved documents names.
     * @param applications the applications containing documents names to resolve and add to
     *            <code>docsNames</code>.
     * @param type the XWikiApplicationClass field where to find documents names list :
     *            {@link XWikiApplicationClass#FIELD_documents},
     *            {@link XWikiApplicationClass#FIELD_docstoinclude},
     *            {@link XWikiApplicationClass#FIELD_docstolink}.
     * @param includeAppDesc if true application descriptor document names is add to
     *            <code>docsNames</code>.
     * @param context the XWiki context.
     * @throws XWikiException
     */
    private static void resolveApplicationsDocsNames(Collection docsNames,
        Collection applications, String type, boolean includeAppDesc, XWikiContext context)
        throws XWikiException
    {
        for (Iterator it = applications.iterator(); it.hasNext();) {
            XWikiApplication app = (XWikiApplication) it.next();

            app.resolveDocumentsNames(docsNames, type, includeAppDesc, context);
        }
    }

    /**
     * Get and resolve all documents names of type <code>type</code> application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param type the XWikiApplicationClass field where to find documents names list :
     *            {@link XWikiApplicationClass#FIELD_documents},
     *            {@link XWikiApplicationClass#FIELD_docstoinclude},
     *            {@link XWikiApplicationClass#FIELD_docstolink}.
     * @param recurse if true it follow recursively all applications dependencies, if false parse
     *            only direct dependencies.
     * @param includeAppDesc if true application descriptor document names is add to
     *            <code>docsNames</code>.
     * @param context the XWiki context.
     * @return all documents names of type <code>type</code> application contains.
     * @throws XWikiException
     */
    private Set getDocsNameSet(String type, boolean recurse, boolean includeAppDesc,
        XWikiContext context) throws XWikiException
    {
        Set documents = new HashSet();

        resolveDocumentsNames(documents, type, includeAppDesc, context);

        if (recurse) {
            resolveApplicationsDocsNames(documents, getXWikiApplicationSet(true, context), type,
                includeAppDesc, context);
        }

        return documents;
    }

    /**
     * Get and resolve all documents names application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param recurse if true it follow recursively all applications dependencies, if false parse
     *            only direct dependencies.
     * @param includeAppDesc if true application descriptor document names is add to
     *            <code>docsNames</code>.
     * @param context the XWiki context.
     * @return all documents names application contains.
     * @throws XWikiException
     * @see #getDocuments()
     * @see XWikiApplicationClass#FIELD_documents
     */
    public Set getDocumentsNames(boolean recurse, boolean includeAppDesc, XWikiContext context)
        throws XWikiException
    {
        return getDocsNameSet(XWikiApplicationClass.FIELD_documents, recurse, includeAppDesc,
            context);
    }

    /**
     * Get and resolve all documents names to include application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param recurse if true it follow recursively all applications dependencies, if false parse
     *            only direct dependencies.
     * @param context the XWiki context.
     * @return all documents names to include application contains.
     * @throws XWikiException
     * @see #getDocsToInclude()
     * @see XWikiApplicationClass#FIELD_docstoinclude
     */
    public Set getDocsNameToInclude(boolean recurse, XWikiContext context) throws XWikiException
    {
        return getDocsNameSet(XWikiApplicationClass.FIELD_docstoinclude, recurse, false, context);
    }

    /**
     * Get and resolve all documents names to include <code>applications</code> XWikiApplication
     * list contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param applications the applications containing documents names to resolve and add to
     *            <code>docsNames</code>.
     * @param recurse if true it follow recursively all applications dependencies, if false parse
     *            only provided applications.
     * @param context the XWiki context.
     * @return all documents names to include <code>applications</code> contains.
     * @throws XWikiException
     * @see #getDocsToInclude()
     * @see XWikiApplicationClass#FIELD_docstoinclude
     */
    public static Set getDocsNameToInclude(Collection applications, boolean recurse,
        XWikiContext context) throws XWikiException
    {
        Set docsToInclude = new HashSet();

        resolveApplicationsDocsNames(docsToInclude, applications,
            XWikiApplicationClass.FIELD_docstoinclude, false, context);

        return docsToInclude;
    }

    /**
     * Get and resolve all documents names to link application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param recurse if true it follow recursively all applications dependencies, if false parse
     *            only direct dependencies.
     * @param context the XWiki context.
     * @return all documents names to link application contains.
     * @throws XWikiException
     * @see #getDocsToLink()
     * @see XWikiApplicationClass#FIELD_docstolink
     */
    public Set getDocsNameToLink(boolean recurse, XWikiContext context) throws XWikiException
    {
        return getDocsNameSet(XWikiApplicationClass.FIELD_docstolink, recurse, false, context);
    }

    /**
     * Get and resolve all documents names to link <code>applications</code> XWikiApplication list
     * contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param applications the applications containing documents names to resolve and add to
     *            <code>docsNames</code>.
     * @param recurse if true it follow recursively all applications dependencies, if false parse
     *            only provided applications.
     * @param context the XWiki context.
     * @return all documents names to link <code>applications</code> contains.
     * @throws XWikiException
     * @see #getDocsToLink()
     * @see XWikiApplicationClass#FIELD_docstolink
     */
    public static Set getDocsNameToLink(Collection applications, boolean recurse,
        XWikiContext context) throws XWikiException
    {
        Set docsToLink = new HashSet();

        resolveApplicationsDocsNames(docsToLink, applications,
            XWikiApplicationClass.FIELD_docstolink, false, context);

        return docsToLink;
    }
}

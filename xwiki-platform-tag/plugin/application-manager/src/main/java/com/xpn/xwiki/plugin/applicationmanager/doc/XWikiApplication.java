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
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultXObjectDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * This class manage an XWiki document containing XApp.XWikiApplicationClass object. It add some
 * specifics methods, getters and setters for this type of object and fields.
 * 
 * @version $Id: $
 */
public class XWikiApplication extends DefaultXObjectDocument
{
    /**
     * Pattern to match that indicate if a document name contains SQL "like" matching string.
     */
    private static final Pattern EXT_DOCNAME_PATTERN = Pattern.compile("^\\[(.*)\\]$");

    /**
     * Create new XWikiApplication managing provided XWikiDocument.
     * 
     * @param xdoc the encapsulated XWikiDocument
     * @param objectId the id of the XWiki object included in the document to manage.
     * @param context the XWiki context
     * @throws XWikiException error when:
     *             <ul>
     *             <li>getting XWikiApplicationClass instance.</li>
     *             <li>or when calling {@link #reload(XWikiContext)}</li>
     *             </ul>
     * @see DefaultXObjectDocument#DefaultXObjectDocument(com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.SuperClass,
     *      XWikiDocument, int, XWikiContext)
     */
    public XWikiApplication(XWikiDocument xdoc, int objectId, XWikiContext context)
        throws XWikiException
    {
        super(XWikiApplicationClass.getInstance(context), xdoc, objectId, context);
    }

    // ///

    /**
     * @return the name of the application
     * @see #setAppName(String)
     */
    public String getAppName()
    {
        return getStringValue(XWikiApplicationClass.FIELD_APPNAME);
    }

    /**
     * Modify the name of the application.
     * 
     * @param appname the new name of the application.
     * @see #getAppName()
     */
    public void setAppName(String appname)
    {
        setStringValue(XWikiApplicationClass.FIELD_APPNAME, appname);
    }

    /**
     * @return the description of the application.
     * @see #setDescription(String)
     */
    public String getDescription()
    {
        return getStringValue(XWikiApplicationClass.FIELD_DESCRIPTION);
    }

    /**
     * Modify the description of the application.
     * 
     * @param description the new description of the application.
     * @see #getDescription()
     */
    public void setDescription(String description)
    {
        setStringValue(XWikiApplicationClass.FIELD_DESCRIPTION, description);
    }

    /**
     * @return the version of the application.
     * @see #setAppVersion(String)
     */
    public String getAppVersion()
    {
        return getStringValue(XWikiApplicationClass.FIELD_APPVERSION);
    }

    /**
     * Modify the version of the application.
     * 
     * @param appversion the version of the application.
     * @see #getAppVersion()
     */
    public void setAppVersion(String appversion)
    {
        setStringValue(XWikiApplicationClass.FIELD_APPVERSION, appversion);
    }

    /**
     * @return the authors of the application.
     * @see #setAppAuthors(String)
     */
    public String getAppAuthors()
    {
        return getStringValue(XWikiApplicationClass.FIELD_APPAUTHORS);
    }

    /**
     * Modify the authors of the application.
     * 
     * @param appauthors the authors of the application.
     * @see #getAppAuthors()
     */
    public void setAppAuthors(String appauthors)
    {
        setStringValue(XWikiApplicationClass.FIELD_APPAUTHORS, appauthors);
    }

    /**
     * @return the license of the application.
     * @see #setLicense(String)
     */
    public String getLicense()
    {
        return getStringValue(XWikiApplicationClass.FIELD_LICENSE);
    }

    /**
     * Modify the version of the application.
     * 
     * @param license the license of the application.
     * @see #getAppVersion()
     */
    public void setLicense(String license)
    {
        setStringValue(XWikiApplicationClass.FIELD_LICENSE, license);
    }

    /**
     * @return the list of plugins on which application depends.
     * @see #setDependencies(List)
     */
    public List getDependencies()
    {
        return getListValue(XWikiApplicationClass.FIELD_DEPENDENCIES);
    }

    /**
     * Modify the list of plugins on which application depends.
     * 
     * @param dependencies the new list of plugins on which application depends.
     * @see #getDependencies()
     */
    public void setDependencies(List dependencies)
    {
        setListValue(XWikiApplicationClass.FIELD_DEPENDENCIES, dependencies);
    }

    /**
     * @return the list of other applications on which current application depends.
     */
    public List getApplications()
    {
        return getListValue(XWikiApplicationClass.FIELD_APPLICATIONS);
    }

    /**
     * Modify the list of other applications on which current application depends.
     * 
     * @param applications the new list of other applications on which current application depends.
     * @see #getApplications()
     */
    public void setApplications(List applications)
    {
        setListValue(XWikiApplicationClass.FIELD_APPLICATIONS, applications);
    }

    /**
     * @return the list of documents application contains.
     * @see #setDocuments(List)
     */
    public List getDocuments()
    {
        return getListValue(XWikiApplicationClass.FIELD_DOCUMENTS);
    }

    /**
     * Modify the list of documents application contains.
     * 
     * @param documents the new list of documents application contains.
     * @see #getDocuments()
     */
    public void setDocuments(List documents)
    {
        setListValue(XWikiApplicationClass.FIELD_DOCUMENTS, documents);
    }

    /**
     * @return the list of document application contains that will be included in place of copy from
     *         wiki template.
     * @see #setDocsToInclude(List)
     */
    public List getDocsToInclude()
    {
        return getListValue(XWikiApplicationClass.FIELD_DOCSTOINCLUDE);
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
        setListValue(XWikiApplicationClass.FIELD_DOCSTOINCLUDE, docstoinclude);
    }

    /**
     * @return the list of document application contains that will be linked in place of copy from a
     *         wiki template.
     * @see #setDocsToLink(List)
     */
    public List getDocsToLink()
    {
        return getListValue(XWikiApplicationClass.FIELD_DOCSTOLINK);
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
        setListValue(XWikiApplicationClass.FIELD_DOCSTOLINK, docstolink);
    }

    /**
     * @return the list of documents containing translations strings.
     *         <p>
     *         Theses documents are added to XWiki.XWikiPreferences "documentBundles" field at
     *         application installation.
     */
    public List getTranslationDocs()
    {
        return getListValue(XWikiApplicationClass.FIELD_TRANSLATIONDOCS);
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
        setListValue(XWikiApplicationClass.FIELD_TRANSLATIONDOCS, translationdocs);
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

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return getAppName() != null ? getAppName().hashCode() : "".hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.api.Document#equals(java.lang.Object)
     */
    public boolean equals(Object object)
    {
        boolean equals = false;

        if (object instanceof XWikiApplication) {
            equals =
                getAppName() == null ? ((XWikiApplication) object).getAppName() == null
                    : getAppName().equalsIgnoreCase(((XWikiApplication) object).getAppName());
        } else if (object instanceof String) {
            equals =
                getAppName() == null ? object == null : getAppName().equalsIgnoreCase(
                    (String) object);
        }

        return equals;
    }

    // ///

    /**
     * Add all applications on which current application depend.
     * 
     * @param rootApplication the root application containing recursively all in
     *            <code>applicationList</code>.
     * @param applicationList the applications.
     * @param recurse if true it add recursively all applications dependencies, if false return only
     *            direct dependencies.
     * @param context the XWiki context.
     * @throws XWikiException error when getting application descriptor document from the database.
     * @see #getApplications()
     */
    protected void addXWikiApplicationSet(XWikiApplication rootApplication,
        Collection applicationList, boolean recurse, XWikiContext context) throws XWikiException
    {
        List applications = getApplications();
        for (Iterator it = applications.iterator(); it.hasNext();) {
            String appname = (String) it.next();

            // Breaks infinite loop if application contains itself in its dependencies at any level.
            if ((rootApplication == null || !rootApplication.equals(appname))
                && !applicationList.contains(appname)) {
                XWikiApplication app =
                    ((XWikiApplicationClass) sclass).getApplication(appname, true, context);
                applicationList.add(app);

                if (recurse) {
                    app
                        .addXWikiApplicationSet(rootApplication, applicationList, recurse,
                            context);
                }
            }
        }
    }

    /**
     * Get set of XWikiApplication containing all applications on which current application depend.
     * 
     * @param recurse if true it add recursively all applications dependencies, if false return only
     *            direct dependencies.
     * @param context the XWiki context.
     * @return the set list of XWikiApplication.
     * @throws XWikiException error when getting application descriptor document from the database.
     * @see #getApplications()
     */
    public Set getXWikiApplicationSet(boolean recurse, XWikiContext context)
        throws XWikiException
    {
        Set applicationSet = new HashSet();

        addXWikiApplicationSet(this, applicationSet, recurse, context);

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
     * @throws XWikiException error when resolving SQL matching.
     * @see #EXT_DOCNAME_PATTERN
     * @see com.xpn.xwiki.store.XWikiStoreInterface#searchDocumentsNames(String, XWikiContext)
     */
    private static void resolveDocumentsNames(Collection docsNames,
        Collection docsNamesToResolve, XWikiContext context) throws XWikiException
    {
        Matcher matcher;

        StringBuffer where = new StringBuffer();
        for (Iterator it = docsNamesToResolve.iterator(); it.hasNext();) {
            String docName = (String) it.next();

            matcher = EXT_DOCNAME_PATTERN.matcher(docName);
            if (matcher.matches()) {
                if (where.length() > 0) {
                    where.append(" or ");
                }
                where.append("doc.fullName like '").append(matcher.group(1)).append("'");
            } else {
                docsNames.add(docName);
            }
        }

        if (where.length() > 0) {
            docsNames.addAll(context.getWiki().getStore().searchDocumentsNames("where " + where,
                context));
        }
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
     *            {@link XWikiApplicationClass#FIELD_DOCUMENTS},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOINCLUDE},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOLINK}.
     * @param includeAppDesc if true application descriptor document names is add to
     *            <code>docsNames</code>.
     * @throws XWikiException error when resolving SQL matching.
     * @see XWikiApplicationClass#FIELD_DOCUMENTS
     * @see XWikiApplicationClass#FIELD_DOCSTOINCLUDE
     * @see XWikiApplicationClass#FIELD_DOCSTOLINK
     * @see #resolveDocumentsNames(Collection, Collection, XWikiContext)
     */
    private void resolveDocumentsNames(Collection docsNames, String type, boolean includeAppDesc)
        throws XWikiException
    {
        if (includeAppDesc) {
            docsNames.add(getFullName());
        }

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
     *            {@link XWikiApplicationClass#FIELD_DOCUMENTS},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOINCLUDE},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOLINK}.
     * @param includeAppDesc if true application descriptor document names is add to
     *            <code>docsNames</code>.
     * @throws XWikiException error when resolving SQL matching.
     */
    private static void resolveApplicationsDocsNames(Collection docsNames,
        Collection applications, String type, boolean includeAppDesc) throws XWikiException
    {
        for (Iterator it = applications.iterator(); it.hasNext();) {
            XWikiApplication app = (XWikiApplication) it.next();

            app.resolveDocumentsNames(docsNames, type, includeAppDesc);
        }
    }

    /**
     * Get and resolve all documents names of type <code>type</code> application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param type the XWikiApplicationClass field where to find documents names list :
     *            {@link XWikiApplicationClass#FIELD_DOCUMENTS},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOINCLUDE},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOLINK}.
     * @param recurse if true it follow recursively all applications dependencies, if false parse
     *            only direct dependencies.
     * @param includeAppDesc if true application descriptor document names is add to
     *            <code>docsNames</code>.
     * @return all documents names of type <code>type</code> application contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li>
     *             <li>or getting applications dependencies descriptors documents from the
     *             database.</li>
     *             </ul>
     */
    private Set getDocsNameSet(String type, boolean recurse, boolean includeAppDesc)
        throws XWikiException
    {
        Set documents = new HashSet();

        resolveDocumentsNames(documents, type, includeAppDesc);

        if (recurse) {
            resolveApplicationsDocsNames(documents, getXWikiApplicationSet(true, context), type,
                includeAppDesc);
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
     * @return all documents names application contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li>
     *             <li>or getting applications dependencies descriptors documents from the
     *             database.</li>
     *             </ul>
     * @see #getDocuments()
     * @see XWikiApplicationClass#FIELD_DOCUMENTS
     */
    public Set getDocumentsNames(boolean recurse, boolean includeAppDesc) throws XWikiException
    {
        return getDocsNameSet(XWikiApplicationClass.FIELD_DOCUMENTS, recurse, includeAppDesc);
    }

    /**
     * Get and resolve all documents names to include application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching
     * string to use with "like".
     * 
     * @param recurse if true it follow recursively all applications dependencies, if false parse
     *            only direct dependencies.
     * @return all documents names to include application contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li>
     *             <li>or getting applications dependencies descriptors documents from the
     *             database.</li>
     *             </ul>
     * @see #getDocsToInclude()
     * @see XWikiApplicationClass#FIELD_DOCSTOINCLUDE
     */
    public Set getDocsNameToInclude(boolean recurse) throws XWikiException
    {
        return getDocsNameSet(XWikiApplicationClass.FIELD_DOCSTOINCLUDE, recurse, false);
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
     * @return all documents names to include <code>applications</code> contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li>
     *             <li>or getting applications dependencies descriptors documents from the
     *             database.</li>
     *             </ul>
     * @see #getDocsToInclude()
     * @see XWikiApplicationClass#FIELD_DOCSTOINCLUDE
     */
    public static Set getDocsNameToInclude(Collection applications) throws XWikiException
    {
        Set docsToInclude = new HashSet();

        resolveApplicationsDocsNames(docsToInclude, applications,
            XWikiApplicationClass.FIELD_DOCSTOINCLUDE, false);

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
     * @return all documents names to link application contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li>
     *             <li>or getting applications dependencies descriptors documents from the
     *             database.</li>
     *             </ul>
     * @see #getDocsToLink()
     * @see XWikiApplicationClass#FIELD_DOCSTOLINK
     */
    public Set getDocsNameToLink(boolean recurse) throws XWikiException
    {
        return getDocsNameSet(XWikiApplicationClass.FIELD_DOCSTOLINK, recurse, false);
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
     * @return all documents names to link <code>applications</code> contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li>
     *             <li>or getting applications dependencies descriptors documents from the
     *             database.</li>
     *             </ul>
     * @see #getDocsToLink()
     * @see XWikiApplicationClass#FIELD_DOCSTOLINK
     */
    public static Set getDocsNameToLink(Collection applications) throws XWikiException
    {
        Set docsToLink = new HashSet();

        resolveApplicationsDocsNames(docsToLink, applications,
            XWikiApplicationClass.FIELD_DOCSTOLINK, false);

        return docsToLink;
    }
}

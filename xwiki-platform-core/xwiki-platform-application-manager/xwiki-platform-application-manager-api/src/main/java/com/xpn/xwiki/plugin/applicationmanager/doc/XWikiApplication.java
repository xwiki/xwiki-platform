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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultXObjectDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * This class manage an XWiki document containing XApp.XWikiApplicationClass object. It add some specifics methods,
 * getters and setters for this type of object and fields.
 * 
 * @version $Id$
 */
public class XWikiApplication extends DefaultXObjectDocument
{
    /**
     * Pattern to match that indicate if a document name contains SQL "like" matching string.
     */
    private static final Pattern EXT_DOCNAME_PATTERN = Pattern.compile("^\\[(.*)\\]$");

    /**
     * HQL where key word.
     */
    private static final String HQL_WHERE = "where";

    /**
     * HQL or key word.
     */
    private static final String HQL_OR = " or ";

    /**
     * HQL and key word.
     */
    private static final String HQL_AND = " and ";

    /**
     * Filter to add in a named HQL query for a specific document.
     */
    private static final String HQL_FILTER_DOC_EQUALS = "doc.fullName = ?";

    /**
     * Filter to add in a named HQL query to filter documents with a pattern.
     */
    private static final String HQL_FILTER_DOC_PATTERN = "doc.fullName like ?";

    /**
     * Open HQL group.
     */
    private static final String HQL_GROUP_OPEN = "(";

    /**
     * Clause HQL group.
     */
    private static final String HQL_GROUP_CLOSE = ")";

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
     * @see DefaultXObjectDocument#DefaultXObjectDocument(com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.XClassManager,
     *      XWikiDocument, int, XWikiContext)
     */
    public XWikiApplication(XWikiDocument xdoc, int objectId, XWikiContext context) throws XWikiException
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
     * @return the pretty name of the application
     * @see #setAppPrettyName(String)
     */
    public String getAppPrettyName()
    {
        return getStringValue(XWikiApplicationClass.FIELD_APPPRETTYNAME);
    }

    /**
     * Modify the pratty name of the application.
     * 
     * @param appprettyname the new pretty name of the application.
     * @see #getAppPrettyName()
     */
    public void setAppPrettyName(String appprettyname)
    {
        setStringValue(XWikiApplicationClass.FIELD_APPPRETTYNAME, appprettyname);
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
    public List<String> getDependencies()
    {
        return getStringListValue(XWikiApplicationClass.FIELD_DEPENDENCIES);
    }

    /**
     * Modify the list of plugins on which application depends.
     * 
     * @param dependencies the new list of plugins on which application depends.
     * @see #getDependencies()
     */
    public void setDependencies(List<String> dependencies)
    {
        setStringListValue(XWikiApplicationClass.FIELD_DEPENDENCIES, dependencies);
    }

    /**
     * @return the list of other applications on which current application depends.
     */
    public List<String> getApplications()
    {
        return getStringListValue(XWikiApplicationClass.FIELD_APPLICATIONS);
    }

    /**
     * Modify the list of other applications on which current application depends.
     * 
     * @param applications the new list of other applications on which current application depends.
     * @see #getApplications()
     */
    public void setApplications(List<String> applications)
    {
        setStringListValue(XWikiApplicationClass.FIELD_APPLICATIONS, applications);
    }

    /**
     * @return the list of documents application contains. This method return the content of the field "documents", if
     *         you want the real list of documents names with resolved patterns and recursive tools you should use
     *         {@link #getDocumentsNames(boolean, boolean)}.
     * @see #setDocuments(List)
     */
    public List<String> getDocuments()
    {
        return getStringListValue(XWikiApplicationClass.FIELD_DOCUMENTS);
    }

    /**
     * Modify the list of documents application contains.
     * 
     * @param documents the new list of documents application contains.
     * @see #getDocuments()
     */
    public void setDocuments(List<String> documents)
    {
        setStringListValue(XWikiApplicationClass.FIELD_DOCUMENTS, documents);
    }

    /**
     * @return the list of document application contains that will be included in place of copy from wiki template. This
     *         method return the content of the field "docstoinclude", if you want the real list of documents names with
     *         resolved patterns and recursive tools you should use {@link #getDocsNameToInclude(boolean)}.
     * @see #setDocsToInclude(List)
     */
    public List<String> getDocsToInclude()
    {
        return getStringListValue(XWikiApplicationClass.FIELD_DOCSTOINCLUDE);
    }

    /**
     * Modify the list of document application contains that will be included in place of copy from wiki template.
     * 
     * @param docstoinclude the new list of document application contains that will be included in place of copy from a
     *            wiki template.
     * @see #getDocsToInclude()
     */
    public void setDocsToInclude(List<String> docstoinclude)
    {
        setStringListValue(XWikiApplicationClass.FIELD_DOCSTOINCLUDE, docstoinclude);
    }

    /**
     * @return the list of document application contains that will be linked in place of copy from a wiki template. This
     *         method return the content of the field "docstolink", if you want the real list of documents names with
     *         resolved patterns and recursive tools you should use {@link #getDocsNameToLink(boolean)}.
     * @see #setDocsToLink(List)
     */
    public List<String> getDocsToLink()
    {
        return getStringListValue(XWikiApplicationClass.FIELD_DOCSTOLINK);
    }

    /**
     * Modify the list of document application contains that will be included in place of copy from wiki template.
     * 
     * @param docstolink the new list of document application contains that will be linked in place of copy from a wiki
     *            template.
     * @see #getDocsToLink()
     */
    public void setDocsToLink(List<String> docstolink)
    {
        setStringListValue(XWikiApplicationClass.FIELD_DOCSTOLINK, docstolink);
    }

    /**
     * @return the list of documents containing translations strings.
     *         <p>
     *         Theses documents are added to XWiki.XWikiPreferences "documentBundles" field at application installation.
     */
    public List<String> getTranslationDocs()
    {
        return getStringListValue(XWikiApplicationClass.FIELD_TRANSLATIONDOCS);
    }

    /**
     * Modify the list of documents containing translations strings.
     * <p>
     * Theses documents are added to XWiki.XWikiPreferences "documentBundles" field at application installation.
     * 
     * @param translationdocs the new list of documents containing translations strings. Theses documents are added to
     *            XWiki.XWikiPreferences "documentBundles" field at application installation.
     */
    public void setTranslationDocs(List<String> translationdocs)
    {
        setStringListValue(XWikiApplicationClass.FIELD_TRANSLATIONDOCS, translationdocs);
    }

    // ///

    @Override
    public String toString()
    {
        return getAppName() + "-" + getAppVersion();
    }

    @Override
    public int hashCode()
    {
        return getAppName() != null ? getAppName().hashCode() : "".hashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        boolean equals = false;

        if (object instanceof XWikiApplication) {
            equals =
                getAppName() == null ? ((XWikiApplication) object).getAppName() == null : getAppName()
                    .equalsIgnoreCase(((XWikiApplication) object).getAppName());
        } else if (object instanceof String) {
            equals = getAppName() == null ? false : getAppName().equalsIgnoreCase((String) object);
        }

        return equals;
    }

    // ///

    /**
     * Add all applications on which current application depend.
     * 
     * @param rootApplication the root application containing recursively all in <code>applicationList</code>.
     * @param applicationList the applications.
     * @param recurse if true it add recursively all applications dependencies, if false return only direct
     *            dependencies.
     * @param context the XWiki context.
     * @throws XWikiException error when getting application descriptor document from the database.
     * @see #getApplications()
     */
    protected void addXWikiApplicationSet(XWikiApplication rootApplication,
        Collection<XWikiApplication> applicationList, boolean recurse, XWikiContext context) throws XWikiException
    {
        List<String> applications = getApplications();
        for (String appname : applications) {
            // Breaks infinite loop if application contains itself in its dependencies at any level.
            if ((rootApplication == null || !rootApplication.equals(appname)) && !applicationList.contains(appname)) {
                XWikiApplication app = ((XWikiApplicationClass) sclass).getApplication(appname, true, context);
                applicationList.add(app);

                if (recurse) {
                    app.addXWikiApplicationSet(rootApplication, applicationList, recurse, context);
                }
            }
        }
    }

    /**
     * Get set of XWikiApplication containing all applications on which current application depend.
     * 
     * @param recurse if true it add recursively all applications dependencies, if false return only direct
     *            dependencies.
     * @param context the XWiki context.
     * @return the set list of XWikiApplication.
     * @throws XWikiException error when getting application descriptor document from the database.
     * @see #getApplications()
     */
    public Set<XWikiApplication> getXWikiApplicationSet(boolean recurse, XWikiContext context) throws XWikiException
    {
        Set<XWikiApplication> applicationSet = new HashSet<XWikiApplication>();

        addXWikiApplicationSet(this, applicationSet, recurse, context);

        return applicationSet;
    }

    /**
     * Create a HQL where clause containing applications documents filter for provided documents type.
     * 
     * @param applications the applications from which to get filters.
     * @param type the XWikiApplicationClass field where to find documents names list :
     *            {@link XWikiApplicationClass#FIELD_DOCUMENTS}, {@link XWikiApplicationClass#FIELD_DOCSTOINCLUDE},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOLINK}.
     * @param values the HQL values list filled for the named query.
     * @param includeAppDesc if true application descriptor document names are included in the generated filter.
     * @return a HQL where clause containing applications documents filter for provided documents type.
     * @throws XWikiException error when creating HQL filter.
     */
    private static String createApplicationsHqlFilter(Collection<XWikiApplication> applications, String type,
        Collection<Object> values, boolean includeAppDesc) throws XWikiException
    {
        StringBuffer filter = new StringBuffer();

        for (XWikiApplication app : applications) {
            String appFilter = app.createHqlFilter(type, values, false, includeAppDesc);

            if (!appFilter.equals("")) {
                if (filter.length() > 0) {
                    filter.append(HQL_OR);
                }
                filter.append(HQL_GROUP_OPEN);
                filter.append(appFilter);
                filter.append(HQL_GROUP_CLOSE);
            }
        }

        return filter.toString();
    }

    /**
     * Create a HQL where clause containing application documents filter for provided documents type.
     * 
     * @param type the XWikiApplicationClass field where to find documents names list :
     *            {@link XWikiApplicationClass#FIELD_DOCUMENTS}, {@link XWikiApplicationClass#FIELD_DOCSTOINCLUDE},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOLINK}.
     * @param values the HQL values list filled for the named query.
     * @param recurse indicate if dependencies applications filters are included in the generated filter.
     * @param includeAppDesc if true application descriptor document names are included in the generated filter.
     * @return a HQL where clause containing application documents filter for provided documents type.
     * @throws XWikiException error when creating HQL filter.
     */
    private String createHqlFilter(String type, Collection<Object> values, boolean recurse, boolean includeAppDesc)
        throws XWikiException
    {
        StringBuffer filter = new StringBuffer();

        List<String> patterns = getStringListValue(type);

        if (!patterns.isEmpty()) {
            // Filter with applications documents
            if (!type.equals(XWikiApplicationClass.FIELD_DOCUMENTS)) {
                filter.append(HQL_GROUP_OPEN);
                filter.append(createHqlFilter(getDocuments(), values, false));
                filter.append(HQL_GROUP_CLOSE);
            }

            // Filter with provided applications documents type
            String typeFilter = createHqlFilter(getStringListValue(type), values, includeAppDesc);

            if (!typeFilter.equals("")) {
                if (filter.length() > 0) {
                    filter.append(HQL_AND);
                }

                filter.append(HQL_GROUP_OPEN);
                filter.append(typeFilter);
                filter.append(HQL_GROUP_CLOSE);
            }
        }

        // Add dependencies applications hql filters for provided type
        if (recurse) {
            Collection<XWikiApplication> applications = getXWikiApplicationSet(true, context);

            String dependenciesFilter = createApplicationsHqlFilter(applications, type, values, includeAppDesc);

            if (!dependenciesFilter.equals("")) {
                if (filter.length() > 0) {
                    filter.append(HQL_OR);
                }
                filter.append(dependenciesFilter);
            }
        }

        return filter.toString();
    }

    /**
     * Convert provided filter list in one hql where clause.
     * 
     * @param docsNamesToResolve the application filters.
     * @param values the HQL values list filled for the named query.
     * @param includeAppDesc if true application descriptor document names are included in the generated filter.
     * @return a HQL where clause containing application documents filter for provided documents type.
     */
    private String createHqlFilter(Collection<String> docsNamesToResolve, Collection<Object> values,
        boolean includeAppDesc)
    {
        StringBuffer filter = new StringBuffer();

        if (includeAppDesc) {
            filter.append(HQL_FILTER_DOC_EQUALS);
            values.add(this.getFullName());
        }

        for (String docName : docsNamesToResolve) {
            if (filter.length() > 0) {
                filter.append(HQL_OR);
            }

            Matcher matcher = EXT_DOCNAME_PATTERN.matcher(docName);
            if (matcher.matches()) {
                // Add a pattern
                filter.append(HQL_FILTER_DOC_PATTERN);
                values.add(matcher.group(1));
            } else {
                // Add a document name
                filter.append(HQL_FILTER_DOC_EQUALS);
                values.add(docName);
            }
        }

        return filter.toString();
    }

    /**
     * Get and resolve all documents names of type <code>type</code> application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching string to use with
     * "like".
     * 
     * @param type type the XWikiApplicationClass field where to find documents names list :
     *            {@link XWikiApplicationClass#FIELD_DOCUMENTS}, {@link XWikiApplicationClass#FIELD_DOCSTOINCLUDE},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOLINK}.
     * @param recurse if true it follow recursively all applications dependencies, if false parse only direct
     *            dependencies.
     * @param includeAppDesc if true application descriptor document names is added to the returned set.
     * @return all documents names of type <code>type</code> application contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li> <li>or getting applications dependencies descriptors documents from
     *             the database.</li>
     *             </ul>
     */
    private Set<String> getDocsNamesByType(String type, boolean recurse, boolean includeAppDesc) throws XWikiException
    {
        List<Object> values = new ArrayList<Object>();

        String where = createHqlFilter(type, values, recurse, includeAppDesc);

        return where.equals("") ? Collections.<String> emptySet() : new HashSet<String>(context.getWiki().getStore()
            .searchDocumentsNames(HQL_WHERE + " " + where, values, context));
    }

    /**
     * Get and resolve all documents names of type <code>type</code> provided applications contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching string to use with
     * "like".
     * 
     * @param applications the applications from which to get documents names.
     * @param type type the XWikiApplicationClass field where to find documents names list :
     *            {@link XWikiApplicationClass#FIELD_DOCUMENTS}, {@link XWikiApplicationClass#FIELD_DOCSTOINCLUDE},
     *            {@link XWikiApplicationClass#FIELD_DOCSTOLINK}.
     * @param includeAppDesc if true application descriptor document names is added to the returned set.
     * @return all documents names of type <code>type</code> provided applications contains.
     * @throws XWikiException error when resolving SQL matching.
     */
    private static Set<String> getApplicationsDocsNamesByType(Collection<XWikiApplication> applications, String type,
        boolean includeAppDesc) throws XWikiException
    {
        Set<String> set = Collections.emptySet();
        if (applications.size() > 0) {
            List<Object> values = new ArrayList<Object>();

            String where = createApplicationsHqlFilter(applications, type, values, includeAppDesc);

            XWikiApplication app = applications.iterator().next();

            if (where.equals("")) {
                set = Collections.emptySet();
            } else {
                set =
                    new HashSet<String>(app.context.getWiki().getStore()
                        .searchDocumentsNames(HQL_WHERE + " " + where, values, app.context));
            }
        }

        return set;
    }

    /**
     * Get and resolve all documents names application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching string to use with
     * "like".
     * 
     * @param recurse if true it follow recursively all applications dependencies, if false parse only direct
     *            dependencies.
     * @param includeAppDesc if true application descriptor document names is added to <code>docsNames</code>.
     * @return all documents names application contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li> <li>or getting applications dependencies descriptors documents from
     *             the database.</li>
     *             </ul>
     * @see #getDocuments()
     * @see XWikiApplicationClass#FIELD_DOCUMENTS
     */
    public Set<String> getDocumentsNames(boolean recurse, boolean includeAppDesc) throws XWikiException
    {
        return getDocsNamesByType(XWikiApplicationClass.FIELD_DOCUMENTS, recurse, includeAppDesc);
    }

    /**
     * Get and resolve all documents names to include application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching string to use with
     * "like".
     * 
     * @param recurse if true it follow recursively all applications dependencies, if false parse only direct
     *            dependencies.
     * @return all documents names to include application contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li> <li>or getting applications dependencies descriptors documents from
     *             the database.</li>
     *             </ul>
     * @see #getDocsToInclude()
     * @see XWikiApplicationClass#FIELD_DOCSTOINCLUDE
     */
    public Set<String> getDocsNameToInclude(boolean recurse) throws XWikiException
    {
        return getDocsNamesByType(XWikiApplicationClass.FIELD_DOCSTOINCLUDE, recurse, false);
    }

    /**
     * Get and resolve all documents names to include <code>applications</code> XWikiApplication list contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching string to use with
     * "like".
     * 
     * @param applications the applications containing documents names to resolve and add to <code>docsNames</code>.
     * @return all documents names to include <code>applications</code> contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li> <li>or getting applications dependencies descriptors documents from
     *             the database.</li>
     *             </ul>
     * @see #getDocsToInclude()
     * @see XWikiApplicationClass#FIELD_DOCSTOINCLUDE
     */
    public static Set<String> getDocsNameToInclude(Collection<XWikiApplication> applications) throws XWikiException
    {
        return getApplicationsDocsNamesByType(applications, XWikiApplicationClass.FIELD_DOCSTOINCLUDE, false);
    }

    /**
     * Get and resolve all documents names to link application contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching string to use with
     * "like".
     * 
     * @param recurse if true it follow recursively all applications dependencies, if false parse only direct
     *            dependencies.
     * @return all documents names to link application contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li> <li>or getting applications dependencies descriptors documents from
     *             the database.</li>
     *             </ul>
     * @see #getDocsToLink()
     * @see XWikiApplicationClass#FIELD_DOCSTOLINK
     */
    public Set<String> getDocsNameToLink(boolean recurse) throws XWikiException
    {
        return getDocsNamesByType(XWikiApplicationClass.FIELD_DOCSTOLINK, recurse, false);
    }

    /**
     * Get and resolve all documents names to link <code>applications</code> XWikiApplication list contains.
     * <p>
     * For each of these documents names, if are between "[" and "]", are considered as SQL matching string to use with
     * "like".
     * 
     * @param applications the applications containing documents names to resolve and add to <code>docsNames</code>.
     * @return all documents names to link <code>applications</code> contains.
     * @throws XWikiException error when:
     *             <ul>
     *             <li>resolving SQL matching.</li> <li>or getting applications dependencies descriptors documents from
     *             the database.</li>
     *             </ul>
     * @see #getDocsToLink()
     * @see XWikiApplicationClass#FIELD_DOCSTOLINK
     */
    public static Set<String> getDocsNameToLink(Collection<XWikiApplication> applications) throws XWikiException
    {
        return getApplicationsDocsNamesByType(applications, XWikiApplicationClass.FIELD_DOCSTOLINK, false);
    }
}

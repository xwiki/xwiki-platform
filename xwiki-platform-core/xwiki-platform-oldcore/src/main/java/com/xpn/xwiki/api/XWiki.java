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
package com.xpn.xwiki.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.diff.delta.Chunk;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Programming;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;

public class XWiki extends Api
{
    /** Logging helper object. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(XWiki.class);

    /** The internal object wrapped by this API. */
    private com.xpn.xwiki.XWiki xwiki;

    /**
     * @see #getStatsService()
     */
    private StatsService statsService;

    /**
     * @see #getCriteriaService()
     */
    private CriteriaService criteriaService;

    /**
     * @see com.xpn.xwiki.internal.model.reference.CurrentMixedStringDocumentReferenceResolver
     */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.class, "currentmixed");

    /**
     * @see org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver
     */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> defaultDocumentReferenceResolver = Utils
        .getComponent(DocumentReferenceResolver.class);

    /**
     * The object used to serialize entity references into strings. We need it because we have script APIs that work
     * with entity references but have to call older, often internal, methods that still use string references.
     */
    @SuppressWarnings("unchecked")
    private EntityReferenceSerializer<String> defaultStringEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.class);

    /**
     * XWiki API Constructor
     * 
     * @param xwiki XWiki Main Object to wrap
     * @param context XWikiContext to wrap
     */
    public XWiki(com.xpn.xwiki.XWiki xwiki, XWikiContext context)
    {
        super(context);

        this.xwiki = xwiki;
        this.statsService = new StatsService(context);
        this.criteriaService = new CriteriaService(context);
    }

    /**
     * Privileged API allowing to access the underlying main XWiki Object
     * 
     * @return Privileged Main XWiki Object
     */
    @Programming
    public com.xpn.xwiki.XWiki getXWiki()
    {
        if (hasProgrammingRights()) {
            return this.xwiki;
        }

        return null;
    }

    /**
     * @return XWiki's version in the format <code>(version).(SVN build number)</code>, or "Unknown version" if it
     *         failed to be retrieved
     */
    public String getVersion()
    {
        return this.xwiki.getVersion();
    }

    /**
     * API Allowing to access the current request URL being requested
     * 
     * @return URL
     * @throws XWikiException
     */
    public String getRequestURL() throws XWikiException
    {
        return getXWikiContext().getURLFactory().getRequestURL(getXWikiContext()).toString();
    }

    /**
     * Loads an Document from the database. Rights are checked before sending back the document.
     * 
     * @param fullName the full name of the XWiki document to be loaded
     * @return a Document object or null if it is not accessible
     * @throws XWikiException
     */
    public Document getDocument(String fullName) throws XWikiException
    {
        DocumentReference reference;

        // We ignore the passed full name if it's null to be backward compatible with previous behaviors.
        if (fullName != null) {
            // Note: We use the CurrentMixed Resolver since we want to use the default page name if the page isn't
            // specified in the passed string, rather than use the current document's page name.
            reference = this.currentMixedDocumentReferenceResolver.resolve(fullName);
        } else {
            reference = this.defaultDocumentReferenceResolver.resolve("");
        }

        return getDocument(reference);
    }

    /**
     * Loads an Document from the database. Rights are checked before sending back the document.
     * 
     * @param reference the reference of the XWiki document to be loaded
     * @return a Document object or null if it is not accessible
     * @throws XWikiException
     * @since 2.3M1
     */
    public Document getDocument(DocumentReference reference) throws XWikiException
    {
        try {
            XWikiDocument doc = this.xwiki.getDocument(reference, getXWikiContext());
            if (this.xwiki.getRightService().hasAccessLevel("view", getXWikiContext().getUser(), doc.getFullName(),
                getXWikiContext()) == false) {
                return null;
            }

            Document newdoc = doc.newDocument(getXWikiContext());
            return newdoc;
        } catch (Exception ex) {
            LOGGER.warn("Failed to access document " + reference + ": " + ex.getMessage());
            return new Document(new XWikiDocument(reference), getXWikiContext());
        }
    }

    /**
     * Loads an Document from the database. Rights are checked on the author (contentAuthor) of the document containing
     * the currently executing script before sending back the loaded document.
     * 
     * @param fullName the full name of the XWiki document to be loaded
     * @return a Document object or null if it is not accessible
     * @throws XWikiException
     * @since 2.3M2
     */
    public Document getDocumentAsAuthor(String fullName) throws XWikiException
    {
        DocumentReference reference;

        // We ignore the passed full name if it's null to match behavior of getDocument
        if (fullName != null) {
            // Note: We use the CurrentMixed Resolver since we want to use the default page name if the page isn't
            // specified in the passed string, rather than use the current document's page name.
            reference = this.currentMixedDocumentReferenceResolver.resolve(fullName);
        } else {
            reference = this.defaultDocumentReferenceResolver.resolve("");
        }

        return getDocumentAsAuthor(reference);
    }

    /**
     * Loads an Document from the database. Rights are checked on the author (contentAuthor) of the document containing
     * the currently executing script before sending back the loaded document.
     * 
     * @param reference the reference of the XWiki document to be loaded
     * @return a Document object or null if it is not accessible
     * @throws XWikiException
     * @since 2.3M2
     */
    public Document getDocumentAsAuthor(DocumentReference reference) throws XWikiException
    {
        String author = this.getEffectiveScriptAuthorName();
        XWikiDocument doc = this.xwiki.getDocument(reference, getXWikiContext());
        if (this.xwiki.getRightService().hasAccessLevel("view", author, doc.getFullName(), getXWikiContext()) == false) {
            return null;
        }

        Document newdoc = doc.newDocument(getXWikiContext());
        return newdoc;
    }

    /**
     * @param fullname the {@link XWikiDocument#getFullName() name} of the document to search for.
     * @param lang an optional {@link XWikiDocument#getLanguage() language} to filter results.
     * @return A list with all the deleted versions of a document in the recycle bin.
     * @throws XWikiException if any error
     */
    public List<DeletedDocument> getDeletedDocuments(String fullname, String lang) throws XWikiException
    {
        XWikiDeletedDocument[] dds = this.xwiki.getDeletedDocuments(fullname, lang, this.context);
        if (dds == null || dds.length == 0) {
            return Collections.emptyList();
        }
        List<DeletedDocument> result = new ArrayList<DeletedDocument>(dds.length);
        for (int i = 0; i < dds.length; i++) {
            result.add(new DeletedDocument(dds[i], this.context));
        }
        return result;
    }

    /**
     * @return specified documents in recycle bin
     * @param fullname - {@link XWikiDocument#getFullName()}
     * @param lang - {@link XWikiDocument#getLanguage()}
     * @throws XWikiException if any error
     */
    public DeletedDocument getDeletedDocument(String fullname, String lang, String index) throws XWikiException
    {
        if (!NumberUtils.isDigits(index)) {
            return null;
        }
        XWikiDeletedDocument dd = this.xwiki.getDeletedDocument(fullname, lang, Integer.parseInt(index), this.context);
        if (dd == null) {
            return null;
        }

        return new DeletedDocument(dd, this.context);
    }

    /**
     * Retrieve all the deleted attachments that belonged to a certain document. Note that this does not distinguish
     * between different incarnations of a document name, and it does not require that the document still exists, it
     * returns all the attachments that at the time of their deletion had a document with the specified name as their
     * owner.
     * 
     * @param docName the {@link XWikiDocument#getFullName() name} of the owner document
     * @return A list with all the deleted attachments which belonged to the specified document. If no such attachments
     *         are found in the trash, an empty list is returned.
     */
    public List<DeletedAttachment> getDeletedAttachments(String docName)
    {
        try {
            List<com.xpn.xwiki.doc.DeletedAttachment> attachments =
                this.xwiki.getDeletedAttachments(docName, this.context);
            if (attachments == null || attachments.isEmpty()) {
                attachments = Collections.emptyList();
            }
            List<DeletedAttachment> result = new ArrayList<DeletedAttachment>(attachments.size());
            for (com.xpn.xwiki.doc.DeletedAttachment attachment : attachments) {
                result.add(new DeletedAttachment(attachment, this.context));
            }
            return result;
        } catch (Exception ex) {
            LOGGER.warn("Failed to retrieve deleted attachments", ex);
        }
        return Collections.emptyList();
    }

    /**
     * Retrieve all the deleted attachments that belonged to a certain document and had the specified name. Multiple
     * versions can be returned since the same file can be uploaded and deleted several times, creating different
     * instances in the trash. Note that this does not distinguish between different incarnations of a document name,
     * and it does not require that the document still exists, it returns all the attachments that at the time of their
     * deletion had a document with the specified name as their owner.
     * 
     * @param docName the {@link DeletedAttachment#getDocName() name of the document} the attachment belonged to
     * @param filename the {@link DeletedAttachment#getFilename() name} of the attachment to search for
     * @return A list with all the deleted attachments which belonged to the specified document and had the specified
     *         filename. If no such attachments are found in the trash, an empty list is returned.
     */
    public List<DeletedAttachment> getDeletedAttachments(String docName, String filename)
    {
        try {
            List<com.xpn.xwiki.doc.DeletedAttachment> attachments =
                this.xwiki.getDeletedAttachments(docName, filename, this.context);
            if (attachments == null) {
                attachments = Collections.emptyList();
            }
            List<DeletedAttachment> result = new ArrayList<DeletedAttachment>(attachments.size());
            for (com.xpn.xwiki.doc.DeletedAttachment attachment : attachments) {
                result.add(new DeletedAttachment(attachment, this.context));
            }
            return result;
        } catch (Exception ex) {
            LOGGER.warn("Failed to retrieve deleted attachments", ex);
        }
        return Collections.emptyList();
    }

    /**
     * Retrieve a specific attachment from the trash.
     * 
     * @param id the unique identifier of the entry in the trash
     * @return specified attachment from the trash, {@code null} if not found
     */
    public DeletedAttachment getDeletedAttachment(String id)
    {
        try {
            com.xpn.xwiki.doc.DeletedAttachment attachment = this.xwiki.getDeletedAttachment(id, this.context);
            if (attachment != null) {
                return new DeletedAttachment(attachment, this.context);
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to retrieve deleted attachment", ex);
        }
        return null;
    }

    /**
     * Returns whether a document exists or not
     * 
     * @param fullname Fullname of the XWiki document to be loaded
     * @return true if the document exists, false if not
     * @throws XWikiException
     */
    public boolean exists(String fullname) throws XWikiException
    {
        return this.xwiki.exists(fullname, getXWikiContext());
    }

    /**
     * Returns whether a document exists or not
     * 
     * @param reference the reference of the document to check for its existence
     * @return true if the document exists, false if not
     * @since 2.3M2
     */
    public boolean exists(DocumentReference reference) throws XWikiException
    {
        return this.xwiki.exists(reference, getXWikiContext());
    }

    /**
     * Verify the rights the current user has on a document. If the document requires rights and the user is not
     * authenticated he will be redirected to the login page.
     * 
     * @param docname fullname of the document
     * @param right right to check ("view", "edit", "admin", "delete")
     * @return true if it exists
     */
    public boolean checkAccess(String docname, String right)
    {
        try {
            XWikiDocument doc = getXWikiContext().getWiki().getDocument(docname, this.context);
            return getXWikiContext().getWiki().checkAccess(right, doc, getXWikiContext());
        } catch (XWikiException e) {
            return false;
        }
    }

    /**
     * Loads an Document from the database. Rights are checked before sending back the document.
     * 
     * @param space Space to use in case no space is defined in the provided <code>fullname</code>
     * @param fullname the full name or relative name of the document to load
     * @return a Document object or null if it is not accessible
     * @throws XWikiException
     */
    public Document getDocument(String space, String fullname) throws XWikiException
    {
        XWikiDocument doc = this.xwiki.getDocument(space, fullname, getXWikiContext());
        if (this.xwiki.getRightService().hasAccessLevel("view", getXWikiContext().getUser(), doc.getFullName(),
            getXWikiContext()) == false) {
            return null;
        }

        return doc.newDocument(getXWikiContext());
    }

    /**
     * Load a specific revision of a document
     * 
     * @param doc Document for which to load a specific revision
     * @param rev Revision number
     * @return Specific revision of a document
     * @throws XWikiException
     */
    public Document getDocument(Document doc, String rev) throws XWikiException
    {
        if ((doc == null) || (doc.getDoc() == null)) {
            return null;
        }

        if (this.xwiki.getRightService().hasAccessLevel("view", getXWikiContext().getUser(), doc.getFullName(),
            getXWikiContext()) == false) {
            // Finally we return null, otherwise showing search result is a real pain
            return null;
        }

        try {
            XWikiDocument revdoc = this.xwiki.getDocument(doc.getDoc(), rev, getXWikiContext());
            return revdoc.newDocument(getXWikiContext());
        } catch (Exception e) {
            // Can't read versioned document
            LOGGER.error("Failed to read versioned document", e);

            return null;
        }
    }

    /**
     * Output content in the edit content textarea
     * 
     * @param content content to output
     * @return the textarea text content
     */
    public String getTextArea(String content)
    {
        return com.xpn.xwiki.XWiki.getTextArea(content, getXWikiContext());
    }

    /**
     * Get the list of available classes in the wiki
     * 
     * @return list of classes names
     * @throws XWikiException
     */
    public List<String> getClassList() throws XWikiException
    {
        return this.xwiki.getClassList(getXWikiContext());
    }

    /**
     * Get the global MetaClass object
     * 
     * @return MetaClass object
     */
    public MetaClass getMetaclass()
    {
        return this.xwiki.getMetaclass();
    }

    /**
     * Privileged API allowing to run a search on the database returning a list of data This search is send to the store
     * engine (Hibernate HQL, JCR XPATH or other)
     * 
     * @param wheresql Query to be run (HQL, XPath)
     * @return A list of rows (Object[])
     * @throws XWikiException
     */
    public <T> List<T> search(String wheresql) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.search(wheresql, getXWikiContext());
        }

        return Collections.emptyList();
    }

    /**
     * Privileged API allowing to run a search on the database returning a list of data. The HQL where clause uses
     * parameters (question marks) instead of values, and the actual values are passed in the parameters list. This
     * allows generating a query which will automatically encode the passed values (like escaping single quotes). This
     * API is recommended to be used over the other similar methods where the values are passed inside the where clause
     * and for which manual encoding/escaping is needed to avoid SQL injections or bad queries.
     * 
     * @param parameterizedWhereClause query to be run (HQL)
     * @param parameterValues the where clause values that replace the question marks
     * @return a list of rows, where each row has either the selected data type ({@link XWikiDocument}, {@code String},
     *         {@code Integer}, etc.), or {@code Object[]} if more than one column was selected
     * @throws XWikiException
     */
    public <T> List<T> search(String parameterizedWhereClause, List< ? > parameterValues) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.getStore().search(parameterizedWhereClause, 0, 0, parameterValues, getXWikiContext());
        }

        return Collections.emptyList();
    }

    /**
     * Privileged API allowing to run a search on the database returning a list of data. This search is sent to the
     * store engine (Hibernate HQL, JCR XPATH or other)
     * 
     * @param wheresql Query to be run (HQL, XPath)
     * @param nb return only 'nb' rows
     * @param start skip the 'start' first elements
     * @return A list of rows (Object[])
     * @throws XWikiException
     */
    public <T> List<T> search(String wheresql, int nb, int start) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.search(wheresql, nb, start, getXWikiContext());
        }

        return Collections.emptyList();
    }

    /**
     * Privileged API allowing to run a search on the database returning a list of data. The HQL where clause uses
     * parameters (question marks) instead of values, and the actual values are passed in the paremeters list. This
     * allows generating a query which will automatically encode the passed values (like escaping single quotes). This
     * API is recommended to be used over the other similar methods where the values are passed inside the where clause
     * and for which manual encoding/escaping is needed to avoid sql injections or bad queries.
     * 
     * @param parameterizedWhereClause query to be run (HQL)
     * @param maxResults maximum number of results to return; if 0 all results are returned
     * @param startOffset skip the first N results; if 0 no items are skipped
     * @param parameterValues the where clause values that replace the question marks
     * @return a list of rows, where each row has either the selected data type ({@link XWikiDocument}, {@code String},
     *         {@code Integer}, etc.), or {@code Object[]} if more than one column was selected
     * @throws XWikiException
     */
    public <T> List<T> search(String parameterizedWhereClause, int maxResults, int startOffset,
        List< ? > parameterValues) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.getStore().search(parameterizedWhereClause, maxResults, startOffset, parameterValues,
                getXWikiContext());
        }

        return Collections.emptyList();
    }

    /**
     * API allowing to search for document names matching a query. Examples:
     * <ul>
     * <li>Query: <code>where doc.space='Main' order by doc.creationDate desc</code>. Result: All the documents in space
     * 'Main' ordered by the creation date from the most recent</li>
     * <li>Query: <code>where doc.name like '%sport%' order by doc.name asc</code>. Result: All the documents containing
     * 'sport' in their name ordered by document name</li>
     * <li>Query: <code>where doc.content like '%sport%' order by doc.author</code> Result: All the documents containing
     * 'sport' in their content ordered by the author</li>
     * <li>Query: <code>where doc.creator = 'XWiki.LudovicDubost' order by doc.creationDate
     *       desc</code>. Result: All the documents with creator LudovicDubost ordered by the creation date from the
     * most recent</li>
     * <li>Query: <code>where doc.author = 'XWiki.LudovicDubost' order by doc.date desc</code>. Result: All the
     * documents with last author LudovicDubost ordered by the last modification date from the most recent.</li>
     * <li>Query: <code>,BaseObject as obj where doc.fullName=obj.name and
     *       obj.className='XWiki.XWikiComments' order by doc.date desc</code>. Result: All the documents with at least
     * one comment ordered by the last modification date from the most recent</li>
     * <li>Query: <code>,BaseObject as obj, StringProperty as prop where
     *       doc.fullName=obj.name and obj.className='XWiki.XWikiComments' and obj.id=prop.id.id
     *       and prop.id.name='author' and prop.value='XWiki.LudovicDubost' order by doc.date
     *       desc</code>. Result: All the documents with at least one comment from LudovicDubost ordered by the last
     * modification date from the most recent</li>
     * </ul>
     * 
     * @param wheresql Query to be run (either starting with ", BaseObject as obj where.." or by "where ..."
     * @return List of document names matching (Main.Page1, Main.Page2)
     * @throws XWikiException
     */
    public List<String> searchDocuments(String wheresql) throws XWikiException
    {
        return this.xwiki.getStore().searchDocumentsNames(wheresql, getXWikiContext());
    }

    /**
     * API allowing to count the total number of documents that would be returned by a query.
     * 
     * @param wheresql Query to use, similar to the ones accepted by {@link #searchDocuments(String)}. If possible, it
     *            should not contain <code>order by</code> or <code>group</code> clauses, since this kind of queries are
     *            not portable.
     * @return The number of documents that matched the query.
     * @throws XWikiException if there was a problem executing the query.
     */
    public int countDocuments(String wheresql) throws XWikiException
    {
        return this.xwiki.getStore().countDocuments(wheresql, getXWikiContext());
    }

    /**
     * API allowing to search for document names matching a query return only a limited number of elements and skipping
     * the first rows. The query part is the same as searchDocuments
     * 
     * @param wheresql query to use similar to searchDocuments(wheresql)
     * @param nb return only 'nb' rows
     * @param start skip the first 'start' rows
     * @return List of document names matching
     * @throws XWikiException
     * @see List searchDocuments(String where sql)
     */
    public List<String> searchDocuments(String wheresql, int nb, int start) throws XWikiException
    {
        return this.xwiki.getStore().searchDocumentsNames(wheresql, nb, start, getXWikiContext());
    }

    /**
     * Privileged API allowing to search for document names matching a query return only a limited number of elements
     * and skipping the first rows. The return values contain the list of columns specified in addition to the document
     * space and name The query part is the same as searchDocuments
     * 
     * @param wheresql query to use similar to searchDocuments(wheresql)
     * @param nb return only 'nb' rows
     * @param start skip the first 'start' rows
     * @param selectColumns List of columns to add to the result
     * @return List of Object[] with the column values of the matching rows
     * @throws XWikiException
     */
    public List<String> searchDocuments(String wheresql, int nb, int start, String selectColumns) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.getStore().searchDocumentsNames(wheresql, nb, start, selectColumns, getXWikiContext());
        }

        return Collections.emptyList();
    }

    /**
     * API allowing to search for documents allowing to have mutliple entries per language
     * 
     * @param wheresql query to use similar to searchDocuments(wheresql)
     * @param distinctbylanguage true to return multiple rows per language
     * @return List of Document object matching
     * @throws XWikiException
     */
    public List<Document> searchDocuments(String wheresql, boolean distinctbylanguage) throws XWikiException
    {
        return convert(this.xwiki.getStore().searchDocuments(wheresql, distinctbylanguage, getXWikiContext()));
    }

    /**
     * API allowing to search for documents allowing to have multiple entries per language
     * 
     * @param wheresql query to use similar to searchDocuments(wheresql)
     * @param distinctbylanguage true to return multiple rows per language
     * @return List of Document object matching
     * @param nb return only 'nb' rows
     * @param start skip the first 'start' rows
     * @throws XWikiException
     */
    public List<Document> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start)
        throws XWikiException
    {
        return convert(this.xwiki.getStore()
            .searchDocuments(wheresql, distinctbylanguage, nb, start, getXWikiContext()));
    }

    /**
     * Search documents by passing HQL where clause values as parameters. This allows generating a Named HQL query which
     * will automatically encode the passed values (like escaping single quotes). This API is recommended to be used
     * over the other similar methods where the values are passed inside the where clause and for which you'll need to
     * do the encoding/escaping yourself before calling them.
     * <p>
     * Example
     * </p>
     * 
     * <pre>
     * &lt;code&gt;
     * #set($orphans = $xwiki.searchDocuments(&quot; where doc.fullName &lt;&gt; ? and (doc.parent = ? or &quot;
     *     + &quot;(doc.parent = ? and doc.space = ?))&quot;,
     *     [&quot;${doc.fullName}as&quot;, ${doc.fullName}, ${doc.name}, ${doc.space}]))
     * &lt;/code&gt;
     * </pre>
     * 
     * @param parameterizedWhereClause the HQL where clause. For example <code>" where doc.fullName
     *        <> ? and (doc.parent = ? or (doc.parent = ? and doc.space = ?))"</code>
     * @param maxResults the number of rows to return. If 0 then all rows are returned
     * @param startOffset the number of rows to skip. If 0 don't skip any row
     * @param parameterValues the where clause values that replace the question marks (?)
     * @return a list of document names
     * @throws XWikiException in case of error while performing the query
     */
    public List<String> searchDocuments(String parameterizedWhereClause, int maxResults, int startOffset,
        List< ? > parameterValues) throws XWikiException
    {
        return this.xwiki.getStore().searchDocumentsNames(parameterizedWhereClause, maxResults, startOffset,
            parameterValues, getXWikiContext());
    }

    /**
     * Same as {@link #searchDocuments(String, int, int, java.util.List)} but returns all rows.
     * 
     * @see #searchDocuments(String, int, int, java.util.List)
     */
    public List<String> searchDocuments(String parameterizedWhereClause, List< ? > parameterValues)
        throws XWikiException
    {
        return this.xwiki.getStore().searchDocumentsNames(parameterizedWhereClause, parameterValues, getXWikiContext());
    }

    /**
     * API allowing to count the total number of documents that would be returned by a parameterized query.
     * 
     * @param parameterizedWhereClause the parameterized query to use, similar to the ones accepted by
     *            {@link #searchDocuments(String, List)}. If possible, it should not contain <code>order by</code> or
     *            <code>group</code> clauses, since this kind of queries are not portable.
     * @param parameterValues The parameter values that replace the question marks.
     * @return The number of documents that matched the query.
     * @throws XWikiException if there was a problem executing the query.
     */
    public int countDocuments(String parameterizedWhereClause, List< ? > parameterValues) throws XWikiException
    {
        return this.xwiki.getStore().countDocuments(parameterizedWhereClause, parameterValues, getXWikiContext());
    }

    /**
     * Search documents in the provided wiki by passing HQL where clause values as parameters. See
     * {@link #searchDocuments(String, int, int, java.util.List)} for more details.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param parameterizedWhereClause the HQL where clause. For example <code>" where doc.fullName
     *        <> ? and (doc.parent = ? or (doc.parent = ? and doc.space = ?))"</code>
     * @param maxResults the number of rows to return. If 0 then all rows are returned
     * @param startOffset the number of rows to skip. If 0 don't skip any row
     * @param parameterValues the where clause values that replace the question marks (?)
     * @return a list of document full names (Space.Name).
     * @see #searchDocuments(String, int, int, java.util.List)
     * @throws XWikiException in case of error while performing the query
     */
    public List<String> searchDocumentsNames(String wikiName, String parameterizedWhereClause, int maxResults,
        int startOffset, List< ? > parameterValues) throws XWikiException
    {
        String database = this.context.getDatabase();

        try {
            this.context.setDatabase(wikiName);

            return searchDocuments(parameterizedWhereClause, maxResults, startOffset, parameterValues);
        } finally {
            this.context.setDatabase(database);
        }
    }

    /**
     * Search spaces by passing HQL where clause values as parameters. See
     * {@link #searchDocuments(String, int, int, List)} for more about parameterized hql clauses.
     * 
     * @param parametrizedSqlClause the HQL where clause. For example <code>" where doc.fullName
     *        <> ? and (doc.parent = ? or (doc.parent = ? and doc.space = ?))"</code>
     * @param nb the number of rows to return. If 0 then all rows are returned
     * @param start the number of rows to skip. If 0 don't skip any row
     * @param parameterValues the where clause values that replace the question marks (?)
     * @return a list of spaces names.
     * @throws XWikiException in case of error while performing the query
     */
    public List<String> searchSpacesNames(String parametrizedSqlClause, int nb, int start, List< ? > parameterValues)
        throws XWikiException
    {
        return this.xwiki.getStore().search(
            "select distinct doc.space from XWikiDocument doc " + parametrizedSqlClause, nb, start, parameterValues,
            this.context);
    }

    /**
     * Function to wrap a list of XWikiDocument into Document objects
     * 
     * @param docs list of XWikiDocument
     * @return list of Document objects
     */
    public List<Document> wrapDocs(List< ? > docs)
    {
        List<Document> result = new ArrayList<Document>();
        if (docs != null) {
            for (java.lang.Object obj : docs) {
                try {
                    if (obj instanceof XWikiDocument) {
                        XWikiDocument doc = (XWikiDocument) obj;
                        Document wrappedDoc = doc.newDocument(getXWikiContext());
                        result.add(wrappedDoc);
                    } else if (obj instanceof Document) {
                        result.add((Document) obj);
                    } else if (obj instanceof String) {
                        Document doc = getDocument(obj.toString());
                        if (doc != null) {
                            result.add(doc);
                        }
                    }
                } catch (XWikiException ex) {
                }
            }
        }

        return result;
    }

    /**
     * API allowing to parse a text content to evaluate velocity scripts
     * 
     * @param content
     * @return evaluated content if the content contains velocity scripts
     */
    public String parseContent(String content)
    {
        return this.xwiki.parseContent(content, getXWikiContext());
    }

    /**
     * API to parse a velocity template provided by the current Skin The template is first looked in the skin active for
     * the user, the space or the wiki. If the template does not exist in that skin, the template is looked up in the
     * "parent skin" of the skin
     * 
     * @param template Template name ("view", "edit", "comment")
     * @return Evaluated content from the template
     */
    public String parseTemplate(String template)
    {
        return this.xwiki.parseTemplate(template, getXWikiContext());
    }

    /**
     * API to render a velocity template provided by the current Skin The template is first looked in the skin active
     * for the user, the space or the wiki. If the template does not exist in that skin, the template is looked up in
     * the "parent skin" of the skin
     * 
     * @param template Template name ("view", "edit", "comment")
     * @return Evaluated content from the template
     */
    public String renderTemplate(String template)
    {
        return this.xwiki.renderTemplate(template, getXWikiContext());
    }

    /**
     * Designed to include dynamic content, such as Servlets or JSPs, inside Velocity templates; works by creating a
     * RequestDispatcher, buffering the output, then returning it as a string.
     * 
     * @param url URL of the servlet
     * @return text result of the servlet
     */
    public String invokeServletAndReturnAsString(String url)
    {
        return this.xwiki.invokeServletAndReturnAsString(url, getXWikiContext());
    }

    /**
     * Return the URL of the static file provided by the current skin The file is first looked in the skin active for
     * the user, the space or the wiki. If the file does not exist in that skin, the file is looked up in the "parent
     * skin" of the skin. The file can be a CSS file, an image file, a javascript file, etc.
     * 
     * @param filename Filename to be looked up in the skin (logo.gif, style.css)
     * @return URL to access this file
     */
    public String getSkinFile(String filename)
    {
        return this.xwiki.getSkinFile(filename, getXWikiContext());
    }

    /**
     * Return the URL of the static file provided by the current skin The file is first looked in the skin active for
     * the user, the space or the wiki. If the file does not exist in that skin, the file is looked up in the "parent
     * skin" of the skin. The file can be a CSS file, an image file, a javascript file, etc.
     * 
     * @param filename Filename to be looked up in the skin (logo.gif, style.css)
     * @param forceSkinAction true to make sure that static files are retrieved through the skin action, to allow
     *            parsing of velocity on CSS files
     * @return URL to access this file
     */
    public String getSkinFile(String filename, boolean forceSkinAction)
    {
        return this.xwiki.getSkinFile(filename, forceSkinAction, getXWikiContext());
    }

    /**
     * API to retrieve the current skin for this request and user The skin is first derived from the request "skin"
     * parameter If this parameter does not exist, the user preference "skin" is looked up If this parameter does not
     * exist or is empty, the space preference "skin" is looked up If this parameter does not exist or is empty, the
     * XWiki preference "skin" is looked up If this parameter does not exist or is empty, the xwiki.cfg parameter
     * xwiki.defaultskin is looked up If this parameter does not exist or is empty, the xwiki.cfg parameter
     * xwiki.defaultbaseskin is looked up If this parameter does not exist or is empty, the skin is "albatross"
     * 
     * @return The current skin for this request and user
     */
    public String getSkin()
    {
        return this.xwiki.getSkin(getXWikiContext());
    }

    /**
     * API to retrieve the current skin for this request and user. Each skin has a skin it is based on. If not the base
     * skin is the xwiki.cfg parameter "xwiki.defaultbaseskin". If this parameter does not exist or is empty, the base
     * skin is "albatross".
     * 
     * @return The current baseskin for this request and user
     */
    public String getBaseSkin()
    {
        return this.xwiki.getBaseSkin(getXWikiContext());
    }

    /**
     * API to access the copyright for this space. The copyright is read in the space preferences. If it does not exist
     * or is empty it is read from the XWiki preferences.
     * 
     * @return the text for the copyright
     */
    public String getSpaceCopyright()
    {
        return this.xwiki.getSpaceCopyright(getXWikiContext());
    }

    /**
     * API to access an XWiki Preference There can be one preference object per language This function will find the
     * right preference object associated to the current active language
     * 
     * @param preference Preference name
     * @return The preference for this wiki and the current language
     */
    public String getXWikiPreference(String preference)
    {
        return this.xwiki.getXWikiPreference(preference, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference There can be one preference object per language This function will find the
     * right preference object associated to the current active language
     * 
     * @param preference Preference name
     * @param defaultValue default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language
     */
    public String getXWikiPreference(String preference, String defaultValue)
    {
        return this.xwiki.getXWikiPreference(preference, defaultValue, getXWikiContext());
    }

    /**
     * API to access an Space Preference There can be one preference object per language This function will find the
     * right preference object associated to the current active language If no preference is found it will look in the
     * XWiki Preferences
     * 
     * @param preference Preference name
     * @return The preference for this wiki and the current language
     */
    public String getSpacePreference(String preference)
    {
        return this.xwiki.getSpacePreference(preference, getXWikiContext());
    }

    /**
     * API to access an Space Preference There can be one preference object per language This function will find the
     * right preference object associated to the current active language If no preference is found it will look in the
     * XWiki Preferences
     * 
     * @param preference Preference name
     * @param space The space for which this preference is requested
     * @return The preference for this wiki and the current language
     */
    public String getSpacePreferenceFor(String preference, String space)
    {
        return this.xwiki.getSpacePreference(preference, space, "", getXWikiContext());
    }

    /**
     * API to access an Space Preference There can be one preference object per language This function will find the
     * right preference object associated to the current active language If no preference is found it will look in the
     * XWiki Preferences
     * 
     * @param preference Preference name
     * @param defaultValue default value to return if the preference does not exist or is empty
     * @return The preference for this wiki and the current language
     */
    public String getSpacePreference(String preference, String defaultValue)
    {
        return this.xwiki.getSpacePreference(preference, defaultValue, getXWikiContext());
    }

    /**
     * API to access a Skin Preference The skin object is the current user's skin
     * 
     * @param preference Preference name
     * @return The preference for the current skin
     */
    public String getSkinPreference(String preference)
    {
        return this.xwiki.getSkinPreference(preference, getXWikiContext());
    }

    /**
     * API to access a Skin Preference The skin object is the current user's skin
     * 
     * @param preference Preference name
     * @param defaultValue default value to return if the preference does not exist or is empty
     * @return The preference for the current skin
     */
    public String getSkinPreference(String preference, String defaultValue)
    {
        return this.xwiki.getSkinPreference(preference, defaultValue, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as a long number There can be one preference object per language This function
     * will find the right preference object associated to the current active language
     * 
     * @param preference Preference name
     * @param space The space for which this preference is requested
     * @param defaultValue default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in long format
     */
    public String getSpacePreferenceFor(String preference, String space, String defaultValue)
    {
        return this.xwiki.getSpacePreference(preference, space, defaultValue, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as a long number There can be one preference object per language This function
     * will find the right preference object associated to the current active language
     * 
     * @param preference Preference name
     * @param defaultValue default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in long format
     */
    public long getXWikiPreferenceAsLong(String preference, long defaultValue)
    {
        return this.xwiki.getXWikiPreferenceAsLong(preference, defaultValue, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as a long number There can be one preference object per language This function
     * will find the right preference object associated to the current active language
     * 
     * @param preference Preference name
     * @return The preference for this wiki and the current language in long format
     */
    public long getXWikiPreferenceAsLong(String preference)
    {
        return this.xwiki.getXWikiPreferenceAsLong(preference, getXWikiContext());
    }

    /**
     * API to access a Space Preference as a long number There can be one preference object per language This function
     * will find the right preference object associated to the current active language If no preference is found it will
     * look for the XWiki Preference
     * 
     * @param preference Preference name
     * @param defaultValue default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in long format
     */
    public long getSpacePreferenceAsLong(String preference, long defaultValue)
    {
        return this.xwiki.getSpacePreferenceAsLong(preference, defaultValue, getXWikiContext());
    }

    /**
     * API to access a Space Preference as a long number There can be one preference object per language This function
     * will find the right preference object associated to the current active language If no preference is found it will
     * look for the XWiki Preference
     * 
     * @param preference Preference name
     * @return The preference for this wiki and the current language in long format
     */
    public long getSpacePreferenceAsLong(String preference)
    {
        return this.xwiki.getSpacePreferenceAsLong(preference, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as an int number There can be one preference object per language This function
     * will find the right preference object associated to the current active language
     * 
     * @param preference Preference name
     * @param defaultValue default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in int format
     */
    public int getXWikiPreferenceAsInt(String preference, int defaultValue)
    {
        return this.xwiki.getXWikiPreferenceAsInt(preference, defaultValue, getXWikiContext());
    }

    /**
     * API to access an XWiki Preference as a int number There can be one preference object per language This function
     * will find the right preference object associated to the current active language
     * 
     * @param preference Preference name
     * @return The preference for this wiki and the current language in int format
     */
    public int getXWikiPreferenceAsInt(String preference)
    {
        return this.xwiki.getXWikiPreferenceAsInt(preference, getXWikiContext());
    }

    /**
     * API to access a space Preference as a int number There can be one preference object per language This function
     * will find the right preference object associated to the current active language If no preference is found it will
     * look for the XWiki Preference
     * 
     * @param preference Preference name
     * @param defaultValue default value to return if the prefenrece does not exist or is empty
     * @return The preference for this wiki and the current language in int format
     */
    public int getSpacePreferenceAsInt(String preference, int defaultValue)
    {
        return this.xwiki.getSpacePreferenceAsInt(preference, defaultValue, getXWikiContext());
    }

    /**
     * API to access a Space Preference as a int number There can be one preference object per language This function
     * will find the right preference object associated to the current active language If no preference is found it will
     * look for the XWiki Preference
     * 
     * @param preference Preference name
     * @return The preference for this wiki and the current language in int format
     */
    public int getSpacePreferenceAsInt(String preference)
    {
        return this.xwiki.getSpacePreferenceAsInt(preference, getXWikiContext());
    }

    /**
     * API to access a User Preference This function will look in the User profile for the preference If no preference
     * is found it will look in the Space Preferences If no preference is found it will look in the XWiki Preferences
     * 
     * @param preference Preference name
     * @return The preference for this wiki and the current language
     */
    public String getUserPreference(String preference)
    {
        return this.xwiki.getUserPreference(preference, getXWikiContext());
    }

    /**
     * API to access a User Preference from cookie This function will look in the session cookie for the preference
     * 
     * @param preference Preference name
     * @return The preference for this wiki and the current language
     */
    public String getUserPreferenceFromCookie(String preference)
    {
        return this.xwiki.getUserPreferenceFromCookie(preference, getXWikiContext());
    }

    /**
     * First try to find the current language in use from the XWiki context. If none is used and if the wiki is not
     * multilingual use the default language defined in the XWiki preferences. If the wiki is multilingual try to get
     * the language passed in the request. If none was passed try to get it from a cookie. If no language cookie exists
     * then use the user default language and barring that use the browser's "Accept-Language" header sent in HTTP
     * request. If none is defined use the default language.
     * 
     * @return the language to use
     */
    public String getLanguagePreference()
    {
        return this.xwiki.getLanguagePreference(getXWikiContext());
    }

    /**
     * API to access the interface language preference for the request Order of evaluation is: Language of the wiki in
     * mono-lingual mode language request paramater language in context language user preference language in cookie
     * language accepted by the navigator
     * 
     * @return the document language preference for the request
     */
    public String getInterfaceLanguagePreference()
    {
        return this.xwiki.getInterfaceLanguagePreference(getXWikiContext());
    }

    /**
     * API to check if wiki is in multi-wiki mode (virtual)
     * 
     * @return true for multi-wiki/false for mono-wiki
     */
    public boolean isVirtualMode()
    {
        return this.xwiki.isVirtualMode();
    }

    /**
     * API to check is wiki is multi-lingual
     * 
     * @return true for multi-lingual/false for mono-lingual
     */
    public boolean isMultiLingual()
    {
        return this.xwiki.isMultiLingual(getXWikiContext());
    }

    /**
     * Priviledged API to flush the cache of the Wiki installation This flushed the cache of all wikis, all plugins, all
     * renderers
     */
    public void flushCache()
    {
        if (hasProgrammingRights()) {
            this.xwiki.flushCache(getXWikiContext());
        }
    }

    /**
     * Priviledged API to reset the rendenring engine This would restore the rendering engine evaluation loop and take
     * into account new configuration parameters
     */
    public void resetRenderingEngine()
    {
        if (hasProgrammingRights()) {
            try {
                this.xwiki.resetRenderingEngine(getXWikiContext());
            } catch (XWikiException e) {
            }
        }
    }

    /**
     * Priviledged API to create a new user from the request This API is used by RegisterNewUser wiki page
     * 
     * @return true for success/false for failure
     * @throws XWikiException
     */
    public int createUser() throws XWikiException
    {
        return createUser(false, "edit");
    }

    /**
     * Priviledged API to create a new user from the request This API is used by RegisterNewUser wiki page This version
     * sends a validation email to the user Configuration of validation email is in the XWiki Preferences
     * 
     * @param withValidation true to send the validationemail
     * @return true for success/false for failure
     * @throws XWikiException
     */
    public int createUser(boolean withValidation) throws XWikiException
    {
        return createUser(withValidation, "edit");
    }

    /**
     * Priviledged API to create a new user from the request This API is used by RegisterNewUser wiki page This version
     * sends a validation email to the user Configuration of validation email is in the XWiki Preferences
     * 
     * @param withValidation true to send the validation email
     * @param userRights Rights to set for the user for it's own page(defaults to "edit")
     * @return true for success/false for failure
     * @throws XWikiException
     */
    public int createUser(boolean withValidation, String userRights) throws XWikiException
    {
        boolean registerRight;
        try {
            // So, what's the register right for? This says that if the creator of the page
            // (Admin) has programming rights, anybody can register. Is this OK?
            if (hasProgrammingRights()) {
                registerRight = true;
            } else {
                registerRight = this.xwiki.getRightService().hasAccessLevel("register", getXWikiContext().getUser(),
                    "XWiki.XWikiPreferences", getXWikiContext());
            }

            if (registerRight) {
                return this.xwiki.createUser(withValidation, userRights, getXWikiContext());
            }

            return -1;
        } catch (Exception e) {
            LOGGER.error("Failed to create user", e);

            return -2;
        }

    }

    /**
     * Priviledged API to create a new Wiki from an existing wiki This creates the database, copies to documents from a
     * existing wiki Assigns the admin rights, creates the Wiki identification page in the main wiki
     * 
     * @param wikiName Wiki Name to create
     * @param wikiUrl Wiki URL to accept requests from
     * @param wikiAdmin Wiki admin user
     * @param baseWikiName Wiki to copy documents from
     * @param failOnExist true to fail if the wiki already exists, false to overwrite
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for other errors
     * @throws XWikiException
     */
    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName, boolean failOnExist)
        throws XWikiException
    {
        return createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, "", null, failOnExist);
    }

    /**
     * Priviledged API to create a new Wiki from an existing wiki This creates the database, copies to documents from a
     * existing wiki Assigns the admin rights, creates the Wiki identification page in the main wiki
     * 
     * @param wikiName Wiki Name to create
     * @param wikiUrl Wiki URL to accept requests from
     * @param wikiAdmin Wiki admin user
     * @param baseWikiName Wiki to copy documents from
     * @param description Description of the Wiki
     * @param failOnExist true to fail if the wiki already exists, false to overwrite
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for other errors
     * @throws XWikiException
     */
    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName,
        String description, boolean failOnExist) throws XWikiException
    {
        return createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, description, null, failOnExist);
    }

    /**
     * Priviledged API to create a new Wiki from an existing wiki This creates the database, copies to documents from a
     * existing wiki Assigns the admin rights, creates the Wiki identification page in the main wiki Copy is limited to
     * documents of a specified language. If a document for the language is not found, the default language document is
     * used
     * 
     * @param wikiName Wiki Name to create
     * @param wikiUrl Wiki URL to accept requests from
     * @param wikiAdmin Wiki admin user
     * @param baseWikiName Wiki to copy documents from
     * @param description Description of the Wiki
     * @param language Language to copy
     * @param failOnExist true to fail if the wiki already exists, false to overwrite
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for other errors
     * @throws XWikiException
     */
    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName,
        String description, String language, boolean failOnExist) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, description, language,
                failOnExist, getXWikiContext());
        }

        return -1;
    }

    /**
     * Priviledged API to validate the return code given by a user in response to an email validation email The
     * validation information are taken from the request object
     * 
     * @param withConfirmEmail true to send a account confirmation email/false to not send it
     * @return Success of Failure code (0 for success, -1 for missing programming rights, > 0 for other errors
     * @throws XWikiException
     */
    public int validateUser(boolean withConfirmEmail) throws XWikiException
    {
        return this.xwiki.validateUser(withConfirmEmail, getXWikiContext());
    }

    /**
     * Priviledged API to add a user to the XWiki.XWikiAllGroup
     * 
     * @param fullwikiname user name to add
     * @throws XWikiException
     */
    public void addToAllGroup(String fullwikiname) throws XWikiException
    {
        if (hasProgrammingRights()) {
            this.xwiki.setUserDefaultGroup(fullwikiname, getXWikiContext());
        }
    }

    /**
     * Priviledged API to send a confirmation email to a user
     * 
     * @param xwikiname user to send the email to
     * @param password password to put in the mail
     * @param email email to send to
     * @param add_message Additional message to send to the user
     * @param contentfield Preference field to use as a mail template
     * @throws XWikiException if the mail was not send successfully
     */
    public void sendConfirmationMail(String xwikiname, String password, String email, String add_message,
        String contentfield) throws XWikiException
    {
        if (hasProgrammingRights()) {
            this.xwiki.sendConfirmationEmail(xwikiname, password, email, add_message, contentfield, getXWikiContext());
        }
    }

    /**
     * Priviledged API to send a confirmation email to a user
     * 
     * @param xwikiname user to send the email to
     * @param password password to put in the mail
     * @param email email to send to
     * @param contentfield Preference field to use as a mail template
     * @throws XWikiException if the mail was not send successfully
     */
    public void sendConfirmationMail(String xwikiname, String password, String email, String contentfield)
        throws XWikiException
    {
        if (hasProgrammingRights()) {
            this.xwiki.sendConfirmationEmail(xwikiname, password, email, "", contentfield, getXWikiContext());
        }
    }

    /**
     * API to copy a document to another document in the same wiki
     * 
     * @param docname source document
     * @param targetdocname target document
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String targetdocname) throws XWikiException
    {
        return this.copyDocument(docname, targetdocname, null, null, null, false, false);
    }

    /**
     * API to copy a translation of a document to another document in the same wiki
     * 
     * @param docname source document
     * @param targetdocname target document
     * @param wikilanguage language to copy
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String targetdocname, String wikilanguage) throws XWikiException
    {
        return this.copyDocument(docname, targetdocname, null, null, wikilanguage, false, false);
    }

    /**
     * API to copy a translation of a document to another document of the same name in another wiki
     * 
     * @param docname source document
     * @param sourceWiki source wiki
     * @param targetWiki target wiki
     * @param wikilanguage language to copy
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String sourceWiki, String targetWiki, String wikilanguage)
        throws XWikiException
    {
        return this.copyDocument(docname, docname, sourceWiki, targetWiki, wikilanguage, true, false);
    }

    /**
     * API to copy a translation of a document to another document of the same name in another wiki additionally
     * resetting the version
     * 
     * @param docname source document
     * @param sourceWiki source wiki
     * @param targetWiki target wiki
     * @param wikilanguage language to copy
     * @param reset true to reset versions
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String targetdocname, String sourceWiki, String targetWiki,
        String wikilanguage, boolean reset) throws XWikiException
    {
        return this.copyDocument(docname, targetdocname, sourceWiki, targetWiki, wikilanguage, reset, false);
    }

    /**
     * API to copy a translation of a document to another document of the same name in another wiki additionally
     * resetting the version and overwriting the previous document
     * 
     * @param docname source document
     * @param sourceWiki source wiki
     * @param targetWiki target wiki
     * @param wikilanguage language to copy
     * @param reset true to reset versions
     * @param force true to overwrite the previous document
     * @return true if the copy was sucessfull
     * @throws XWikiException if the document was not copied properly
     */
    public boolean copyDocument(String docname, String targetdocname, String sourceWiki, String targetWiki,
        String wikilanguage, boolean reset, boolean force) throws XWikiException
    {
        DocumentReference sourceDocumentReference = this.currentMixedDocumentReferenceResolver.resolve(docname);
        if (!StringUtils.isEmpty(sourceWiki)) {
            sourceDocumentReference.setWikiReference(new WikiReference(sourceWiki));
        }

        DocumentReference targetDocumentReference = this.currentMixedDocumentReferenceResolver.resolve(targetdocname);
        if (!StringUtils.isEmpty(targetWiki)) {
            targetDocumentReference.setWikiReference(new WikiReference(targetWiki));
        }

        return this.copyDocument(sourceDocumentReference, targetDocumentReference, wikilanguage, reset, force);
    }

    /**
     * API to copy a translation of a document to another document of the same name in another wiki additionally
     * resetting the version and overwriting the previous document
     * 
     * @param sourceDocumentReference the reference to the document to copy
     * @param targetDocumentReference the reference to the document to create
     * @param wikilanguage language to copy
     * @param resetHistory {@code true} to reset versions
     * @param overwrite {@code true} to overwrite the previous document
     * @return {@code true} if the copy was sucessful
     * @throws XWikiException if the document was not copied properly
     * @since 3.0M3
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        String wikilanguage, boolean resetHistory, boolean overwrite) throws XWikiException
    {
        // In order to copy the source document the user must have at least the right to view it.
        if (hasAccessLevel("view", this.defaultStringEntityReferenceSerializer.serialize(sourceDocumentReference))) {
            String targetDocStringRef = this.defaultStringEntityReferenceSerializer.serialize(targetDocumentReference);
            // To create the target document the user must have edit rights. If the target document exists and the user
            // wants to overwrite it then he needs delete right.
            // Note: We have to check if the target document exists before checking the delete right because delete
            // right is denied if not explicitly specified.
            if (hasAccessLevel("edit", targetDocStringRef)
                && (!overwrite || !exists(targetDocumentReference) || hasAccessLevel("delete", targetDocStringRef))) {
                // Reset creation data otherwise the required rights for page copy need to be reconsidered.
                return this.xwiki.copyDocument(sourceDocumentReference, targetDocumentReference, wikilanguage,
                    resetHistory, overwrite, true, getXWikiContext());
            }
        }

        return false;
    }

    /**
     * Privileged API to copy a space to another wiki, optionally deleting all document of the target space
     * 
     * @param space source Space
     * @param sourceWiki source Wiki
     * @param targetWiki target Wiki
     * @param language language to copy
     * @param clean true to delete all document of the target space
     * @return number of copied documents
     * @throws XWikiException if the space was not copied properly
     */
    public int copySpaceBetweenWikis(String space, String sourceWiki, String targetWiki, String language, boolean clean)
        throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.copySpaceBetweenWikis(space, sourceWiki, targetWiki, language, clean, getXWikiContext());
        }

        return -1;
    }

    /**
     * API to include a topic into another The topic is rendered fully in the context of itself
     * 
     * @param topic page name of the topic to include
     * @return the content of the included page
     * @throws XWikiException if the include failed
     */
    public String includeTopic(String topic) throws XWikiException
    {
        return includeTopic(topic, true);
    }

    /**
     * API to execute a form in the context of an including topic The rendering is evaluated in the context of the
     * including topic All velocity variables are the one of the including topic This api is usually called using
     * #includeForm in a page, which modifies the behavior of "Edit this page" button to direct for Form mode (inline)
     * 
     * @param topic page name of the form to execute
     * @return the content of the included page
     * @throws XWikiException if the include failed
     */
    public String includeForm(String topic) throws XWikiException
    {
        return includeForm(topic, true);
    }

    /**
     * API to include a topic into another, optionally surrounding the content with {pre}{/pre} to avoid future wiki
     * rendering. The topic is rendered fully in the context of itself.
     * 
     * @param topic page name of the topic to include
     * @param pre true to add {pre} {/pre} (only if includer document is 1.0 syntax)
     * @return the content of the included page
     * @throws XWikiException if the include failed
     */
    public String includeTopic(String topic, boolean pre) throws XWikiException
    {
        String result = this.xwiki.include(topic, false, getXWikiContext());

        if (pre) {
            String includerSyntax = this.xwiki.getCurrentContentSyntaxId(null, this.context);

            if (includerSyntax != null && Syntax.XWIKI_1_0.toIdString().equals(includerSyntax)) {
                result = "{pre}" + result + "{/pre}";
            }
        }

        return result;
    }

    /**
     * API to execute a form in the context of an including topic, optionnaly surrounding the content with {pre}{/pre}
     * to avoid future wiki rendering The rendering is evaluated in the context of the including topic All velocity
     * variables are the one of the including topic This api is usually called using #includeForm in a page, which
     * modifies the behavior of "Edit this page" button to direct for Form mode (inline).
     * 
     * @param topic page name of the form to execute
     * @param pre true to add {pre} {/pre} (only if includer document is 1.0 syntax)
     * @return the content of the included page
     * @throws XWikiException if the include failed
     */
    public String includeForm(String topic, boolean pre) throws XWikiException
    {
        String result = this.xwiki.include(topic, true, getXWikiContext());

        if (pre) {
            String includerSyntax = this.xwiki.getCurrentContentSyntaxId(null, this.context);

            if (includerSyntax != null && Syntax.XWIKI_1_0.toIdString().equals(includerSyntax)) {
                result = "{pre}" + result + "{/pre}";
            }
        }

        return result;
    }

    /**
     * API to check rights on the current document for the current user
     * 
     * @param level right to check (view, edit, comment, delete)
     * @return true if right is granted/false if not
     */
    public boolean hasAccessLevel(String level)
    {
        return hasAccessLevel(level, getXWikiContext().getUser(), getXWikiContext().getDoc().getFullName());
    }

    /**
     * API to check rights on a document for a given user
     * 
     * @param level right to check (view, edit, comment, delete)
     * @param user user for which to check the right
     * @param docname document on which to check the rights
     * @return true if right is granted/false if not
     */
    public boolean hasAccessLevel(String level, String user, String docname)
    {
        try {
            return this.xwiki.getRightService().hasAccessLevel(level, user, docname, getXWikiContext());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * API to render a text in the context of a document
     * 
     * @param text text to render
     * @param doc the text is evaluated in the content of this document
     * @return evaluated content
     * @throws XWikiException if the evaluation went wrong
     */
    public String renderText(String text, Document doc) throws XWikiException
    {
        return this.xwiki.getRenderingEngine().renderText(text, doc.getDoc(), getXWikiContext());
    }

    /**
     * API to render a chunk (difference between two versions
     * 
     * @param chunk difference between versions to render
     * @param doc document to use as a context for rendering
     * @return resuilt of the rendering
     */
    public String renderChunk(Chunk chunk, Document doc)
    {
        return renderChunk(chunk, false, doc);
    }

    /**
     * API to render a chunk (difference between two versions
     * 
     * @param chunk difference between versions to render
     * @param doc document to use as a context for rendering
     * @param source true to render the difference as wiki source and not as wiki rendered text
     * @return resuilt of the rendering
     */
    public String renderChunk(Chunk chunk, boolean source, Document doc)
    {
        StringBuffer buf = new StringBuffer();
        chunk.toString(buf, "", "\n");
        if (source == true) {
            return buf.toString();
        }

        try {
            return this.xwiki.getRenderingEngine().renderText(buf.toString(), doc.getDoc(), getXWikiContext());
        } catch (Exception e) {
            return buf.toString();
        }
    }

    /**
     * API to list the current spaces in thiswiki
     * 
     * @return a list for strings reprenseting the spaces
     * @throws XWikiException if something went wrong
     */
    public List<String> getSpaces() throws XWikiException
    {
        return this.xwiki.getSpaces(getXWikiContext());
    }

    /**
     * API to list all documents in a space
     * 
     * @param SpaceName space tolest
     * @return A list of strings to lest the document
     * @throws XWikiException if the loading went wrong
     */
    public List<String> getSpaceDocsName(String SpaceName) throws XWikiException
    {
        return this.xwiki.getSpaceDocsName(SpaceName, getXWikiContext());
    }

    /**
     * API to retrieve the current encoding of the wiki engine The encoding is stored in xwiki.cfg Default encoding is
     * ISO-8891-1
     * 
     * @return encoding active in this wiki
     */
    public String getEncoding()
    {
        return this.xwiki.getEncoding();
    }

    /**
     * API to retrieve the URL of an attached file in a Wiki Document The URL is generated differently depending on the
     * environement (Servlet, Portlet, PDF, etc..) The URL generation can be modified by implementing a new
     * XWikiURLFactory object For compatibility with any target environement (and especially the portlet environment) It
     * is important to always use the URL functions to generate URL and never hardcode URLs
     * 
     * @param fullname page name which includes the attached file
     * @param filename attached filename to create a link for
     * @return a URL as a string pointing to the filename
     * @throws XWikiException if the URL could not be generated properly
     */
    public String getAttachmentURL(String fullname, String filename) throws XWikiException
    {
        return this.xwiki.getAttachmentURL(fullname, filename, getXWikiContext());
    }

    /**
     * API to retrieve the URL of an a Wiki Document in view mode The URL is generated differently depending on the
     * environement (Servlet, Portlet, PDF, etc..) The URL generation can be modified by implementing a new
     * XWikiURLFactory object For compatibility with any target environement (and especially the portlet environment) It
     * is important to always use the URL functions to generate URL and never hardcode URLs
     * 
     * @param fullname the name of the document for which to return the URL for
     * @return a URL as a string pointing to the wiki document in view mode
     * @throws XWikiException if the URL could not be generated properly
     */
    public String getURL(String fullname) throws XWikiException
    {
        return this.xwiki.getURL(fullname, "view", getXWikiContext());
    }

    /**
     * API to retrieve the URL of an a Wiki Document in view mode The URL is generated differently depending on the
     * environement (Servlet, Portlet, PDF, etc..) The URL generation can be modified by implementing a new
     * XWikiURLFactory object For compatibility with any target environement (and especially the portlet environment) It
     * is important to always use the URL functions to generate URL and never hardcode URLs
     * 
     * @param reference the reference to the document for which to return the URL for
     * @return a URL as a string pointing to the wiki document in view mode
     * @throws XWikiException if the URL could not be generated properly
     * @since 2.3M2
     */
    public String getURL(DocumentReference reference) throws XWikiException
    {
        return this.xwiki.getURL(reference, "view", getXWikiContext());
    }

    /**
     * API to retrieve the URL of an a Wiki Document in any mode. The URL is generated differently depending on the
     * environment (Servlet, Portlet, PDF, etc..). The URL generation can be modified by implementing a new
     * XWikiURLFactory object For compatibility with any target environement (and especially the portlet environment).
     * It is important to always use the URL functions to generate URL and never hardcode URLs.
     * 
     * @param fullname the page name which includes the attached file
     * @param action the mode in which to access the document (view/edit/save/..). Any valid XWiki action is possible.
     * @return a URL as a string pointing to the wiki document in view mode
     * @throws XWikiException if the URL could not be generated properly
     */
    public String getURL(String fullname, String action) throws XWikiException
    {
        return this.xwiki.getURL(fullname, action, getXWikiContext());
    }

    /**
     * API to retrieve the URL of a Wiki Document in any mode, optionally adding a query string The URL is generated
     * differently depending on the environment (Servlet, Portlet, PDF, etc..) The URL generation can be modified by
     * implementing a new XWikiURLFactory object. The query string will be modified to be added in the way the
     * environment needs it. It is important to not add the query string parameter manually after a URL. Some
     * environments will not accept this (like the Portlet environement).
     * 
     * @param fullname the page name which includes the attached file
     * @param action the mode in which to access the document (view/edit/save/..). Any valid XWiki action is possible
     * @param querystring the Query String to provide in the usual mode (name1=value1&name2=value=2) including encoding
     * @return a URL as a string pointing to the wiki document in view mode
     * @throws XWikiException if the URL could not be generated properly
     */
    public String getURL(String fullname, String action, String querystring) throws XWikiException
    {
        return this.xwiki.getURL(fullname, action, querystring, getXWikiContext());
    }

    /**
     * API to retrieve the URL of a Wiki Document in any mode, optionally adding a query string The URL is generated
     * differently depending on the environment (Servlet, Portlet, PDF, etc..) The URL generation can be modified by
     * implementing a new XWikiURLFactory object. The query string will be modified to be added in the way the
     * environment needs it. It is important to not add the query string parameter manually after a URL. Some
     * environments will not accept this (like the Portlet environement).
     * 
     * @param reference the reference to the document for which to return the URL for
     * @param action the mode in which to access the document (view/edit/save/..). Any valid XWiki action is possible
     * @param querystring the Query String to provide in the usual mode (name1=value1&name2=value=2) including encoding
     * @return a URL as a string pointing to the wiki document in view mode
     * @throws XWikiException if the URL could not be generated properly
     * @since 3.0M3
     */
    public String getURL(DocumentReference reference, String action, String querystring) throws XWikiException
    {
        return this.xwiki.getURL(reference, action, querystring, null, getXWikiContext());
    }

    /**
     * API to retrieve the URL of an a Wiki Document in any mode, optionally adding an anchor. The URL is generated
     * differently depending on the environement (Servlet, Portlet, PDF, etc..) The URL generation can be modified by
     * implementing a new XWikiURLFactory object. The anchor will be modified to be added in the way the environment
     * needs it. It is important to not add the anchor parameter manually after a URL. Some environments will not accept
     * this (like the Portlet environement).
     * 
     * @param fullname the page name which includes the attached file
     * @param action the mode in which to access the document (view/edit/save/..). Any valid XWiki action is possible
     * @param querystring the Query String to provide in the usual mode (name1=value1&name2=value=2) including encoding
     * @param anchor the anchor that points at a location within the passed document name
     * @return a URL as a string pointing to the wiki document in view mode
     * @throws XWikiException if the URL could not be generated properly
     */
    public String getURL(String fullname, String action, String querystring, String anchor) throws XWikiException
    {
        return this.xwiki.getURL(fullname, action, querystring, anchor, getXWikiContext());
    }

    /**
     * API to access the current starts for the Wiki for a specific action It retrieves the number of times the action
     * was performed for the whole wiki The statistics module need to be activated (xwiki.stats=1 in xwiki.cfg)
     * 
     * @param action action for which to retrieve statistics (view/save/download)
     * @return A DocumentStats object with number of actions performed, unique visitors, number of visits
     * @deprecated use {@link #getStatsService()} instead
     */
    @Deprecated
    public DocumentStats getCurrentMonthXWikiStats(String action)
    {
        return getXWikiContext().getWiki().getStatsService(getXWikiContext())
            .getDocMonthStats("", action, new Date(), getXWikiContext());
    }

    /**
     * API to retrieve a viewable referer text for a referer Referers are URL where users have clicked on a link to an
     * XWiki page Search engine referer URLs are transformed to a nicer view (Google: search query string) For other URL
     * the http:// part is stripped
     * 
     * @param referer referer URL to transform
     * @return A viewable string
     */
    public String getRefererText(String referer)
    {
        try {
            return this.xwiki.getRefererText(referer, getXWikiContext());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * API to retrieve a viewable referer text for a referer with a maximum length Referers are URL where users have
     * clicked on a link to an XWiki page Search engine referer URLs are transformed to a nicer view (Google: search
     * query string) For other URL the http:// part is stripped
     * 
     * @param referer referer URL to transform
     * @param length Maximum length. "..." is added to the end of the text
     * @return A viewable string
     */
    public String getShortRefererText(String referer, int length)
    {
        try {
            return this.xwiki.getRefererText(referer, getXWikiContext()).substring(0, length);
        } catch (Exception e) {
            return this.xwiki.getRefererText(referer, getXWikiContext());
        }
    }

    /**
     * Deprecated API which was retrieving the SQL to represent the fullName Document field depending on the database
     * used This is not needed anymore and returns 'doc.fullName' for all databases
     * 
     * @deprecated
     * @return "doc.fullName"
     */
    @Deprecated
    public String getFullNameSQL()
    {
        return this.xwiki.getFullNameSQL();
    }

    /**
     * API to retrieve a link to the User Name page displayed for the first name and last name of the user The link will
     * link to the page on the wiki where the user is registered (in virtual wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getUserName(String user)
    {
        return this.xwiki.getUserName(user, null, getXWikiContext());
    }

    /**
     * API to retrieve a link to the User Name page displayed with a custom view. The link will link to the page on the
     * wiki where the user is registered (in virtual wiki mode) The formating is done using the format parameter which
     * can contain velocity scripting and access all properties of the User profile using variables ($first_name
     * $last_name $email $city)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param format formatting to be used ("$first_name $last_name", "$first_name")
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getUserName(String user, String format)
    {
        return this.xwiki.getUserName(user, format, getXWikiContext());
    }

    /**
     * API to retrieve a link to the User Name page displayed for the first name and last name of the user The link will
     * link to the page on the local wiki even if the user is registered on a different wiki (in virtual wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getLocalUserName(String user)
    {
        try {
            return this.xwiki.getUserName(user.substring(user.indexOf(":") + 1), null, getXWikiContext());
        } catch (Exception e) {
            return this.xwiki.getUserName(user, null, getXWikiContext());
        }
    }

    /**
     * API to retrieve a link to the User Name page displayed with a custom view The link will link to the page on the
     * local wiki even if the user is registered on a different wiki (in virtual wiki mode) The formating is done using
     * the format parameter which can contain velocity scripting and access all properties of the User profile using
     * variables ($first_name $last_name $email $city)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param format formatting to be used ("$first_name $last_name", "$first_name")
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getLocalUserName(String user, String format)
    {
        try {
            return this.xwiki.getUserName(user.substring(user.indexOf(":") + 1), format, getXWikiContext());
        } catch (Exception e) {
            return this.xwiki.getUserName(user, format, getXWikiContext());
        }
    }

    /**
     * API to retrieve a text representing the user with the first name and last name of the user With the link param
     * set to false it will not link to the user page With the link param set to true, the link will link to the page on
     * the wiki where the user was registered (in virtual wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param link false to not add an HTML link to the user profile
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getUserName(String user, boolean link)
    {
        return this.xwiki.getUserName(user, null, link, getXWikiContext());
    }

    /**
     * API to retrieve a text representing the user with a custom view With the link param set to false it will not link
     * to the user page With the link param set to true, the link will link to the page on the wiki where the user was
     * registered (in virtual wiki mode) The formating is done using the format parameter which can contain velocity
     * scripting and access all properties of the User profile using variables ($first_name $last_name $email $city)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param format formatting to be used ("$first_name $last_name", "$first_name")
     * @param link false to not add an HTML link to the user profile
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getUserName(String user, String format, boolean link)
    {
        return this.xwiki.getUserName(user, format, link, getXWikiContext());
    }

    /**
     * API to retrieve a text representing the user with the first name and last name of the user With the link param
     * set to false it will not link to the user page With the link param set to true, the link will link to the page on
     * the local wiki even if the user is registered on a different wiki (in virtual wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param link false to not add an HTML link to the user profile
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getLocalUserName(String user, boolean link)
    {
        try {
            return this.xwiki.getUserName(user.substring(user.indexOf(":") + 1), null, link, getXWikiContext());
        } catch (Exception e) {
            return this.xwiki.getUserName(user, null, link, getXWikiContext());
        }
    }

    /**
     * API to retrieve a text representing the user with a custom view The formating is done using the format parameter
     * which can contain velocity scripting and access all properties of the User profile using variables ($first_name
     * $last_name $email $city) With the link param set to false it will not link to the user page With the link param
     * set to true, the link will link to the page on the local wiki even if the user is registered on a different wiki
     * (in virtual wiki mode)
     * 
     * @param user Fully qualified username as retrieved from $context.user (XWiki.LudovicDubost)
     * @param format formatting to be used ("$first_name $last_name", "$first_name")
     * @param link false to not add an HTML link to the user profile
     * @return The first name and last name fields surrounded with a link to the user page
     */
    public String getLocalUserName(String user, String format, boolean link)
    {
        try {
            return this.xwiki.getUserName(user.substring(user.indexOf(":") + 1), format, link, getXWikiContext());
        } catch (Exception e) {
            return this.xwiki.getUserName(user, format, link, getXWikiContext());
        }
    }

    public User getUser()
    {
        return this.xwiki.getUser(getXWikiContext());
    }

    public User getUser(String username)
    {
        return this.xwiki.getUser(username, getXWikiContext());
    }

    /**
     * API allowing to format a date according to the default Wiki setting The date format is provided in the
     * 'dateformat' parameter of the XWiki Preferences
     * 
     * @param date date object to format
     * @return A string with the date formating from the default Wiki setting
     */
    public String formatDate(Date date)
    {
        return this.xwiki.formatDate(date, null, getXWikiContext());
    }

    /**
     * API allowing to format a date according to a custom format The date format is from java.text.SimpleDateFormat
     * Example: "dd/MM/yyyy HH:mm:ss" or "d MMM yyyy" If the format is invalid the default format will be used to show
     * the date
     * 
     * @param date date to format
     * @param format format of the date to be used
     * @return the formatted date
     * @see java.text.SimpleDateFormat
     */
    public String formatDate(Date date, String format)
    {
        return this.xwiki.formatDate(date, format, getXWikiContext());
    }

    /*
     * Allow to read user setting providing the user timezone All dates will be expressed with this timezone @return the
     * timezone
     */
    public String getUserTimeZone()
    {
        return this.xwiki.getUserTimeZone(this.context);
    }

    /**
     * Returns a plugin from the plugin API. Plugin Rights can be verified. Note that although this API is a duplicate
     * of {@link #getPlugin(String)} it used to provide an easy access from Velocity to XWiki plugins. Indeed Velocity
     * has a feature in that if a class has a get method, using the dot notation will automatically call the get method
     * for the class. See http://velocity.apache.org/engine/releases/velocity-1.5/user-guide.html#propertylookuprules.
     * This this allows the following constructs: <code>$xwiki.pluginName.somePluginMethod()</code>
     * 
     * @param name Name of the plugin to retrieve (either short of full class name)
     * @return a plugin object
     */
    public Api get(String name)
    {
        return this.xwiki.getPluginApi(name, getXWikiContext());
    }

    /**
     * Returns a plugin from the plugin API. Plugin Rights can be verified.
     * 
     * @param name Name of the plugin to retrieve (either short of full class name)
     * @return a plugin object
     */
    public Api getPlugin(String name)
    {
        return this.xwiki.getPluginApi(name, getXWikiContext());
    }

    /**
     * Returns the Advertisement system from the preferences
     * 
     * @return "google" or "none"
     */
    public String getAdType()
    {
        return this.xwiki.getAdType(getXWikiContext());
    }

    /**
     * Returns the Advertisement client ID from the preferences
     * 
     * @return an Ad affiliate ID
     */
    public String getAdClientId()
    {
        return this.xwiki.getAdClientId(getXWikiContext());
    }

    /**
     * Returns the content of an HTTP/HTTPS URL protected using Basic Authentication
     * 
     * @param surl url to retrieve
     * @param username username for the basic authentication
     * @param password password for the basic authentication
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl, String username, String password) throws IOException
    {
        try {
            return this.xwiki.getURLContent(surl, username, password, this.context);
        } catch (Exception e) {
            LOGGER.warn("Failed to retrieve content from [" + surl + "]", e);
            return "";
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL
     * 
     * @param surl url to retrieve
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl) throws IOException
    {
        try {
            return this.xwiki.getURLContent(surl, this.context);
        } catch (Exception e) {
            LOGGER.warn("Failed to retrieve content from [" + surl + "]", e);
            return "";
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL protected using Basic Authentication
     * 
     * @param surl url to retrieve
     * @param username username for the basic authentication
     * @param password password for the basic authentication
     * @param timeout manuel timeout in milliseconds
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl, String username, String password, int timeout) throws IOException
    {
        try {
            return this.xwiki.getURLContent(surl, username, password, timeout,
                this.xwiki.getHttpUserAgent(this.context));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL
     * 
     * @param surl url to retrieve
     * @param timeout manuel timeout in milliseconds
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl, int timeout) throws IOException
    {
        try {
            return this.xwiki.getURLContent(surl, timeout, this.xwiki.getHttpUserAgent(this.context));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL protected using Basic Authentication as Bytes
     * 
     * @param surl url to retrieve
     * @param username username for the basic authentication
     * @param password password for the basic authentication
     * @return Content of the specified URL
     * @throws IOException
     */
    public byte[] getURLContentAsBytes(String surl, String username, String password) throws IOException
    {
        try {
            return this.xwiki.getURLContentAsBytes(surl, username, password, this.context);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL as Bytes
     * 
     * @param surl url to retrieve
     * @return Content of the specified URL
     * @throws IOException
     */
    public byte[] getURLContentAsBytes(String surl) throws IOException
    {
        try {
            return this.xwiki.getURLContentAsBytes(surl, this.context);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the list of Macros documents in the specified content
     * 
     * @param defaultSpace Default space to use for relative path names
     * @param content Content to parse
     * @return ArrayList of document names
     */
    public List<String> getIncludedMacros(String defaultSpace, String content)
    {
        return this.xwiki.getIncludedMacros(defaultSpace, content, getXWikiContext());
    }

    /**
     * returns true if xwiki.readonly is set in the configuration file
     * 
     * @return the value of xwiki.isReadOnly()
     * @see com.xpn.xwiki.XWiki
     */
    public boolean isReadOnly()
    {
        return this.xwiki.isReadOnly();
    }

    /**
     * Privileged API to set/unset the readonly status of the Wiki After setting this to true no writing to the database
     * will be performed All Edit buttons will be removed and save actions disabled This is used for maintenance
     * purposes
     * 
     * @param ro true to set read-only mode/false to unset
     */
    public void setReadOnly(boolean ro)
    {
        if (hasAdminRights()) {
            this.xwiki.setReadOnly(ro);
        }
    }

    /**
     * Priviledge API to regenerate the links/backlinks table Normally links and backlinks are stored when a page is
     * modified This function will regenerate all the backlinks This function can be long to run
     * 
     * @throws XWikiException exception if the generation fails
     */
    public void refreshLinks() throws XWikiException
    {
        if (hasAdminRights()) {
            this.xwiki.refreshLinks(getXWikiContext());
        }
    }

    /**
     * API to check if the backlinks feature is active Backlinks are activated in xwiki.cfg or in the XWiki Preferences
     * 
     * @return true if the backlinks feature is active
     * @throws XWikiException exception if the preference could not be retrieved
     */
    public boolean hasBacklinks() throws XWikiException
    {
        return this.xwiki.hasBacklinks(getXWikiContext());
    }

    /**
     * API to check if the tags feature is active. Tags are activated in xwiki.cfg or in the XWiki Preferences
     * 
     * @return true if the tags feature is active, false otherwise
     * @throws XWikiException exception if the preference could not be retrieved
     */
    public boolean hasTags() throws XWikiException
    {
        return this.xwiki.hasTags(getXWikiContext());
    }

    /**
     * API to check if the edit comment feature is active Edit comments are activated in xwiki.cfg or in the XWiki
     * Preferences
     * 
     * @return
     */
    public boolean hasEditComment()
    {
        return this.xwiki.hasEditComment(this.context);
    }

    /**
     * API to check if the edit comment field is shown in the edit form Edit comments are activated in xwiki.cfg or in
     * the XWiki Preferences
     * 
     * @return
     */
    public boolean isEditCommentFieldHidden()
    {
        return this.xwiki.isEditCommentFieldHidden(this.context);
    }

    /**
     * API to check if the edit comment is suggested (prompted once by Javascript if empty) Edit comments are activated
     * in xwiki.cfg or in the XWiki Preferences
     * 
     * @return
     */
    public boolean isEditCommentSuggested()
    {
        return this.xwiki.isEditCommentSuggested(this.context);
    }

    /**
     * API to check if the edit comment is mandatory (prompted by Javascript if empty) Edit comments are activated in
     * xwiki.cfg or in the XWiki Preferences
     * 
     * @return
     */
    public boolean isEditCommentMandatory()
    {
        return this.xwiki.isEditCommentMandatory(this.context);
    }

    /**
     * API to check if the minor edit feature is active minor edit is activated in xwiki.cfg or in the XWiki Preferences
     */
    public boolean hasMinorEdit()
    {
        return this.xwiki.hasMinorEdit(this.context);
    }

    /**
     * API to check if the recycle bin feature is active recycle bin is activated in xwiki.cfg or in the XWiki
     * Preferences
     */
    public boolean hasRecycleBin()
    {
        return this.xwiki.hasRecycleBin(this.context);
    }

    /**
     * API to rename a page (experimental) Rights are necessary to edit the source and target page All objects and
     * attachments ID are modified in the process to link to the new page name
     * 
     * @param doc page to rename
     * @param newFullName target page name to move the information to
     * @throws XWikiException exception if the rename fails
     */
    public boolean renamePage(Document doc, String newFullName)
    {
        try {
            if (this.xwiki.exists(newFullName, getXWikiContext()) && !this.xwiki.getRightService().hasAccessLevel(
                "delete", getXWikiContext().getUser(), newFullName, getXWikiContext())) {
                return false;
            }
            if (this.xwiki.getRightService().hasAccessLevel("edit", getXWikiContext().getUser(), doc.getFullName(),
                getXWikiContext())) {
                this.xwiki.renamePage(doc.getFullName(), newFullName, getXWikiContext());
            }
        } catch (XWikiException e) {
            return false;
        }

        return true;
    }

    /**
     * Retrieves the current editor preference for the request The preference is first looked up in the user preference
     * and then in the space and wiki preference
     * 
     * @return "wysiwyg" or "text"
     */
    public String getEditorPreference()
    {
        return this.xwiki.getEditorPreference(getXWikiContext());
    }

    /**
     * Privileged API to retrieve an object instantiated from groovy code in a String. Note that Groovy scripts
     * compilation is cached.
     * 
     * @param script the Groovy class definition string (public class MyClass { ... })
     * @return An object instantiating this class
     * @throws XWikiException
     */
    public java.lang.Object parseGroovyFromString(String script) throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.xwiki.parseGroovyFromString(script, getXWikiContext());
        }
        return "groovy_missingrights";
    }

    /**
     * Privileged API to retrieve an object instantiated from groovy code in a String, using a classloader including all
     * JAR files located in the passed page as attachments. Note that Groovy scripts compilation is cached
     * 
     * @param script the Groovy class definition string (public class MyClass { ... })
     * @return An object instantiating this class
     * @throws XWikiException
     */
    public java.lang.Object parseGroovyFromPage(String script, String jarWikiPage) throws XWikiException
    {
        XWikiDocument doc = this.xwiki.getDocument(script, getXWikiContext());
        if (this.xwiki.getRightService().hasProgrammingRights(doc, getXWikiContext())) {
            return this.xwiki.parseGroovyFromString(doc.getContent(), jarWikiPage, getXWikiContext());
        }
        return "groovy_missingrights";
    }

    /**
     * Privileged API to retrieve an object instanciated from groovy code in a String Groovy scripts compilation is
     * cached
     * 
     * @param fullname // script containing a Groovy class definition (public class MyClass { ... })
     * @return An object instanciating this class
     * @throws XWikiException
     */
    public java.lang.Object parseGroovyFromPage(String fullname) throws XWikiException
    {
        XWikiDocument doc = this.xwiki.getDocument(fullname, getXWikiContext());
        if (this.xwiki.getRightService().hasProgrammingRights(doc, getXWikiContext())) {
            return this.xwiki.parseGroovyFromString(doc.getContent(), getXWikiContext());
        }
        return "groovy_missingrights";
    }

    /**
     * API to get the macro list from the XWiki Preferences The macro list are the macros available from the Macro
     * Mapping System
     * 
     * @return String with each macro on each line
     */
    public String getMacroList()
    {
        return this.xwiki.getMacroList(getXWikiContext());
    }

    /**
     * API to check if using which toolbars in Wysiwyg editor
     * 
     * @return a string value
     */
    public String getWysiwygToolbars()
    {
        return this.xwiki.getWysiwygToolbars(getXWikiContext());
    }

    /**
     * API to create an object from the request The parameters are the ones that are created from
     * doc.display("field","edit") calls
     * 
     * @param className XWiki Class Name to create the object from
     * @return a BaseObject wrapped in an Object
     * @throws XWikiException exception if the object could not be read
     */
    public com.xpn.xwiki.api.Object getObjectFromRequest(String className) throws XWikiException
    {
        return new com.xpn.xwiki.api.Object(this.xwiki.getObjectFromRequest(className, getXWikiContext()),
            getXWikiContext());
    }

    /**
     * API to create an empty document
     * 
     * @return an XWikiDocument wrapped in a Document
     */
    public Document createDocument()
    {
        return new XWikiDocument().newDocument(getXWikiContext());
    }

    /**
     * API to convert the username depending on the configuration The username can be converted from email to a valid
     * XWiki page name hidding the email address The username can be then used to login and link to the right user page
     * 
     * @param username username to use for login
     * @return converted wiki page name for this username
     */
    public String convertUsername(String username)
    {
        return this.xwiki.convertUsername(username, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class This field data can then be used
     * to generate an XWiki Query showing a table with the relevant data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearchColumns(String className, XWikiQuery query) throws XWikiException
    {
        return this.xwiki.displaySearchColumns(className, "", query, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class, optionally adding a prefix This
     * field data can then be used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param prefix Prefix to add to the field name
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearchColumns(String className, String prefix, XWikiQuery query) throws XWikiException
    {
        return this.xwiki.displaySearchColumns(className, prefix, query, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class This field data can then be used
     * to generate the order element of an XWiki Query showing a table with the relevant data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearchOrder(String className, XWikiQuery query) throws XWikiException
    {
        return this.xwiki.displaySearchOrder(className, "", query, getXWikiContext());
    }

    /**
     * API to display a select box for the list of available field for a specific class, optionally adding a prefix This
     * field data can then be used to generate the order element of an XWiki Query showing a table with the relevant
     * data
     * 
     * @param className XWiki Class Name to display the list of columns for
     * @param prefix Prefix to add to the field name
     * @param query Query to pre-select the currently selected columns
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearchOrder(String className, String prefix, XWikiQuery query) throws XWikiException
    {
        return this.xwiki.displaySearchOrder(className, prefix, query, getXWikiContext());
    }

    /**
     * API to display a field in search mode for a specific class without preselected values This field data can then be
     * used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param fieldname field name in the class
     * @param className class name to display the field from
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearch(String fieldname, String className) throws XWikiException
    {
        return this.xwiki.displaySearch(fieldname, className, getXWikiContext());
    }

    /**
     * API to display a field in search mode for a specific class with preselected values This field data can then be
     * used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param fieldname field name in the class
     * @param className class name to display the field from
     * @param criteria XWikiCriteria object (usually the XWikiQuery object) to take the preselected values from
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearch(String fieldname, String className, XWikiCriteria criteria) throws XWikiException
    {
        return this.xwiki.displaySearch(fieldname, className, criteria, getXWikiContext());
    }

    /**
     * API to display a field in search mode for a specific class with preselected values, optionally adding a prefix to
     * the field name This field data can then be used to generate an XWiki Query showing a table with the relevant data
     * 
     * @param fieldname field name in the class
     * @param className class name to display the field from
     * @param prefix prefix to add to the field name
     * @param criteria XWikiCriteria object (usually the XWikiQuery object) to take the preselected values from
     * @return text of the select field
     * @throws XWikiException exception is a failure occured
     */
    public String displaySearch(String fieldname, String className, String prefix, XWikiCriteria criteria)
        throws XWikiException
    {
        return this.xwiki.displaySearch(fieldname, className, prefix, criteria, getXWikiContext());
    }

    /**
     * API to run a search from an XWikiQuery Object An XWikiQuery object can be created from a request using the
     * createQueryFromRequest function
     * 
     * @param query query to run the search for
     * @return A list of document names matching the query
     * @throws XWikiException exception is a failure occured
     */
    public <T> List<T> search(XWikiQuery query) throws XWikiException
    {
        return this.xwiki.search(query, getXWikiContext());
    }

    /**
     * API to create a query from a request Object The request object is the result of a form created from the
     * displaySearch() and displaySearchColumns() functions
     * 
     * @param className class name to create the query from
     * @return an XWikiQuery object matching the selected values in the request object
     * @throws XWikiException exception is a failure occured
     */
    public XWikiQuery createQueryFromRequest(String className) throws XWikiException
    {
        return this.xwiki.createQueryFromRequest(className, getXWikiContext());
    }

    /**
     * API to run a search from an XWikiQuery Object and display it as a HTML table An XWikiQuery object can be created
     * from a request using the createQueryFromRequest function
     * 
     * @param query query to run the search for
     * @return An HTML table showing the result
     * @throws XWikiException exception is a failure occured
     */
    public String searchAsTable(XWikiQuery query) throws XWikiException
    {
        return this.xwiki.searchAsTable(query, getXWikiContext());
    }

    /**
     * API to get the Property object from a class based on a property path A property path looks like
     * XWiki.ArticleClass_fieldname
     * 
     * @param propPath Property path
     * @return a PropertyClass object from a BaseClass object
     */
    public com.xpn.xwiki.api.PropertyClass getPropertyClassFromName(String propPath)
    {
        return new PropertyClass(this.xwiki.getPropertyClassFromName(propPath, getXWikiContext()), getXWikiContext());
    }

    /**
     * Generates a unique page name based on initial page name and already existing pages
     * 
     * @param name
     * @return a unique page name
     */
    public String getUniquePageName(String name)
    {
        return this.xwiki.getUniquePageName(name, getXWikiContext());
    }

    /**
     * Generates a unique page name based on initial page name and already existing pages
     * 
     * @param space
     * @param name
     * @return a unique page name
     */
    public String getUniquePageName(String space, String name)
    {
        return this.xwiki.getUniquePageName(space, name, getXWikiContext());
    }

    /**
     * Inserts a tooltip using toolTip.js
     * 
     * @param html HTML viewed
     * @param message HTML Tooltip message
     * @param params Parameters in Javascropt added to the tooltip config
     * @return HTML with working tooltip
     */
    public String addTooltip(String html, String message, String params)
    {
        return this.xwiki.addTooltip(html, message, params, getXWikiContext());
    }

    /**
     * Inserts a tooltip using toolTip.js
     * 
     * @param html HTML viewed
     * @param message HTML Tooltip message
     * @return HTML with working tooltip
     */
    public String addTooltip(String html, String message)
    {
        return this.xwiki.addTooltip(html, message, getXWikiContext());
    }

    /**
     * Inserts the tooltip Javascript
     * 
     * @return
     */
    public String addTooltipJS()
    {
        return this.xwiki.addTooltipJS(getXWikiContext());
    }

    /*
     * Inserts a Mandatory asterix
     */
    public String addMandatory()
    {
        return this.xwiki.addMandatory(getXWikiContext());
    }

    /**
     * Get the XWiki Class object defined in the passed Document name.
     * <p>
     * Note: This method doesn't require any rights for accessing the passed Document (as opposed to the
     * {@link com.xpn.xwiki.api.Document#getClass()} method which does require to get a Document object first. This is
     * thus useful in cases where the calling code doesn't have the access right to the specified Document. It is safe
     * because there are no sensitive data stored in a Class definition.
     * </p>
     * 
     * @param documentName the name of the document for which to get the Class object. For example
     *            "XWiki.XWikiPreferences"
     * @return the XWiki Class object defined in the passed Document name. If the passed Document name points to a
     *         Document with no Class defined then an empty Class object is returned (i.e. a Class object with no
     *         properties).
     * @throws XWikiException if the passed document name doesn't point to a valid Document
     */
    public Class getClass(String documentName) throws XWikiException
    {
        // TODO: The implementation should be done in com.xpn.xwiki.XWiki as this class should
        // delegate all implementations to that Class.
        return new Class(this.xwiki.getDocument(documentName, this.context).getXClass(), this.context);
    }

    /**
     * Provides an absolute counter
     * 
     * @param name Counter name
     * @return String
     */
    public String getCounter(String name)
    {
        XWikiEngineContext econtext = this.context.getEngineContext();
        Integer counter = (Integer) econtext.getAttribute(name);
        if (counter == null) {
            counter = new Integer(0);
        }
        counter = new Integer(counter.intValue() + 1);
        econtext.setAttribute(name, counter);

        return counter.toString();
    }

    /**
     * Check authentication from request and set according persitent login information If it fails user is unlogged
     * 
     * @return null if failed, non null XWikiUser if sucess
     * @throws XWikiException
     */
    public XWikiUser checkAuth() throws XWikiException
    {
        return this.context.getWiki().getAuthService().checkAuth(this.context);
    }

    /**
     * Check authentication from username and password and set according persitent login information If it fails user is
     * unlogged
     * 
     * @param username username to check
     * @param password password to check
     * @param rememberme "1" if you want to remember the login accross navigator restart
     * @return null if failed, non null XWikiUser if sucess
     * @throws XWikiException
     */
    public XWikiUser checkAuth(String username, String password, String rememberme) throws XWikiException
    {
        return this.context.getWiki().getAuthService().checkAuth(username, password, rememberme, this.context);
    }

    /**
     * Access statistics api
     * 
     * @return a StatsService instance that can be used to retrieve different xwiki statistics
     */
    public StatsService getStatsService()
    {
        return this.statsService;
    }

    /**
     * API to get the xwiki criteria service which allow to create various criteria : integer ranges, date periods, date
     * intervals, etc.
     * 
     * @return the xwiki criteria service
     */
    public CriteriaService getCriteriaService()
    {
        return this.criteriaService;
    }

    /**
     * @return the ids of configured syntaxes for this wiki (eg "xwiki/1.0", "xwiki/2.0", "mediawiki/1.0", etc)
     */
    public List<String> getConfiguredSyntaxes()
    {
        return this.xwiki.getConfiguredSyntaxes();
    }

    /**
     * @return secure {@link QueryManager} for execute queries to store.
     * @deprecated since XE 2.4M2 use the Query Manager Script Service
     */
    @Deprecated
    public QueryManager getQueryManager()
    {
        return Utils.getComponent(QueryManager.class, "secure");
    }

    /**
     * API to get the Servlet path for a given wiki. In mono wiki this is "bin/" or "xwiki/". In virtual mode and if
     * <tt>xwiki.virtual.usepath</tt> is enabled in xwiki.cfg, it is "wiki/wikiname/".
     * 
     * @param wikiName wiki for which to get the path
     * @return The servlet path
     */
    public String getServletPath(String wikiName)
    {
        return this.xwiki.getServletPath(wikiName, this.context);
    }

    /**
     * API to get the Servlet path for the current wiki. In mono wiki this is "bin/" or "xwiki/". In virtual mode and if
     * <tt>xwiki.virtual.usepath</tt> is enabled in xwiki.cfg, it is "wiki/wikiname/".
     * 
     * @return The servlet path
     */
    public String getServletPath()
    {
        return this.xwiki.getServletPath(this.context.getDatabase(), this.context);
    }

    /**
     * API to get the webapp path for the current wiki. This usually is "xwiki/". It can be configured in xwiki.cfg with
     * the config <tt>xwiki.webapppath</tt>.
     * 
     * @return The servlet path
     */
    public String getWebAppPath()
    {
        return this.xwiki.getWebAppPath(this.context);
    }

    /**
     * @return the syntax id of the syntax to use when creating new documents.
     */
    public String getDefaultDocumentSyntax()
    {
        return this.xwiki.getDefaultDocumentSyntax();
    }

    /**
     * Find the corresponding available renderer syntax.
     * <p>
     * If <code>syntaxVersion</code> is null the last version of the available provided syntax type is returned.
     * 
     * @param syntaxType the syntax type
     * @param syntaxVersion the syntax version
     * @return the available corresponding {@link Syntax}. Null if no available renderer can be found.
     */
    public Syntax getAvailableRendererSyntax(String syntaxType, String syntaxVersion)
    {
        Syntax syntax = null;

        List<PrintRendererFactory> factories = Utils.getComponentList(PrintRendererFactory.class);
        for (PrintRendererFactory factory : factories) {
            Syntax factorySyntax = factory.getSyntax();
            if (syntaxVersion != null) {
                if (factorySyntax.getType().getId().equalsIgnoreCase(syntaxType)
                    && factorySyntax.getVersion().equals(syntaxVersion)) {
                    syntax = factorySyntax;
                    break;
                }
            } else {
                // TODO: improve version comparaison since it does not work when comparing 2.0 and 10.0 for example. We
                // should have a Version which implements Comparable like we have SyntaxId in Syntax
                if (factorySyntax.getType().getId().equalsIgnoreCase(syntaxType)
                    && (syntax == null || factorySyntax.getVersion().compareTo(syntax.getVersion()) > 0)) {
                    syntax = factorySyntax;
                }
            }
        }

        return syntax;
    }

    /**
     * @return the section depth for which section editing is available (can be configured through
     *         {@code xwiki.section.depth} configuration property. Defaults to 2 when not defined
     */
    public long getSectionEditingDepth()
    {
        return this.xwiki.getSectionEditingDepth();
    }

    /**
     * @return true if title handling should be using the compatibility mode or not. When the compatibility mode is
     *         active, if the document's content first header (level 1 or level 2) matches the document's title the
     *         first header is stripped.
     */
    public boolean isTitleInCompatibilityMode()
    {
        return this.xwiki.isTitleInCompatibilityMode();
    }

    /**
     * Get the syntax of the document currently being executed.
     * <p>
     * The document currently being executed is not the same than the context document since when including a page with
     * velocity #includeForm(), method for example the context doc is the includer document even if includeForm() fully
     * execute and render the included document before insert it in the includer document.
     * <p>
     * If the current document can't be found, the method assume that the executed document is the context document
     * (it's generally the case when a document is directly rendered with
     * {@link XWikiDocument#getRenderedContent(XWikiContext)} for example).
     * 
     * @return the syntax identifier
     */
    public String getCurrentContentSyntaxId()
    {
        return this.xwiki.getCurrentContentSyntaxId(getXWikiContext());
    }
}

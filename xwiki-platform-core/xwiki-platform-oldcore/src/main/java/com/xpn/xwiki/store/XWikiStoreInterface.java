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
package com.xpn.xwiki.store;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;

@Role
public interface XWikiStoreInterface
{
    void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException;

    /**
     * Atomic operation for renaming a document.
     * This operation will rename the document in DB by performing updates in all tables the document name is used.
     *
     * @param doc the actual document instance to rename.
     * @param newReference the new reference to use for renaming.
     * @param context the current context.
     * @throws XWikiException in case of problem during the rename.
     * @since 12.5RC1
     */
    default void renameXWikiDoc(XWikiDocument doc, DocumentReference newReference, XWikiContext context)
        throws XWikiException
    {
        // Avoid breaking backward compatibility.
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException;

    XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException;

    void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException;

    List<String> getClassList(XWikiContext context) throws XWikiException;

    /**
     * API allowing to count the total number of documents that would be returned by a query.
     *
     * @param wheresql Query to use, similar to the ones accepted by {@link #searchDocuments(String, XWikiContext)}. It
     *            should not contain {@code order by} or {@code group} clauses, since this kind of queries are not
     *            portable.
     * @param context The current request context.
     * @return The number of documents that matched the query.
     * @throws XWikiException if there was a problem executing the query.
     */
    int countDocuments(String wheresql, XWikiContext context) throws XWikiException;

    /**
     * @since 2.2M2
     */
    List<DocumentReference> searchDocumentReferences(String wheresql, XWikiContext context) throws XWikiException;

    /**
     * @deprecated use {@link #searchDocumentReferences(String, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated(since = "2.2M2")
    List<String> searchDocumentsNames(String wheresql, XWikiContext context) throws XWikiException;

    /**
     * @since 2.2M2
     */
    List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, XWikiContext context)
        throws XWikiException;

    /**
     * @deprecated use {@link #searchDocumentReferences(String, int, int, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated(since = "2.2M2")
    List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context) throws XWikiException;

    /**
     * @since 2.2M2
     */
    List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start, String selectColumns,
        XWikiContext context) throws XWikiException;

    /**
     * @deprecated use {@link #searchDocumentReferences(String, int, int, String, XWikiContext)}
     */
    @Deprecated(since = "2.2M2")
    List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns, XWikiContext context)
        throws XWikiException;

    /**
     * Search documents by passing HQL where clause values as parameters. This allows generating a Named HQL query which
     * will automatically encode the passed values (like escaping single quotes). This API is recommended to be used
     * over the other similar methods where the values are passed inside the where clause and for which you'll need to
     * do the encoding/escaping yourself before calling them.
     * <p>
     * Example:
     * 
     * <pre>
     * {@code
     * #set($orphans = $xwiki.searchDocuments(" where doc.fullName <> ?1 and (doc.parent = ?2 or "
     *     + "(doc.parent = ?3 and doc.space = ?4))",
     *     ["${doc.fullName}as", ${doc.fullName}, ${doc.name}, ${doc.space}]))
     * }
     * </pre>
     *
     * @param parametrizedSqlClause the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param nb the number of rows to return. If 0 then all rows are returned
     * @param start the number of rows to skip. If 0 don't skip any row
     * @param parameterValues the where clause values that replace the question marks (?)
     * @param context the XWiki context required for getting information about the execution context
     * @return a list of document references
     * @throws XWikiException in case of error while performing the query
     * @since 2.2M1
     */
    List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, int nb, int start,
        List<?> parameterValues, XWikiContext context) throws XWikiException;

    /**
     * @deprecated use {@link #searchDocumentReferences(String, int, int, List, XWikiContext)}
     */
    @Deprecated(since = "2.2M2")
    List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start, List<?> parameterValues,
        XWikiContext context) throws XWikiException;

    /**
     * Same as {@link #searchDocumentReferences(String, int, int, List, XWikiContext)} but returns all rows.
     *
     * @see #searchDocumentReferences(String, int, int, java.util.List, com.xpn.xwiki.XWikiContext)
     * @since 2.2M2
     */
    List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, List<?> parameterValues,
        XWikiContext context) throws XWikiException;

    /**
     * @deprecated use {@link #searchDocumentReferences(String, List, XWikiContext)}
     */
    @Deprecated(since = "2.2M2")
    List<String> searchDocumentsNames(String parametrizedSqlClause, List<?> parameterValues, XWikiContext context)
        throws XWikiException;

    /**
     * API allowing to count the total number of documents that would be returned by a parameterized query.
     *
     * @param parametrizedSqlClause Parameterized query to use, similar to the ones accepted by
     *            {@link #searchDocuments(String, List, XWikiContext)}. It should not contain {@code order by} or
     *            {@code group} clauses, since this kind of queries are not portable.
     * @param parameterValues The parameter values that replace the question marks.
     * @return The number of documents that matched the query.
     * @param context The current request context.
     * @throws XWikiException if there was a problem executing the query.
     */
    int countDocuments(String parametrizedSqlClause, List<?> parameterValues, XWikiContext context)
        throws XWikiException;

    /**
     * Search documents in the storing system.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     */
    List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, XWikiContext context)
        throws XWikiException;

    /**
     * Search documents in the storing system.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     */
    List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException;

    /**
     * Search documents in the storing system.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param customMapping inject custom mapping in session.
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     */
    List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        XWikiContext context) throws XWikiException;

    /**
     * Search documents in the storing system.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     */
    List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        XWikiContext context) throws XWikiException;

    /**
     * Search documents in the storing system.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param parameterValues the where clause values that replace the question marks (?).
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     * @since 1.1.2
     * @since 1.2M2
     */
    List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start,
        List<?> parameterValues, XWikiContext context) throws XWikiException;

    /**
     * Search documents in the storing system.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param customMapping inject custom mapping in session.
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     */
    List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping, int nb,
        int start, XWikiContext context) throws XWikiException;

    /**
     * Search documents in the storing system.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     */
    List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context) throws XWikiException;

    /**
     * Search documents in the storing system.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param customMapping inject custom mapping in session.
     * @param checkRight if true check for each found document if context's user has "view" rights for it.
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     */
    List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, XWikiContext context) throws XWikiException;

    /**
     * Search documents in the storing system.
     * <p>
     * Search documents by passing HQL where clause values as parameters. This allows generating a Named HQL query which
     * will automatically encode the passed values (like escaping single quotes). This API is recommended to be used
     * over the other similar methods where the values are passed inside the where clause and for which you'll need to
     * do the encoding/escpaing yourself before calling them.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param parameterValues the where clause values that replace the question marks (?).
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     * @since 1.1.2
     * @since 1.2M2
     */
    List<XWikiDocument> searchDocuments(String wheresql, List<?> parameterValues, XWikiContext context)
        throws XWikiException;

    /**
     * Search documents in the storing system.
     * <p>
     * Search documents by passing HQL where clause values as parameters. This allows generating a Named HQL query which
     * will automatically encode the passed values (like escaping single quotes). This API is recommended to be used
     * over the other similar methods where the values are passed inside the where clause and for which you'll need to
     * do the encoding/escpaing yourself before calling them.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param customMapping inject custom mapping in session.
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param parameterValues the where clause values that replace the question marks (?).
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     * @since 1.1.2
     * @since 1.2M2
     */
    List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping, int nb,
        int start, List<?> parameterValues, XWikiContext context) throws XWikiException;

    /**
     * Search documents in the storing system.
     * <p>
     * Search documents by passing HQL where clause values as parameters. This allows generating a Named HQL query which
     * will automatically encode the passed values (like escaping single quotes). This API is recommended to be used
     * over the other similar methods where the values are passed inside the where clause and for which you'll need to
     * do the encoding/escpaing yourself before calling them.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param parameterValues the where clause values that replace the question marks (?).
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     * @since 1.1.2
     * @since 1.2M2
     */
    List<XWikiDocument> searchDocuments(String wheresql, int nb, int start, List<?> parameterValues,
        XWikiContext context) throws XWikiException;

    /**
     * Search documents in the storing system.
     * <p>
     * Search documents by passing HQL where clause values as parameters. This allows generating a Named HQL query which
     * will automatically encode the passed values (like escaping single quotes). This API is recommended to be used
     * over the other similar methods where the values are passed inside the where clause and for which you'll need to
     * do the encoding/escpaing yourself before calling them.
     *
     * @param wheresql the HQL where clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param distinctbylanguage when a document has multiple version for each language it is returned as one document a
     *            language.
     * @param customMapping inject custom mapping in session.
     * @param checkRight if true check for each found document if context's user has "view" rights for it.
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param parameterValues the where clause values that replace the question marks (?).
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     * @since 1.1.2
     * @since 1.2M2
     */
    List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, boolean customMapping,
        boolean checkRight, int nb, int start, List<?> parameterValues, XWikiContext context) throws XWikiException;

    XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction) throws XWikiException;

    void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException;

    void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction) throws XWikiException;

    /**
     * @deprecated use {@link org.xwiki.link.LinkStore} APIs instead
     */
    @Deprecated(since = "14.8RC1")
    List<XWikiLink> loadLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException;

    /**
     * Returns the backlinks for the given document reference.
     *
     * @param documentReference the document reference to search backlinks for
     * @param bTransaction {@code true} if a transaction must be create
     * @param context the current context
     * @since 2.2M2
     * @deprecated use {@link org.xwiki.link.LinkStore} APIs instead
     */
    @Deprecated(since = "14.8RC1")
    List<DocumentReference> loadBacklinks(DocumentReference documentReference, boolean bTransaction,
        XWikiContext context) throws XWikiException;

    /**
     * Returns the backlinks for the given attachment reference.
     *
     * @param documentReference the attachment reference to search backlinks for
     * @param bTransaction {@code true} if a transaction must be create
     * @param context the current context
     * @since 14.2RC1
     * @deprecated use {@link org.xwiki.link.LinkStore} APIs instead
     */
    @Deprecated(since = "14.8RC1")
    default List<DocumentReference> loadBacklinks(AttachmentReference documentReference, boolean bTransaction,
        XWikiContext context) throws XWikiException
    {
        return List.of();
    }

    /**
     * @deprecated use {@link org.xwiki.link.LinkStore} APIs instead
     */
    @Deprecated(since = "2.2M2")
    List<String> loadBacklinks(String fullName, XWikiContext context, boolean bTransaction) throws XWikiException;

    /**
     * @deprecated link storage and indexing moved to Solr (implemented in xwiki-platform-search-solr-api)
     */
    @Deprecated(since = "14.8RC1")
    void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction) throws XWikiException;

    /**
     * @deprecated link storage and indexing moved to Solr (implemented in xwiki-platform-search-solr-api)
     */
    @Deprecated(since = "14.8RC1")
    void deleteLinks(long docId, XWikiContext context, boolean bTransaction) throws XWikiException;

    /**
     * Execute a reading request and return result.
     *
     * @param sql the HQL request clause. For example: {@code where doc.fullName
     *        <> ?1 and (doc.parent = ?2 or (doc.parent = ?3 and doc.space = ?4))}
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     */
    <T> List<T> search(String sql, int nb, int start, XWikiContext context) throws XWikiException;

    /**
     * Execute a reading request with parameters and return result.
     * <p>
     * Execute query by passing HQL request values as parameters. This allows generating a Named HQL query which will
     * automatically encode the passed values (like escaping single quotes). This API is recommended to be used over the
     * other similar methods where the values are passed inside the where clause and for which you'll need to do the
     * encoding/escaping yourself before calling them.
     *
     * @param sql the HQL request.
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param parameterValues the where clause values that replace the question marks (?).
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     * @since 1.1.2
     * @since 1.2M2
     */
    <T> List<T> search(String sql, int nb, int start, List<?> parameterValues, XWikiContext context)
        throws XWikiException;

    /**
     * Execute a reading request and return result.
     *
     * @param sql the HQL request.
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param whereParams if not null add to {@code sql} a where clause based on a table of table containing field name,
     *            field value and compared symbol ({@code =}, {@code >}, etc.).
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     */
    <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException;

    /**
     * Execute a reading request with parameters and return result.
     * <p>
     * Execute query by passing HQL request values as parameters. This allows generating a Named HQL query which will
     * automatically encode the passed values (like escaping single quotes). This API is recommended to be used over the
     * other similar methods where the values are passed inside the where clause and for which you'll need to do the
     * encoding/escaping yourself before calling them.
     *
     * @param sql the HQL request.
     * @param nb the number of rows to return. If 0 then all rows are returned.
     * @param start the number of rows to skip. If 0 don't skip any row.
     * @param whereParams if not null add to {@code sql} a where clause based on a table of table containing field name,
     *            field value and compared symbol ({@code =}, {@code >}, etc.).
     * @param parameterValues the where clause values that replace the question marks (?).
     * @param context the XWiki context required for getting information about the execution context.
     * @return a list of XWikiDocument.
     * @throws XWikiException in case of error while performing the query.
     * @since 1.1.2
     * @since 1.2M2
     */
    <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, List<?> parameterValues,
        XWikiContext context) throws XWikiException;

    void cleanUp(XWikiContext context);

    /**
     * Indicate if the provided wiki name could be used to create a new wiki.
     *
     * @param wikiName the name of the wiki.
     * @param context the XWiki context.
     * @return true if the name is already used, false otherwise.
     * @throws XWikiException error when looking if wiki name already used.
     */
    boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException;

    /**
     * Allows to create a new wiki database and initialize the default tables.
     *
     * @param wikiName the name of the new wiki.
     * @param context the XWiki context.
     * @throws XWikiException error when creating new wiki.
     */
    void createWiki(String wikiName, XWikiContext context) throws XWikiException;

    /**
     * Delete a wiki database.
     *
     * @param wikiName the name of the wiki.
     * @param context the XWiki context.
     * @throws XWikiException error when deleting wiki database.
     */
    void deleteWiki(String wikiName, XWikiContext context) throws XWikiException;

    boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException;

    /**
     * @deprecated use {@link #isCustomMappingValid(BaseClass, String)}
     */
    @Deprecated(since = "11.5RC1")
    boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context) throws XWikiException;

    /**
     * @since 11.5RC1
     */
    default boolean isCustomMappingValid(BaseClass bclass, String custommapping1)
    {
        try {
            return isCustomMappingValid(bclass, custommapping1, null);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @deprecated use {@link #injectCustomMapping(BaseClass)} instead
     */
    @Deprecated(since = "11.5RC1")
    boolean injectCustomMapping(BaseClass doc1class, XWikiContext xWikiContext) throws XWikiException;

    /**
     * @since 11.5RC1
     */
    default boolean injectCustomMapping(BaseClass doc1class) throws XWikiException
    {
        return injectCustomMapping(doc1class, null);
    }

    boolean injectCustomMappings(XWikiDocument doc, XWikiContext context) throws XWikiException;

    List<String> getCustomMappingPropertyList(BaseClass bclass);

    void injectCustomMappings(XWikiContext context) throws XWikiException;

    void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException;

    List<String> getTranslationList(XWikiDocument doc, XWikiContext context) throws XWikiException;

    /**
     * @return QueryManager used for creating queries to store. Use QueryManager instead of #search* methods because it
     *         is more abstract from store implementation and support multiple query languages.
     */
    QueryManager getQueryManager();

    /**
     * Get the limit size of a property.
     * 
     * @param entityType the entityType where the property is located.
     * @param property the property on which we want the limit size.
     * @param context the context of the wiki to retrieve the property
     * @return an integer representing the limit size.
     * @since 11.4RC1
     */
    int getLimitSize(XWikiContext context, Class<?> entityType, String property);
}

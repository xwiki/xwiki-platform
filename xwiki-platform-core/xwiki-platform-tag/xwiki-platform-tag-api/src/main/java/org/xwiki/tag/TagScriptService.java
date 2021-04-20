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
package org.xwiki.tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.tag.internal.TagDocumentManager;
import org.xwiki.tag.internal.TagQueryManager;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * This script service allows to manipulate tags easily. It allows to query, get, rename and delete tags.
 *
 * @version $Id$
 * @since 13.1RC1
 */
@Component
@Named("tag")
@Singleton
@Unstable
public class TagScriptService implements ScriptService
{
    private static final Pattern LIKE_ESCAPE = Pattern.compile("[_%\\\\]");

    private static final String LIKE_REPLACEMENT = "\\\\$0";

    private static final String LIKE_APPEND = ".%";

    @Inject
    private Logger logger;

    @Inject
    private TagQueryManager tagQueryManager;

    @Inject
    private TagDocumentManager tagDocumentManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private ContextualAuthorizationManager contextualAuthorization;

    /**
     * Get cardinality map of tags for a specific wiki space.
     *
     * @param space the space to get tags in
     * @return map of tags with their occurrences counts
     * @throws TagException if search query fails (possible failures: DB access problems, etc)
     */
    public Map<String, Integer> getTagCount(SpaceReference space) throws TagException
    {
        if (space != null) {
            // Make sure to escape the LIKE syntax
            String spaceReference = this.entityReferenceSerializer.serialize(space);
            String escapedSpaceReference = LIKE_ESCAPE.matcher(spaceReference).replaceAll(LIKE_REPLACEMENT);

            return getTagCountForQuery("", "(doc.space = ?1 OR doc.space LIKE ?2)",
                Arrays.asList(spaceReference, escapedSpaceReference + LIKE_APPEND));
        }

        return getTagCountForQuery(null, null, (Map<String, ?>) null);
    }

    /**
     * Get cardinality map of tags for list wiki spaces.
     *
     * @param spaces the list of space to get tags in, as a comma separated, quoted string
     * @return map of tags with their occurrences counts
     * @throws TagException if search query fails (possible failures: DB access problems, etc)
     */
    public Map<String, Integer> getTagCountForSpaces(List<SpaceReference> spaces) throws TagException
    {
        List<Object> queryParameters = new ArrayList<>();
        StringBuilder where = new StringBuilder();
        boolean first = true;
        int parameterIndex = 1;
        if (spaces == null) {
            return getTagCountForQuery("", "", Collections.emptyList());
        }
        for (SpaceReference space : spaces) {
            if (first) {
                where.append("(");
                first = false;
            } else {
                where.append(" OR ");
            }
            where.append("doc.space = ?");
            where.append(parameterIndex++);
            where.append(' ');
            String spaceReference = this.entityReferenceSerializer.serialize(space);
            queryParameters.add(spaceReference);

            where.append("OR doc.space LIKE ?");
            where.append(parameterIndex++);
            String escapedSpaceReference = LIKE_ESCAPE
                .matcher(spaceReference)
                .replaceAll(LIKE_REPLACEMENT);
            queryParameters.add(escapedSpaceReference + LIKE_APPEND);
        }
        // If first is true the "for" loop never ran, and spaces is empty so only close brace if first is false.
        if (!first) {
            where.append(')');
        }

        return getTagCountForQuery("", where.toString(), queryParameters);
    }

    /**
     * Get cardinality map of tags matching an hql query (parameterized version). Example of usage:
     * <ul>
     * <li><code>
     * $services.tag.getTagCountForQuery("", "doc.creator = :creator", {'creator' : "$!{request.creator}"})
     * </code> will return the cardinality map of tags for documents created by user-provided creator name</li>
     * </ul>
     *
     * @param from the from fragment of the query
     * @param where the parameterized where fragment from the query
     * @param parameters map of named parameters for the query
     * @return map of tags with their occurrences counts
     * @throws TagException if search query fails (possible failures: DB access problems, incorrect query
     *     fragments).
     */
    public Map<String, Integer> getTagCountForQuery(String from, String where, Map<String, ?> parameters)
        throws TagException
    {
        return this.tagQueryManager.getTagCountForQuery(from, where, parameters);
    }

    /**
     * Get cardinality map of tags matching an hql query (parameterized version). Example of usage:
     * <ul>
     * <li><code>
     * $services.tag.getTagCountForQuery("", "doc.creator = ?1", ["$!{request.creator}"])
     * </code> will return the cardinality map of tags for documents created by user-provided creator name</li>
     * </ul>
     *
     * @param from the from fragment of the query
     * @param where the parameterized where fragment from the query
     * @param parameterValues list of parameter values for the query
     * @return map of tags with their occurrences counts
     * @throws TagException if search query fails (possible failures: DB access problems, incorrect query
     *     fragments).
     */
    public Map<String, Integer> getTagCountForQuery(String from, String where, List<?> parameterValues)
        throws TagException
    {
        return this.tagQueryManager.getTagCountForQuery(from, where, parameterValues);
    }

    /**
     * Get all the documents containing the given tag.
     *
     * @param tag tag to match
     * @return list of pages
     * @throws TagException if search query fails (possible failures: DB access problems, etc)
     */
    public List<String> getDocumentsWithTag(String tag) throws TagException
    {
        return this.tagQueryManager.getDocumentsWithTag(tag);
    }

    /**
     * Count the document containing the given tag.
     *
     * @param tag the tag to match
     * @return the count of document containing the given tag
     * @throws TagException in case of error during the count
     */
    public Long countDocumentsWithTag(String tag) throws TagException
    {
        return this.tagQueryManager.countDocumentsWithTag(tag, false);
    }

    /**
     * Get tags from a document.
     *
     * @param documentReference the document reference
     * @return list of tags
     * @throws TagException if document read fails (possible failures: insufficient rights, DB access problems,
     *     etc).
     */
    public List<String> getTagsFromDocument(DocumentReference documentReference) throws TagException
    {
        return this.tagDocumentManager.getTagsFromDocument(documentReference);
    }

    /**
     * Add a tag to a document. The document is saved (minor edit) after this operation.
     *
     * @param tag the tag to add to the document
     * @param documentReference the reference of the target document
     * @return the {@link TagOperationResult result} of the operation. {@link TagOperationResult#NO_EFFECT} is returned
     *     only if all the tags were already set on the document, {@link TagOperationResult#OK} is returned even if only
     *     some of the tags are new.
     */
    public TagOperationResult addTagToDocument(String tag, DocumentReference documentReference)
    {
        return addTagsToDocument(Arrays.asList(tag), documentReference);
    }

    /**
     * Add a list of tags to a document. The document is saved (minor edit) after this operation.
     *
     * @param tags the tags to add to the document
     * @param documentReference the reference of the target document
     * @return the {@link TagOperationResult result} of the operation. {@link TagOperationResult#NO_EFFECT} is returned
     *     only if all the tags were already set on the document, {@link TagOperationResult#OK} is returned even if only
     *     some of the tags are new.
     */
    public TagOperationResult addTagsToDocument(List<String> tags, DocumentReference documentReference)
    {
        try {
            if (!this.contextualAuthorization.hasAccess(Right.EDIT, documentReference)) {
                return TagOperationResult.NOT_ALLOWED;
            }
            return this.tagDocumentManager.addTagsToDocument(tags, documentReference);
        } catch (Exception e) {
            this.logger.warn("Failed to add tag [{}] to document [{}]. Cause: [{}].", tags, documentReference,
                getRootCauseMessage(e));
            return TagOperationResult.FAILED;
        }
    }

    /**
     * Remove a tag from a document. The document is saved (minor edit) after this operation.
     *
     * @param tag tag to remove
     * @param documentReference the reference of the document
     * @return the {@link TagOperationResult result} of the operation
     */
    public TagOperationResult removeTagFromDocument(String tag, DocumentReference documentReference)
    {
        try {
            if (!this.contextualAuthorization.hasAccess(Right.EDIT, documentReference)) {
                return TagOperationResult.NOT_ALLOWED;
            }
            return this.tagDocumentManager.removeTagFromDocument(tag, documentReference);
        } catch (Exception e) {
            this.logger.warn("Failed to remove tag [{}] to document [{}]. Cause: [{}].", tag, documentReference,
                getRootCauseMessage(e));
            return TagOperationResult.FAILED;
        }
    }

    /**
     * Rename a tag in all the documents that contains it. Requires admin rights. Document containing this tag are saved
     * (minor edit) during this operation.
     *
     * @param tag tag to rename
     * @param newTag new tag
     * @return the {@link TagOperationResult result} of the operation
     */
    public TagOperationResult renameTag(String tag, String newTag)
    {
        try {
            if (!this.contextualAuthorization.hasAccess(Right.ADMIN)) {
                return TagOperationResult.NOT_ALLOWED;
            }
            return this.tagDocumentManager.renameTag(tag, newTag);
        } catch (Exception e) {
            this.logger.warn("Failed to rename tag [{}] to [{}]. Cause: [{}].", tag, newTag,
                getRootCauseMessage(e));
            return TagOperationResult.FAILED;
        }
    }

    /**
     * Delete a tag from all the documents that contains it. Requires admin rights. Document containing this tag are
     * saved (minor edit) during this operation.
     *
     * @param tag tag to delete
     * @return the {@link TagOperationResult result} of the operation
     */
    public TagOperationResult deleteTag(String tag)
    {
        try {
            if (!this.contextualAuthorization.hasAccess(Right.ADMIN)) {
                return TagOperationResult.NOT_ALLOWED;
            }
            return this.tagDocumentManager.deleteTag(tag);
        } catch (Exception e) {
            this.logger.warn("Failed to remove tag [{}]. Cause: [{}].", tag, getRootCauseMessage(e));
            return TagOperationResult.FAILED;
        }
    }
}

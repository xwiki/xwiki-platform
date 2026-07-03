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
package com.xpn.xwiki.plugin.tag;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * API for the TagPlugin. TagPlugin is a plugin that allows to manipulate tags easily. It allows to get, rename and
 * delete tags.
 * 
 * @see PluginApi
 * @version $Id$
 */
public class TagPluginApi extends PluginApi<TagPlugin>
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TagPluginApi.class);

    /** The required access level for modifying document tags. */
    private static final String TAG_ACCESS_RIGHT = "edit";

    /**
     * XWiki Plugin API constructor.
     * 
     * @param plugin The wrapped plugin.
     * @param context The current request context.
     * @see PluginApi#PluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface, XWikiContext)
     */
    public TagPluginApi(TagPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Get tags within the wiki.
     * 
     * @return list of tags.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     */
    public List<String> getAllTags() throws XWikiException
    {
        return this.getProtectedPlugin().getAllTags(this.context);
    }

    /**
     * Get cardinality map of tags within the wiki.
     * 
     * @return map of tags with their occurences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     */
    public Map<String, Integer> getTagCount() throws XWikiException
    {
        return this.getProtectedPlugin().getTagCount(this.context);
    }

    /**
     * Get cardinality map of tags for a specific wiki space.
     * 
     * @param space the space to get tags in
     * @return map of tags with their occurences counts
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 1.2
     */
    public Map<String, Integer> getTagCount(String space) throws XWikiException
    {
        return this.getProtectedPlugin().getTagCount(space, this.context);
    }

    /**
     * Get cardinality map of tags for list wiki spaces.
     * 
     * @param spaces the list of space to get tags in, as a comma separated, quoted string
     * @return map of tags with their occurences counts
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 8.1
     */
    public Map<String, Integer> getTagCountForSpaces(String spaces) throws XWikiException
    {
        return this.getProtectedPlugin().getTagCountForSpaces(spaces, this.context);
    }

    /**
     * Get cardinality map of tags matching an hql query. Examples of usage:
     * <ul>
     * <li><code>
     * $xwiki.tag.getTagCountForQuery("","doc.creator='XWiki.JohnDoe'")
     * </code> will return the cardinality map of tags for documents created by user XWiki.JohnDoe</li>
     * <li><code>
     * $xwiki.tag.getTagCountForQuery(", BaseObject as obj", 
     *    "obj.name=doc.fullName and obj.className='Blog.BlogPostClass'")
     * </code> will return the cardinality map of tags associated to blog post documents</li>
     * <li><code>
     * $xwiki.tag.getTagCountForQuery("", "")
     * </code> will return all tags within the wiki</li>
     * </ul>
     * 
     * @param from the from fragment of the query
     * @param where the where fragment from the query
     * @return map of tags with their occurrences counts
     * @throws XWikiException if search query fails (possible failures: DB access problems, incorrect query fragments).
     * @since 1.2
     * @see TagPluginApi#getTagCountForQuery(String, String, java.util.List)
     */
    public Map<String, Integer> getTagCountForQuery(String from, String where) throws XWikiException
    {
        return getTagCountForQuery(from, where, (Map) null);
    }

    /**
     * Get cardinality map of tags matching an hql query (parameterized version). Example of usage:
     * <ul>
     * <li><code>
     * $xwiki.tag.getTagCountForQuery("", "doc.creator = ?1", ["$!{request.creator}"])
     * </code> will return the cardinality map of tags for documents created by user-provided creator name</li>
     * </ul>
     * 
     * @param from the from fragment of the query
     * @param where the parameterized where fragment from the query
     * @param parameterValues list of parameter values for the query
     * @return map of tags with their occurrences counts
     * @throws XWikiException if search query fails (possible failures: DB access problems, incorrect query fragments).
     * @since 1.18
     */
    public Map<String, Integer> getTagCountForQuery(String from, String where, List<?> parameterValues)
        throws XWikiException
    {
        return this.getProtectedPlugin().getTagCountForQuery(from, where, parameterValues, this.context);
    }

    /**
     * Get cardinality map of tags matching an hql query (parameterized version). Example of usage:
     * <ul>
     * <li><code>
     * $xwiki.tag.getTagCountForQuery("", "doc.creator = :creator", {'creator' : "$!{request.creator}"})
     * </code> will return the cardinality map of tags for documents created by user-provided creator name</li>
     * </ul>
     * 
     * @param from the from fragment of the query
     * @param where the parameterized where fragment from the query
     * @param parameters map of named parameters for the query
     * @return map of tags with their occurrences counts
     * @throws XWikiException if search query fails (possible failures: DB access problems, incorrect query fragments).
     * @since 11.7RC1
     */
    public Map<String, Integer> getTagCountForQuery(String from, String where, Map<String, ?> parameters)
        throws XWikiException
    {
        return this.getProtectedPlugin().getTagCountForQuery(from, where, parameters, this.context);
    }

    /**
     * Get all the documents containing the given tag.
     * 
     * @param tag tag to match.
     * @return list of pages.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     */
    public List<String> getDocumentsWithTag(String tag) throws XWikiException
    {
        return this.getProtectedPlugin().getDocumentsWithTag(tag, this.context);
    }

    /**
     * Get tags from a document.
     * 
     * @param documentName name of the document.
     * @return list of tags.
     * @throws XWikiException if document read fails (possible failures: insufficient rights, DB access problems, etc).
     */
    public List<String> getTagsFromDocument(String documentName) throws XWikiException
    {
        return this.getProtectedPlugin().getTagsFromDocument(documentName, this.context);
    }

    /**
     * Add a tag to a document. The document is saved (minor edit) after this operation.
     * 
     * @param tag tag to set.
     * @param documentName name of the document.
     * @return the {@link TagOperationResult result} of the operation
     */
    public TagOperationResult addTagToDocument(String tag, String documentName)
    {
        TagOperationResult result;
        try {
            XWikiDocument document = this.context.getWiki().getDocument(documentName, this.context);
            if (this.context.getWiki().checkAccess(TAG_ACCESS_RIGHT, document, this.context)) {
                // Avoid modifying the cached document
                document = document.clone();

                result = this.getProtectedPlugin().addTagToDocument(tag, document, this.context);
            } else {
                result = TagOperationResult.NOT_ALLOWED;
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to add tag to document: [{}]", ex.getMessage());
            result = TagOperationResult.FAILED;
        }
        return result;
    }

    /**
     * Add a list of tags to a document. The document is saved (minor edit) after this operation
     * 
     * @param tags the comma separated list of tags to set; whitespace around the tags is stripped
     * @param documentName the name of the target document
     * @return the {@link TagOperationResult result} of the operation. {@link TagOperationResult#NO_EFFECT} is returned
     *         only if all the tags were already set on the document, {@link TagOperationResult#OK} is returned even if
     *         only some of the tags are new.
     */
    public TagOperationResult addTagsToDocument(String tags, String documentName)
    {
        TagOperationResult result;
        try {
            XWikiDocument document = this.context.getWiki().getDocument(documentName, this.context);
            if (this.context.getWiki().checkAccess(TAG_ACCESS_RIGHT, document, this.context)) {
                // Avoid modifying the cached document
                document = document.clone();

                result = this.getProtectedPlugin().addTagsToDocument(tags, document, this.context);
            } else {
                result = TagOperationResult.NOT_ALLOWED;
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to add tags to document: [{}]", ex.getMessage());
            result = TagOperationResult.FAILED;
        }
        return result;
    }

    /**
     * Remove a tag from a document. The document is saved (minor edit) after this operation.
     * 
     * @param tag tag to remove.
     * @param documentName name of the document.
     * @return the {@link TagOperationResult result} of the operation
     */
    public TagOperationResult removeTagFromDocument(String tag, String documentName)
    {
        TagOperationResult result;
        try {
            XWikiDocument document = this.context.getWiki().getDocument(documentName, this.context);
            if (this.context.getWiki().checkAccess(TAG_ACCESS_RIGHT, document, this.context)) {
                result = this.getProtectedPlugin().removeTagFromDocument(tag, documentName, this.context);
            } else {
                result = TagOperationResult.NOT_ALLOWED;
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to remove tag from document: [{}]", ex.getMessage());
            result = TagOperationResult.FAILED;
        }
        return result;
    }

    /**
     * Rename a tag in all the documents that contains it. Requires admin rights. Document containing this tag are saved
     * (minor edit) during this operation.
     * 
     * @param tag tag to rename.
     * @param newTag new tag.
     * @return the {@link TagOperationResult result} of the operation
     */
    public TagOperationResult renameTag(String tag, String newTag)
    {
        TagOperationResult result;
        try {
            if (hasAdminRights()) {
                result = this.getProtectedPlugin().renameTag(tag, newTag, this.context);
            } else {
                result = TagOperationResult.NOT_ALLOWED;
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to rename tag: [{}]", ex.getMessage());
            result = TagOperationResult.FAILED;
        }
        return result;
    }

    /**
     * Delete a tag from all the documents that contains it. Requires admin rights. Document containing this tag are
     * saved (minor edit) during this operation.
     * 
     * @param tag tag to delete.
     * @return the {@link TagOperationResult result} of the operation
     */
    public TagOperationResult deleteTag(String tag)
    {
        TagOperationResult result;
        try {
            if (hasAdminRights()) {
                result = this.getProtectedPlugin().deleteTag(tag, this.context);
            } else {
                result = TagOperationResult.NOT_ALLOWED;
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to delete tag: [{}]", ex.getMessage());
            result = TagOperationResult.FAILED;
        }
        return result;
    }
}

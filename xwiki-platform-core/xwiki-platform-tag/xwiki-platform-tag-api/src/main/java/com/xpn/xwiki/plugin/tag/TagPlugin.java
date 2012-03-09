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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * TagPlugin is a plugin that allows to manipulate tags easily. It allows to get, rename and delete tags.
 * 
 * @version $Id$
 */
public class TagPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
    /** Logging helper object. */
    public static final Logger LOGGER = LoggerFactory.getLogger(TagPlugin.class);

    /**
     * The identifier for this plugin; used for accessing the plugin from velocity, and as the action returning the
     * extension content.
     */
    public static final String PLUGIN_NAME = "tag";

    /**
     * XWiki class defining tags.
     */
    public static final String TAG_CLASS = "XWiki.TagClass";

    /**
     * XWiki property of XWiki.TagClass storing tags.
     */
    public static final String TAG_PROPERTY = "tags";

    /** L10N key for the "tag added" document edit comment. */
    public static final String DOC_COMMENT_TAG_ADDED = "plugin.tag.editcomment.added";

    /**
     * Tag plugin constructor.
     * 
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public TagPlugin(String name, String className, XWikiContext context)
    {
        super(PLUGIN_NAME, className, context);
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new TagPluginApi((TagPlugin) plugin, context);
    }

    /**
     * Get tags of the given document.
     * 
     * @param document document to search in.
     * @return list of tags. The list is a snapshot of the current tags. Changes to this list won't affect the document,
     *         and changes to the document's tags won't be visible in the returned list.
     */
    @SuppressWarnings("unchecked")
    private List<String> getTagsFromDocument(XWikiDocument document)
    {
        try {
            BaseProperty prop = (BaseProperty) document.getObject(TAG_CLASS).safeget(TAG_PROPERTY);
            return new ArrayList<String>((List<String>) prop.getValue());
        } catch (NullPointerException ex) {
            return new ArrayList<String>();
        }
    }

    /**
     * Set tags of the given document.
     * 
     * @param document document to put the tags to.
     * @param tags list of tags.
     * @param context XWiki context.
     */
    private void setDocumentTags(XWikiDocument document, List<String> tags, XWikiContext context)
    {
        BaseProperty prop = (BaseProperty) document.getObject(TAG_CLASS, true, context).safeget(TAG_PROPERTY);
        // Properties aren't added to an object unless a value is specified either from the Web or from an XML.
        if (prop == null) {
            prop = createTagProperty(document.getObject(TAG_CLASS, true, context), context);
        }
        prop.setValue(tags);
    }

    /**
     * Create and add the main tag property to the provided tag object. The new property corresponds to the definition
     * in the tag class, but in case of an error, the default type is a relational-stored list.
     * 
     * @param tagObject the target tag object
     * @param context the current request context
     * @return the created property
     * @see #TAG_PROPERTY
     */
    private BaseProperty createTagProperty(BaseObject tagObject, XWikiContext context)
    {
        BaseProperty tagProperty;
        try {
            BaseClass tagClass = context.getWiki().getClass(TAG_CLASS, context);
            PropertyClass tagPropertyDefinition = (PropertyClass) tagClass.getField(TAG_PROPERTY);
            tagProperty = tagPropertyDefinition.newProperty();
        } catch (XWikiException ex) {
            LOGGER.warn("Failed to properly create tag property for the tag object, creating a default one");
            tagProperty = new DBStringListProperty();
        }
        tagProperty.setName(TAG_PROPERTY);
        tagProperty.setObject(tagObject);
        tagObject.safeput(TAG_PROPERTY, tagProperty);
        return tagProperty;
    }

    /**
     * Get all tags within the wiki.
     * 
     * @param context XWiki context.
     * @return list of tags (alphabetical order).
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     */
    public List<String> getAllTags(XWikiContext context) throws XWikiException
    {
        List<String> results;

        String hql =
            "select distinct elements(prop.list) from BaseObject as obj, "
            + "DBStringListProperty as prop where obj.className='XWiki.TagClass' "
            + "and obj.id=prop.id.id and prop.id.name='tags'";
        results = context.getWiki().search(hql, context);
        Collections.sort(results, String.CASE_INSENSITIVE_ORDER);

        return results;
    }

    /**
     * Get cardinality map of tags within the wiki.
     * 
     * @param context XWiki context.
     * @return map of tags (alphabetical order) with their occurences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     */
    public Map<String, Integer> getTagCount(XWikiContext context) throws XWikiException
    {
        return this.getTagCountForQuery(null, null, context);
    }

    /**
     * Get cardinality map of tags for a specific wiki space.
     * 
     * @param space the wiki space to get tags from. If blank, return tags for the whole wiki.
     * @param context XWiki context.
     * @return map of tags (alphabetical order) with their occurrences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 1.2
     */
    public Map<String, Integer> getTagCount(String space, XWikiContext context) throws XWikiException
    {
        if (!StringUtils.isBlank(space)) {
            return getTagCountForQuery("", "doc.space = ?", Collections.singletonList(space), context);
        }
        return getTagCount(context);
    }

    /**
     * Get cardinality map of tags matching a hql query.
     * 
     * @param fromHql the <code>from</code> fragment of the hql query
     * @param whereHql the <code>where</code> fragment of the hql query
     * @param context XWiki context.
     * @return map of tags (alphabetical order) with their occurrences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 1.2
     * @see TagPluginApi#getTagCountForQuery(String, String)
     */
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, XWikiContext context)
        throws XWikiException
    {
        return getTagCountForQuery(fromHql, whereHql, null, context);
    }

    /**
     * Get cardinality map of tags matching a parameterized hql query.
     * 
     * @param fromHql the <code>from</code> fragment of the hql query
     * @param whereHql the <code>where</code> fragment of the hql query
     * @param parameterValues list of parameter values for the query
     * @param context XWiki context.
     * @return map of tags (alphabetical order) with their occurrences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 1.18
     * @see TagPluginApi#getTagCountForQuery(String, String, java.util.List)
     */
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, List< ? > parameterValues,
            XWikiContext context) throws XWikiException
    {
        List<String> results = null;
        Map<String, Integer> tagCount = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

        String from = "select elements(prop.list) from BaseObject as tagobject, DBStringListProperty as prop";
        String where =
            " where tagobject.className='XWiki.TagClass' and tagobject.id=prop.id.id and prop.id.name='tags'";

        // If at least one of the fragments is passed, the query should be matching XWiki documents
        if (!StringUtils.isBlank(fromHql) || !StringUtils.isBlank(whereHql)) {
            from += ", XWikiDocument as doc" + fromHql;
        }
        if (!StringUtils.isBlank(whereHql)) {
            where += " and doc.fullName=tagobject.name and " + whereHql;
        }

        List<?> params = parameterValues;
        if (params == null) {
            params = new ArrayList<String>();
        }
        results = context.getWiki().getStore().search(from + where, 0, 0, params, context);
        Collections.sort(results, String.CASE_INSENSITIVE_ORDER);
        Map<String, String> processedTags = new HashMap<String, String>();

        // We have to manually build a cardinality map since we have to ignore tags case.
        for (String result : results) {
            // This key allows to keep track of the case variants we've encountered.
            String lowerTag = result.toLowerCase();

            // We store the first case variant to reuse it in the final result set.
            if (!processedTags.containsKey(lowerTag)) {
                processedTags.put(lowerTag, result);
            }

            String tagCountKey = processedTags.get(lowerTag);
            int tagCountForTag = 0;
            if (tagCount.get(tagCountKey) != null) {
                tagCountForTag = tagCount.get(tagCountKey);
            }
            tagCount.put(tagCountKey, tagCountForTag + 1);
        }

        return tagCount;
    }

    /**
     * Get documents with the given tags.
     * 
     * @param tag a list of tags to match.
     * @param context XWiki context.
     * @return list of docNames.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     */
    public List<String> getDocumentsWithTag(String tag, XWikiContext context) throws XWikiException
    {
        String hql = ", BaseObject as obj, DBStringListProperty as prop join prop.list item "
            + "where obj.className=? and obj.name=doc.fullName and obj.id=prop.id.id "
            + "and prop.id.name='tags' and lower(item)=lower(?) order by doc.fullName";
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(TAG_CLASS);
        parameters.add(tag);

        return context.getWiki().getStore().searchDocumentsNames(hql, parameters, context);
    }

    /**
     * Get tags from a document.
     * 
     * @param documentName name of the document.
     * @param context XWiki context.
     * @return list of tags.
     * @throws XWikiException if document read fails (possible failures: insufficient rights, DB access problems, etc).
     */
    public List<String> getTagsFromDocument(String documentName, XWikiContext context) throws XWikiException
    {
        return getTagsFromDocument(context.getWiki().getDocument(documentName, context));
    }

    /**
     * Get tags from a document.
     * 
     * @param document the document.
     * @param context XWiki context.
     * @return list of tags.
     * @throws XWikiException if document read fails (possible failures: insufficient rights, DB access problems, etc).
     */
    public List<String> getTagsFromDocument(XWikiDocument document, XWikiContext context) throws XWikiException
    {
        return getTagsFromDocument(document);
    }

    /**
     * Add a tag to a document. The document is saved (minor edit) after this operation.
     * 
     * @param tag tag to set.
     * @param documentName name of the document.
     * @param context XWiki context.
     * @return the {@link TagOperationResult result} of the operation
     * @throws XWikiException if document save fails (possible failures: insufficient rights, DB access problems, etc).
     */
    public TagOperationResult addTagToDocument(String tag, String documentName, XWikiContext context)
        throws XWikiException
    {
        return addTagToDocument(tag, context.getWiki().getDocument(documentName, context), context);
    }

    /**
     * Add a tag to a document. The document is saved (minor edit) after this operation.
     * 
     * @param tag tag to set.
     * @param document the document.
     * @param context XWiki context.
     * @return the {@link TagOperationResult result} of the operation
     * @throws XWikiException if document save fails (possible failures: insufficient rights, DB access problems, etc).
     */
    public TagOperationResult addTagToDocument(String tag, XWikiDocument document, XWikiContext context)
        throws XWikiException
    {
        List<String> tags = getTagsFromDocument(document);
        if (!StringUtils.isBlank(tag) && !tags.contains(tag)) {
            tags.add(tag);
            setDocumentTags(document, tags, context);

            List<String> commentArgs = new ArrayList<String>();
            commentArgs.add(tag);
            String comment = context.getMessageTool().get(DOC_COMMENT_TAG_ADDED, commentArgs);

            // Since we're changing the document we need to set the new author
            document.setAuthorReference(context.getUserReference());

            context.getWiki().saveDocument(document, comment, true, context);

            return TagOperationResult.OK;
        }
        return TagOperationResult.NO_EFFECT;
    }

    /**
     * Add a list of tags to a document. The document is saved (minor edit) after this operation.
     * 
     * @param tags the comma separated list of tags to set; whitespace around the tags is stripped
     * @param documentName the name of the target document
     * @param context the current request context.
     * @return the {@link TagOperationResult result} of the operation. {@link TagOperationResult#NO_EFFECT} is returned
     *         only if all the tags were already set on the document, {@link TagOperationResult#OK} is returned even if
     *         only some of the tags are new.
     * @throws XWikiException if document save fails (possible failures: insufficient rights, DB access problems, etc).
     */
    public TagOperationResult addTagsToDocument(String tags, String documentName, XWikiContext context)
        throws XWikiException
    {
        return addTagsToDocument(tags, context.getWiki().getDocument(documentName, context), context);
    }

    /**
     * Add a list of tags to a document. The document is saved (minor edit) after this operation.
     * 
     * @param tags the comma separated list of tags to set; whitespace around the tags is stripped
     * @param document the target document
     * @param context the current request context
     * @return the {@link TagOperationResult result} of the operation. {@link TagOperationResult#NO_EFFECT} is returned
     *         only if all the tags were already set on the document, {@link TagOperationResult#OK} is returned even if
     *         only some of the tags are new.
     * @throws XWikiException if document save fails (possible failures: insufficient rights, DB access problems, etc).
     */
    public TagOperationResult addTagsToDocument(String tags, XWikiDocument document, XWikiContext context)
        throws XWikiException
    {
        List<String> documentTags = getTagsFromDocument(document);
        String[] newTags = tags.trim().split("\\s*+,\\s*+");
        boolean added = false;

        for (String tag : newTags) {
            if (!StringUtils.isBlank(tag) && !containsIgnoreCase(documentTags, tag)) {
                documentTags.add(tag);
                added = true;
            }
        }

        if (added) {
            setDocumentTags(document, documentTags, context);
            String comment = context.getMessageTool().get(DOC_COMMENT_TAG_ADDED, Collections.singletonList(tags));

            // Since we're changing the document we need to set the new author
            document.setAuthorReference(context.getUserReference());

            context.getWiki().saveDocument(document, comment, true, context);

            return TagOperationResult.OK;
        }

        return TagOperationResult.NO_EFFECT;
    }

    /**
     * @param collection a collection of strings
     * @param item a string
     * @return {@code true} if there is an item in the given collection that equals ignoring case the given string
     */
    private boolean containsIgnoreCase(Collection<String> collection, String item)
    {
        for (String existingItem : collection) {
            if (existingItem.equalsIgnoreCase(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a tag from a document. The document is saved (minor edit) after this operation.
     * 
     * @param tag tag to remove.
     * @param documentName name of the document.
     * @param context XWiki context.
     * @return the {@link TagOperationResult result} of the operation
     * @throws XWikiException if document save fails for some reason (Insufficient rights, DB access, etc).
     */
    public TagOperationResult removeTagFromDocument(String tag, String documentName, XWikiContext context)
        throws XWikiException
    {
        return removeTagFromDocument(tag, context.getWiki().getDocument(documentName, context), context);
    }

    /**
     * Remove a tag from a document. The document is saved (minor edit) after this operation.
     * 
     * @param tag tag to remove.
     * @param document the document.
     * @param context XWiki context.
     * @return the {@link TagOperationResult result} of the operation
     * @throws XWikiException if document save fails for some reason (Insufficient rights, DB access, etc).
     */
    public TagOperationResult removeTagFromDocument(String tag, XWikiDocument document, XWikiContext context)
        throws XWikiException
    {
        List<String> tags = getTagsFromDocument(document);
        boolean needsUpdate = false;

        ListIterator<String> it = tags.listIterator();
        while (it.hasNext()) {
            if (tag.equalsIgnoreCase(it.next())) {
                needsUpdate = true;
                it.remove();
            }
        }

        if (needsUpdate) {
            setDocumentTags(document, tags, context);
            List<String> commentArgs = new ArrayList<String>();
            commentArgs.add(tag);
            String comment = context.getMessageTool().get("plugin.tag.editcomment.removed", commentArgs);

            // Since we're changing the document we need to set the new author
            document.setAuthorReference(context.getUserReference());

            context.getWiki().saveDocument(document, comment, true, context);

            return TagOperationResult.OK;
        } else {
            // Document doesn't contain this tag.
            return TagOperationResult.NO_EFFECT;
        }
    }

    /**
     * Rename a tag.
     * 
     * @param tag tag to rename.
     * @param newTag new tag.
     * @param context XWiki context.
     * @return the {@link TagOperationResult result} of the operation
     * @throws XWikiException if document save fails for some reason (Insufficient rights, DB access, etc).
     */
    protected TagOperationResult renameTag(String tag, String newTag, XWikiContext context) throws XWikiException
    {
        List<String> docNamesToProcess = getDocumentsWithTag(tag, context);
        if (StringUtils.equals(tag, newTag) || docNamesToProcess.size() == 0 || StringUtils.isBlank(newTag)) {
            return TagOperationResult.NO_EFFECT;
        }
        List<String> commentArgs = new ArrayList<String>();
        commentArgs.add(tag);
        commentArgs.add(newTag);
        String comment = context.getMessageTool().get("plugin.tag.editcomment.renamed", commentArgs);

        for (String docName : docNamesToProcess) {
            XWikiDocument doc = context.getWiki().getDocument(docName, context);
            List<String> tags = getTagsFromDocument(doc);
            for (int i = 0; i < tags.size(); i++) {
                if (tags.get(i).equalsIgnoreCase(tag)) {
                    tags.set(i, newTag);
                }
            }
            setDocumentTags(doc, tags, context);

            // Since we're changing the document we need to set the new author
            doc.setAuthorReference(context.getUserReference());

            context.getWiki().saveDocument(doc, comment, true, context);
        }

        return TagOperationResult.OK;
    }

    /**
     * Delete a tag.
     * 
     * @param tag tag to delete.
     * @param context XWiki context.
     * @return the {@link TagOperationResult result} of the operation
     * @throws XWikiException if document save fails for some reason (Insufficient rights, DB access, etc).
     */
    protected TagOperationResult deleteTag(String tag, XWikiContext context) throws XWikiException
    {
        List<String> docsToProcess = getDocumentsWithTag(tag, context);

        if (docsToProcess.size() == 0) {
            return TagOperationResult.NO_EFFECT;
        }
        for (String docName : docsToProcess) {
            removeTagFromDocument(tag, docName, context);
        }

        return TagOperationResult.OK;
    }
}

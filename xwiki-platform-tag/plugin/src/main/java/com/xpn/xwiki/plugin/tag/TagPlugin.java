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
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * TagPlugin is a plugin that allows to manipulate tags easily. It allows to get, rename and delete tags.
 * 
 * @version $Id$
 */
public class TagPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface
{
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

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi
     */
    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new TagPluginApi((TagPlugin) plugin, context);
    }

    /**
     * Get tags of the given document.
     * 
     * @param document document to search in.
     * @return list of tags.
     */
    @SuppressWarnings("unchecked")
    private List<String> getTagsFromDocument(XWikiDocument document)
    {
        try {
            BaseProperty prop = (BaseProperty) document.getObject(TAG_CLASS).safeget(TAG_PROPERTY);
            return (List<String>) prop.getValue();
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
        prop.setValue(tags);
    }

    /**
     * Get all tags within the wiki.
     * 
     * @param context XWiki context.
     * @return list of tags (alphabetical order).
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     */
    @SuppressWarnings("unchecked")
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
     * @return map of tags (alphabetical order) with their occurences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 1.2
     */
    public Map<String, Integer> getTagCount(String space, XWikiContext context) throws XWikiException
    {
        if (!StringUtils.isBlank(space)) {
            return getTagCountForQuery("", "doc.space='" + space + "'", context);
        }
        return getTagCount(context);
    }

    /**
     * Get cardinality map of tags matching a hql query.
     * 
     * @param fromHql the <code>from</code> fragment of the hql query
     * @param whereHql the <code>where</code> fragment of the hql query
     * @param context XWiki context.
     * @return map of tags (alphabetical order) with their occurences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * @since 1.2
     * @see TagPluginApi#getTagCountForQuery(String, String)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getTagCountForQuery(String fromHql, String whereHql, XWikiContext context)
        throws XWikiException
    {
        List<String> results = null;
        Map<String, Integer> tagCount = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

        String from = "select elements(prop.list) from BaseObject as tagobject, DBStringListProperty " + "as prop";
        String where =
            " where tagobject.className='XWiki.TagClass' and tagobject.id=prop.id.id and prop.id.name='tags'";

        // If at least one of the fragments is passed, the query should be matching XWiki documents
        if (!StringUtils.isBlank(fromHql) || !StringUtils.isBlank(whereHql)) {
            from += ", XWikiDocument as doc" + fromHql;
        }
        if (!StringUtils.isBlank(whereHql)) {
            where += " and doc.fullName=tagobject.name and " + whereHql;
        }
        results = context.getWiki().search(from + where, context);
        Collections.sort(results, String.CASE_INSENSITIVE_ORDER);

        tagCount.putAll(CollectionUtils.getCardinalityMap(results));

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
    @SuppressWarnings("unchecked")
    public List<String> getDocumentsWithTag(String tag, XWikiContext context) throws XWikiException
    {
        String hql =
            "select doc.fullName from XWikiDocument as doc, BaseObject as obj, DBStringListProperty as prop "
                + "where obj.name=doc.fullName and obj.className='XWiki.TagClass' and obj.id=prop.id.id "
                + "and prop.id.name='tags' and '" + tag + "' in elements(prop.list) order by doc.name asc";
        return context.getWiki().search(hql, context);
    }

    /**
     * Get tags from a document.
     * 
     * @param fullName name of the document.
     * @param context XWiki context.
     * @return list of tags.
     * @throws XWikiException if document read fails (possible failures: insufficient rights, DB access problems, etc).
     */
    public List<String> getTagsFromDocument(String fullName, XWikiContext context) throws XWikiException
    {
        return getTagsFromDocument(context.getWiki().getDocument(fullName, context));
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
     * @param fullName name of the document.
     * @param context XWiki context.
     * @return the {@link TagOperationResult result} of the operation
     * @throws XWikiException if document save fails (possible failures: insufficient rights, DB access problems, etc).
     */
    public TagOperationResult addTagToDocument(String tag, String fullName, XWikiContext context) throws XWikiException
    {
        return addTagToDocument(tag, context.getWiki().getDocument(fullName, context), context);
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
            String comment = context.getMessageTool().get("plugin.tag.editcomment.added", commentArgs);
            context.getWiki().saveDocument(document, comment, true, context);

            return TagOperationResult.OK;
        }
        return TagOperationResult.NO_EFFECT;
    }

    /**
     * Remove a tag from a document. The document is saved (minor edit) after this operation.
     * 
     * @param tag tag to remove.
     * @param fullName name of the document.
     * @param context XWiki context.
     * @return the {@link TagOperationResult result} of the operation
     * @throws XWikiException if document save fails for some reason (Insufficient rights, DB access, etc).
     */
    public TagOperationResult removeTagFromDocument(String tag, String fullName, XWikiContext context)
        throws XWikiException
    {
        return removeTagFromDocument(tag, context.getWiki().getDocument(fullName, context), context);
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

        if (tags.contains(tag)) {
            ListIterator<String> it = tags.listIterator();
            while (it.hasNext()) {
                if (tag.equals(it.next())) {
                    it.remove();
                }
            }
            setDocumentTags(document, tags, context);

            List<String> commentArgs = new ArrayList<String>();
            commentArgs.add(tag);
            String comment = context.getMessageTool().get("plugin.tag.editcomment.removed", commentArgs);
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
                if (tags.get(i).equals(tag)) {
                    tags.set(i, newTag);
                }
            }
            setDocumentTags(doc, tags, context);
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

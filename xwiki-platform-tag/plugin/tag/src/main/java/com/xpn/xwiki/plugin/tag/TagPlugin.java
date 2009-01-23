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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * TagPlugin is a plugin that allows to manipulate tags easily.
 * It allows to get, rename and delete tags.
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
     * 
     */
    public TagPlugin(String name, String className, XWikiContext context)
    {        
        super(PLUGIN_NAME, className, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi
     * 
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
     * 
     */
    @SuppressWarnings("unchecked")
    private List<String> getTagsFromDocument(XWikiDocument document)
    {
        BaseProperty prop = (BaseProperty) document.getObject(TagPlugin.TAG_CLASS).safeget(TagPlugin.TAG_PROPERTY);
        return (List<String>) prop.getValue();
    }
    
    /**
     * Set tags of the given document.
     * 
     * @param document document to put the tags to.
     * @param tags list of tags.
     * 
     */
    private void setDocumentTags(XWikiDocument document, List<String> tags)
    {                   
        BaseProperty prop = (BaseProperty) document.getObject(TagPlugin.TAG_CLASS).safeget(TagPlugin.TAG_PROPERTY);
        prop.setValue(tags);       
    }

    /**
     * Get all tags within the wiki.
     * 
     * @param context XWiki context.
     * @return list of tags (alphabetical order).
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc). 
     * 
     */
    @SuppressWarnings("unchecked")
    public List<String> getAllTags(XWikiContext context)  throws XWikiException
    {               
        List<String> results;
        
        String hql = "select distinct elements(prop.list) from BaseObject as obj, "
                + "DBStringListProperty as prop where obj.className='XWiki.TagClass' "
                + "and obj.id=prop.id.id and prop.id.name='tags'";
        results = (List<String>) context.getWiki().search(hql, context); 
        Collections.sort(results, String.CASE_INSENSITIVE_ORDER);
        
        return results;        
    }
    
    /**
     * Get tags within the wiki with their occurences counts.
     * 
     * @param context XWiki context.
     * @return map of tags (alphabetical order) with their occurences counts.
     * @throws XWikiException if search query fails (possible failures: DB access problems, etc).
     * 
     */
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getTagCount(XWikiContext context) throws XWikiException
    {       
        String previousTag = "";
        int count = 1;
        List<String> results = null;
        Map<String, Integer> tagCount = new TreeMap<String, Integer>(String.CASE_INSENSITIVE_ORDER);

        String hql = "select elements(prop.list) from BaseObject as obj, DBStringListProperty "
                + "as prop where obj.className='XWiki.TagClass' and obj.id=prop.id.id and prop.id.name='tags'";        
        results = (List<String>) context.getWiki().search(hql, context);
        Collections.sort(results);
                
        for (String tag : results) {
            if (tag.equals(previousTag)) {
                count++;
            } else {
                if (!StringUtils.isBlank(previousTag)) {
                    tagCount.put(previousTag, count);                                    
                    count = 1;
                }
                previousTag = tag;
            }            
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
     * 
     */
    @SuppressWarnings("unchecked")
    public List<String> getDocumentsWithTag(String tag, XWikiContext context) throws XWikiException
    {
        String hql = "select doc.fullName from XWikiDocument as doc, BaseObject as obj, DBStringListProperty as prop "
                + "where obj.name=doc.fullName and obj.className='XWiki.TagClass' and obj.id=prop.id.id "
                + "and prop.id.name='tags' and '" + tag + "' in elements(prop.list) order by doc.name asc";
        return (List<String>) context.getWiki().search(hql, context);                                  
    }
    
    /**
     * Get tags from a document. 
     *  
     * @param fullName name of the document.
     * @param context XWiki context.
     * @return list of tags. 
     * @throws XWikiException if document read fails (possible failures: insufficient rights, DB access problems, etc). 
     * 
     */
    public List<String> getTagsFromDocument(String fullName, XWikiContext context) throws XWikiException
    {
        XWikiDocument document = context.getWiki().getDocument(fullName, context);        
        return getTagsFromDocument(document);
    }
    
    /**
     * Add a tag to a document.
     * The document is saved (minor edit) after this operation.
     * 
     * @param tag tag to set.
     * @param fullName name of the document.
     * @param context XWiki context.
     * @return true if the tag has been added, false if the tag was already present.
     * @throws XWikiException if document save fails (possible failures: insufficient rights, DB access problems, etc).
     * 
     */
    public boolean addTagToDocument(String tag, String fullName, XWikiContext context) throws XWikiException
    {        
        List<String> commentArgs = new ArrayList<String>();
        commentArgs.add(tag);
        String comment = context.getMessageTool().get("plugin.tag.editcomment.added", commentArgs);
        XWikiDocument doc = context.getWiki().getDocument(fullName, context);        
        if (!getTagsFromDocument(doc).contains(tag)) {
            List<String> tags = getTagsFromDocument(doc);
            tags.add(tag);
            setDocumentTags(doc, tags);
            context.getWiki().saveDocument(doc, comment, true, context);            
            return true;
        } else {
            // Document already contains this tag.
            return false;
        }
    }
    
    /**
     * Remove a tag from a document.
     * The document is saved (minor edit) after this operation.
     * 
     * @param tag tag to remove.
     * @param fullName name of the document.
     * @param context XWiki context.
     * @return true if the tag has been removed, false if the tag was not present.
     * @throws XWikiException if document save fails for some reason (Insufficient rights, DB access, etc). 
     * 
     */
    public boolean removeTagFromDocument(String tag, String fullName, XWikiContext context) throws XWikiException
    {
        List<String> commentArgs = new ArrayList<String>();
        commentArgs.add(tag);        
        String comment = context.getMessageTool().get("plugin.tag.editcomment.removed", commentArgs);
        XWikiDocument doc = context.getWiki().getDocument(fullName, context);
        List<String> tags = getTagsFromDocument(doc);
        
        if (tags.contains(tag)) {
            int i = 0;
            while (i < tags.size()) {
                if (tags.get(i).equals(tag)) {
                    tags.remove(i);
                    if (i > 0) {
                        i--;
                    }
                } else {
                    i++;
                }
            }        
            setDocumentTags(doc, tags);
            context.getWiki().saveDocument(doc, comment, true, context);
            
            return true;
        } else {
            // Document doesn't contain this tag.
            return false;
        }
    }

    /**
     * Rename a tag.
     * 
     * @param tag tag to rename.
     * @param newTag new tag.
     * @param context XWiki context.
     * @return true if the rename has succeeded.
     * @throws XWikiException if document save fails for some reason (Insufficient rights, DB access, etc).
     *  
     */
    protected boolean renameTag(String tag, String newTag, XWikiContext context) throws XWikiException
    {
        List<String> docNamesToProcess = getDocumentsWithTag(tag, context); 
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
            setDocumentTags(doc, tags);            
            context.getWiki().saveDocument(doc, comment, true, context);            
        }
        
        return true;
    }

    /**
     * Delete a tag.
     * 
     * @param tag tag to delete.
     * @param context XWiki context.
     * @return true if the delete has succeeded.
     * @throws XWikiException if document save fails for some reason (Insufficient rights, DB access, etc).
     *  
     */    
    protected boolean deleteTag(String tag, XWikiContext context) throws XWikiException
    {
        List<String> docsToProcess = getDocumentsWithTag(tag, context);        
        
        for (String docName : docsToProcess) {                               
            removeTagFromDocument(tag, docName, context);
        }                
        
        return true;
    }    
}

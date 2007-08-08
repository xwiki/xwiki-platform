/*
 * Copyright 2005-2007, XpertNet SARL, and individual contributors.
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
 *
 */
package com.xpn.xwiki.doc;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.suigeneris.jrcs.rcs.Version;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.rcs.XWikiPatch;
import com.xpn.xwiki.doc.rcs.XWikiRCSArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * Contains document history.
 * Allows to load any version of document.
 * @version $Id: $ 
 */
public class XWikiDocumentArchive
{
    /** =docId. */
    private long id;
    /** SortedMap from Version to XWikiRCSNodeInfo. */
    private SortedMap  versionToNode = new TreeMap();
    /** SortedSet of Version - versions which has full document, not patch.
     *  Latest version is always full. */
    private SortedSet  fullVersions     = new TreeSet();
    
    // store-specific information
    /** Set of {@link XWikiRCSNodeInfo} which need to delete. */
    private Set deletedNodes = new TreeSet();
    /** Set of {@link XWikiRCSNodeInfo} which need to saveOrUpdate. */
    private Set updatedNodeInfos = new TreeSet();
    /** Set of {@link XWikiRCSNodeContent} which need to update. */
    private Set updatedNodeContents = new TreeSet();
    
    /** @param id = {@link XWikiDocument#getId()} */
    public XWikiDocumentArchive(long id) {
        this();
        setId(id);
    }
    /** default constructor. */
    public XWikiDocumentArchive() { }
    
    // helper methods
    /**
     * @param cur - current version
     * @param isMinor - is modification is minor
     * @return next version
     */
    protected Version createNextVersion(Version cur, boolean isMinor) {
        Version result;
        if (cur == null) {
            result = new Version(1, 1);
        } else if (!isMinor) {
            result = cur.getBase(1).next().newBranch(1);
        } else {
            result = cur.next();
        }
        return result;
    }
    /** @param node - node added to versionToNode and fullNodes */
    protected void updateNode(XWikiRCSNodeInfo node) {
        Version ver = node.getId().getVersion();
        versionToNode.put(ver, node);
        if (!node.isDiff()) {
            fullVersions.add(ver);
        } else {
            fullVersions.remove(ver);
        }
    }
    /** 
     * @param ver - version of new node
     * @return new {@link XWikiRCSNodeId} constructing from getId() and ver
     */
    protected XWikiRCSNodeId newNodeId(Version ver) {
        return new XWikiRCSNodeId(getId(), ver);
    }
    /**
     * Make a patch. It is store only modified nodes(latest). New nodes need be saved after.
     * @param newnode    - new node information
     * @param doc        - document for that patch created
     * @param context    - used for loading node contents and generating xml
     * @return node content for newnode
     * @throws XWikiException if exception while loading content
     */
    protected XWikiRCSNodeContent makePatch(XWikiRCSNodeInfo newnode, XWikiDocument doc,
        XWikiContext context) throws XWikiException
    {
        XWikiRCSNodeContent result = new XWikiRCSNodeContent();
        result.setPatch(new XWikiPatch().setFullVersion(doc, context));
        newnode.setContent(result);
        XWikiRCSNodeInfo latestnode = getLatestNode();
        if (latestnode != null) {
            int nodescount = getNodes().size();
            int nodesperfull = context.getWiki() == null ? 5 : Integer.parseInt(
                context.getWiki().getConfig().getProperty("xwiki.store.rcs.nodesPerFull", "5"));
            if (nodesperfull <= 0 || (nodescount % nodesperfull) != 0) {
                XWikiRCSNodeContent latestcontent = latestnode.getContent(context);
                latestcontent.getPatch().setDiffVersion(doc, 
                    latestcontent.getPatch().getContent(), context);
                latestnode.setContent(latestcontent);
                updateNode(latestnode);
                getUpdetedNodeContents().add(latestcontent);
            }
        }
        return result;
    }
    /** @return  {@link XWikiDocument#getId()} - primary key */
    public long getId() {
        return id;
    }
    /** @param id = {@link XWikiDocument#getId()} */
    public void setId(long id) {
        this.id = id;
    }
    /** @return collection of XWikiRCSNodeInfo order by version desc */
    public Collection getNodes() {
        return versionToNode.values();
    }
    /** 
     * @return collection of XWikiRCSNodeInfo where vfrom>=version>=vto order by version desc
     * @param vfrom - start version
     * @param vto   - end version
     */
    public Collection getNodes(Version vfrom, Version vto) {
        int[] ito = vto.getNumbers();
        ito[1]--;
        return versionToNode.subMap(vfrom, new Version(ito)).values();
    }
    /** @param versions - collection of XWikiRCSNodeInfo */
    public void setNodes(Collection versions) {
        resetArchive();
        for (Iterator it = versions.iterator(); it.hasNext();) {
            updateNode((XWikiRCSNodeInfo) it.next());
        }
        if (getNodes().size() > 0) {
            // ensure latest version is full
            getLatestNode().setDiff(false);
            updateNode(getLatestNode());
        }
    }
    /**
     * @param context - used for load nodes content
     * @return serialization of class
     * used in {@link PackagePlugin}.
     * @throws XWikiException if any error 
     */
    public String getArchive(XWikiContext context) throws XWikiException {
        XWikiRCSArchive archive = new XWikiRCSArchive(getNodes(), context);        
        return archive.toString();
    }
    /**
     * Deserialize class.
     * Used in {@link PackagePlugin}.
     * @param text - archive in JRCS format 
     * @throws XWikiException if parse error
     */
    public void setArchive(String text) throws XWikiException {
        XWikiRCSArchive archive;
        try {
            archive = new XWikiRCSArchive(text);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF, 
                XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR, 
                "Exception while constructing document archive", e);
        }
        resetArchive();
        Collection nodes = archive.getNodes(getId());
        for (Iterator it = nodes.iterator(); it.hasNext();) {
            XWikiRCSNodeInfo    nodeinfo    = (XWikiRCSNodeInfo) it.next();
            XWikiRCSNodeContent nodecontent = (XWikiRCSNodeContent) it.next();
            updateNode(nodeinfo);
            updatedNodeInfos.add(nodeinfo);
            updatedNodeContents.add(nodecontent);
        }
    }
    /**
     * Update history with new document version.
     * @param author  - author of version 
     * @param date    - date of version
     * @param comment - version comment
     * @param isMinor - is minor version
     * @param doc     - document for this version
     * @param context - used for loading nodes content
     * @throws XWikiException in any error
     */
    public void updateArchive(String author, Date date, String comment, boolean isMinor, 
        XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        XWikiRCSNodeInfo newnode = new XWikiRCSNodeInfo(newNodeId(
            createNextVersion(getLatestVersion(), isMinor)));
        newnode.setAuthor(author);
        newnode.setComment(comment);
        newnode.setDate(date);
        XWikiRCSNodeContent newcontent = makePatch(newnode, doc, context);
        
        updateNode(newnode);
        updatedNodeInfos.add(newnode);
        updatedNodeContents.add(newcontent);
    }
    /**
     * Remove document versions from vfrom to vto, inclusive.
     * @param context - used for loading nodes content
     * @param vfrom   - start version
     * @param vto     - end version
     * @param doc     - document for this archive
     * @throws XWikiException if any error 
     */
    public void removeVersions(Version vfrom, Version vto, XWikiDocument doc, XWikiContext context)
        throws XWikiException 
    {
        Version vfrom0 = vfrom;
        Version vto0 = vto;
        if (vfrom0.compareVersions(vto0) < 0) {
            Version tmp = vfrom0;
            vfrom0 = vto0;
            vto0 = tmp;
        }
        Version vfrom1 = getNextVersion(vfrom0);
        Version vto1 = getPrevVersion(vto0);
        if (vfrom1 == null && vto1 == null) {
            resetArchive();
            return;
        }
        if (vfrom1 == null) {
            // store full version in vto1
            XWikiDocument docto1 = loadDocument(doc, vto1, context);
            XWikiRCSNodeInfo nito1 = getNode(vto1);
            XWikiRCSNodeContent ncto1 = nito1.getContent(context);
            ncto1.getPatch().setFullVersion(docto1, context);
            nito1.setContent(ncto1);
            updateNode(nito1);
            getUpdetedNodeContents().add(ncto1);
        } else if (vto1 != null) {
            XWikiDocument docfrom1    = loadDocument(doc, vfrom1, context);
            XWikiDocument docto1      = loadDocument(doc, vto1, context);
            XWikiRCSNodeInfo nito1    = getNode(vto1);
            XWikiRCSNodeContent ncto1 = nito1.getContent(context);
            ncto1.getPatch().setDiffVersion(docfrom1, docto1, context);
            nito1.setContent(ncto1);
            updateNode(nito1);
            getUpdetedNodeContents().add(ncto1);
        } // if (vto1==null) => nothing to do, except delete
        for (Iterator it = getNodes(vfrom0, vto0).iterator(); it.hasNext();) {
            XWikiRCSNodeInfo ni = (XWikiRCSNodeInfo) it.next();
            fullVersions.remove(ni.getId().getVersion());
            deletedNodes.add(ni);
            it.remove();
        }
    }
    /**
     * @return selected version of document
     * @param origdoc - load old version for this document
     * @param version - which version to load
     * @param context - used for loading
     * @throws XWikiException if any error
     */
    public XWikiDocument loadDocument(XWikiDocument origdoc, Version version, XWikiContext context)
        throws XWikiException
    {
        return context.getWiki().getVersioningStore().loadXWikiDoc(origdoc, 
            version.toString(), context);
    }
    /** 
     * @return {@link XWikiRCSNodeInfo} by version. null if none.
     * @param version which version to get
     */
    public XWikiRCSNodeInfo getNode(Version version)
    {
        return version == null ? null : (XWikiRCSNodeInfo) versionToNode.get(version);
    }
    /** @return latest version in history for document. null if none. */
    public Version getLatestVersion()
    {
        return versionToNode.size() == 0 ? null : (Version) versionToNode.firstKey();
    }
    /** @return latest node in history for document. null if none. */
    public XWikiRCSNodeInfo getLatestNode() {
        return getNode(getLatestVersion());
    }
    /** 
     * @return next version in history. null if none
     * @param ver - current version
     */    
    public Version getNextVersion(Version ver) {
        // headMap is exclusive
        SortedMap headmap = versionToNode.headMap(ver);
        return (headmap.size() == 0) ? null : (Version) headmap.lastKey();
    }
    /**
     * @return previous version in history. null if none
     * @param ver - current version
     */
    public Version getPrevVersion(Version ver) {
        // tailMap is inclusive
        SortedMap tailmap = versionToNode.tailMap(ver);
        if (tailmap.size() <= 1) {
            return null;
        }
        Iterator it = tailmap.keySet().iterator();
        it.next();
        return (Version) it.next();
    }
    /**
     * @param ver - for what version find nearest
     * @return nearest version which contain full information (not patch)
     */
    public Version getNearestFullVersion(Version ver)
    {
        if (fullVersions.contains(ver)) {
            return ver;
        }
        SortedSet headSet = fullVersions.headSet(ver);
        return (Version) ((headSet.size() == 0) ? null : headSet.last());        
    }
    /** reset history. history becomes empty. */
    public void resetArchive()
    {
        versionToNode.clear();
        fullVersions.clear();
        deletedNodes.addAll(updatedNodeInfos);
        updatedNodeInfos.clear();
        updatedNodeContents.clear();
    }
    /** @return mutable Set of {@link XWikiRCSNodeInfo} which are need for delete  */
    public Set getDeletedNodeInfo()
    {
        return deletedNodes;
    }
    /** @return mutable Set of {@link XWikiRCSNodeInfo} which are need for saveOrUpdate */
    public Set getUpdetedNodeInfos()
    {
        return updatedNodeInfos; 
    }
    /** @return mutable Set of {@link XWikiRCSNodeContent} which are need for update */
    public Set getUpdetedNodeContents()
    {
        return updatedNodeContents; 
    }    
}

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
 *
 */
package com.xpn.xwiki.doc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.util.ToString;

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
        XWikiRCSNodeInfo latestNode = getLatestNode();
        if (latestNode != null) {
            int nodesCount = getNodes().size();
            int nodesPerFull = context.getWiki() == null ? 5 : Integer.parseInt(
                context.getWiki().getConfig().getProperty("xwiki.store.rcs.nodesPerFull", "5"));
            if (nodesPerFull <= 0 || (nodesCount % nodesPerFull) != 0) {
                XWikiRCSNodeContent latestContent = latestNode.getContent(context);
                latestContent.getPatch().setDiffVersion(doc, 
                    latestContent.getPatch().getContent(), context);
                latestNode.setContent(latestContent);
                updateNode(latestNode);
                getUpdatedNodeContents().add(latestContent);
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
        try {
            XWikiRCSArchive archive = new XWikiRCSArchive(text);
            resetArchive();
            Collection nodes = archive.getNodes(getId());
            for (Iterator it = nodes.iterator(); it.hasNext();) {
                XWikiRCSNodeInfo    nodeInfo    = (XWikiRCSNodeInfo) it.next();
                XWikiRCSNodeContent nodeContent = (XWikiRCSNodeContent) it.next();
                // if archive is old. (there is no author, comment, date fields in archive nodes)
                if (nodeInfo.getAuthor() == null || nodeInfo.getAuthor().indexOf('.') < 0) {
                    Version ver = nodeInfo.getVersion();
                    String xml = archive.getRevisionAsString(ver);
                    XWikiDocument doc = new XWikiDocument();
                    doc.fromXML(xml);
                    // set this fields from old document
                    nodeInfo.setAuthor(doc.getAuthor());
                    nodeInfo.setComment(doc.getComment());
                    nodeInfo.setDate(doc.getDate());
                }
                
                updateNode(nodeInfo);
                updatedNodeInfos.add(nodeInfo);
                updatedNodeContents.add(nodeContent);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF, 
                XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR, 
                "Exception while constructing document archive", e);
        }
    }
    /**
     * Update history with new document version.
     * @param doc     - document for this version
     * @param author  - author of version 
     * @param date    - date of version
     * @param comment - version comment
     * @param version - preferably document version in history
     * @param context - used for loading nodes content
     * @throws XWikiException in any error
     */
    public void updateArchive(XWikiDocument doc, String author, Date date, String comment,
        Version version, XWikiContext context) throws XWikiException
    {
        Version oldLatestVer = getLatestVersion();
        Version newVer = version;
        if (newVer == null || oldLatestVer != null && newVer.compareVersions(oldLatestVer) <= 0) {
            newVer = createNextVersion(oldLatestVer, doc.isMinorEdit());
        }
        XWikiRCSNodeInfo newNode = new XWikiRCSNodeInfo(new XWikiRCSNodeId(getId(), newVer));
        newNode.setAuthor(author);
        newNode.setComment(comment);
        newNode.setDate(date);
        XWikiRCSNodeContent newContent = makePatch(newNode, doc, context);
        
        updateNode(newNode);
        updatedNodeInfos.add(newNode);
        updatedNodeContents.add(newContent);
    }
    /**
     * Remove document versions from vfrom to vto, inclusive.
     * @param context - used for loading nodes content
     * @param vfrom   - start version
     * @param vto     - end version
     * @throws XWikiException if any error 
     */
    public void removeVersions(Version vfrom, Version vto, XWikiContext context)
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
            XWikiDocument docto1 = loadDocument(vto1, context);
            XWikiRCSNodeInfo nito1 = getNode(vto1);
            XWikiRCSNodeContent ncto1 = nito1.getContent(context);
            ncto1.getPatch().setFullVersion(docto1, context);
            nito1.setContent(ncto1);
            updateNode(nito1);
            getUpdatedNodeContents().add(ncto1);
        } else if (vto1 != null) {
            XWikiDocument docfrom1    = loadDocument(vfrom1, context);
            XWikiDocument docto1      = loadDocument(vto1, context);
            XWikiRCSNodeInfo nito1    = getNode(vto1);
            XWikiRCSNodeContent ncto1 = nito1.getContent(context);
            ncto1.getPatch().setDiffVersion(docfrom1, docto1, context);
            nito1.setContent(ncto1);
            updateNode(nito1);
            getUpdatedNodeContents().add(ncto1);
        } // if (vto1==null) => nothing to do, except delete
        for (Iterator it = getNodes(vfrom0, vto0).iterator(); it.hasNext();) {
            XWikiRCSNodeInfo ni = (XWikiRCSNodeInfo) it.next();
            fullVersions.remove(ni.getId().getVersion());
            deletedNodes.add(ni);
            it.remove();
        }
    }
    /**
     * @return selected version of document, null if version is not found.
     * @param version - which version to load
     * @param context - used for loading
     * @throws XWikiException if any error
     */
    public XWikiDocument loadDocument(Version version, XWikiContext context)
        throws XWikiException
    {
        XWikiRCSNodeInfo nodeInfo = getNode(version);
        if (nodeInfo == null) {
            return null;            
        }
        try {    
            Version nearestFullVersion = getNearestFullVersion(version);
            
            List lstContent = loadRCSNodeContents(nearestFullVersion, version, context);
            List origText = new ArrayList();
            for (Iterator it = lstContent.iterator(); it.hasNext();) {
                XWikiRCSNodeContent nodeContent = (XWikiRCSNodeContent) it.next();
                nodeContent.getPatch().patch(origText);
            }
            
            String content = ToString.arrayToString(origText.toArray());
            XWikiDocument doc = new XWikiDocument();
            doc.fromXML(content);
            
            doc.setRCSVersion(version);
            doc.setComment(nodeInfo.getComment());
            doc.setDate(nodeInfo.getDate());
            doc.setCreationDate(nodeInfo.getDate());
            doc.setContentUpdateDate(nodeInfo.getDate());
            doc.setAuthor(nodeInfo.getAuthor());
            doc.setMinorEdit(nodeInfo.isMinorEdit());
            doc.setMostRecent(version.equals(getLatestVersion()));
            return doc;
        } catch (Exception e) {
            Object[] args = {version.toString()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, 
                XWikiException.ERROR_XWIKI_STORE_RCS_READING_REVISIONS,
                    "Exception while reading document version {1}", e, args);
        }
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
    /**
     * @return List of {@link XWikiRCSNodeContent} where vfrom<=version<=vto order by version
     * @param vfrom - start version
     * @param vto - end version
     * @param context - used everywhere
     * @throws XWikiException if any error
     */
    private List loadRCSNodeContents(Version vfrom, Version vto, XWikiContext context)
        throws XWikiException
    {
        List result = new ArrayList();
        for (Iterator it = getNodes(vfrom, vto).iterator(); it.hasNext();) {
            XWikiRCSNodeInfo nodeInfo = (XWikiRCSNodeInfo) it.next();
            XWikiRCSNodeContent nodeContent = nodeInfo.getContent(context);
            result.add(nodeContent);
        }
        return result;
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
    public Set getUpdatedNodeInfos()
    {
        return updatedNodeInfos; 
    }
    /** @return mutable Set of {@link XWikiRCSNodeContent} which are need for update */
    public Set getUpdatedNodeContents()
    {
        return updatedNodeContents; 
    }
    /**
     * @return full copy of this archive with specified docId
     * @param docId - new {@link #getId()}
     * @param context - used for loading content
     * @throws XWikiException if any error
     */
    public XWikiDocumentArchive clone(long docId, XWikiContext context) throws XWikiException
    {
        XWikiDocumentArchive result = new XWikiDocumentArchive(docId);
        result.setArchive(getArchive(context));
        return result;
    }
}

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
import org.xwiki.model.reference.WikiReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.rcs.XWikiPatch;
import com.xpn.xwiki.doc.rcs.XWikiRCSArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * Contains document history. Allows to load any version of document.
 *
 * @version $Id$
 */
public class XWikiDocumentArchive
{
    private final WikiReference wikiReference;

    /** =docId. */
    private long id;

    /** SortedMap from Version to XWikiRCSNodeInfo. */
    private SortedMap<Version, XWikiRCSNodeInfo> versionToNode = new TreeMap<Version, XWikiRCSNodeInfo>();

    /**
     * SortedSet of Version - versions which has full document, not patch. Latest version is always full.
     */
    private SortedSet<Version> fullVersions = new TreeSet<Version>();

    // store-specific information
    /** Set of {@link XWikiRCSNodeInfo} which need to delete. */
    private Set<XWikiRCSNodeInfo> deletedNodes = new TreeSet<XWikiRCSNodeInfo>();

    /** Set of {@link XWikiRCSNodeInfo} which need to saveOrUpdate. */
    private Set<XWikiRCSNodeInfo> updatedNodeInfos = new TreeSet<XWikiRCSNodeInfo>();

    /** Set of {@link XWikiRCSNodeContent} which need to update. */
    private Set<XWikiRCSNodeContent> updatedNodeContents = new TreeSet<XWikiRCSNodeContent>();

    /**
     * @param wikiReference the wiki of the document
     * @param id the local identifier of the document
     * @since 13.10.7
     * @since 14.4.1
     * @since 14.5
     */
    public XWikiDocumentArchive(WikiReference wikiReference, long id)
    {
        this.wikiReference = wikiReference;
        this.id = id;
    }

    /**
     * @param id the local identifier of the document
     * @deprecated since 13.10.7, 14.4.1, 14.5, use {@link #XWikiDocumentArchive(WikiReference, long)} instead
     */
    @Deprecated(since = "13.10.7")
    public XWikiDocumentArchive(long id)
    {
        this(null, id);
    }

    /**
     * @deprecated since 13.10.7, 14.4.1, 14.5, use {@link #XWikiDocumentArchive(WikiReference, long)} instead
     */
    @Deprecated(since = "13.10.7")
    public XWikiDocumentArchive()
    {
        this.wikiReference = null;
    }

    // helper methods

    /**
     * @param cur - current version
     * @param isMinor - is modification is minor
     * @return next version
     */
    protected Version createNextVersion(Version cur, boolean isMinor)
    {
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
    protected void updateNode(XWikiRCSNodeInfo node)
    {
        Version ver = node.getId().getVersion();
        this.versionToNode.put(ver, node);
        if (!node.isDiff()) {
            this.fullVersions.add(ver);
        } else {
            this.fullVersions.remove(ver);
        }
    }

    /**
     * Make a patch. It is store only modified nodes(latest). New nodes need be saved after.
     *
     * @param newnode - new node information
     * @param doc - document for that patch created
     * @param context - used for loading node contents and generating xml
     * @return node content for newnode
     * @throws XWikiException if exception while loading content
     */
    protected XWikiRCSNodeContent makePatch(XWikiRCSNodeInfo newnode, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        XWikiRCSNodeContent result = new XWikiRCSNodeContent();
        result.setPatch(new XWikiPatch().setFullVersion(doc, context));
        newnode.setContent(result);
        XWikiRCSNodeInfo latestNode = getLatestNode();
        if (latestNode != null) {
            int nodesCount = getNodes().size();
            int nodesPerFull = context.getWiki() == null ? 5
                : Integer.parseInt(context.getWiki().getConfig().getProperty("xwiki.store.rcs.nodesPerFull", "5"));
            if (nodesPerFull <= 0 || (nodesCount % nodesPerFull) != 0) {
                XWikiRCSNodeContent latestContent = latestNode.getContent(context);
                latestContent.getPatch().setDiffVersion(latestContent.getPatch().getContent(), doc, context);
                latestNode.setContent(latestContent);
                updateNode(latestNode);
                getUpdatedNodeContents().add(latestContent);
            }
        }
        return result;
    }

    /**
     * @return the wikiReference the wiki of the document
     * @since 13.10.7
     * @since 14.4.1
     * @since 14.5
     */
    public WikiReference getWikiReference()
    {
        return this.wikiReference;
    }

    /** @return {@link XWikiDocument#getId()} - primary key */
    public long getId()
    {
        return this.id;
    }

    /**
     * @param id = {@link XWikiDocument#getId()}
     * @deprecated since 13.10.7, 14.4.1, 14.5, use {@link #XWikiDocumentArchive(WikiReference, long)} instead
     */
    @Deprecated(since = "13.10.7")
    public void setId(long id)
    {
        this.id = id;
    }

    /** @return collection of XWikiRCSNodeInfo order by version desc */
    public Collection<XWikiRCSNodeInfo> getNodes()
    {
        return this.versionToNode.values();
    }

    /**
     * Be careful when using that method: the first argument is the upper bound and the second is the lower
     * bound. So if you want to get nodes for versions between [2.1,3.4] you need to call it with {@code 3.4,2.1}.
     *
     * @return collection of XWikiRCSNodeInfo where upperBound &gt;= version &gt;= lowerBound order by version desc
     * @param upperBound - start version (upper bound)
     * @param lowerBound - end version (lower bound)
     */
    public Collection<XWikiRCSNodeInfo> getNodes(Version upperBound, Version lowerBound)
    {
        int[] ito = lowerBound.getNumbers();
        // We do perform this call because by default subMap considers the first argument as inclusive and the second
        // as exclusive. So if we call subMap(2.2,2.2) and we do have a version 2.2 it will return no version.
        // So if getNodes is called with [2.2,2.2] submap will be called with (2.2,2.1): first argument is inclusive,
        // and second exclusive but it's a lower bound than 2.2, so 2.2 version will be retrieved.
        // Note that it works because we never have a version X.0 in XWiki: we always have X.1, so if we want to get
        // [2.1,2.1] it will look for (2.1,2.0) and it works.
        if (ito.length > 1) {
            ito[1]--;
        }
        return this.versionToNode.subMap(upperBound, new Version(ito)).values();
    }

    /** @param versions - collection of XWikiRCSNodeInfo */
    public void setNodes(Collection<XWikiRCSNodeInfo> versions)
    {
        resetArchive();
        for (XWikiRCSNodeInfo node : versions) {
            updateNode(node);
        }
        if (getNodes().size() > 0) {
            // ensure latest version is full
            getLatestNode().setDiff(false);
            updateNode(getLatestNode());
        }
    }

    /**
     * @param context - used for load nodes content
     * @return serialization of class used in {@link com.xpn.xwiki.plugin.packaging.PackagePlugin}.
     * @throws XWikiException if any error
     */
    public String getArchive(XWikiContext context) throws XWikiException
    {
        XWikiRCSArchive archive = new XWikiRCSArchive(getNodes(), context);
        return archive.toString();
    }

    /**
     * Deserialize class. Used in {@link com.xpn.xwiki.plugin.packaging.PackagePlugin}.
     *
     * @param text - archive in JRCS format
     * @throws XWikiException if parse error
     */
    public void setArchive(String text) throws XWikiException
    {
        try {
            XWikiRCSArchive archive = new XWikiRCSArchive(text);
            resetArchive();
            Collection nodes = archive.getNodes(getWikiReference(), getId());
            for (Iterator it = nodes.iterator(); it.hasNext();) {
                XWikiRCSNodeInfo nodeInfo = (XWikiRCSNodeInfo) it.next();
                if (!it.hasNext()) {
                    throw new IllegalStateException("The number of nodes is an odd number.");
                }
                XWikiRCSNodeContent nodeContent = (XWikiRCSNodeContent) it.next();
                updateNode(nodeInfo);
                this.updatedNodeInfos.add(nodeInfo);
                this.updatedNodeContents.add(nodeContent);
            }
        } catch (Exception e) {
            Object[] args = {text, Long.valueOf(getId())};
            throw new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR,
                "Exception while constructing archive for JRCS string [{0}] for document [{1}]", e, args);
        }
    }

    /**
     * Update history with new document version.
     *
     * @param doc - document for this version
     * @param author - author of version
     * @param date - date of version
     * @param comment - version comment
     * @param version - preferably document version in history
     * @param context - used for loading nodes content
     * @throws XWikiException in any error
     */
    public void updateArchive(XWikiDocument doc, String author, Date date, String comment, Version version,
        XWikiContext context) throws XWikiException
    {
        Version oldLatestVer = getLatestVersion();
        Version newVer = version;
        if (newVer == null || oldLatestVer != null && newVer.compareVersions(oldLatestVer) <= 0) {
            newVer = createNextVersion(oldLatestVer, doc.isMinorEdit());
        }
        XWikiRCSNodeInfo newNode = new XWikiRCSNodeInfo(new XWikiRCSNodeId(getWikiReference(), getId(), newVer));
        newNode.setAuthor(author);
        newNode.setComment(comment);
        newNode.setDate(date);
        XWikiRCSNodeContent newContent = makePatch(newNode, doc, context);

        updateNode(newNode);
        this.updatedNodeInfos.add(newNode);
        this.updatedNodeContents.add(newContent);
    }

    /**
     * Remove document versions from vfrom to vto, inclusive.
     *
     * @param newerVersion - start version
     * @param olderVersion - end version
     * @param context - used for loading nodes content
     * @throws XWikiException if any error
     */
    public void removeVersions(Version newerVersion, Version olderVersion, XWikiContext context) throws XWikiException
    {
        Version upperBound = newerVersion;
        Version lowerBound = olderVersion;
        if (upperBound.compareVersions(lowerBound) < 0) {
            Version tmp = upperBound;
            upperBound = lowerBound;
            lowerBound = tmp;
        }
        Version firstVersionAfter = getNextVersion(upperBound);
        Version firstVersionBefore = getPrevVersion(lowerBound);

        // If there's no version matching the range, then the user inserted a wrong version: we don't do anything.
        if (!getNodes(upperBound, lowerBound).isEmpty()) {
            // If the versions specified are the first and latest one then we just reset everything.
            if (firstVersionAfter == null && firstVersionBefore == null) {
                resetArchive();
            } else {
                this.performSpecificVersionsRemoval(firstVersionBefore, firstVersionAfter, lowerBound, upperBound,
                    context);
            }
        }
    }

    private void performSpecificVersionsRemoval(Version firstVersionBefore, Version firstVersionAfter,
        Version lowerBound, Version upperBound, XWikiContext context)
        throws XWikiException
    {
        if (firstVersionAfter == null) {
            // Deleting the most recent version.
            // Store full version in firstVersionBefore
            String xmlBefore = getVersionXml(firstVersionBefore, context);
            XWikiRCSNodeInfo niBefore = getNode(firstVersionBefore);
            XWikiRCSNodeContent ncBefore = niBefore.getContent(context);
            ncBefore.getPatch().setFullVersion(xmlBefore);
            niBefore.setContent(ncBefore);
            updateNode(niBefore);
            getUpdatedNodeContents().add(ncBefore);
        } else if (firstVersionBefore != null) {
            // We're not deleting from the first version, so we must make a new diff jumping over
            // the deleted versions.
            String xmlAfter = getVersionXml(firstVersionAfter, context);
            String xmlBefore = getVersionXml(firstVersionBefore, context);
            XWikiRCSNodeInfo niBefore = getNode(firstVersionBefore);
            XWikiRCSNodeContent ncBefore = niBefore.getContent(context);
            ncBefore.getPatch().setDiffVersion(xmlBefore, xmlAfter, "");
            niBefore.setContent(ncBefore);
            updateNode(niBefore);
            getUpdatedNodeContents().add(ncBefore);
        }
        // if (firstVersionBefore == null) => nothing else to do, except delete
        for (Iterator<XWikiRCSNodeInfo> it = getNodes(upperBound, lowerBound).iterator(); it.hasNext();) {
            XWikiRCSNodeInfo ni = it.next();
            this.fullVersions.remove(ni.getId().getVersion());
            this.deletedNodes.add(ni);
            it.remove();
        }
    }

    /**
     * @return selected version of document, null if version is not found.
     * @param version - which version to load
     * @param context - used for loading
     * @throws XWikiException if any error
     */
    public XWikiDocument loadDocument(Version version, XWikiContext context) throws XWikiException
    {
        XWikiRCSNodeInfo nodeInfo = getNode(version);
        if (nodeInfo == null) {
            return null;
        }
        try {
            String content = getVersionXml(version, context);
            XWikiDocument doc = new XWikiDocument();
            doc.fromXML(content);

            doc.setRCSVersion(version);
            doc.setComment(nodeInfo.getComment());
            doc.setAuthor(nodeInfo.getAuthor());
            doc.setMinorEdit(nodeInfo.isMinorEdit());
            doc.setMostRecent(version.equals(getLatestVersion()));

            // A document coming from the archive should never be considered new.
            doc.setNew(false);

            return doc;
        } catch (Exception e) {
            Object[] args = {version.toString(), Long.valueOf(getId())};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_RCS_READING_REVISIONS,
                "Exception while reading version [{0}] for document id [{1,number}]", e, args);
        }
    }

    /**
     * Return the XML corresponding to a version. If the version node contains just a diff, then restore the complete
     * XML by applying all patches from the nearest full version to the requested version.
     *
     * @param version The version to retrieve.
     * @param context The {@link com.xpn.xwiki.XWikiContext context}.
     * @return The XML corresponding to the version.
     * @throws XWikiException If any exception occured.
     */
    public String getVersionXml(Version version, XWikiContext context) throws XWikiException
    {
        Version nextFullVersion = getNextFullVersion(version);

        List<XWikiRCSNodeContent> lstContent = loadRCSNodeContents(version, nextFullVersion, context);
        List<String> origText = new ArrayList<String>();
        for (XWikiRCSNodeContent nodeContent : lstContent) {
            nodeContent.getPatch().patch(origText);
        }

        return ToString.arrayToString(origText.toArray());
    }

    /**
     * @return {@link XWikiRCSNodeInfo} by version. null if none.
     * @param version which version to get
     */
    public XWikiRCSNodeInfo getNode(Version version)
    {
        return version == null ? null : (XWikiRCSNodeInfo) this.versionToNode.get(version);
    }

    /** @return latest version in history for document. null if none. */
    public Version getLatestVersion()
    {
        return this.versionToNode.size() == 0 ? null : (Version) this.versionToNode.firstKey();
    }

    /** @return latest node in history for document. null if none. */
    public XWikiRCSNodeInfo getLatestNode()
    {
        return getNode(getLatestVersion());
    }

    /**
     * @return next version in history. null if none
     * @param ver - current version
     */
    public Version getNextVersion(Version ver)
    {
        // headMap is exclusive
        SortedMap<Version, XWikiRCSNodeInfo> headmap = this.versionToNode.headMap(ver);
        return (headmap.size() == 0) ? null : headmap.lastKey();
    }

    /**
     * @return previous version in history. null if none
     * @param ver - current version
     */
    public Version getPrevVersion(Version ver)
    {
        // tailMap is inclusive
        SortedMap<Version, XWikiRCSNodeInfo> tailmap = this.versionToNode.tailMap(ver);
        if (tailmap.size() <= 1) {
            return null;
        }
        Iterator<Version> it = tailmap.keySet().iterator();
        it.next();
        return it.next();
    }

    /**
     * @param ver - for what version find next full one
     * @return next version which contain full information (not patch)
     * @deprecated This method has been deprecated as its name is error-prone, and it has never done what's documented:
     * it always returned the next full version, and never the nearest one. So we renamed it to
     * {@link #getNextFullVersion(Version)}.
     */
    @Deprecated(since = "14.10.12,15.5RC1")
    public Version getNearestFullVersion(Version ver)
    {
        return getNextFullVersion(ver);
    }

    /**
     * @param ver - for what version find next full one
     * @return next version which contain full information (not patch)
     * @since 14.10.12
     * @since 15.5RC1
     */
    @Unstable
    public Version getNextFullVersion(Version ver)
    {
        if (this.fullVersions.contains(ver)) {
            return ver;
        }
        SortedSet<Version> headSet = this.fullVersions.headSet(ver);
        return headSet.isEmpty() ? null : headSet.last();
    }

    /**
     * @return List of {@link XWikiRCSNodeContent} where lowerBound<=version<=upperBound order by version
     * @param lowerBound - start version
     * @param upperBound - end version
     * @param context - used everywhere
     * @throws XWikiException if any error
     */
    private List<XWikiRCSNodeContent> loadRCSNodeContents(Version lowerBound, Version upperBound, XWikiContext context)
        throws XWikiException
    {
        List<XWikiRCSNodeContent> result = new ArrayList<XWikiRCSNodeContent>();
        for (XWikiRCSNodeInfo nodeInfo : getNodes(upperBound, lowerBound)) {
            XWikiRCSNodeContent nodeContent = nodeInfo.getContent(context);
            result.add(nodeContent);
        }
        return result;
    }

    /** reset history. history becomes empty. */
    public void resetArchive()
    {
        this.versionToNode.clear();
        this.fullVersions.clear();
        this.deletedNodes.addAll(this.updatedNodeInfos);
        this.updatedNodeInfos.clear();
        this.updatedNodeContents.clear();
    }

    /** @return mutable Set of {@link XWikiRCSNodeInfo} which are need for delete */
    public Set<XWikiRCSNodeInfo> getDeletedNodeInfo()
    {
        return this.deletedNodes;
    }

    /** @return mutable Set of {@link XWikiRCSNodeInfo} which are need for saveOrUpdate */
    public Set<XWikiRCSNodeInfo> getUpdatedNodeInfos()
    {
        return this.updatedNodeInfos;
    }

    /** @return mutable Set of {@link XWikiRCSNodeContent} which are need for update */
    public Set<XWikiRCSNodeContent> getUpdatedNodeContents()
    {
        return this.updatedNodeContents;
    }

    /**
     * @return full copy of this archive with specified docId
     * @param docId - new {@link #getId()}
     * @param context - used for loading content
     * @throws XWikiException if any error
     */
    public XWikiDocumentArchive clone(long docId, XWikiContext context) throws XWikiException
    {
        XWikiDocumentArchive result = new XWikiDocumentArchive(this.wikiReference, docId);
        result.setArchive(getArchive(context));
        return result;
    }
}

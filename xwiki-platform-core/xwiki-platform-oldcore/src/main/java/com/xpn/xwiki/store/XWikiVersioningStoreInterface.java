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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeContent;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

/**
 * Interface for manipulate document history.
 *
 * @version $Id$
 */
@Role
public interface XWikiVersioningStoreInterface
{
    void loadXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context)
        throws XWikiException;

    void saveXWikiDocArchive(XWikiDocumentArchive archivedoc, boolean bTransaction, XWikiContext context)
        throws XWikiException;

    void updateXWikiDocArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException;

    Version[] getXWikiDocVersions(XWikiDocument doc, XWikiContext context) throws XWikiException;

    private boolean matchCriteria(XWikiRCSNodeInfo nodeinfo, RevisionCriteria criteria)
    {
        // Author matching
        if (criteria.getAuthor().isEmpty() || criteria.getAuthor().equals(nodeinfo.getAuthor())) {
            // Date range matching
            Date versionDate = nodeinfo.getDate();
            return (versionDate.after(criteria.getMinDate()) && versionDate.before(criteria.getMaxDate()));
        }
        return false;
    }

    private Collection<String> getXWikiDocStringVersions(XWikiDocument doc, RevisionCriteria criteria,
        XWikiContext context) throws XWikiException
    {
        List<String> results = new ArrayList<>();

        Version[] revisions = getXWikiDocVersions(doc, context);
        XWikiDocumentArchive archive = getXWikiDocumentArchive(doc, context);

        Iterator<Version> revisionsIterator = Arrays.stream(revisions).iterator();
        if (!revisionsIterator.hasNext()) {
            return List.of();
        }

        XWikiRCSNodeInfo nodeinfo = archive.getNode(revisionsIterator.next());
        XWikiRCSNodeInfo nextNodeinfo;

        while (revisionsIterator.hasNext()) {
            nextNodeinfo = archive.getNode(revisionsIterator.next());

            // Minor/Major version matching
            if ((criteria.getIncludeMinorVersions() || !nextNodeinfo.isMinorEdit())
                && (matchCriteria(nodeinfo, criteria)))
            {
                results.add(nodeinfo.getVersion().toString());
            }
            nodeinfo = nextNodeinfo;
        }

        if (matchCriteria(nodeinfo, criteria)) {
            results.add(nodeinfo.getVersion().toString());
        }

        return criteria.getRange().subList(results);
    }

    /**
     * Gets versions from a given document matching criteria like author, minimum creation date, etc.
     *
     * @param doc the document
     * @param criteria criteria used to match versions
     * @param context the XWiki context
     * @return the matching versions
     * @since 15.10.8
     * @since 16.2.0RC1
     */
    @Unstable
    default Collection<Version> getXWikiDocVersions(XWikiDocument doc, RevisionCriteria criteria, XWikiContext context)
        throws XWikiException
    {
        return getXWikiDocStringVersions(doc, criteria, context).stream()
            .map(Version::new)
            .collect(Collectors.toList());
    }

    /**
     * Gets the number of versions from a given document matching criteria like author, minimum creation date, etc.
     *
     * @param doc the document
     * @param criteria criteria used to match versions
     * @param context the XWiki context
     * @return the number of matching versions
     * @since 15.10.8
     * @since 16.2.0RC1
     */
    @Unstable
    default long getXWikiDocVersionsCount(XWikiDocument doc, RevisionCriteria criteria, XWikiContext context)
        throws XWikiException
    {
        return getXWikiDocStringVersions(doc, criteria, context).size();
    }

    XWikiDocument loadXWikiDoc(XWikiDocument doc, String version, XWikiContext context) throws XWikiException;

    void resetRCSArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException;

    XWikiDocumentArchive getXWikiDocumentArchive(XWikiDocument doc, XWikiContext context) throws XWikiException;

    /**
     * Delete all history for a document.
     *
     * @param doc the document for which to delete all the history
     */
    void deleteArchive(XWikiDocument doc, boolean bTransaction, XWikiContext context) throws XWikiException;

    /**
     * Load {@link XWikiRCSNodeContent} on demand. Used in
     * {@link com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo#getContent(XWikiContext)} for example.
     *
     * @return the RCS node content for the passed node id
     * @param id the node id (see {@link XWikiRCSNodeContent#getId()})
     */
    XWikiRCSNodeContent loadRCSNodeContent(XWikiRCSNodeId id, boolean bTransaction, XWikiContext context)
        throws XWikiException;
}

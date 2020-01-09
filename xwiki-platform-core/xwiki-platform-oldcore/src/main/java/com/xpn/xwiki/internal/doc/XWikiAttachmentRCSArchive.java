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
package com.xpn.xwiki.internal.doc;

import java.util.Date;

import org.suigeneris.jrcs.diff.DiffException;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.InvalidFileFormatException;
import org.suigeneris.jrcs.rcs.InvalidVersionNumberException;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.rcs.impl.Node;
import org.suigeneris.jrcs.rcs.impl.NodeNotFoundException;
import org.suigeneris.jrcs.util.ToString;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.internal.doc.rcs.XWikiTrunkNode;

/**
 * Extends JRCS's {@link Archive} class in order to control the data ot the TrunkNode that are created. By default they
 * are created with the current date/time but we want them to have the date of the attachment so that we can build a
 * JRCS-based string representation of a list of attachments (used to serialize attachment history into filesystem files
 * or into XAR XML format).
 *
 * @version $Id$
 * @since 11.7RC1
 */
public class XWikiAttachmentRCSArchive extends Archive
{
    /**
     * @param revisionAttachment the attachment object for a given version of the attachment. Its date will be used to
     *                           overwrite the JRCS's TrunkNode date
     * @param context            the xwiki context, used to get the XML representation of the attachment for that
     *                           revision
     * @throws XWikiException in case of failure to get the XML data
     */
    public XWikiAttachmentRCSArchive(XWikiAttachment revisionAttachment, XWikiContext context) throws
        XWikiException
    {
        super(ToString.stringToArray(revisionAttachment.toStringXML(true, false, context)),
            revisionAttachment.getFilename(), revisionAttachment.getVersion());
        normalizeNode(this.head, revisionAttachment.getDate(), revisionAttachment.getAuthor());
    }

    /**
     * @param revisionAttachment the attachment object for a given version of the attachment. Its date will be used to
     *                           overwrite the JRCS's TrunkNode date
     * @param context            the xwiki context, used to get the XML representation of the attachment for that
     *                           revision
     * @return the added version
     * @throws XWikiException             in case of failure to get the XML data
     * @throws InvalidFileFormatException if an error occurs when adding a revision in JRCS
     * @throws DiffException              if an error occurs when adding a revision in JRCS
     */
    public Version addRevision(XWikiAttachment revisionAttachment, XWikiContext context) throws XWikiException,
        InvalidFileFormatException, DiffException
    {
        Version version = super.addRevision(
            ToString.stringToArray(revisionAttachment.toStringXML(true, false, context)), "");
        normalizeNode(this.head, revisionAttachment.getDate(), revisionAttachment.getAuthor());
        return version;
    }

    /**
     * Ensures that all created {@link Node} object are of type {@link XWikiTrunkNode}.
     */
    @Override
    protected Node newNode(Version vernum, Node prev)
        throws InvalidVersionNumberException, NodeNotFoundException
    {
        if (!vernum.isRevision()) {
            throw new InvalidVersionNumberException(vernum);
        }
        XWikiTrunkNode node = (XWikiTrunkNode) nodes.get(vernum);
        if (node == null) {
            node = new XWikiTrunkNode(Node.newNode(vernum, prev));
            node.setRCSNext(prev);
            nodes.put(vernum, node);
        }
        return node;
    }

    private void normalizeNode(Node node, Date date, String author)
    {
        XWikiTrunkNode xwikiNode = (XWikiTrunkNode) node;

        // Change the date of the newly created TrunkNode to be the date from the versioned attachment.
        xwikiNode.setDate(date);

        // Change the author of the newly created TrunkNode to be the author from the versioned attachment as
        // otherwise, see https://jira.xwiki.org/browse/XCOMMONS-1819
        xwikiNode.setAuthor(author);
    }
}

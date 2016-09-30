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
package com.xpn.xwiki.doc.rcs;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.diff.PatchFailedException;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.InvalidFileFormatException;
import org.suigeneris.jrcs.rcs.InvalidTrunkVersionNumberException;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.rcs.impl.Node;
import org.suigeneris.jrcs.rcs.impl.NodeNotFoundException;
import org.suigeneris.jrcs.rcs.impl.TrunkNode;
import org.suigeneris.jrcs.rcs.parse.ParseException;
import org.suigeneris.jrcs.util.ToString;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Class for String [de]serialization for {@link com.xpn.xwiki.doc.XWikiDocumentArchive}.
 *
 * @version $Id$
 * @since 1.2M1
 */
public class XWikiRCSArchive extends Archive
{
    /** logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiRCSArchive.class);

    /**
     * Used to serialize {@link com.xpn.xwiki.doc.XWikiDocumentArchive}.
     *
     * @param nodeInfos - collection of {@link XWikiRCSNodeInfo} in any order
     * @param context - for loading nodes content
     * @throws XWikiException if can't load nodes content
     */
    public XWikiRCSArchive(Collection<XWikiRCSNodeInfo> nodeInfos, XWikiContext context) throws XWikiException
    {
        super(new Object[0], "");
        this.nodes.clear();
        this.head = null;
        if (nodeInfos.size() > 0) {
            for (XWikiRCSNodeInfo nodeInfo : nodeInfos) {
                XWikiJRCSNode node = new XWikiJRCSNode(nodeInfo.getId().getVersion(), null);
                node.setAuthor(nodeInfo.getAuthor());
                node.setDate(nodeInfo.getDate());
                node.setLog(nodeInfo.getComment());
                XWikiRCSNodeContent content = nodeInfo.getContent(context);
                // Ensure we never set the text to NULL since this can cause errors on some DB such as Oracle.
                node.setText(StringUtils.defaultString(content.getPatch().getContent()));
                node.setDiff(nodeInfo.isDiff());
                this.nodes.put(node.getVersion(), node);
            }
            XWikiJRCSNode last = null;
            for (Iterator it = this.nodes.keySet().iterator(); it.hasNext();) {
                Version ver = (Version) it.next();
                XWikiJRCSNode node = (XWikiJRCSNode) this.nodes.get(ver);
                if (last != null) {
                    last.setRCSNext(node);
                }
                last = node;
                if (this.head == null) {
                    this.head = node;
                }
            }
        }
    }

    /**
     * Used to deserialize {@link com.xpn.xwiki.doc.XWikiDocumentArchive}.
     *
     * @param archiveText - archive text in JRCS format
     * @throws ParseException if syntax errors
     */
    public XWikiRCSArchive(String archiveText) throws ParseException
    {
        super("", new StringReader(archiveText));
    }

    /**
     * Helper class for convert from {@link XWikiRCSNodeInfo} to JRCS {@link Node}.
     */
    private static class XWikiJRCSNode extends TrunkNode
    {
        /** bug if author=="". see http://www.suigeneris.org/issues/browse/JRCS-24 */
        public static String sauthorIfEmpty = "_";

        /** mark that node contains full text, not diff. */
        public static String sfullVersion = "full";

        /** mark that node contains diff. */
        public static String sdiffVersion = "diff";

        /**
         * @param vernum - version of node
         * @param next - next node (with smaller version) in history
         * @throws InvalidTrunkVersionNumberException if version is invalid
         */
        public XWikiJRCSNode(Version vernum, TrunkNode next)
            throws InvalidTrunkVersionNumberException
        {
            super(vernum, next);
        }

        /** @param other - create class from copying this node */
        public XWikiJRCSNode(Node other)
        {
            this(other.version, null);
            this.setDate(other.getDate());
            this.author = other.getAuthor(); // setAuthor is encoding
            this.setState(other.getState());
            this.setLog(other.getLog());
            this.setLocker(other.getLocker());
            this.setText(other.getText());
        }

        /**
         * @param date - date of modification.
         * @see Node#setDate(int[])
         */
        public void setDate(Date date)
        {
            this.date = date;
        }

        /** bitset of chars allowed in author field */
        static BitSet safeAuthorChars = new BitSet();
        static {
            safeAuthorChars.set('-');
            for (char c = 'A', c1 = 'a'; c <= 'Z'; c++, c1++) {
                safeAuthorChars.set(c);
                safeAuthorChars.set(c1);
            }
        }

        /** @param user - user of modification */
        @Override
        public void setAuthor(String user)
        {
            // empty author is error in jrcs
            if (user == null || "".equals(user)) {
                super.setAuthor(sauthorIfEmpty);
            } else {
                byte[] enc = URLCodec.encodeUrl(safeAuthorChars, user.getBytes());
                String senc = new String(enc).replace('%', '_');
                super.setAuthor(senc);
            }
        }

        /**
         * @return user of modification can't override getAuthor, so getAuthor1
         * @see Node#getAuthor()
         */
        public String getAuthor1()
        {
            String result = super.getAuthor();
            if (sauthorIfEmpty.equals(result)) {
                return "";
            } else {
                result = result.replace('_', '%');
                try {
                    byte[] dec = URLCodec.decodeUrl(result.getBytes());
                    result = new String(dec);
                } catch (DecoderException e) {
                    // Probably the archive was created before introducing this encoding (1.2M1/M2).
                    result = super.getAuthor();
                    if (!result.matches("^(\\w|\\d|\\.)++$")) {
                        // It's safer to use an empty author than to use an invalid value.
                        result = "";
                    }
                }
            }
            return result;
        }

        /** @return is this node store diff or full version */
        public boolean isDiff()
        {
            boolean isdiff = !sfullVersion.equals(getState());
            if (getTextString() != null && isdiff != !getTextString().startsWith("<")) {
                LOGGER.warn("isDiff: Archive is inconsistent. Text and diff field are contradicting. version="
                    + getVersion());
                isdiff = !isdiff;
            }
            return isdiff;
        }

        /** @param isdiff - true if node stores a diff, false - if full version */
        public void setDiff(boolean isdiff)
        {
            if (getTextString() != null && isdiff != !getTextString().startsWith("<")) {
                LOGGER.warn("setDiff: Archive is inconsistent. Text and diff field are contradicting. version="
                    + getVersion());
                isdiff = !isdiff;
            }
            setState(isdiff ? sdiffVersion : sfullVersion);
        }

        /** @return is this revision has old format. (xwiki-core<1.2, without author,comment,state fields) */
        public boolean hasOldFormat()
        {
            return !sfullVersion.equals(getState()) && !sdiffVersion.equals(getState());
        }

        /**
         * @return text of modification.
         * @see Node#getText()
         */
        public String getTextString()
        {
            return mergedText()[0].toString();
        }

        @Override
        public void patch(List original, boolean annotate) throws InvalidFileFormatException, PatchFailedException
        {
            if (isDiff()) {
                super.patch(original, annotate);
            } else {
                // there is full version, so simply copy. @see TrunkNode#patch0(..)
                // impossible because org.suigeneris.jrcs.rcs.impl.Line is final with default constructor.
                throw new IllegalArgumentException();
                /*
                 * original.clear(); Object[] lines = getText(); for (int it = 0; it < lines.length; it++) {
                 * original.add(new Line(this, lines[it])); }
                 */
            }
        }
    }

    /**
     * @return Collection of pairs [{@link XWikiRCSNodeInfo}, {@link XWikiRCSNodeContent}]
     * @param docId - docId which will be wrote in {@link XWikiRCSNodeId#setDocId(long)}
     * @throws PatchFailedException
     * @throws InvalidFileFormatException
     * @throws NodeNotFoundException
     */
    public Collection getNodes(long docId) throws NodeNotFoundException, InvalidFileFormatException,
        PatchFailedException
    {
        Collection result = new ArrayList(this.nodes.values().size());
        for (Iterator it = this.nodes.values().iterator(); it.hasNext();) {
            XWikiJRCSNode node = new XWikiJRCSNode((Node) it.next());
            XWikiRCSNodeInfo nodeInfo = new XWikiRCSNodeInfo();
            nodeInfo.setId(new XWikiRCSNodeId(docId, node.getVersion()));
            nodeInfo.setDiff(node.isDiff());

            if (!node.hasOldFormat()) {
                nodeInfo.setAuthor(node.getAuthor1());
                nodeInfo.setComment(node.getLog());
                nodeInfo.setDate(node.getDate());
            } else {
                // If the archive node is old so there is no author, comment and date fields so we set them using the
                // ones from a XWikiDocment object that we construct using the archive content.
                try {
                    String xml = getRevisionAsString(node.getVersion());
                    XWikiDocument doc = new XWikiDocument();
                    doc.fromXML(xml);
                    // set this fields from old document
                    nodeInfo.setAuthor(doc.getAuthor());
                    nodeInfo.setComment(doc.getComment());
                    nodeInfo.setDate(doc.getDate());
                } catch (Exception e) {
                    // 3 potential known errors:
                    // 1) Revision 1.1 doesn't exist. Some time in the past there was a bug in XWiki where version
                    //    were starting at 1.2. When this happens the returned xml has a value of "\n".
                    // 2) A Class property with an invalid XML name was created.
                    //    See http://jira.xwiki.org/jira/browse/XWIKI-1855
                    // 3) Cannot get the revision as a string from a node version. Not sure why this
                    //    is happening though... See http://jira.xwiki.org/jira/browse/XWIKI-2076
                    LOGGER.warn("Error in revision [" + node.getVersion().toString() + "]: [" + e.getMessage()
                        + "]. Ignoring non-fatal error, the Author, Comment and Date are not set.");
                }
            }

            XWikiRCSNodeContent content = new XWikiRCSNodeContent(nodeInfo.getId());
            content.setPatch(new XWikiPatch(node.getTextString(), node.isDiff()));
            nodeInfo.setContent(content);
            result.add(nodeInfo);
            result.add(content);
        }
        // Ensure that the latest revision is set set to have the full content and not a diff.
        if (result.size() > 0) {
            ((XWikiRCSNodeInfo) ((ArrayList) result).get(0)).setDiff(false);
            ((XWikiRCSNodeContent) ((ArrayList) result).get(1)).getPatch().setDiff(false);
        }
        return result;
    }

    /**
     * @return The text of the revision if found.
     * @param version - the version number.
     * @throws NodeNotFoundException if the revision could not be found.
     * @throws InvalidFileFormatException if any of the deltas cannot be parsed.
     * @throws PatchFailedException if any of the deltas could not be applied
     */
    public String getRevisionAsString(Version version) throws NodeNotFoundException,
        InvalidFileFormatException, PatchFailedException
    {
        return ToString.arrayToString(super.getRevision(version), RCS_NEWLINE);
    }
}

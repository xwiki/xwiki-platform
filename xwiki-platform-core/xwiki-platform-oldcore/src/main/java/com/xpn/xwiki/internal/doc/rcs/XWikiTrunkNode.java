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
package com.xpn.xwiki.internal.doc.rcs;

import java.util.BitSet;
import java.util.Date;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;
import org.suigeneris.jrcs.rcs.InvalidTrunkVersionNumberException;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.rcs.impl.Node;
import org.suigeneris.jrcs.rcs.impl.TrunkNode;

/**
 * Extends JRCS's {@link TrunkNode} to be able to encore the author and to offer some additional helper APIs.
 *
 * @version $Id$
 * @since 11.10.2
 * @since 12.0RC1
 */
public class XWikiTrunkNode extends TrunkNode
{
    /**
     * Bug if author=="". See http://www.suigeneris.org/issues/browse/JRCS-24
     */
    private static String sauthorIfEmpty = "_";

    /**
     * Bitset of chars allowed in author field.
     */
    private static BitSet safeAuthorChars = new BitSet();

    static {
        safeAuthorChars.set('-');
        for (char c = 'A', c1 = 'a'; c <= 'Z'; c++, c1++) {
            safeAuthorChars.set(c);
            safeAuthorChars.set(c1);
        }
    }

    /**
     * Create a XWikiTrunkNode bu copying another XWikiTrunkNode.
     *
     * @param other the existing instance to clone
     */
    public XWikiTrunkNode(Node other)
    {
        this(other.version, null);
        this.date = other.getDate();
        this.author = other.getAuthor();
        this.state = other.getState();
        this.log = other.getLog();
        this.locker = other.getLocker();
    }

    /**
     * @param vernum the version of the new node
     * @param next   the node to set as the next node of the new node that will be created
     * @throws InvalidTrunkVersionNumberException if the node fails to be created
     */
    public XWikiTrunkNode(Version vernum, TrunkNode next) throws InvalidTrunkVersionNumberException
    {
        super(vernum, next);
    }

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
     * The {@link Node#getAuthor()} method is final and we can't extend it, which is why we introduce this new method.
     *
     * @return the author of the node's revision (undecoded)
     * @see Node#getAuthor()
     */
    public String getExtendedAuthor()
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

    /**
     * @param date the date of the node's revision
     * @see Node#setDate(int[])
     */
    public void setDate(Date date)
    {
        this.date = date;
    }
}

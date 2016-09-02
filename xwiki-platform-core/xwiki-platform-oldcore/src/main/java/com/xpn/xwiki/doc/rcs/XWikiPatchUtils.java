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

import java.util.List;
import java.util.StringTokenizer;

import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.PatchFailedException;
import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.diff.delta.AddDelta;
import org.suigeneris.jrcs.diff.delta.Chunk;
import org.suigeneris.jrcs.diff.delta.DeleteDelta;
import org.suigeneris.jrcs.rcs.InvalidFileFormatException;
import org.suigeneris.jrcs.util.ToString;

/**
 * Diff and patch utility.
 *
 * @version $Id$
 * @since 1.2M1
 */
public class XWikiPatchUtils
{
    /** prevent to create utility class. */
    private XWikiPatchUtils()
    {
    }

    /**
     * @param orig - original text
     * @param rev - new text
     * @return diff in JRCS format
     * @throws DifferentiationFailedException if error when creating diff
     */
    public static String getDiff(Object[] orig, Object[] rev) throws DifferentiationFailedException
    {
        return Diff.diff(orig, rev).toRCSString();
    }

    /**
     * @param orig - original text
     * @param rev - new text
     * @return diff in JRCS format
     * @throws DifferentiationFailedException if error when creating diff
     */
    public static String getDiff(String orig, String rev) throws DifferentiationFailedException
    {
        return getDiff(ToString.stringToArray(orig), ToString.stringToArray(rev));
    }

    /**
     * From {@link org.suigeneris.jrcs.rcs.impl.Node#patch(List, boolean)}.
     *
     * @param orig - text to patch, List&lt;String&gt; of lines.
     * @param diff - diff to patch, {@link Diff} format
     * @throws InvalidFileFormatException if diff is incorrect
     * @throws PatchFailedException if error in patching
     */
    public static void patch(List<String> orig, String diff)
        throws InvalidFileFormatException, PatchFailedException
    {
        Revision revision = new Revision();
        Object[] lines = ToString.stringToArray(diff);
        for (int it = 0; it < lines.length; it++) {
            String cmd = lines[it].toString();
            if (cmd.length() == 0) {
                break;
            }

            java.util.StringTokenizer t = new StringTokenizer(cmd, "ad ", true);
            char action;
            int n;
            int count;

            try {
                action = t.nextToken().charAt(0);
                n = Integer.parseInt(t.nextToken());
                // skip the space
                t.nextToken();
                count = Integer.parseInt(t.nextToken());
            } catch (Exception e) {
                throw new InvalidFileFormatException("line:" + ":" + e.getClass().getName(),
                    e);
            }

            if (action == 'd') {
                revision.addDelta(new DeleteDelta(new Chunk(n - 1, count)));
            } else if (action == 'a') {
                revision.addDelta(new AddDelta(n, new Chunk(getTextLines(lines,
                    it + 1, it + 1 + count), 0, count, n - 1)));
                it += count;
            } else {
                throw new InvalidFileFormatException();
            }
        }
        revision.applyTo(orig);
    }

    /**
     * @param lines - some text
     * @param from - from that line
     * @param to - to that line
     * @return selected lines of text
     */
    private static Object[] getTextLines(Object[] lines, int from, int to)
    {
        Object[] ret = new Object[to - from + 1];
        for (int i = from; i < to; i++) {
            ret[i - from] = lines[i];
        }
        return ret;
    }
}

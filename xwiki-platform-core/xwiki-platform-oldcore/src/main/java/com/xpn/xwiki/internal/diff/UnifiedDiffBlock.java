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
package com.xpn.xwiki.internal.diff;

import java.util.ArrayList;

import com.xpn.xwiki.internal.diff.UnifiedDiffLine.LineType;

/**
 * Represents a block of lines from a unified diff. A block includes one or more lines that have been changed (added or
 * removed) in a specific context.
 * 
 * @version $Id$
 * @since 4.1M2
 */
public class UnifiedDiffBlock extends ArrayList<UnifiedDiffLine>
{
    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @return the line number where this block starts in the original version
     */
    public int getOriginalStart()
    {
        for (UnifiedDiffLine line : this) {
            if (line.getType() != LineType.ADDED) {
                return line.getNumber();
            }
        }
        return 0;
    }

    /**
     * @return the size of this block (number of lines) in the original version
     */
    public int getOriginalSize()
    {
        int size = 0;
        for (UnifiedDiffLine line : this) {
            if (line.getType() != LineType.ADDED) {
                size++;
            }
        }
        return size;
    }

    /**
     * @return the line number where this block starts in the revised version
     */
    public int getRevisedStart()
    {
        for (UnifiedDiffLine line : this) {
            if (line.getType() != LineType.DELETED) {
                return line.getNumber();
            }
        }
        return 0;
    }

    /**
     * @return the size of this block (number of lines) in the revised version
     */
    public int getRevisedSize()
    {
        int size = 0;
        for (UnifiedDiffLine line : this) {
            if (line.getType() != LineType.DELETED) {
                size++;
            }
        }
        return size;
    }

    @Override
    public String toString()
    {
        StringBuilder result = new StringBuilder();
        // The line number starts from 0 so we add 1 for display.
        result.append(String.format("@@ -%s,%s +%s,%s @@\n", getOriginalStart() + 1, getOriginalSize(),
            getRevisedStart() + 1, getRevisedSize()));
        for (UnifiedDiffLine line : this) {
            result.append(line);
        }
        return result.toString();
    }
}

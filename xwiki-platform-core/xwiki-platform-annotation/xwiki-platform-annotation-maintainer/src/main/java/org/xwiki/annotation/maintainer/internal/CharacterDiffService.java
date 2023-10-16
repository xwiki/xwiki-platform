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

package org.xwiki.annotation.maintainer.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Singleton;

import org.incava.diff.Diff;
import org.incava.diff.Difference;
import org.xwiki.annotation.maintainer.DiffService;
import org.xwiki.annotation.maintainer.XDelta;
import org.xwiki.component.annotation.Component;

/**
 * DiffService implementation providing character level differences between content.
 *
 * @version $Id$
 * @since 2.3M1
 */
@Component(hints = {"default", "character" })
@Singleton
public class CharacterDiffService implements DiffService
{
    @Override
    public Collection<XDelta> getDifferences(String previous, String current)
    {
        // get differences at character level
        // FIXME: do we want at character level or we'd better get word level, to have it working faster
        Collection<XDelta> deltas = new ArrayList<XDelta>();
        List<Character> previousContent = new ArrayList<Character>();
        for (int i = 0; i < previous.length(); ++i) {
            previousContent.add(previous.charAt(i));
        }
        List<Character> currentContent = new ArrayList<Character>();
        for (int i = 0; i < current.length(); ++i) {
            currentContent.add(current.charAt(i));
        }
        Diff<Character> diff = new Diff<Character>(previousContent, currentContent);
        // prepare the XDeltas for all diffs
        for (Difference it : diff.diff()) {
            XDelta delta = getDelta(previous, current, it);
            if (delta != null) {
                deltas.add(delta);
            }
        }
        return deltas;
    }

    /**
     * Helper function to prepare an {@link XDelta} object for the passed content.
     *
     * @param previous the previous content
     * @param current the current content
     * @param diff the difference to prepare the XDelta object for
     * @return an {@link XDelta} object corresponding to {@code diff}
     */
    private XDelta getDelta(String previous, String current, Difference diff)
    {
        int position;
        String original = "";
        String modified = "";

        // deleted is from previous, added is in current

        if (diff.getDeletedStart() == Difference.NONE || diff.getAddedStart() == Difference.NONE) {
            // this diff doesn't make sense, ignore it
            return null;
        }

        position = diff.getDeletedStart();

        // the content that was deleted
        if (diff.getDeletedEnd() != Difference.NONE) {
            original = previous.substring(diff.getDeletedStart(), diff.getDeletedEnd() + 1);
        }

        // the content that was added
        if (diff.getAddedEnd() != Difference.NONE) {
            modified = current.substring(diff.getAddedStart(), diff.getAddedEnd() + 1);
        }

        // else return the built chunk
        return new ChunksXDelta(position, original, modified);
    }
}

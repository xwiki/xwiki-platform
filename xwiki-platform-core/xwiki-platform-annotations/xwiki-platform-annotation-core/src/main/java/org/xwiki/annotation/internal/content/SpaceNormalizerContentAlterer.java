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
package org.xwiki.annotation.internal.content;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.annotation.content.AlteredContent;
import org.xwiki.annotation.content.filter.Filter;
import org.xwiki.component.annotation.Component;

/**
 * Space normalizer content alterer. Will trim all leading and trailing white spaces in the passed sequence along with
 * collapsing all the inner white spaces to a single space. It also replaces all sorts of white spaces such as
 * non-breakable spaces with regular spaces.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("space-normalizer")
@Singleton
public class SpaceNormalizerContentAlterer extends AbstractContentAlterer
{
    /**
     * The whitespace filter, to identify all characters which are whitespace.
     */
    @Inject
    @Named("whitespace")
    private Filter whitespaceFilter;

    @Override
    public AlteredContent alter(CharSequence sequence)
    {
        // same as filtering only that on encountering the first space in a series of whitespace, only print the first
        // one
        StringBuffer buffer = new StringBuffer();
        Map<Integer, Integer> initialToAltered = new HashMap<Integer, Integer>();
        Map<Integer, Integer> alteredToInitial = new HashMap<Integer, Integer>();

        // number of refused chars
        int removedChars = 0;
        Character c;
        // initially assume we're in whitespace printing, since we need to trim all leading spaces
        boolean isInWhitespace = true;
        for (int i = 0; i < sequence.length(); ++i) {
            c = sequence.charAt(i);
            boolean isWhitespace = !whitespaceFilter.accept(c);
            // if either it's a non-whitespace or it's a whitespace but it's the first whitespace after some characters
            if (!isWhitespace || (isWhitespace && !isInWhitespace)) {
                // update the whitespace printing state according to the the type of the current character
                isInWhitespace = isWhitespace;

                // if it's whitespace print a plain space, not the char itself
                buffer.append(isWhitespace ? " " : c);
                // update the altered indexes for all the removed characters in this removed fragment to point to this
                // position
                for (int t = 0; t <= removedChars; ++t) {
                    // 1+0;1 // 1+1;1
                    initialToAltered.put(i - t, buffer.length() - 1);
                }

                // restore the removed chars count
                removedChars = 0;

                // map this altered index to the index in the original sequence
                alteredToInitial.put(buffer.length() - 1, i);
            } else {
                removedChars++;
            }
        }
        // if the last character is a space, remove it and add it to the removed chars
        if (buffer.length() > 0 && buffer.charAt(buffer.length() - 1) == ' ') {
            buffer.deleteCharAt(buffer.length() - 1);
            removedChars++;
            // remove the mapping from the altered to initial mapping since it doesn't exist anymore. buffer.length is
            // now the old buffer length - 1
            alteredToInitial.remove(buffer.length());
        }
        // finally update the indexes for the last stream of removed chars
        if (buffer.length() > 0) {
            // add the offsets for the remaining removed chars
            for (int t = 0; t < removedChars; ++t) {
                initialToAltered.put(sequence.length() - 1 - t, buffer.length() - 1 - 1);
            }
        }

        return new OffsetsMapAlteredContent(buffer.toString(), sequence.length(), initialToAltered, alteredToInitial);
    }
}

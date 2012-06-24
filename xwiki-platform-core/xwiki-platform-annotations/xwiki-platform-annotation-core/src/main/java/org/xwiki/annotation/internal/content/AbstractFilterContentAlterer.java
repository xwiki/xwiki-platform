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

import org.xwiki.annotation.content.AlteredContent;
import org.xwiki.annotation.content.filter.Filter;

/**
 * Content alterer to filter out characters which are part of a wiki syntax. This class should be extended to provide
 * the {@link Filter} used to determine which characters are not meaningful text and should be preserved (not
 * syntax characters).
 * 
 * @version $Id$
 * @since 2.3M1
 */
public abstract class AbstractFilterContentAlterer extends AbstractContentAlterer
{
    /**
     * @return Syntax filter used to determine the accepted characters in the content altered by this alterer.
     */
    protected abstract Filter getFilter();

    @Override
    public AlteredContent alter(CharSequence sequence)
    {
        StringBuffer buffer = new StringBuffer();
        Map<Integer, Integer> initialToAltered = new HashMap<Integer, Integer>();
        Map<Integer, Integer> alteredToInitial = new HashMap<Integer, Integer>();

        // index altered
        int j = 0;
        // number of refused chars
        int z = 0;
        Character c;
        for (int i = 0; i < sequence.length(); ++i) {
            c = sequence.charAt(i);
            if (getFilter().accept(c)) {
                buffer.append(c);
                for (int t = 0; t <= z; ++t) {
                    // 1+0;1 // 1+1;1
                    initialToAltered.put(i - t, j);
                }
                alteredToInitial.put(j, i);
                ++j;
                z = 0;
            } else {
                z++;
            }
        }
        if (j != 0) {
            for (int t = 0; t < z; ++t) {
                initialToAltered.put(sequence.length() - 1 - t, j - 1);
            }
        }
        return new OffsetsMapAlteredContent(buffer.toString(), sequence.length(), initialToAltered, alteredToInitial);
    }
}

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
package org.xwiki.gwt.wysiwyg.client.plugin.sync;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.xwiki.gwt.wysiwyg.client.diff.Chunk;
import org.xwiki.gwt.wysiwyg.client.diff.Delta;
import org.xwiki.gwt.wysiwyg.client.diff.Revision;


/**
 * Utility methods for content synchronization.
 * 
 * @version $Id$
 */
public final class SyncTools
{
    /**
     * Default constructor is private because this is a utility class.
     */
    private SyncTools()
    {
    }

    /**
     * This will relocate the patches on rev2 based on changes in rev1.
     * 
     * @param rev2
     * @param rev1
     * @return the computed revision
     */
    public static Revision relocateRevision(Revision rev2, Revision rev1)
    {
        Map<Chunk, Integer> shiftList = new HashMap<Chunk, Integer>();
        for (int i = 0; i < rev1.size(); i++) {
            Delta delta = rev1.getDelta(i);
            Chunk orig = delta.getOriginal();
            Chunk revised = delta.getRevised();

            // look for chars that have changed
            if (revised.size() > orig.size()) {
                int origpos = orig.anchor() + 1;
                String origstr = (String) orig.chunk().get(1);
                String revisedstr1 = (String) revised.chunk().get(1);
                String revisedstr2 = (String) revised.chunk().get(revised.chunk().size() - 1);
                int nbunreloc = orig.size() - revised.size();
                updateRevision(rev2, origpos, origstr, revisedstr1, revisedstr2, nbunreloc, shiftList);
            }

        }

        for (int i = 0; i < rev1.size(); i++) {
            Delta delta = rev1.getDelta(i);
            Chunk orig = delta.getOriginal();
            Chunk revised = delta.getRevised();
            if (orig.size() != revised.size()) {
                int position = orig.anchor();
                int deltaSize = revised.size() - orig.size();
                relocateRevision(rev2, position, deltaSize);
            }
        }

        // perform the delayed unreloc
        Iterator<Chunk> it = shiftList.keySet().iterator();
        while (it.hasNext()) {
            Chunk chunk = it.next();
            int nbreloc = shiftList.get(chunk).intValue();
            chunk.moveAnchor(nbreloc);
        }
        return rev2;
    }

    /**
     * Updates the given revision.
     * 
     * @param rev2
     * @param position
     * @param origchar
     * @param newchar1
     * @param newchar2
     * @param nbunreloc
     * @param shiftList
     */
    private static void updateRevision(Revision rev2, int position, String origchar, String newchar1, String newchar2,
        int nbunreloc, Map<Chunk, Integer> shiftList)
    {
        for (int i = 0; i < rev2.size(); i++) {
            Delta delta = rev2.getDelta(i);
            Chunk orig = delta.getOriginal();
            Chunk revised = delta.getRevised();
            if (orig.anchor() == position) {
                // we have a chunk with a first char that has changed
                String origchar2 = (String) orig.chunk().get(0);
                if (origchar.equals(origchar2)) {
                    orig.chunk().set(0, newchar2);
                    revised.chunk().set(0, newchar2);
                }
            }
            if ((orig.anchor() + orig.size() - 1) == position) {
                // we have a chunk with a last char that has changed
                String origchar2 = (String) orig.chunk().get(orig.size() - 1);
                if (origchar.equals(origchar2)) {
                    orig.chunk().set(orig.size() - 1, newchar1);
                    revised.chunk().set(revised.size() - 1, newchar1);
                    shiftList.put(orig, new Integer(nbunreloc));
                    shiftList.put(revised, new Integer(nbunreloc));
                }
            }

        }
    }

    /**
     * Relocates the given revision.
     * 
     * @param rev2
     * @param position
     * @param deltaSize
     */
    private static void relocateRevision(Revision rev2, int position, int deltaSize)
    {
        for (int j = 0; j < rev2.size(); j++) {
            Delta delta2 = rev2.getDelta(j);
            // relocate Delta
            Chunk orig2 = delta2.getOriginal();
            Chunk revised2 = delta2.getRevised();
            if (orig2.anchor() >= position) {
                orig2.moveAnchor(deltaSize);
                revised2.moveAnchor(deltaSize);
            }
        }
    }
}

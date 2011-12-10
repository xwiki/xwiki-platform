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
package org.xwiki.gwt.wysiwyg.client.diff;

import java.util.*;

/**
 * Implements a simple differencing algortithm.
 * <p>
 * 
 * @date $Date: 2006/03/12 00:24:21 $
 * @version $Revision: 1.1 $
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * 
 * <p>
 * <b>Overview of Algorithm</b>
 * </p>
 * 
 * <p>
 * <i>by <a href='http://www.topmeadow.net/bwm'> bwm</a>
 * </p>
 * 
 * <p>
 * The algorithm is optimised for situations where the input sequences have few
 * repeated objects. If it is given input with many repeated objects it will
 * report sub-optimal changes. However, given appropriate input, it is fast, and
 * linear in memory usage.
 * </p>
 * 
 * <p>
 * The algorithm consists of the following steps:
 * </p>
 * <ul>
 * <li>compute an equivalence set for the input data</li>
 * <li>translate each element of the orginal and revised input sequences to a
 * member of the equivalence set </li>
 * <li>match the the input sequences to determine the deltas, i.e. the
 * differences between the original and revised sequences.</li>
 * </ul>
 * 
 * <p>
 * The first step is to compute a an equivalence set for the input data. The
 * equivalence set is computed from objects that are in the original input
 * sequence
 * </p>
 * 
 * <pre>
 *    eq(x) = the index of the first occurence of x in the original sequence.
 * </pre>
 * 
 * <p>
 * With this equivalence function, the algorithm can compare integers rather
 * than strings, which is considerably more efficient.
 * </p>
 * 
 * <p>
 * The second step is to compute the datastructure on which the algorithm will
 * operate. Having computed the equivalence function in the previous step, we
 * can compute two arrays where indx[i] = eqs(orig[i]) and jndx[i] =
 * eqs(rev[i]). The algorithm can now operate on indx and jndx instead of orig
 * and rev. Thus, comparisons are then on O(int == int) instead of
 * O(Object.equals(Object)).
 * </p>
 * 
 * <p>
 * The algorithm now matches indx and jndx. Whilst indx[i] == jndx[i] it skips
 * matching objects in the sequence. In seeking to match objects in the input
 * sequence it assumes that each object is likely to be unique. It uses the
 * known characteristics of the unique equivalence function. It can tell from
 * the eq value if this object appeared in the other sequence at all. If it did
 * not, there is no point in searching for a match.
 * </p>
 * 
 * <p>
 * Recall that the eq function value is the index earliest occurrence in the
 * orig sequence. This information is used to search efficiently for the next
 * match. The algorithm is perfect when all input objects are unique, but
 * degrades when input objects are not unique. When input objects are not unique
 * an optimal match may not be found, but a correct match will be.
 * </p>
 * 
 * <p>
 * Having identified common matching objects in the orig and revised sequences,
 * the differences between them are easily computed.
 * </p>
 * 
 * @see Delta
 * @see Revision Modifications:
 * 
 * 27/Apr/2003 bwm Added some comments whilst trying to figure out the algorithm
 * 
 * 03 May 2003 bwm Created this implementation class by refactoring it out of
 * the Diff class to enable plug in difference algorithms
 * 
 */
public class SimpleDiff implements DiffAlgorithm
{

    static final int NOT_FOUND_i = -2;
    static final int NOT_FOUND_j = -1;
    static final int EOS = Integer.MAX_VALUE;

    public SimpleDiff()
    {
    }

    protected int scan(int[] ndx, int i, int target)
    {
        while (ndx[i] < target)
        {
            i++;
        }
        return i;
    }

    /**
     * Compute the difference between original and revised sequences.
     * 
     * @param orig
     *            The original sequence.
     * @param rev
     *            The revised sequence to be compared with the original.
     * @return A Revision object describing the differences.
     * @throws DifferenciationFailedException
     *             if the diff could not be computed.
     */
    public Revision diff(Object[] orig, Object[] rev)
            throws DifferentiationFailedException
    {
        // create map eqs, such that for each item in both orig and rev
        // eqs(item) = firstOccurrence(item, orig);
        Map eqs = buildEqSet(orig, rev);

        // create an array such that
        // indx[i] = NOT_FOUND_i if orig[i] is not in rev
        // indx[i] = firstOccurrence(orig[i], orig)
        int[] indx = buildIndex(eqs, orig, NOT_FOUND_i);

        // create an array such that
        // jndx[j] = NOT_FOUND_j if orig[j] is not in rev
        // jndx[j] = firstOccurrence(rev[j], orig)
        int[] jndx = buildIndex(eqs, rev, NOT_FOUND_j);

        // what in effect has been done is to build a unique hash
        // for each item that is in both orig and rev
        // and to label each item in orig and new with that hash value
        // or a marker that the item is not common to both.

        eqs = null; // let gc know we're done with this

        Revision deltas = new Revision(); // !!! new Revision()
        int i = 0;
        int j = 0;

        // skip matching
        // skip leading items that are equal
        // could be written
        // for (i=0; indx[i] != EOS && indx[i] == jndx[i]; i++);
        // j = i;
        for (; indx[i] != EOS && indx[i] == jndx[j]; i++, j++)
        {
            /* void */
        }

        while (indx[i] != jndx[j])
        { // only equal if both == EOS
            // they are different
            int ia = i;
            int ja = j;

            // size of this delta
            do
            {
                // look down rev for a match
                // stop at a match
                // or if the FO(rev[j]) > FO(orig[i])
                // or at the end
                while (jndx[j] < 0 || jndx[j] < indx[i])
                {
                    j++;
                }
                // look down orig for a match
                // stop at a match
                // or if the FO(orig[i]) > FO(rev[j])
                // or at the end
                while (indx[i] < 0 || indx[i] < jndx[j])
                {
                    i++;
                }

                // this doesn't do a compare each line with each other line
                // so it won't find all matching lines
            }
            while (indx[i] != jndx[j]);

            // on exit we have a match

            // they are equal, reverse any exedent matches
            // it is possible to overshoot, so count back matching items
            while (i > ia && j > ja && indx[i - 1] == jndx[j - 1])
            {
                --i;
                --j;
            }

            deltas.addDelta(Delta.newDelta(new Chunk(orig, ia, i - ia),
                    new Chunk(rev, ja, j - ja)));
            // skip matching
            for (; indx[i] != EOS && indx[i] == jndx[j]; i++, j++)
            {
                /* void */
            }
        }
        return deltas;
    }

    /**
     * create a <code>Map</code> from each common item in orig and rev to the
     * index of its first occurrence in orig
     * 
     * @param orig
     *            the original sequence of items
     * @param rev
     *            the revised sequence of items
     */
    protected Map buildEqSet(Object[] orig, Object[] rev)
    {
        // construct a set of the objects that orig and rev have in common

        // first construct a set containing all the elements in orig
        Set items = new HashSet(Arrays.asList(orig));

        // then remove all those not in rev
        items.retainAll(Arrays.asList(rev));

        Map eqs = new HashMap();
        for (int i = 0; i < orig.length; i++)
        {
            // if its a common item and hasn't been found before
            if (items.contains(orig[i]))
            {
                // add it to the map
                eqs.put(orig[i], new Integer(i));
                // and make sure its not considered again
                items.remove(orig[i]);
            }
        }
        return eqs;
    }

    /**
     * build a an array such each a[i] = eqs([i]) or NF if eqs([i]) undefined
     * 
     * @param eqs
     *            a mapping from Object to Integer
     * @param seq
     *            a sequence of objects
     * @param NF
     *            the not found marker
     */
    protected int[] buildIndex(Map eqs, Object[] seq, int NF)
    {
        int[] result = new int[seq.length + 1];
        for (int i = 0; i < seq.length; i++)
        {
            Integer value = (Integer) eqs.get(seq[i]);
            if (value == null || value.intValue() < 0)
            {
                result[i] = NF;
            }
            else
            {
                result[i] = value.intValue();
            }
        }
        result[seq.length] = EOS;
        return result;
    }

}

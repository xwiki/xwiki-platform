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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A Revision holds the series of deltas that describe the differences between
 * two sequences.
 * 
 * @version $Revision: 1.1 $ $Date: 2006/03/12 00:24:21 $
 * 
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * @author <a href="mailto:bwm@hplb.hpl.hp.com">Brian McBride</a>
 * 
 * @see Delta
 * @see Diff
 * @see Chunk
 * @see Revision
 * 
 * modifications 27 Apr 2003 bwm
 * 
 * Added visitor pattern Visitor interface and accept() method.
 */

public class Revision extends ToString
{

    List deltas_ = new ArrayList();

    /**
     * Creates an empty Revision.
     */
    public Revision()
    {
    }

    /**
     * Adds a delta to this revision.
     * 
     * @param delta
     *            the {@link Delta Delta} to add.
     */
    public synchronized void addDelta(Delta delta)
    {
        if (delta == null)
        {
            throw new IllegalArgumentException("new delta is null");
        }
        deltas_.add(delta);
    }

    /**
     * Adds a delta to the start of this revision.
     * 
     * @param delta
     *            the {@link Delta Delta} to add.
     */
    public synchronized void insertDelta(Delta delta)
    {
        if (delta == null)
        {
            throw new IllegalArgumentException("new delta is null");
        }
        deltas_.add(0, delta);
    }

    /**
     * Retrieves a delta from this revision by position.
     * 
     * @param i
     *            the position of the delta to retrieve.
     * @return the specified delta
     */
    public Delta getDelta(int i)
    {
        return (Delta) deltas_.get(i);
    }

    /**
     * Returns the number of deltas in this revision.
     * 
     * @return the number of deltas.
     */
    public int size()
    {
        return deltas_.size();
    }

    /**
     * Applies the series of deltas in this revision as patches to the given
     * text.
     * 
     * @param src
     *            the text to patch, which the method doesn't change.
     * @return the resulting text after the patches have been applied.
     * @throws PatchFailedException
     *             if any of the patches cannot be applied.
     */
    public Object[] patch(Object[] src) throws PatchFailedException
    {
        List target = new ArrayList(Arrays.asList(src));
        applyTo(target);
        return target.toArray();
    }

    /**
     * Applies the series of deltas in this revision as patches to the given
     * text.
     * 
     * @param target
     *            the text to patch.
     * @throws PatchFailedException
     *             if any of the patches cannot be applied.
     */
    public synchronized void applyTo(List target) throws PatchFailedException
    {
        ListIterator i = deltas_.listIterator(deltas_.size());
        while (i.hasPrevious())
        {
            Delta delta = (Delta) i.previous();
            delta.patch(target);
        }
    }

    /**
     * Converts this revision into its Unix diff style string representation.
     * 
     * @param s
     *            a {@link StringBuffer StringBuffer} to which the string
     *            representation will be appended.
     */
    public synchronized void toString(StringBuffer s)
    {
        Iterator i = deltas_.iterator();
        while (i.hasNext())
        {
            ((Delta) i.next()).toString(s);
        }
    }

    /**
     * Converts this revision into its RCS style string representation.
     * 
     * @param s
     *            a {@link StringBuffer StringBuffer} to which the string
     *            representation will be appended.
     * @param EOL
     *            the string to use as line separator.
     */
    public synchronized void toRCSString(StringBuffer s, String EOL)
    {
        Iterator i = deltas_.iterator();
        while (i.hasNext())
        {
            ((Delta) i.next()).toRCSString(s, EOL);
        }
    }

    /**
     * Converts this revision into its RCS style string representation.
     * 
     * @param s
     *            a {@link StringBuffer StringBuffer} to which the string
     *            representation will be appended.
     */
    public void toRCSString(StringBuffer s)
    {
        toRCSString(s, Diff.NL);
    }

    /**
     * Converts this delta into its RCS style string representation.
     * 
     * @param EOL
     *            the string to use as line separator.
     */
    public String toRCSString(String EOL)
    {
        StringBuffer s = new StringBuffer();
        toRCSString(s, EOL);
        return s.toString();
    }

    /**
     * Converts this delta into its RCS style string representation using the
     * default line separator.
     */
    public String toRCSString()
    {
        return toRCSString(Diff.NL);
    }

    /**
     * Accepts a visitor.
     * 
     * @param visitor
     *            the visitor visiting this instance
     */
    public void accept(RevisionVisitor visitor)
    {
        visitor.visit(this);
        Iterator iter = deltas_.iterator();
        while (iter.hasNext())
        {
            ((Delta) iter.next()).accept(visitor);
        }
    }

}

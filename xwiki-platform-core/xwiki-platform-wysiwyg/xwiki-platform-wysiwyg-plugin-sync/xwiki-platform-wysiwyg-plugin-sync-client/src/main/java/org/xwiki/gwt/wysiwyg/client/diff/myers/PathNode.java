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
package org.xwiki.gwt.wysiwyg.client.diff.myers;

/**
 * A node in a diffpath.
 * 
 * @version $Revision: 1.1 $ $Date: 2006/03/12 00:24:21 $
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * 
 * @see DiffNode
 * @see Snake
 * 
 */
public abstract class PathNode
{
    /** Position in the original sequence. */
    public final int i;
    /** Position in the revised sequence. */
    public final int j;
    /** The previous node in the path. */
    public final PathNode prev;

    /**
     * Concatenates a new path node with an existing diffpath.
     * 
     * @param i
     *            The position in the original sequence for the new node.
     * @param j
     *            The position in the revised sequence for the new node.
     * @param prev
     *            The previous node in the path.
     */
    public PathNode(int i, int j, PathNode prev)
    {
        this.i = i;
        this.j = j;
        this.prev = prev;
    }

    /**
     * Is this node a {@link Snake Snake node}?
     * 
     * @return true if this is a {@link Snake Snake node}
     */
    public abstract boolean isSnake();

    /**
     * Is this a bootstrap node?
     * <p>
     * In bottstrap nodes one of the two corrdinates is less than zero.
     * 
     * @return tru if this is a bootstrap node.
     */
    public boolean isBootstrap()
    {
        return i < 0 || j < 0;
    }

    /**
     * Skips sequences of {@link DiffNode DiffNodes} until a {@link Snake} or
     * bootstrap node is found, or the end of the path is reached.
     * 
     * @return The next first {@link Snake} or bootstrap node in the path, or
     *         <code>null</code> if none found.
     */
    public final PathNode previousSnake()
    {
        if (isBootstrap())
            return null;
        if (!isSnake() && prev != null)
            return prev.previousSnake();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer("[");
        PathNode node = this;
        while (node != null)
        {
            buf.append("(");
            buf.append(Integer.toString(node.i));
            buf.append(",");
            buf.append(Integer.toString(node.j));
            buf.append(")");
            node = node.prev;
        }
        buf.append("]");
        return buf.toString();
    }
}
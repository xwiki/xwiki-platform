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
 * Represents a snake in a diffpath.
 * <p>
 * 
 * {@link DiffNode DiffNodes} and {@link Snake Snakes} allow for compression of
 * diffpaths, as each snake is represented by a single {@link Snake Snake} node
 * and each contiguous series of insertions and deletions is represented by a
 * single {@link DiffNode DiffNodes}.
 * 
 * @version $Revision: 1.1 $ $Date: 2006/03/12 00:24:21 $
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 * 
 */
public final class Snake extends PathNode
{
    /**
     * Constructs a snake node.
     * 
     * @param the
     *            position in the original sequence
     * @param the
     *            position in the revised sequence
     * @param prev
     *            the previous node in the path.
     */
    public Snake(int i, int j, PathNode prev)
    {
        super(i, j, prev);
    }

    /**
     * {@inheritDoc}
     * 
     * @return true always
     */
    public boolean isSnake()
    {
        return true;
    }

}
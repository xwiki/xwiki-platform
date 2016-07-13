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
package org.xwiki.rendering.signature.internal;

import java.io.IOException;
import java.io.OutputStream;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.Block;

/**
 * Dump a {@link Block} into a binary stream.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Role
public interface BlockDumper
{
    /**
     * Dump a {@link org.xwiki.rendering.block.MacroBlock} or a {@link org.xwiki.rendering.block.MacroMarkerBlock} into
     * a binary stream.
     *
     * @param out the stream to be written.
     * @param block the {@link org.xwiki.rendering.block.MacroBlock}
     *              or {@link org.xwiki.rendering.block.MacroMarkerBlock} to dump.
     * @throws IOException on error.
     */
    void dump(OutputStream out, Block block) throws IOException;

    /**
     * Dump a {@link org.xwiki.rendering.block.MacroBlock} or a {@link org.xwiki.rendering.block.MacroMarkerBlock} into
     * a byte array.
     *
     * @param block the {@link org.xwiki.rendering.block.MacroBlock}
     *              or {@link org.xwiki.rendering.block.MacroMarkerBlock} to dump.
     * @return the resulting byte array.
     * @throws IOException on error.
     */
    byte[] dump(Block block) throws IOException;
}

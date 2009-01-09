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
package org.xwiki.rendering.block;

import org.xwiki.rendering.listener.Listener;

/**
 * A type of {@link Block} that has children Blocks. For example the Paragraph Block, the Bold Block, the List Block,
 * etc.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public interface FatherBlock extends Block
{
    /**
     * Send {@link org.xwiki.rendering.listener.Listener} events corresponding to the start of the father block. For
     * example for a Bold block, this allows an XHTML Listener (aka a Renderer) to output <code>&lt;b&gt;</code>.
     * 
     * @param listener the listener that will receive the events sent by the father block before the children blocks
     *            have emitted their own events.
     */
    void before(Listener listener);

    /**
     * Send {@link Listener} events corresponding to the end of the father block. For example for a Bold block, this
     * allows an XHTML Listener (aka a Renderer) to output <code>&lt;/b&gt;</code>.
     * 
     * @param listener the listener that will receive the events sent by the father block after the children blocks have
     *            emitted their own events.
     */
    void after(Listener listener);
}

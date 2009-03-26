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
import org.xwiki.rendering.util.IdGenerator;

import java.util.List;
import java.util.Collections;

/**
 * Contains the full tree of {@link Block} that represent a XWiki Document's content.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XDOM extends AbstractFatherBlock
{
    /**
     * Constructs an empty XDOM. Useful for example when calling a macro that doesn't use the XDOM parameter passed to
     * it.
     */
    public static final XDOM EMPTY = new XDOM(Collections.<Block> emptyList());

    /**
     * Sateful id generator for this document. We store it in the XDOM because it's the only object which remain is the
     * same between parsing, transformation and rendering and we need to generate id during parsing and during
     * transformation.
     */
    private IdGenerator idGenerator;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractFatherBlock#AbstractFatherBlock(java.util.List)
     */
    public XDOM(List<Block> childBlocks)
    {
        super(childBlocks);

        this.idGenerator = new IdGenerator();
    }

    public XDOM(List<Block> childBlocks, IdGenerator idGenerator)
    {
        super(childBlocks);

        this.idGenerator = idGenerator;
    }

    /**
     * @return a stateful id generator for the whole document.
     */
    public IdGenerator getIdGenerator()
    {
        return this.idGenerator;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractBlock#setParent(org.xwiki.rendering.block.Block)
     */
    @Override
    public void setParent(Block parentBlock)
    {
        if (parentBlock != null) {
            // The document become a embedded document
            this.idGenerator = null;
        } else if (this.idGenerator == null) {
            // The embedded document become a root document
            this.idGenerator = new IdGenerator();
        }

        super.setParent(parentBlock);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractFatherBlock#before(org.xwiki.rendering.listener.Listener)
     */
    public void before(Listener listener)
    {
        listener.beginDocument();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractFatherBlock#after(org.xwiki.rendering.listener.Listener)
     */
    public void after(Listener listener)
    {
        listener.endDocument();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractBlock#clone()
     */
    @Override
    public XDOM clone()
    {
        return (XDOM) super.clone();
    }
}

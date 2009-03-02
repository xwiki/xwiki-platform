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

import java.util.Collections;
import java.util.List;

import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.xml.XMLNode;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class XMLBlock extends AbstractFatherBlock
{
    private XMLNode node;

    public XMLBlock(List<Block> childrenBlocks, XMLNode node)
    {
        super(childrenBlocks);
        this.node = node;
    }

    public XMLBlock(XMLNode node)
    {
        this(Collections.<Block> emptyList(), node);
    }

    public XMLNode getXMLNode()
    {
        return this.node;
    }

    public void before(Listener listener)
    {
        listener.beginXMLNode(getXMLNode());
    }

    public void after(Listener listener)
    {
        listener.endXMLNode(getXMLNode());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractBlock#clone(org.xwiki.rendering.block.BlockFilter)
     * @since 1.8RC2
     */
    @Override
    public XMLBlock clone(BlockFilter blockFilter)
    {
        XMLBlock clone = (XMLBlock) super.clone(blockFilter);
        clone.node = getXMLNode().clone();
        return clone;
    }
}

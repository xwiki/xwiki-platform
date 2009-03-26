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

import java.util.List;
import java.util.Map;

import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.util.IdGenerator;
import org.xwiki.rendering.util.RenderersUtils;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class HeaderBlock extends AbstractFatherBlock
{
    private HeaderLevel level;

    private String id;

    public HeaderBlock(List<Block> childBlocks, HeaderLevel level)
    {
        super(childBlocks);

        this.level = level;
    }

    public HeaderBlock(List<Block> childBlocks, HeaderLevel level, Map<String, String> parameters)
    {
        super(childBlocks, parameters);

        this.level = level;
    }

    public HeaderBlock(List<Block> childBlocks, HeaderLevel level, String id)
    {
        this(childBlocks, level);

        this.id = id;
    }

    public HeaderBlock(List<Block> childBlocks, HeaderLevel level, Map<String, String> parameters, String id)
    {
        this(childBlocks, level, parameters);

        this.id = id;
    }

    public HeaderLevel getLevel()
    {
        return this.level;
    }

    public SectionBlock getSection()
    {
        return (SectionBlock) getParent();
    }

    public String getId()
    {
        return this.id;
    }

    /**
     * Force to regenerate id. This method get the root parent {@link XDOM} {@link IdGenerator} to generate unique
     * identifier.
     */
    public void generateId()
    {
        Block rootBlock = getRoot();
        if (rootBlock instanceof XDOM) {
            XDOM xdom = (XDOM) rootBlock;

            IdGenerator idGenerator = xdom.getIdGenerator();
            if (this.id != null) {
                idGenerator.remove(this.id);
            }

            if (idGenerator != null) {
                this.id = "H" + idGenerator.generateUniqueId(getPlainTextTitle());
            } else {
                this.id = "H" + getPlainTextTitle();
            }
        } else {
            this.id = "H" + getPlainTextTitle();
        }
    }

    public String getPlainTextTitle()
    {
        RenderersUtils renderersUtils = new RenderersUtils();

        return renderersUtils.renderPlainText(getChildren());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.AbstractBlock#setParent(org.xwiki.rendering.block.Block)
     */
    @Override
    public void setParent(Block parentBlock)
    {
        super.setParent(parentBlock);

        if (id == null) {
            generateId();
        }
    }

    public void before(Listener listener)
    {
        listener.beginHeader(getLevel(), getId(), getParameters());
    }

    public void after(Listener listener)
    {
        listener.endHeader(getLevel(), getId(), getParameters());
    }
}

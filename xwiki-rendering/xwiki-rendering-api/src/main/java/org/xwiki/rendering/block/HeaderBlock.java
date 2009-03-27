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
    /**
     * Header identifier prefix. This is to make sure the identifier complies with the XHTML specification.
     */
    private static final String ID_PREFIX = "H";

    /**
     * The level of the header.
     */
    private HeaderLevel level;

    /**
     * The id of the header.
     */
    private String id;

    /**
     * @param childBlocks the children of the header.
     * @param level the level of the header
     */
    public HeaderBlock(List<Block> childBlocks, HeaderLevel level)
    {
        super(childBlocks);

        this.level = level;
    }

    /**
     * @param childBlocks the children of the header.
     * @param level the level of the header
     * @param parameters the parameters of the header
     */
    public HeaderBlock(List<Block> childBlocks, HeaderLevel level, Map<String, String> parameters)
    {
        super(childBlocks, parameters);

        this.level = level;
    }

    /**
     * @param childBlocks the children of the header.
     * @param level the level of the header
     * @param id the id of the header.
     */
    public HeaderBlock(List<Block> childBlocks, HeaderLevel level, String id)
    {
        this(childBlocks, level);

        this.id = id;
    }

    /**
     * @param childBlocks the children of the header.
     * @param level the level of the header
     * @param parameters the parameters of the header
     * @param id the id of the header.
     */
    public HeaderBlock(List<Block> childBlocks, HeaderLevel level, Map<String, String> parameters, String id)
    {
        this(childBlocks, level, parameters);

        this.id = id;
    }

    /**
     * @return the level of the header
     */
    public HeaderLevel getLevel()
    {
        return this.level;
    }

    /**
     * @return the id of the header.
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the {@link SectionBlock} corresponding to this header
     */
    public SectionBlock getSection()
    {
        return (SectionBlock) getParent();
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

            generateId(idGenerator);
        } else {
            generateId(null);
        }
    }

    /**
     * Generate header identifier. If idGenerator is null the id is generated based on "H" prefix and a cleaned (with
     * only characters matching [a-zA-Z0-9]) plain text title.
     * 
     * @param idGenerator the id generator.
     */
    private void generateId(IdGenerator idGenerator)
    {
        if (idGenerator != null) {
            this.id = idGenerator.generateUniqueId(ID_PREFIX, getPlainTextTitle());
        } else {
            this.id = ID_PREFIX + getPlainTextTitle().replaceAll("[^a-zA-Z0-9]", "");
        }
    }

    /**
     * Generate a plain text title from header children blocks.
     * 
     * @return the plain text title.
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.FatherBlock#before(org.xwiki.rendering.listener.Listener)
     */
    public void before(Listener listener)
    {
        listener.beginHeader(getLevel(), getId(), getParameters());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.FatherBlock#after(org.xwiki.rendering.listener.Listener)
     */
    public void after(Listener listener)
    {
        listener.endHeader(getLevel(), getId(), getParameters());
    }
}

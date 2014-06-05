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
package org.xwiki.model.reference.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.BlockReference;
import org.xwiki.model.reference.BlockReferenceResolver;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;

/**
 * Resolve block references from {@link Block} instances for the purpose of linking them to signatures and use their
 * metadata source as parent when available in XDOM.
 *
 * The name of the block reference is an SHA-1 digest based on the macro block content, parameters, and the
 * metadata source if found in XDOM. The metadata source is also resolved using a current document reference resolver
 * to be set as parent.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("currentsignedmacro")
@Singleton
public class CurrentSignedMacroBlockReferenceResolver implements BlockReferenceResolver<Block>
{
    @Inject
    @Named("signedmacro")
    private BlockReferenceResolver<Block> blockResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> sourceResolver;

    /**
     * {@inheritDoc}
     *
     * This implementation is specific for macro blocks. The block argument is recommended to have a known content
     * source could be either a {@link org.xwiki.rendering.block.MacroBlock} or
     * a {@link org.xwiki.rendering.block.MacroMarkerBlock}.
     */
    @Override
    public BlockReference resolve(Block block, Object... parameters)
    {
        return blockResolver.resolve(block, getSourceReference(block));
    }

    private EntityReference getSourceReference(Block block)
    {
        EntityReference sourceRef = null;
        MetaDataBlock metaDataBlock =
            block.getFirstBlock(new MetadataBlockMatcher(MetaData.SOURCE), Block.Axes.ANCESTOR);
        if (metaDataBlock != null) {
            sourceRef = sourceResolver.resolve((String) metaDataBlock.getMetaData().getMetaData(MetaData.SOURCE));
        }
        return sourceRef;
    }
}

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
package org.xwiki.rendering.block.match;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.listener.MetaData;

/**
 * Implementation of {@link BlockMatcher} which matches {@link MetaData} information.
 * 
 * @version $Id$
 */
public class MetadataBlockMatcher extends ClassBlockMatcher
{
    /**
     * The key of the {@link MetaData}.
     */
    private String metadataKey;

    /**
     * The value of the {@link MetaData}.
     */
    private Object metadataValue;

    /**
     * Match {@link MetaDataBlock} containing the provided key.
     * 
     * @param metadataKey the key of the {@link MetaData}
     */
    public MetadataBlockMatcher(String metadataKey)
    {
        this(metadataKey, null);
    }

    /**
     * Match {@link MetaDataBlock} containing the provided key/value pair.
     * 
     * @param metadataKey the key of the {@link MetaData}
     * @param metadataValue the value of the {@link MetaData}
     */
    public MetadataBlockMatcher(String metadataKey, Object metadataValue)
    {
        super(MetaDataBlock.class);

        this.metadataKey = metadataKey;
        this.metadataValue = metadataValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.block.match.ClassBlockMatcher#match(org.xwiki.rendering.block.Block)
     */
    @Override
    public boolean match(Block block)
    {
        return super.match(block) && matchMetadata(((MetaDataBlock) block).getMetaData());
    }

    /**
     * Matches the {@link MetaData} for provided key and value.
     * 
     * @param metadata the {@link MetaData} to analyze
     * @return true is the {@link MetaData} is matched, false otherwise
     */
    private boolean matchMetadata(MetaData metadata)
    {
        boolean match;

        if (this.metadataValue != null) {
            Object value = metadata.getMetaData(this.metadataKey);
            match = value != null && value.equals(this.metadataValue);
        } else {
            match = metadata.contains(this.metadataKey);
        }

        return match;
    }
}

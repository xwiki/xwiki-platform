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
package org.xwiki.localization.internal.message;

import org.xwiki.localization.message.TranslationMessage;
import org.xwiki.rendering.block.Block;

/**
 * A static {@link Block} returned as it is without any modification.
 * 
 * @version $Id$
 * @since 4.3M2
 */
public class BlockTranslationMessage extends BlockTranslationMessageElement implements TranslationMessage
{
    /**
     * The source of the translation message.
     */
    private String rawSource;

    /**
     * @param rawSource the source of the translarion message
     * @param block the block
     */
    public BlockTranslationMessage(String rawSource, Block block)
    {
        super(block);

        this.rawSource = rawSource;
    }

    @Override
    public String getRawSource()
    {
        return this.rawSource;
    }

    @Override
    public String toString()
    {
        return getRawSource();
    }
}

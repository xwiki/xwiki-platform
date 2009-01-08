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

import org.xwiki.rendering.listener.Listener;

/**
 * Block that notifies a failure during parsing when parsing a wiki syntax.
 *
 * @version $Id$
 * @since 1.7M3
 */
public class ErrorBlock extends AbstractFatherBlock
{
    /**
     * The error message.
     */
    private String message;
    
    /**
     * The error description, can be technical such as a stack trace. Provides more details than the error
     * message.
     */
    private String description;
    
    public ErrorBlock(List<Block> childrenBlocks, String message, String description)
    {
        super(childrenBlocks);
        this.message = message;
        this.description = description;
    }

    public String getMessage()
    {
        return this.message;
    }
    
    public String getDescription()
    {
        return this.description;
    }
    
    public void before(Listener listener)
    {
        listener.beginError(getMessage(), getDescription());
    }

    public void after(Listener listener)
    {
        listener.endError(getMessage(), getDescription());
    }
}

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

import java.util.Map;

/**
 * Common implementation for standalone verbatim blocks and inline verbatim blocks.
 *
 * @version $Id$
 * @since 1.6M2
 */
public abstract class AbstractVerbatimBlock extends AbstractBlock implements VerbatimBlock
{
    /**
     * The string to protect from rendering.
     */
    private String protectedString;

    /**
     * @param protectedString the string to protect from rendering
     */
    public AbstractVerbatimBlock(String protectedString)
    {
        this.protectedString = protectedString;
    }

    /**
     * @param protectedString the string to protect from rendering
     * @param parameters the parameters to set
     */
    public AbstractVerbatimBlock(String protectedString, Map<String, String> parameters)
    {
        super(parameters);
        this.protectedString = protectedString;
    }

    /**
     * @return the string to protect from rendering
     */
    public String getProtectedString()
    {
        return this.protectedString;
    }
}

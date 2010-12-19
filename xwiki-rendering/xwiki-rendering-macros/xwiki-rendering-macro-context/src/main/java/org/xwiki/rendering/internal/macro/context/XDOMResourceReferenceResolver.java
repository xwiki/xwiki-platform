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
package org.xwiki.rendering.internal.macro.context;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;

/**
 * Finds all relative Resource References in an XDOM and make them absolute.
 *
 * @version $Id$
 * @since 3.0M1
 */
@ComponentRole
public interface XDOMResourceReferenceResolver
{
    /**
     * @param blocks the blocks to look into for transforming relative references into absolute ones
     * @param docReference the entity reference used to resolve relative references against
     */
    void resolve(List<Block> blocks, DocumentReference docReference);
}

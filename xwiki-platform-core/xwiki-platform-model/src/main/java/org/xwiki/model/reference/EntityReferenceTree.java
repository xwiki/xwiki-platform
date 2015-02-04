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
package org.xwiki.model.reference;

import java.util.Comparator;

/**
 * Expose a set of references as a tree. The sort order is configurable using a {@link Comparator}. By default the
 * references will be sorted in default {@link String} order.
 * <p>
 * Only a set of references starting at same level is supported. For example you can pass the following lists:
 * <ul>
 * <li>[<code>wiki:space.page</code>, <code>wiki2:space2.page2</code>]</li>
 * <li>[<code>wiki:space.page</code>, <code>wiki:space</code>]</li>
 * <li>[<code>space.page</code>, <code>space</code>]</li>
 * </ul>
 * but not [<code>space.page</code>, <code>wiki:space</code>].
 * 
 * @version $Id$
 * @since 5.4RC1
 */
public class EntityReferenceTree extends EntityReferenceTreeNode
{
    /**
     * @param references the references to fill the tree with
     */
    public EntityReferenceTree(Iterable< ? extends EntityReference> references)
    {
        this(null, references);
    }

    /**
     * @param references the references to fill the tree with
     */
    public EntityReferenceTree(EntityReference... references)
    {
        this(null, references);
    }

    /**
     * @param comparator control the order of references names
     * @param references the references to fill the tree with
     */
    public EntityReferenceTree(Comparator<String> comparator, Iterable< ? extends EntityReference> references)
    {
        super(comparator);

        for (EntityReference reference : references) {
            addChild(reference);
        }
    }

    /**
     * @param comparator control the order of references names
     * @param references the references to fill the tree with
     */
    public EntityReferenceTree(Comparator<String> comparator, EntityReference... references)
    {
        super(comparator);

        for (EntityReference reference : references) {
            addChild(reference);
        }
    }
}

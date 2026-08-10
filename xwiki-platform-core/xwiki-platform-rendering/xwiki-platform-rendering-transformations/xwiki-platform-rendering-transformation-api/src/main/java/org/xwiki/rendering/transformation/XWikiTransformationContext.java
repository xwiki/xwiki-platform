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
package org.xwiki.rendering.transformation;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Extends the generic {@link TransformationContext} to add XWiki specific information.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Unstable
public class XWikiTransformationContext extends TransformationContext
{
    private EntityReference contentEntityReference;

    /**
     * @return the reference of the document or object property whose content is being transformed; some
     * transformations require specific access rights, which are evaluated for the user specified in this context
     * against the entity whose content is being transformed
     * @since 18.6.0
     * @since 18.4.3
     */
    public EntityReference getContentEntityReference()
    {
        return this.contentEntityReference;
    }

    /**
     * Set the reference of the document or object property whose content is being transformed. Some transformations
     * require specific access rights, which are evaluated for the specified user against the entity whose content is
     * being transformed. The reference must be absolute (i.e. rooted at a wiki) so that it unambiguously identifies the
     * entity.
     *
     * @param contentEntityReference the absolute reference of the document or object property whose content is being
     *     transformed, or {@code null} if there is no such entity
     * @throws IllegalArgumentException if the passed reference is not {@code null} and not absolute (i.e. not rooted at
     *     a wiki reference)
     * @since 18.6.0
     * @since 18.4.3
     */
    public void setContentEntityReference(EntityReference contentEntityReference)
    {
        if (contentEntityReference != null
            && contentEntityReference.getRoot().getType() != EntityType.WIKI) {
            throw new IllegalArgumentException(String.format(
                "The content entity reference [%s] must be absolute (i.e. rooted at a wiki reference).",
                contentEntityReference));
        }

        this.contentEntityReference = contentEntityReference;
    }
}

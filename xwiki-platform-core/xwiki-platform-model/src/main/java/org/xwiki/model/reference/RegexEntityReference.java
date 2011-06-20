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

import java.util.regex.Pattern;

import org.xwiki.model.EntityType;

/**
 * An {@link EntityReference} used to match another one.
 * <p>
 * This {@link EntityReference} contains a {@link Pattern} which will tries to match provided {@link EntityReference}
 * name and type.
 * <p>
 * The {@link RegexEntityReference} can be partial or contain holes, for example if only the space regex reference is
 * provided it will tries to match only space part of the provided reference and consider other parts as matched.
 * <p>
 * It's of course possible to mix regex and "normal" {@link EntityReference} member. In that case normal member
 * {@link #equals(Object)} method behave as usual and then call following member {@link #equals(Object)} etc.
 * 
 * @version $Id$
 * @since 3.2M1
 */
public class RegexEntityReference extends EntityReference
{
    /**
     * 
     */
    private Pattern pattern;

    /**
     * @param pattern the pattern used to match the name
     * @param type the type of the reference
     */
    public RegexEntityReference(Pattern pattern, EntityType type)
    {
        super(pattern.pattern(), type);

        this.pattern = pattern;
    }

    /**
     * @param pattern the pattern used to match the name
     * @param type the type of the reference
     * @param parent the parent of the reference
     */
    public RegexEntityReference(Pattern pattern, EntityType type, EntityReference parent)
    {
        super(pattern.pattern(), type, parent);

        this.pattern = pattern;
    }

    /**
     * @return the pattern used to match the name
     */
    public Pattern getPattern()
    {
        return this.pattern;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This equals method actually matches provided reference using patterns.
     * 
     * @see org.xwiki.model.reference.EntityReference#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof EntityReference)) {
            return false;
        }

        EntityReference reference = (EntityReference) obj;

        for (EntityReference entity = reference; entity != null; entity = entity.getParent()) {
            if (getType().equals(entity.getType())) {
                if (this.pattern != null && !this.pattern.matcher(entity.getName()).matches()) {
                    return false;
                } else {
                    return getParent() != null ? getParent().equals(entity) : true;
                }
            }
        }

        return true;
    }
}

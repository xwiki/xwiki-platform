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
package org.xwiki.component.wiki.internal.bridge;

import java.util.List;

import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiObjectComponentBuilder;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.objects.BaseObject;

/**
 * This interface uses {@link WikiObjectComponentBuilder} to ease the operation of building new components from
 * XObjects using the old core classes such as {@link BaseObject}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
public interface WikiBaseObjectComponentBuilder extends WikiObjectComponentBuilder
{
    /**
     * Build the components that is linked to the given {@link EntityReference}.
     *
     * @param baseObject the XObject that should be used to create the component.
     * @return the new components
     * @throws WikiComponentException if the given {@link EntityReference} is incompatible with the current builder or
     * if the {@link WikiComponentBuilder} has not been able to instanciate the component.
     */
    List<WikiComponent> buildComponents(BaseObject baseObject) throws WikiComponentException;

    /**
     * As we’re currently extending a public interface for its internal use, we can discard the default implementation
     * of {@link WikiObjectComponentBuilder#buildComponents(EntityReference)}.
     */
    @Override
    default List<WikiComponent> buildComponents(EntityReference entityReference) throws WikiComponentException
    {
        throw new UnsupportedOperationException("This component builder does not support entity references. Please use "
                + "WikiObjectComponentBuilder.");
    }
}

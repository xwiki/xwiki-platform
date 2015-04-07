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
package org.xwiki.component.internal;

import javax.inject.Inject;

import org.xwiki.component.internal.multi.AbstractGenericComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Proxy Component Manager that creates and queries individual Component Managers specific to the current entity in the
 * Execution Context. These Component Managers are created on the fly the first time a component is registered for the
 * current entity.
 * 
 * @version $Id$
 * @since 5.0M2
 */
public abstract class AbstractEntityComponentManager extends AbstractGenericComponentManager implements Initializable
{
    @Inject
    private EntityReferenceSerializer<String> serializer;

    protected abstract EntityReference getCurrentReference();

    @Override
    protected String getKey()
    {
        EntityReference reference = getCurrentReference();

        return reference != null ? reference.getType().getLowerCase() + ':' + this.serializer.serialize(reference)
            : null;
    }
}

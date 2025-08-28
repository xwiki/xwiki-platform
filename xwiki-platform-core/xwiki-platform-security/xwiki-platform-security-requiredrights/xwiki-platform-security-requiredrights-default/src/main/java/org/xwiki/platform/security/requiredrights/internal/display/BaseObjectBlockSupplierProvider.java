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
package org.xwiki.platform.security.requiredrights.internal.display;

import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;
import org.xwiki.rendering.block.Block;

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Block supplier provider for {@link BaseObject}.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
public class BaseObjectBlockSupplierProvider implements BlockSupplierProvider<BaseObject>
{
    @Inject
    private BlockSupplierProvider<BaseCollection<? extends EntityReference>> baseCollectionBlockSupplierProvider;

    @Override
    public Supplier<Block> get(BaseObject object, Object... parameters)
    {
        return this.baseCollectionBlockSupplierProvider.get(object, parameters);
    }
}

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
package org.xwiki.model.internal;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.Entity;
import org.xwiki.model.EntityManager;
import org.xwiki.model.UniqueReference;

/**
 * Facade implementation that delegates to the underlying implementation defined in the Model Configuration. This
 * allows choosing which Model implementation to use by using a configuration parameter.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class DefaultEntityManager implements EntityManager
{
    /**
     * Get the correct Entity Manager based on the Model Configuration.
     */
    @Inject
    private Provider<EntityManager> entityManagerProvider;

    @Override
    public <T extends Entity> T getEntity(UniqueReference reference)
    {
        return this.entityManagerProvider.get().getEntity(reference);
    }

    @Override
    public boolean hasEntity(UniqueReference reference)
    {
        return this.entityManagerProvider.get().hasEntity(reference);
    }

    @Override
    public void removeEntity(UniqueReference reference)
    {
        this.entityManagerProvider.get().removeEntity(reference);
    }

    @Override
    public <T extends Entity> T addEntity(UniqueReference reference)
    {
        return this.entityManagerProvider.get().addEntity(reference);
    }
}

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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.EntityManager;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.ModelRuntimeException;

/**
 * Returns an {@link EntityManager} implementation based on the configuration property defined in the Model
 * Configuration.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Singleton
public class EntityManagerProvider implements Provider<EntityManager>, Initializable
{
    /**
     * The Model Configuration from where to get the Model implementation to use.
     */
    @Inject
    private ModelConfiguration modelConfiguration;

    /**
     * Used to get an instance of the Entity Manager dynamically based on the Model implementation to use.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Then Entity Manager implementation to use, cached for improved performances.
     */
    private EntityManager entityManager;

    @Override
    public void initialize() throws InitializationException
    {
        String hint = this.modelConfiguration.getImplementationHint();
        EntityManager em;
        try {
            em = this.componentManager.getInstance(EntityManager.class, hint);
        } catch (ComponentLookupException e) {
            throw new ModelRuntimeException("Failed to find Model implementation for hint [%s]", hint);
        }
        this.entityManager = em;
    }

    @Override
    public EntityManager get()
    {
        return this.entityManager;
    }
}

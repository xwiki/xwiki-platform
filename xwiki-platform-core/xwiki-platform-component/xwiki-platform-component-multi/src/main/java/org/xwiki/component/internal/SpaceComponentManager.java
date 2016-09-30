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
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * Proxy Component Manager that creates and queries individual Component Managers specific to the current space in the
 * Execution Context. These Component Managers are created on the fly the first time a component is registered for the
 * current space.
 * 
 * @version $Id$
 * @since 5.0M2
 */
@Component
@Named(SpaceComponentManager.ID)
@Singleton
public class SpaceComponentManager extends AbstractEntityComponentManager implements Initializable
{
    /**
     * The identifier of this {@link ComponentManager}.
     */
    public static final String ID = "space";

    /**
     * The prefix of space namespace.
     * 
     * @since 8.4RC1
     */
    public static final String NAMESPACE_PREFIX = ID + ':';

    @Inject
    @Named("current")
    private Provider<SpaceReference> referenceProvider;

    /**
     * The Component Manager to be used as parent when a component is not found in the current Component Manager.
     */
    @Inject
    @Named(WikiComponentManager.ID)
    private ComponentManager wikiComponentManager;

    @Override
    protected EntityReference getCurrentReference()
    {
        return this.referenceProvider.get();
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Set the parent to the Root Component Manager since if a component isn't found for a particular wiki
        // we want to check if it's available in the Root Component Manager.
        setInternalParent(this.wikiComponentManager);
    }
}

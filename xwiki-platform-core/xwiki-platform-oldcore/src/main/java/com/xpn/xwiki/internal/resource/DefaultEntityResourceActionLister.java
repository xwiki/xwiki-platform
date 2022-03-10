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
package com.xpn.xwiki.internal.resource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.resource.internal.entity.EntityResourceReferenceHandler;

import com.xpn.xwiki.internal.web.LegacyAction;

/**
 * Takes into account both the old legacy way of defining Actions {@link LegacyAction} and the new way using
 * {@link EntityResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Singleton
public class DefaultEntityResourceActionLister implements EntityResourceActionLister
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Override
    public List<String> listActions()
    {
        List<String> actionNames = new ArrayList<>();

        addComponentHint(LegacyAction.class, actionNames);
        addComponentHint(new DefaultParameterizedType(null, ResourceReferenceHandler.class, EntityResourceAction.class),
            actionNames);

        return actionNames;
    }

    private <T> void addComponentHint(Type role, List<String> actionNames)
    {
        List<ComponentDescriptor<T>> descriptors =
            this.contextComponentManagerProvider.get().getComponentDescriptorList(role);

        for (ComponentDescriptor<T> descriptor : descriptors) {
            actionNames.add(descriptor.getRoleHint());
        }
    }
}

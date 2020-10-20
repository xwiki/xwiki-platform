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
package org.xwiki.livedata.internal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.OperatorDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.WithParameters;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Base {@link LiveDataPropertyDescriptorStore} implementation.
 * 
 * @version $Id$
 * @since 12.9
 */
public abstract class AbstractLiveDataPropertyDescriptorStore extends WithParameters
    implements LiveDataPropertyDescriptorStore
{
    @Inject
    private ContextualLocalizationManager l10n;

    @Override
    public boolean add(LiveDataPropertyDescriptor propertyDescriptor)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<LiveDataPropertyDescriptor> get(String propertyId) throws LiveDataException
    {
        return get().stream().filter(property -> Objects.equals(property.getId(), propertyId)).findFirst();
    }

    @Override
    public boolean update(LiveDataPropertyDescriptor propertyDescriptor)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<LiveDataPropertyDescriptor> remove(String propertyId)
    {
        throw new UnsupportedOperationException();
    }

    protected List<LiveDataPropertyDescriptor> translate(List<LiveDataPropertyDescriptor> properties)
    {
        return properties.stream().map(this::translate).collect(Collectors.toList());
    }

    protected LiveDataPropertyDescriptor translate(LiveDataPropertyDescriptor property)
    {
        String translationPrefix = String.valueOf(getParameters().getOrDefault("translationPrefix", ""));
        if (property.getName() == null) {
            property.setName(this.l10n.getTranslationPlain(translationPrefix + property.getId()));
        }
        if (property.getDescription() == null) {
            property.setDescription(this.l10n.getTranslationPlain(translationPrefix + property.getId() + ".hint"));
        }
        return property;
    }

    protected OperatorDescriptor createOperator(String id)
    {
        String name = this.l10n.getTranslationPlain("liveData.filterOperator." + id);
        if (name == null) {
            name = id;
        }
        return new OperatorDescriptor(id, id);
    }
}

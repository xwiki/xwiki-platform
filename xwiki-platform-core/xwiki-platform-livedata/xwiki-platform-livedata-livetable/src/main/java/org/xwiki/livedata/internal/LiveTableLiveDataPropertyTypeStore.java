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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.WithParameters;

/**
 * {@link LiveDataPropertyDescriptorStore} implementation that exposes the known live table column types as live data
 * property types.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@Component
@Named("liveTable/propertyType")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class LiveTableLiveDataPropertyTypeStore implements LiveDataPropertyDescriptorStore, WithParameters
{
    private static final String ID = "id";

    private static final String SUGGEST = "suggest";

    private static final String HTML = "html";

    private static final String TEXT = "text";

    private final Map<String, Object> parameters = new HashMap<>();

    @Override
    public Map<String, Object> getParameters()
    {
        return this.parameters;
    }

    @Override
    public boolean add(LiveDataPropertyDescriptor propertyDescriptor)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<LiveDataPropertyDescriptor> get(String typeId)
    {
        return get().stream().filter(property -> Objects.equals(property.getId(), typeId)).findFirst();
    }

    @Override
    public Collection<LiveDataPropertyDescriptor> get()
    {
        List<LiveDataPropertyDescriptor> types = new ArrayList<>();
        types.add(createTypeDescriptor("Boolean"));
        types.add(createTypeDescriptor("ComputedField", HTML));
        types.add(createTypeDescriptor("DBList", null, SUGGEST));
        types.add(createTypeDescriptor("DBTreeList", null, SUGGEST));
        types.add(createTypeDescriptor("Date"));
        types.add(createTypeDescriptor("Email", HTML, TEXT));
        types.add(createTypeDescriptor("Groups", HTML, SUGGEST));
        types.add(createTypeDescriptor("Number"));
        types.add(createTypeDescriptor("Page", HTML, SUGGEST));
        types.add(createTypeDescriptor("Password", null));
        types.add(createTypeDescriptor("StaticList", null, "list"));
        types.add(createTypeDescriptor("String", null, TEXT));
        types.add(createTypeDescriptor("TextArea", HTML, TEXT));
        types.add(createTypeDescriptor("Users", HTML, SUGGEST));
        return types;
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

    private LiveDataPropertyDescriptor createTypeDescriptor(String id)
    {
        return createTypeDescriptor(id, id.toLowerCase(), id.toLowerCase());
    }

    private LiveDataPropertyDescriptor createTypeDescriptor(String id, String displayer)
    {
        return createTypeDescriptor(id, displayer, "none");
    }

    private LiveDataPropertyDescriptor createTypeDescriptor(String id, String displayer, String filter)
    {
        LiveDataPropertyDescriptor type = new LiveDataPropertyDescriptor();
        type.setId(id);
        if (displayer != null) {
            type.getDisplayer().put(ID, displayer);
        }
        if (filter != null) {
            type.getFilter().put(ID, filter);
        }
        return type;
    }
}

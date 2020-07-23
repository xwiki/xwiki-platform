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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.OperatorDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;

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
public class LiveTableLiveDataPropertyTypeStore extends AbstractLiveDataPropertyDescriptorStore
{
    private static final String SUGGEST = "suggest";

    private static final String HTML = "html";

    private static final String TEXT = "text";

    @Override
    public Collection<LiveDataPropertyDescriptor> get()
    {
        OperatorDescriptor equals = createOperator("equals");
        OperatorDescriptor contains = createOperator("contains");
        OperatorDescriptor startsWith = createOperator("startsWith");
        OperatorDescriptor isBetween = createOperator("isBetween");

        OperatorDescriptor[] stringOperators = new OperatorDescriptor[] {contains, startsWith, equals};
        OperatorDescriptor[] listOperators = new OperatorDescriptor[] {equals, startsWith, contains};

        List<LiveDataPropertyDescriptor> types = new ArrayList<>();
        types.add(createTypeDescriptor("Boolean", equals));
        types.add(createTypeDescriptor("ComputedField", HTML, null));
        types.add(createTypeDescriptor("DBList", null, SUGGEST, listOperators));
        types.add(createTypeDescriptor("DBTreeList", null, SUGGEST, listOperators));
        types.add(createTypeDescriptor("Date", isBetween, contains));
        types.add(createTypeDescriptor("Email", HTML, TEXT, stringOperators));
        types.add(createTypeDescriptor("Groups", HTML, SUGGEST, listOperators));
        types.add(createTypeDescriptor("Number", equals));
        types.add(createTypeDescriptor("Page", HTML, SUGGEST, listOperators));
        types.add(createTypeDescriptor("Password", null, (String) null));
        types.add(createTypeDescriptor("StaticList", null, "list", listOperators));
        types.add(createTypeDescriptor("String", null, TEXT, stringOperators));
        types.add(createTypeDescriptor("TextArea", HTML, TEXT, stringOperators));
        types.add(createTypeDescriptor("Users", HTML, SUGGEST, listOperators));
        return types;
    }

    private LiveDataPropertyDescriptor createTypeDescriptor(String id, OperatorDescriptor... operators)
    {
        return createTypeDescriptor(id, id.toLowerCase(), id.toLowerCase(), operators);
    }

    private LiveDataPropertyDescriptor createTypeDescriptor(String id, String displayer, String filter,
        OperatorDescriptor... operators)
    {
        LiveDataPropertyDescriptor type = new LiveDataPropertyDescriptor();
        type.setId(id);
        if (displayer != null) {
            type.getDisplayer().setId(displayer);
        }
        if (filter != null) {
            type.setSortable(true);
            type.setFilterable(true);
            type.getFilter().setId(filter);
            if (operators != null) {
                type.getFilter().getOperators().addAll(Arrays.asList(operators));
            }
        } else {
            type.setSortable(false);
            type.setFilterable(false);
        }
        return type;
    }
}

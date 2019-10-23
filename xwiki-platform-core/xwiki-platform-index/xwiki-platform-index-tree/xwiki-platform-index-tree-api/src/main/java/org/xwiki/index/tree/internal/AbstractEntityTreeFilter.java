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
package org.xwiki.index.tree.internal;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.converter.Converter;
import org.xwiki.tree.TreeFilter;

/**
 * Base class for entity tree filters.
 * 
 * @version $Id$
 * @since 11.10RC1
 */
public abstract class AbstractEntityTreeFilter implements TreeFilter
{
    @Inject
    @Named("entityTreeNodeId")
    private Converter<EntityReference> entityTreeNodeIdConverter;

    @Override
    public Set<String> getExclusions(String parentNodeId)
    {
        return getExclusions(resolve(parentNodeId)).stream().map(this::serialize).collect(Collectors.toSet());
    }

    protected EntityReference resolve(String nodeId)
    {
        return this.entityTreeNodeIdConverter.convert(EntityReference.class, nodeId);
    }

    protected String serialize(EntityReference entityReference)
    {
        return this.entityTreeNodeIdConverter.convert(String.class, entityReference);
    }

    protected abstract Set<EntityReference> getExclusions(EntityReference parentReference);
}

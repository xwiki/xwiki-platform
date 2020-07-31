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
package org.xwiki.livedata;

import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataQuery.Filter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link LiveDataConfiguration}.
 * 
 * @version $Id$
 * @since 12.6
 */
class LiveDataConfigurationTest
{
    @Test
    void initialize()
    {
        LiveDataConfiguration config = new LiveDataConfiguration();

        config.initialize();

        assertTrue(config.getData().getEntries().isEmpty());

        assertTrue(config.getQuery().getProperties().isEmpty());
        assertTrue(config.getQuery().getSource().getParameters().isEmpty());
        assertTrue(config.getQuery().getSort().isEmpty());
        assertTrue(config.getQuery().getFilters().isEmpty());

        assertTrue(config.getMeta().getLayouts().isEmpty());
        assertTrue(config.getMeta().getPropertyDescriptors().isEmpty());
        assertTrue(config.getMeta().getPropertyTypes().isEmpty());
        assertTrue(config.getMeta().getPagination().getPageSizes().isEmpty());
    }

    @Test
    void initializeNoOverwrite()
    {
        LiveDataConfiguration config = new LiveDataConfiguration();
        config.initialize();
        config.getQuery().getFilters().add(new Filter());
        config.getMeta().getPropertyDescriptors().add(new LiveDataPropertyDescriptor());

        config.initialize();

        assertTrue(config.getQuery().getFilters().get(0).getConstraints().isEmpty());
        assertTrue(config.getMeta().getPropertyDescriptors().iterator().next().getFilter().getOperators().isEmpty());
    }
}

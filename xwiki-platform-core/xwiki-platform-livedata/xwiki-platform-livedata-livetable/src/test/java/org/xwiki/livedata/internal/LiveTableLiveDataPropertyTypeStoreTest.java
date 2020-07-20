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

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link LiveTableLiveDataPropertyTypeStore}.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@ComponentTest
class LiveTableLiveDataPropertyTypeStoreTest
{
    @InjectMockComponents
    LiveTableLiveDataPropertyTypeStore typeStore;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void before()
    {
        this.objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
        this.typeStore.getParameters().clear();
    }

    @Test
    void getAll() throws Exception
    {
        StringBuilder expectedJSON = new StringBuilder("[");
        expectedJSON.append("{'id':'Boolean','displayer':{'id':'boolean'},'filter':{'id':'boolean'}},");
        expectedJSON.append("{'id':'ComputedField','displayer':{'id':'html'},'filter':{'id':'none'}},");
        expectedJSON.append("{'id':'DBList','filter':{'id':'suggest'}},");
        expectedJSON.append("{'id':'DBTreeList','filter':{'id':'suggest'}},");
        expectedJSON.append("{'id':'Date','displayer':{'id':'date'},'filter':{'id':'date'}},");
        expectedJSON.append("{'id':'Email','displayer':{'id':'html'},'filter':{'id':'text'}},");
        expectedJSON.append("{'id':'Groups','displayer':{'id':'html'},'filter':{'id':'suggest'}},");
        expectedJSON.append("{'id':'Number','displayer':{'id':'number'},'filter':{'id':'number'}},");
        expectedJSON.append("{'id':'Page','displayer':{'id':'html'},'filter':{'id':'suggest'}},");
        expectedJSON.append("{'id':'Password','filter':{'id':'none'}},");
        expectedJSON.append("{'id':'StaticList','filter':{'id':'list'}},");
        expectedJSON.append("{'id':'String','filter':{'id':'text'}},");
        expectedJSON.append("{'id':'TextArea','displayer':{'id':'html'},'filter':{'id':'text'}},");
        expectedJSON.append("{'id':'Users','displayer':{'id':'html'},'filter':{'id':'suggest'}}");
        expectedJSON.append("]");

        Collection<LiveDataPropertyDescriptor> types = this.typeStore.get();
        assertEquals(expectedJSON.toString().replace('\'', '"'), objectMapper.writeValueAsString(types));
    }

    @Test
    void getOne() throws Exception
    {
        String expectedJSON = "{'id':'Page','displayer':{'id':'html'},'filter':{'id':'suggest'}}";
        LiveDataPropertyDescriptor pageType = this.typeStore.get("Page").get();
        assertEquals(expectedJSON.replace('\'', '"'), objectMapper.writeValueAsString(pageType));
    }
}

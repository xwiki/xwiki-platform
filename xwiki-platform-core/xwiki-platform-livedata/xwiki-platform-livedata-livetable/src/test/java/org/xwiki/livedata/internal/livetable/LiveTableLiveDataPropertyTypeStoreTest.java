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
package org.xwiki.livedata.internal.livetable;

import java.io.FileReader;
import java.util.Collection;

import org.apache.commons.io.IOUtils;
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
 * @since 12.10
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
        String expectedJSON = IOUtils.toString(new FileReader("src/test/resources/propertyTypes.json"));
        expectedJSON = expectedJSON.replaceAll("\\n\\s*", "");

        Collection<LiveDataPropertyDescriptor> types = this.typeStore.get();
        assertEquals(expectedJSON, objectMapper.writeValueAsString(types));
    }

    @Test
    void getOne() throws Exception
    {
        String expectedJSON = "{'id':'Page','sortable':true,'filterable':true,'editable':true,"
            + "'displayer':{'id':'html'},'filter':{'id':'suggest','operators':[{'id':'equals','name':'equals'},"
            + "{'id':'startsWith','name':'startsWith'},{'id':'contains','name':'contains'}]}}";
        LiveDataPropertyDescriptor pageType = this.typeStore.get("Page").get();
        assertEquals(expectedJSON.replace('\'', '"'), objectMapper.writeValueAsString(pageType));
    }
}

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
package org.xwiki.model.script;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ComponentTest
class LegacyModelScriptServiceTest
{
    @InjectMockComponents
    private LegacyModelScriptService service;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @Mock
    private EntityReferenceValueProvider valueProvider;

    @Test
    void getEntityReferenceValue() throws Exception
    {
        when(this.componentManager.getInstance(EntityReferenceValueProvider.class, "current"))
            .thenReturn(this.valueProvider);
        when(this.valueProvider.getDefaultValue(EntityType.WIKI)).thenReturn("somewiki");

        assertEquals("somewiki", this.service.getEntityReferenceValue(EntityType.WIKI));
    }

    @Test
    void getEntityReferenceValueWithInvalidHint() throws Exception
    {
        when(this.componentManager.getInstance(EntityReferenceValueProvider.class, "invalid"))
            .thenThrow(new ComponentLookupException("error"));

        assertNull(this.service.getEntityReferenceValue(EntityType.WIKI, "invalid"));
    }

    @Test
    void getEntityReferenceValueWithNullType() throws Exception
    {
        assertNull(this.service.getEntityReferenceValue(null));
    }
}
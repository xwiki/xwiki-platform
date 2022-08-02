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
package org.xwiki.livedata.internal.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test of {@link LiveDataResourceContextInitializer}.
 *
 * @version $Id$
 * @since 12.10.9
 * @since 13.4.2
 * @since 13.5.1
 * @since 13.6RC1
 */
@ComponentTest
class LiveDataResourceContextInitializerTest
{
    @InjectMockComponents
    private LiveDataResourceContextInitializer contextInitializer;

    @MockComponent
    private ModelContext modelContext;

    @ParameterizedTest
    @ValueSource(strings = { "something:value", "12-6" })
    @NullSource
    void initializeNoContextChange(String namespace)
    {
        this.contextInitializer.initialize(namespace);
        verifyNoInteractions(this.modelContext);
    }
    
    @Test
    void initialize()
    {
        this.contextInitializer.initialize("wiki:s1");
        verify(this.modelContext).setCurrentEntityReference(new WikiReference("s1"));
    }
}

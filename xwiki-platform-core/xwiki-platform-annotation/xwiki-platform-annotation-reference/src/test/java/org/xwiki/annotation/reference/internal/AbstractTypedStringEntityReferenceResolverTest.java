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
package org.xwiki.annotation.reference.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extensions;
import org.mockito.Mock;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.test.junit5.mockito.MockitoComponentManagerExtension;

import static org.mockito.Mockito.verify;

/**
 * Test of {@link AbstractTypedStringEntityReferenceResolver}.
 *
 * @version $Id$
 * @since 13.9RC1
 * @since 13.4.4
 * @since 12.10.11
 */
@Extensions({ @ExtendWith({ MockitoComponentManagerExtension.class }) })
class AbstractTypedStringEntityReferenceResolverTest
{
    @Mock
    private EntityReferenceResolver<String> entityResolver;
    
    private final AbstractTypedStringEntityReferenceResolver resolver = new AbstractTypedStringEntityReferenceResolver()
    {
        @Override
        protected EntityReferenceResolver<String> getResolver()
        {
            return AbstractTypedStringEntityReferenceResolverTest.this.entityResolver;
        }
    };

    @Test
    void resolve()
    {
        this.resolver.resolve("WIKI://xwiki", EntityType.BLOCK);
        verify(this.entityResolver).resolve("xwiki", EntityType.WIKI);
    }

    @Test
    void resolveEntityReferenceIsNull()
    {
        this.resolver.resolve(null, EntityType.BLOCK);
        verify(this.entityResolver).resolve(null, EntityType.BLOCK);
    }
}
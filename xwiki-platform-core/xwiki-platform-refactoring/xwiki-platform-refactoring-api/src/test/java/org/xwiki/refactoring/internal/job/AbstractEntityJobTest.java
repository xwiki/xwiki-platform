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
package org.xwiki.refactoring.internal.job;

import org.junit.jupiter.api.BeforeEach;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.when;

/**
 * Base class for writing unit tests for jobs extending {@link AbstractEntityJob}.
 *
 * @version $Id$
 * @since 7.4M2
 */
public abstract class AbstractEntityJobTest extends AbstractJobTest
{
    @MockComponent
    protected AuthorizationManager authorization;

    @MockComponent
    protected ObservationManager observationManager;

    @MockComponent
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @Override
    @BeforeEach
    protected void configure() throws Exception
    {
        super.configure();
        when(defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new EntityReference("WebHome", EntityType.DOCUMENT));
    }
}

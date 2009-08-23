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
package org.xwiki.rendering.scaffolding;

import org.xwiki.test.AbstractComponentTestCase;
import org.junit.Before;

/**
 * To be used when testing components that use {@link org.xwiki.rendering.wiki.WikiModel}. 
 * 
 * @version $Id$
 * @since 2.0M1
 */
public abstract class AbstractXWikiRenderingTestCase extends AbstractComponentTestCase
{
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractComponentTestCase#setUp()
     */
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        
        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
        getComponentManager().registerComponent(MockDocumentNameSerializer.getComponentDescriptor());
    }
}

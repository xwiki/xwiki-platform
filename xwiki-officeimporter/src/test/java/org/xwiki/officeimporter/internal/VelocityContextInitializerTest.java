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
package org.xwiki.officeimporter.internal;

import org.apache.velocity.VelocityContext;
import org.xwiki.test.AbstractXWikiComponentTestCase;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.VelocityContextInitializer;

/**
 * Test class for various {@link VelocityContextInitializer} implementations.
 * 
 * @version $Id$
 * @since 1.9
 */
public class VelocityContextInitializerTest extends AbstractXWikiComponentTestCase
{
    /**
     * The {@link VelocityContextFactory} component.
     */
    private VelocityContextFactory contextFactory;
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        this.contextFactory = (VelocityContextFactory) getComponentManager().lookup(VelocityContextFactory.class);        
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.test.AbstractXWikiComponentTestCase#registerComponents()
     */
    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();
        getComponentManager().registerComponent(MockDocumentAccessBridge.getComponentDescriptor());
        getComponentManager().registerComponent(MockDocumentNameSerializer.getComponentDescriptor());
    }
    
    /**
     * Test the presence of velocity bridges.
     * 
     * @throws Exception
     */
    public void testVelocityBridges() throws Exception
    {
        VelocityContext context = contextFactory.createContext();
        assertNotNull(context.get("officeimporter"));
        assertNotNull(context.get("ooconfig"));
        assertNotNull(context.get("oomanager"));
    }
}

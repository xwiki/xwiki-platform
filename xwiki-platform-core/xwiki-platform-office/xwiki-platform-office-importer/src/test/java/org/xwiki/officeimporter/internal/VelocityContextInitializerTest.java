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
import org.junit.Test;
import org.xwiki.velocity.VelocityContextFactory;
import org.xwiki.velocity.VelocityContextInitializer;

/**
 * Test class for various {@link VelocityContextInitializer} implementations.
 * 
 * @version $Id$
 * @since 1.9RC2
 */
public class VelocityContextInitializerTest extends AbstractOfficeImporterTest
{
    /**
     * Test the presence of velocity bridges.
     * 
     * @throws Exception
     */
    @Test
    public void testVelocityBridges() throws Exception
    {
        VelocityContextFactory factory = getComponentManager().getInstance(VelocityContextFactory.class);
        VelocityContext context = factory.createContext();

        /*
        TODO: Asiri needs to verify this. I think it should be replaced by the new velocity bridge but not sure.
        Assert.assertNotNull(context.get("officeimporter"));
        Assert.assertNotNull(context.get("ooconfig"));
        Assert.assertNotNull(context.get("oomanager"));
        */
    }
}

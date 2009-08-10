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

package org.xwiki.rendering;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Unit test for {@link org.xwiki.rendering.internal.macro.message.AbstractMessageMacro} macros (for tests that cannot
 * be performed with the rendering test framework).
 *
 * @version $Id$
 * @since 2.0M3
 */
public class MessageMacroTest extends AbstractComponentTestCase
{
    /**
     * Tests whether message macro content descriptor is never null.
     */
    @Test
    public void testMacroContentDescriptorIsNotNull() throws Exception
    {
        Macro messageMacro = getComponentManager().lookup(Macro.class, "info");
        Assert.assertNotNull(messageMacro.getDescriptor().getContentDescriptor());
        messageMacro = getComponentManager().lookup(Macro.class, "warning");
        Assert.assertNotNull(messageMacro.getDescriptor().getContentDescriptor());
        messageMacro = getComponentManager().lookup(Macro.class, "error");
        Assert.assertNotNull(messageMacro.getDescriptor().getContentDescriptor());
    }
}

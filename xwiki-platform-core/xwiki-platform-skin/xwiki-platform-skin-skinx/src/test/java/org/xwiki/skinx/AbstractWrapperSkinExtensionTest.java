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
package org.xwiki.skinx;

import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.Test;
import org.xwiki.component.annotation.Component;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link org.xwiki.skinx.AbstractWrapperSkinExtension}.
 *
 * @version $Id$
 * @since 6.4M1
 */
public class AbstractWrapperSkinExtensionTest
{
    @Component(staticRegistration = false)
    @Named("test")
    @Singleton
    private class WithNamedAbstractWrapperSkinExtension extends AbstractWrapperSkinExtension
    {
    }

    @Component(value = "test", staticRegistration = false)
    @Singleton
    private class WithComponentAbstractWrapperSkinExtension extends AbstractWrapperSkinExtension
    {
    }

    @Test
    public void getNameWhenUsingNameAnnotation()
    {
        AbstractWrapperSkinExtension extension = new WithNamedAbstractWrapperSkinExtension();
        assertEquals("test", extension.getName());
    }

    /**
     * Test backward compatibility use case.
     */
    @Test
    public void getNameWhenUsingComponentAnnotation()
    {
        AbstractWrapperSkinExtension extension = new WithComponentAbstractWrapperSkinExtension();
        assertEquals("test", extension.getName());
    }
}

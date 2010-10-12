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
package org.xwiki.rendering.macro.toc;

import java.util.Collections;

import junit.framework.Assert;

import org.junit.Test;
import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.rendering.internal.macro.toc.TocMacro;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Validate {@link TocMacro}.
 * 
 * @version $Id$
 */
public class TocMacroTest extends AbstractComponentTestCase
{
    private BeanManager beanManager;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.beanManager = getComponentManager().lookup(BeanManager.class);
    }

    @Test
    public void testStartTooLow()
    {
        try {
            this.beanManager.populate(new TocMacroParameters(), Collections.singletonMap("start", "0"));
            Assert.fail("populate should have failed");
        } catch (PropertyException expected) {
            // expected
        }
    }

    @Test
    public void testDepthTooLow()
    {
        try {
            this.beanManager.populate(new TocMacroParameters(), Collections.singletonMap("depth", "0"));
            Assert.fail("populate should have failed");
        } catch (PropertyException expected) {
            // expected
        }
    }
}

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
package org.xwiki.properties.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.properties.BeanManager;
import org.xwiki.properties.PropertyException;
import org.xwiki.properties.PropertyMandatoryException;
import org.xwiki.properties.internal.DefaultBeanDescriptorTest.BeanTest;
import org.xwiki.test.AbstractXWikiComponentTestCase;

/**
 * Validate {@link DefaultBeanManager}.
 * 
 * @version $Id$
 */
public class DefaultPropertiesManagerTest extends AbstractXWikiComponentTestCase
{
    BeanManager defaultPropertiesManager;

    @Override
    public void setUp() throws Exception
    {
        this.defaultPropertiesManager = getComponentManager().lookup(BeanManager.class);
    }

    public void testPopulate() throws PropertyException
    {
        BeanTest beanTest = new BeanTest();

        Map<String, String> values = new HashMap<String, String>();

        values.put("lowerprop", "lowerpropvalue");
        values.put("upperprop", "upperPropvalue");
        values.put("prop2", "42");
        values.put("prop3", "true");
        values.put("hiddenProperty", "hiddenPropertyvalue");
        values.put("publicField", "publicFieldvalue");

        this.defaultPropertiesManager.populate(beanTest, values);

        assertEquals("lowerpropvalue", beanTest.getLowerprop());
        assertEquals("upperPropvalue", beanTest.getUpperProp());
        assertEquals(42, beanTest.getProp2());
        assertEquals(true, beanTest.getProp3());
        assertEquals(null, beanTest.getHiddenProperty());
        assertEquals("publicFieldvalue", beanTest.publicField);
    }

    public void testPopulateWhenMissingMandatoryProperty() throws PropertyException
    {
        BeanTest beanTest = new BeanTest();

        Map<String, String> values = new HashMap<String, String>();

        try {
            this.defaultPropertiesManager.populate(beanTest, values);
            fail("Should have thrown a PropertyMandatoryException exception");
        } catch (PropertyMandatoryException e) {
            // expected
        }
    }
}

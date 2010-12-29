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

import java.lang.reflect.ParameterizedType;
import java.util.List;

import junit.framework.TestCase;

import org.xwiki.properties.PropertyDescriptor;
import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyHidden;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.properties.annotation.PropertyName;

/**
 * Validate {@link DefaultBeanDescriptor}.
 * 
 * @version $Id$
 */
public class DefaultBeanDescriptorTest extends TestCase
{
    public static class BeanTest
    {
        private String lowerprop;

        private String upperProp;

        private String prop1 = "defaultprop1";

        private int prop2;

        private boolean prop3;

        private String hiddenProperty;
        
        private List<Integer> genericProp;
        
        @PropertyName("Public Field")
        @PropertyDescription("a public field")
        public String publicField;
        
        public List<Integer> genericField;

        public void setLowerprop(String lowerprop)
        {
            this.lowerprop = lowerprop;
        }

        public String getLowerprop()
        {
            return this.lowerprop;
        }

        public void setUpperProp(String upperProp)
        {
            this.upperProp = upperProp;
        }

        public String getUpperProp()
        {
            return this.upperProp;
        }

        @PropertyDescription("prop1 description")
        public void setProp1(String prop1)
        {
            this.prop1 = prop1;
        }

        public String getProp1()
        {
            return this.prop1;
        }

        @PropertyMandatory
        @PropertyDescription("prop2 description")
        public void setProp2(int prop1)
        {
            this.prop2 = prop1;
        }

        public int getProp2()
        {
            return this.prop2;
        }

        public void setProp3(boolean prop1)
        {
            this.prop3 = prop1;
        }

        @PropertyMandatory
        @PropertyDescription("prop3 description")
        public boolean getProp3()
        {
            return this.prop3;
        }

        @PropertyHidden
        public void setHiddenProperty(String hiddenProperty)
        {
            this.hiddenProperty = hiddenProperty;
        }

        public String getHiddenProperty()
        {
            return hiddenProperty;
        }
        
        public List<Integer> getGenericProp()
        {
            return genericProp;
        }
        
        public void setGenericProp(List<Integer> genericProp)
        {
            this.genericProp = genericProp;
        }
    }

    private DefaultBeanDescriptor beanDescriptor;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.beanDescriptor = new DefaultBeanDescriptor(BeanTest.class);
    }

    public void testPropertyDescriptor()
    {
        assertNull(this.beanDescriptor.getProperty("hiddenProperty"));

        PropertyDescriptor lowerPropertyDescriptor = this.beanDescriptor.getProperty("lowerprop");

        assertNotNull(lowerPropertyDescriptor);
        assertEquals("lowerprop", lowerPropertyDescriptor.getId());
        assertEquals("lowerprop", lowerPropertyDescriptor.getName());
        assertEquals("lowerprop", lowerPropertyDescriptor.getDescription());
        assertSame(String.class, lowerPropertyDescriptor.getPropertyClass());
        assertEquals(null, lowerPropertyDescriptor.getDefaultValue());
        assertEquals(false, lowerPropertyDescriptor.isMandatory());
        assertNotNull(lowerPropertyDescriptor.getWriteMethod());
        assertNotNull(lowerPropertyDescriptor.getReadMethod());
        assertNull(lowerPropertyDescriptor.getFied());

        PropertyDescriptor prop1Descriptor = this.beanDescriptor.getProperty("prop1");

        assertEquals("defaultprop1", prop1Descriptor.getDefaultValue());
    }

    public void testPropertyDescriptorWithUpperCase()
    {
        PropertyDescriptor upperPropertyDescriptor = this.beanDescriptor.getProperty("upperProp");

        assertNotNull(upperPropertyDescriptor);
        assertEquals("upperProp", upperPropertyDescriptor.getId());
        assertEquals("upperProp", upperPropertyDescriptor.getName());
        assertEquals("upperProp", upperPropertyDescriptor.getDescription());
        assertSame(String.class, upperPropertyDescriptor.getPropertyClass());
        assertEquals(false, upperPropertyDescriptor.isMandatory());
        assertNotNull(upperPropertyDescriptor.getWriteMethod());
        assertNotNull(upperPropertyDescriptor.getReadMethod());
        assertNull(upperPropertyDescriptor.getFied());
    }

    public void testPropertyDescriptorPublicField()
    {
        PropertyDescriptor publicFieldPropertyDescriptor = this.beanDescriptor.getProperty("publicField");

        assertNotNull(publicFieldPropertyDescriptor);
        assertEquals("publicField", publicFieldPropertyDescriptor.getId());
        assertEquals("Public Field", publicFieldPropertyDescriptor.getName());
        assertEquals("a public field", publicFieldPropertyDescriptor.getDescription());
        assertSame(String.class, publicFieldPropertyDescriptor.getPropertyClass());
        assertEquals(false, publicFieldPropertyDescriptor.isMandatory());
        assertNull(publicFieldPropertyDescriptor.getWriteMethod());
        assertNull(publicFieldPropertyDescriptor.getReadMethod());
        assertNotNull(publicFieldPropertyDescriptor.getFied());
    }

    public void testPropertyDescriptorWithDescription()
    {
        PropertyDescriptor prop1Descriptor = this.beanDescriptor.getProperty("prop1");

        assertNotNull(prop1Descriptor);
        assertEquals("prop1", prop1Descriptor.getId());
        assertEquals("prop1 description", prop1Descriptor.getDescription());
        assertSame(String.class, prop1Descriptor.getPropertyClass());
        assertEquals(false, prop1Descriptor.isMandatory());
        assertNotNull(prop1Descriptor.getWriteMethod());
        assertNotNull(prop1Descriptor.getReadMethod());
        assertNull(prop1Descriptor.getFied());
    }

    public void testPropertyDescriptorWithDescriptionAndMandatory()
    {
        PropertyDescriptor prop2Descriptor = this.beanDescriptor.getProperty("prop2");

        assertNotNull(prop2Descriptor);
        assertEquals("prop2", prop2Descriptor.getId());
        assertEquals("prop2 description", prop2Descriptor.getDescription());
        assertSame(int.class, prop2Descriptor.getPropertyClass());
        assertEquals(true, prop2Descriptor.isMandatory());
        assertNotNull(prop2Descriptor.getWriteMethod());
        assertNotNull(prop2Descriptor.getReadMethod());
        assertNull(prop2Descriptor.getFied());
    }

    public void testPropertyDescriptorWithDescriptionAndMandatoryOnSetter()
    {
        PropertyDescriptor prop3Descriptor = this.beanDescriptor.getProperty("prop3");

        assertNotNull(prop3Descriptor);
        assertEquals("prop3", prop3Descriptor.getId());
        assertEquals("prop3 description", prop3Descriptor.getDescription());
        assertSame(boolean.class, prop3Descriptor.getPropertyClass());
        assertEquals(true, prop3Descriptor.isMandatory());
        assertNotNull(prop3Descriptor.getWriteMethod());
        assertNotNull(prop3Descriptor.getReadMethod());
        assertNull(prop3Descriptor.getFied());
    }
    
    public void testPropertyDescriptorGeneric()
    {
        PropertyDescriptor genericPropertyDescriptor = this.beanDescriptor.getProperty("genericProp");

        assertNotNull(genericPropertyDescriptor);
        assertEquals("genericProp", genericPropertyDescriptor.getId());
        assertEquals("genericProp", genericPropertyDescriptor.getName());
        assertEquals("genericProp", genericPropertyDescriptor.getDescription());
        assertSame(List.class, ((ParameterizedType)genericPropertyDescriptor.getPropertyType()).getRawType());
        assertSame(Integer.class, ((ParameterizedType)genericPropertyDescriptor.getPropertyType()).getActualTypeArguments()[0]);
        assertEquals(null, genericPropertyDescriptor.getDefaultValue());
        assertEquals(false, genericPropertyDescriptor.isMandatory());
        assertNotNull(genericPropertyDescriptor.getWriteMethod());
        assertNotNull(genericPropertyDescriptor.getReadMethod());
        assertNull(genericPropertyDescriptor.getFied());

        PropertyDescriptor prop1Descriptor = this.beanDescriptor.getProperty("prop1");

        assertEquals("defaultprop1", prop1Descriptor.getDefaultValue());
    }
    
    public void testPropertyDescriptorGenericField()
    {
        PropertyDescriptor genericFieldPropertyDescriptor = this.beanDescriptor.getProperty("genericField");

        assertNotNull(genericFieldPropertyDescriptor);
        assertEquals("genericField", genericFieldPropertyDescriptor.getId());
        assertEquals("genericField", genericFieldPropertyDescriptor.getName());
        assertEquals("genericField", genericFieldPropertyDescriptor.getDescription());
        assertSame(List.class, ((ParameterizedType)genericFieldPropertyDescriptor.getPropertyType()).getRawType());
        assertSame(Integer.class, ((ParameterizedType)genericFieldPropertyDescriptor.getPropertyType()).getActualTypeArguments()[0]);
        assertEquals(false, genericFieldPropertyDescriptor.isMandatory());
        assertNull(genericFieldPropertyDescriptor.getWriteMethod());
        assertNull(genericFieldPropertyDescriptor.getReadMethod());
        assertNotNull(genericFieldPropertyDescriptor.getFied());
    }
}

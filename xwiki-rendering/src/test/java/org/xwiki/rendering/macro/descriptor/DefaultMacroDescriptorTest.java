package org.xwiki.rendering.macro.descriptor;

import java.util.Map;

import junit.framework.TestCase;

/**
 * Validate {@link DefaultMacroDescriptor} and {@link AbstractMacroDescriptor}.
 * 
 * @version $Id$
 */
public class DefaultMacroDescriptorTest extends TestCase
{
    private static class ParametersTests
    {
        private String lowerparam;

        private String upperParam;

        private String param1;

        private int param2;

        private boolean param3;

        public void setLowerparam(String lowerparam)
        {
            this.lowerparam = lowerparam;
        }

        public String getLowerparam()
        {
            return this.lowerparam;
        }

        public void setUpperParam(String upperParam)
        {
            this.upperParam = upperParam;
        }

        public String getUpperParam()
        {
            return this.upperParam;
        }

        @ParameterDescription("param1 description")
        public void setParam1(String param1)
        {
            this.param1 = param1;
        }

        public String getParam1()
        {
            return this.param1;
        }

        @ParameterMandatory
        @ParameterDescription("param2 description")
        public void setParam2(int param1)
        {
            this.param2 = param1;
        }

        public int getParam2()
        {
            return this.param2;
        }

        public void setParam3(boolean param1)
        {
            this.param3 = param1;
        }

        @ParameterMandatory
        @ParameterDescription("param3 description")
        public boolean getParam3()
        {
            return this.param3;
        }
    }

    private DefaultMacroDescriptor macroDescriptor = new DefaultMacroDescriptor("Description", ParametersTests.class);

    public void testParameterDescriptor()
    {
        Map<String, ParameterDescriptor> map = this.macroDescriptor.getParameterDescriptorMap();

        ParameterDescriptor lowerParamDescriptor = map.get("lowerparam");

        assertNotNull(lowerParamDescriptor);
        assertEquals("lowerparam", lowerParamDescriptor.getName());
        assertEquals("lowerparam", lowerParamDescriptor.getDescription());
        assertSame(String.class, lowerParamDescriptor.getType());
        assertEquals(false, lowerParamDescriptor.isMandatory());
    }

    public void testParameterDescriptorWithUpperCase()
    {
        Map<String, ParameterDescriptor> map = this.macroDescriptor.getParameterDescriptorMap();

        ParameterDescriptor upperParamDescriptor = map.get("upperParam".toLowerCase());

        assertNotNull(upperParamDescriptor);
        assertEquals("upperParam", upperParamDescriptor.getName());
        assertEquals("upperParam", upperParamDescriptor.getDescription());
        assertSame(String.class, upperParamDescriptor.getType());
        assertEquals(false, upperParamDescriptor.isMandatory());
    }

    public void testParameterDescriptorWithDescription()
    {
        Map<String, ParameterDescriptor> map = this.macroDescriptor.getParameterDescriptorMap();

        ParameterDescriptor param1Descriptor = map.get("param1".toLowerCase());

        assertNotNull(param1Descriptor);
        assertEquals("param1", param1Descriptor.getName());
        assertEquals("param1 description", param1Descriptor.getDescription());
        assertSame(String.class, param1Descriptor.getType());
        assertEquals(false, param1Descriptor.isMandatory());
    }

    public void testParameterDescriptorWithDescriptionAndMandatory()
    {
        Map<String, ParameterDescriptor> map = this.macroDescriptor.getParameterDescriptorMap();

        ParameterDescriptor param2Descriptor = map.get("param2".toLowerCase());

        assertNotNull(param2Descriptor);
        assertEquals("param2", param2Descriptor.getName());
        assertEquals("param2 description", param2Descriptor.getDescription());
        assertSame(int.class, param2Descriptor.getType());
        assertEquals(true, param2Descriptor.isMandatory());
    }

    public void testParameterDescriptorWithDescriptionAndMandatoryOnSetter()
    {
        Map<String, ParameterDescriptor> map = this.macroDescriptor.getParameterDescriptorMap();

        ParameterDescriptor param3Descriptor = map.get("param3".toLowerCase());

        assertNotNull(param3Descriptor);
        assertEquals("param3", param3Descriptor.getName());
        assertEquals("param3 description", param3Descriptor.getDescription());
        assertSame(boolean.class, param3Descriptor.getType());
        assertEquals(true, param3Descriptor.isMandatory());
    }
}

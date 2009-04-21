package com.xpn.xwiki.test;

import com.xpn.xwiki.CoreConfiguration;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;

public class TestCoreConfiguration implements CoreConfiguration
{
    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor getComponentDescriptor()
    {
        DefaultComponentDescriptor componentDescriptor = new DefaultComponentDescriptor();

        componentDescriptor.setRole(CoreConfiguration.class);
        componentDescriptor.setImplementation(TestCoreConfiguration.class.getName());

        return componentDescriptor;
    }

    public String getDefaultDocumentSyntax()
    {
        return "xwiki/1.0";
    }
}

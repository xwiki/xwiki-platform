package org.xwiki.cache.tests;

import java.util.Collections;
import java.util.List;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConfigurationSourceCollection;

/**
 * @version $Id$
 * @since 1.7M1
 */
public class ConfigurationSourceCollectionMock implements ConfigurationSourceCollection
{
    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor getComponentDescriptor()
    {
        DefaultComponentDescriptor componentDescriptor = new DefaultComponentDescriptor();

        componentDescriptor.setRole(ConfigurationSourceCollection.class);
        componentDescriptor.setImplementation(ConfigurationSourceCollectionMock.class.getName());

        return componentDescriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.configuration.ConfigurationSourceCollection#getConfigurationSources()
     */
    public List<ConfigurationSource> getConfigurationSources()
    {
        return Collections.emptyList();
    }
}

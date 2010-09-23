package org.xwiki.extension.test;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.internal.DefaultCoreExtension;
import org.xwiki.extension.repository.internal.DefaultCoreExtensionRepository;

@Component
public class ConfigurableDefaultCoreExtensionRepository extends DefaultCoreExtensionRepository
{
    public static void register(ComponentManager componentManager) throws ComponentRepositoryException
    {
        DefaultComponentDescriptor<CoreExtensionRepository> componentDescriptor =
            new DefaultComponentDescriptor<CoreExtensionRepository>();
        componentDescriptor.setImplementation(ConfigurableDefaultCoreExtensionRepository.class);
        componentDescriptor.setRole(CoreExtensionRepository.class);

        componentManager.registerComponent(componentDescriptor);
    }

    public void addExtensions(CoreExtension extension)
    {
        this.extensions.put(extension.getId(), extension);
    }

    public void addExtensions(String name, String version)
    {
        this.extensions.put(name, new DefaultCoreExtension(name, version));
    }
}

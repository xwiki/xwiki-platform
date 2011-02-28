package org.xwiki.extension.test;

import org.junit.Before;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.task.InstallRequest;
import org.xwiki.extension.task.Task;
import org.xwiki.extension.task.TaskManager;
import org.xwiki.extension.task.UninstallRequest;
import org.xwiki.test.AbstractComponentTestCase;

public abstract class AbstractExtensionHandlerTest extends AbstractComponentTestCase
{
    private LocalExtensionRepository localExtensionRepository;

    private RepositoryUtil repositoryUtil;

    private TaskManager taskManager;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil = new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource());
        this.repositoryUtil.setup();

        // lookup

        this.taskManager = getComponentManager().lookup(TaskManager.class);
        this.localExtensionRepository = getComponentManager().lookup(LocalExtensionRepository.class);
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        ConfigurableDefaultCoreExtensionRepository.register(getComponentManager());
    }

    protected LocalExtension install(ExtensionId extensionId) throws ComponentLookupException, Exception
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(extensionId);
        Task installTask = this.taskManager.install(installRequest);

        if (installTask.getExceptions() != null) {
            throw installTask.getExceptions().get(0);
        }

        return (LocalExtension) this.localExtensionRepository.resolve(extensionId);
    }

    protected LocalExtension uninstall(ExtensionId extensionId) throws ComponentLookupException, Exception
    {
        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.addExtension(extensionId);
        Task uninstallTask = this.taskManager.uninstall(uninstallRequest);

        if (uninstallTask.getExceptions() != null) {
            throw uninstallTask.getExceptions().get(0);
        }

        return (LocalExtension) this.localExtensionRepository.resolve(extensionId);
    }
}

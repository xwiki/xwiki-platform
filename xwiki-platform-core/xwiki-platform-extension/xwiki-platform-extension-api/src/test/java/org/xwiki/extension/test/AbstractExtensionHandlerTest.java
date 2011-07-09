package org.xwiki.extension.test;

import java.util.List;

import org.junit.Before;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.Job;
import org.xwiki.extension.job.JobManager;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.event.LogLevel;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.AbstractComponentTestCase;

public abstract class AbstractExtensionHandlerTest extends AbstractComponentTestCase
{
    private LocalExtensionRepository localExtensionRepository;

    private RepositoryUtil repositoryUtil;

    private JobManager jobManager;

    private ObservationManager observation;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.repositoryUtil =
            new RepositoryUtil(getClass().getSimpleName(), getConfigurationSource(), getComponentManager());
        this.repositoryUtil.setup();

        // lookup

        this.jobManager = getComponentManager().lookup(JobManager.class);
        this.localExtensionRepository = getComponentManager().lookup(LocalExtensionRepository.class);
        this.observation = getComponentManager().lookup(ObservationManager.class);
    }

    public ObservationManager getObservation()
    {
        return this.observation;
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        ConfigurableDefaultCoreExtensionRepository.register(getComponentManager());
    }

    protected LocalExtension install(ExtensionId extensionId) throws Throwable
    {
        InstallRequest installRequest = new InstallRequest();
        installRequest.addExtension(extensionId);
        Job installJob = this.jobManager.install(installRequest);

        List<LogEvent> errors = installJob.getStatus().getLog(LogLevel.ERROR);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }

        return (LocalExtension) this.localExtensionRepository.resolve(extensionId);
    }

    protected void uninstall(ExtensionId extensionId) throws Throwable
    {
        UninstallRequest uninstallRequest = new UninstallRequest();
        uninstallRequest.addExtension(extensionId);
        Job uninstallJob = this.jobManager.uninstall(uninstallRequest);

        List<LogEvent> errors = uninstallJob.getStatus().getLog(LogLevel.ERROR);
        if (!errors.isEmpty()) {
            throw errors.get(0).getThrowable();
        }
    }
}

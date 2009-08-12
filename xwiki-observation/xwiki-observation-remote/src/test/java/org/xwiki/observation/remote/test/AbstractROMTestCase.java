package org.xwiki.observation.remote.test;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.container.ApplicationContext;
import org.xwiki.container.Container;
import org.xwiki.observation.remote.internal.jgroups.JGroupsRemoteObservationManager;
import org.xwiki.test.XWikiComponentInitializer;

public abstract class AbstractROMTestCase
{
    private XWikiComponentInitializer initializer1 = new XWikiComponentInitializer();

    private XWikiComponentInitializer initializer2 = new XWikiComponentInitializer();

    @Before
    protected void setUp() throws Exception
    {
        this.initializer1.initializeContainer();
        this.initializer1.initializeConfigurationSource();
        this.initializer1.initializeExecution();

        this.initializer2.initializeContainer();
        this.initializer2.initializeConfigurationSource();
        this.initializer2.initializeExecution();

        ApplicationContext applicationContext = new ApplicationContext()
        {
            public File getTemporaryDirectory()
            {
                throw new RuntimeException("Not implemented");
            }

            public InputStream getResourceAsStream(String resourceName)
            {
                return this.getClass().getClassLoader().getResourceAsStream(
                    resourceName.substring(("/WEB-INF/" + JGroupsRemoteObservationManager.CONFIGURATION_PATH).length()));
            }

            public URL getResource(String resourceName) throws MalformedURLException
            {
                throw new RuntimeException("Not implemented");
            }
        };

        getComponentManager1().lookup(Container.class).setApplicationContext(applicationContext);
        getComponentManager2().lookup(Container.class).setApplicationContext(applicationContext);
    }

    public EmbeddableComponentManager getComponentManager1() throws Exception
    {
        return this.initializer1.getComponentManager();
    }

    public EmbeddableComponentManager getComponentManager2() throws Exception
    {
        return this.initializer2.getComponentManager();
    }
}

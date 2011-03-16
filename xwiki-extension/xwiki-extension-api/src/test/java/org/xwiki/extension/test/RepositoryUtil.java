package org.xwiki.extension.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.test.MockConfigurationSource;

public class RepositoryUtil
{
    private String name = "test";

    private MockConfigurationSource configurationSource;

    private File workingDirectory;

    private File repositoriesDirectory;

    private File localRepository;

    private File aetherRepository;

    private ComponentManager componentManager;

    public RepositoryUtil(String name, MockConfigurationSource configurationSource, ComponentManager componentManager)
    {
        this.name = name;
        this.configurationSource = configurationSource;
        this.componentManager = componentManager;

        this.workingDirectory = new File("target/" + this.name + "/");
        this.repositoriesDirectory = new File(this.workingDirectory, "repository/");
        this.localRepository = new File(this.repositoriesDirectory, "local/");
        this.aetherRepository = new File(this.repositoriesDirectory, "aether/");
    }

    public String getName()
    {
        return name;
    }

    public File getLocalRepository()
    {
        return this.localRepository;
    }

    public File getAetherRepository()
    {
        return this.aetherRepository;
    }

    public void setup() throws IOException, ComponentLookupException
    {
        clean();

        // copy

        File localRepository = getLocalRepository();

        copyResourceFolder(localRepository, "repository.local");

        // configuration

        this.configurationSource.setProperty("extension.localRepository", getLocalRepository().getAbsolutePath());
        this.configurationSource.setProperty("extension.aether.localRepository", getAetherRepository()
            .getAbsolutePath());

        // repositories

        ExtensionRepositoryManager repositoryManager = this.componentManager.lookup(ExtensionRepositoryManager.class);

        ResourceExtensionRepository resourceExtensionrepository =
            new ResourceExtensionRepository(getClass().getClassLoader(), "repository/remote/");
        repositoryManager.addRepository(resourceExtensionrepository);
    }

    public void copyResourceFolder(File targetFolder, String resourcePackage) throws IOException
    {
        targetFolder.mkdirs();

        Reflections reflections =
            new Reflections(new ConfigurationBuilder().setScanners(new ResourcesScanner())
                .setUrls(ClasspathHelper.getUrlsForPackagePrefix(""))
                .filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix(resourcePackage))));

        for (String resource : reflections.getResources(Pattern.compile(".*"))) {
            File targetFile = new File(targetFolder, resource.substring(resourcePackage.length() + 1));

            InputStream resourceStream = getClass().getResourceAsStream("/" + resource);

            try {
                FileUtils.copyInputStreamToFile(resourceStream, targetFile);
            } finally {
                resourceStream.close();
            }
        }
    }

    public void clean() throws IOException
    {
        if (this.workingDirectory.exists()) {
            FileUtils.deleteDirectory(this.workingDirectory);
        }
    }
}

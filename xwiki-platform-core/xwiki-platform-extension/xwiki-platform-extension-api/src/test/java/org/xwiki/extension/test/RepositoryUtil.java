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
package org.xwiki.extension.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.vfs.Vfs;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.ExtensionRepositoryException;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.test.MockConfigurationSource;

import com.google.common.base.Predicates;

public class RepositoryUtil
{
    private static final String MAVENREPOSITORY_ID = "test-maven";

    private String name = "test";

    private MockConfigurationSource configurationSource;

    private File workingDirectory;

    private File repositoriesDirectory;

    private File localRepositoryRoot;

    private File aetherRepositoryRoot;

    private File mavenRepositoryRoot;

    private File remoteRepositoryRoot;

    private FileExtensionRepository remoteRepository;

    private ComponentManager componentManager;

    private ExtensionPackager extensionPackager;

    public RepositoryUtil(String name, MockConfigurationSource configurationSource, ComponentManager componentManager)
    {
        this.name = name;
        this.configurationSource = configurationSource;
        this.componentManager = componentManager;

        this.workingDirectory = new File("target/" + this.name + "/");
        this.repositoriesDirectory = new File(this.workingDirectory, "repository/");
        this.localRepositoryRoot = new File(this.repositoriesDirectory, "local/");
        this.aetherRepositoryRoot = new File(this.repositoriesDirectory, "aether/");
        this.mavenRepositoryRoot = new File(this.repositoriesDirectory, "maven/");
        this.remoteRepositoryRoot = new File(this.repositoriesDirectory, "remote/");

        this.extensionPackager = new ExtensionPackager(this.workingDirectory, this.remoteRepositoryRoot);
    }

    public String getName()
    {
        return name;
    }

    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    public File getLocalRepository()
    {
        return this.localRepositoryRoot;
    }

    public File getAetherRepository()
    {
        return this.aetherRepositoryRoot;
    }

    public File getRemoteRepository()
    {
        return this.remoteRepositoryRoot;
    }

    public File getMavenRepository()
    {
        return this.mavenRepositoryRoot;
    }

    public String getRemoteRepositoryId()
    {
        return MAVENREPOSITORY_ID;
    }

    public void setup() throws IOException, ComponentLookupException, ExtensionRepositoryException, URISyntaxException
    {
        clean();

        // copy

        copyResourceFolder(getLocalRepository(), "repository.local");

        // configuration

        this.configurationSource.setProperty("extension.localRepository", getLocalRepository().getAbsolutePath());
        this.configurationSource.setProperty("extension.aether.localRepository", getAetherRepository()
            .getAbsolutePath());

        // remote repositories

        ExtensionRepositoryManager repositoryManager = this.componentManager.lookup(ExtensionRepositoryManager.class);

        // lite remote repository

        if (copyResourceFolder(getRemoteRepository(), "repository.remote") > 0) {
            this.remoteRepository = new FileExtensionRepository(getRemoteRepository());
            repositoryManager.addRepository(remoteRepository);
        }

        // maven resource repository

        URL url = getClass().getClassLoader().getResource("repository/maven");
        if (url != null) {
            repositoryManager.addRepository(new ExtensionRepositoryId(MAVENREPOSITORY_ID, "maven", url.toURI()));
        }

        // generated extensions

        this.extensionPackager.generateExtensions();
    }

    public int copyResourceFolder(File targetFolder, String resourcePackage) throws IOException
    {
        int nb = 0;

        targetFolder.mkdirs();

        Reflections reflections =
            new Reflections(new ConfigurationBuilder().setScanners(new ResourcesScanner())
                .setUrls(ClasspathHelper.forPackage(""))
                .filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix(resourcePackage))));

        for (String resource : reflections.getResources(Pattern.compile(".*"))) {
            File targetFile = new File(targetFolder, resource.substring(resourcePackage.length() + 1));

            InputStream resourceStream = getClass().getResourceAsStream("/" + resource);

            try {
                FileUtils.copyInputStreamToFile(resourceStream, targetFile);
                ++nb;
            } finally {
                resourceStream.close();
            }
        }

        return nb;
    }

    public void clean() throws IOException
    {
        if (this.workingDirectory.exists()) {
            FileUtils.deleteDirectory(this.workingDirectory);
        }
    }
}

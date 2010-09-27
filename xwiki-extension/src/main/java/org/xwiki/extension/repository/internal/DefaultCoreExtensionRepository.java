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
package org.xwiki.extension.repository.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;

import com.google.common.base.Predicates;

@Component
public class DefaultCoreExtensionRepository extends AbstractLogEnabled implements CoreExtensionRepository,
    Initializable
{
    public static final String COMPONENT_OVERRIDE_LIST = "META-INF/pom.xml";

    private ExtensionRepositoryId repositoryId;

    protected Map<String, CoreExtension> extensions;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.repositoryId = new ExtensionRepositoryId("core", "xwiki-core", null);

        loadExtensions();
    }

    private void loadExtensions()
    {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setScanners(new ResourcesScanner());
        configurationBuilder.setUrls(ClasspathHelper.getUrlsForPackagePrefix("META-INF.maven"));
        configurationBuilder.filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix("META-INF.maven")));

        Reflections reflections = new Reflections(configurationBuilder);

        Set<String> descriptors = reflections.getResources(Predicates.equalTo("pom.xml"));

        this.extensions = new LinkedHashMap<String, CoreExtension>(descriptors.size());

        for (String descriptor : descriptors) {
            URL descriptorUrl = getClass().getClassLoader().getResource(descriptor);

            // TODO: extract jar URL from descriptorUrl

            InputStream descriptorStream = getClass().getClassLoader().getResourceAsStream(descriptor);
            try {
                CoreExtension coreExtension = new DefaultCoreExtension(this, descriptorUrl, descriptorStream);

                this.extensions.put(coreExtension.getId(), coreExtension);
            } catch (Exception e) {
                getLogger().error("Failed to parse descriptor [" + descriptorUrl + "]", e);
            } finally {
                try {
                    descriptorStream.close();
                } catch (IOException e) {
                    // Should not happen
                    getLogger().error("Failed to close descriptor stream [" + descriptorUrl + "]", e);
                }
            }
        }
    }

    // Repository

    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        Extension extension = getCoreExtension(extensionId.getId());

        if (extension == null
            || (extensionId.getVersion() != null && !extension.getVersion().equals(extensionId.getVersion()))) {
            throw new ResolveException("Could not find extension [" + extensionId + "]");
        }

        return extension;
    }

    public boolean exists(ExtensionId extensionId)
    {
        Extension extension = getCoreExtension(extensionId.getId());

        if (extension == null
            || (extensionId.getVersion() != null && !extension.getVersion().equals(extensionId.getVersion()))) {
            return false;
        }

        return true;
    }

    public boolean exists(String id)
    {
        return this.extensions.containsKey(id);
    }

    public ExtensionRepositoryId getId()
    {
        return this.repositoryId;
    }

    // LocalRepository

    public int countExtensions()
    {
        return this.extensions.size();
    }

    public List<CoreExtension> getCoreExtensions()
    {
        return new ArrayList<CoreExtension>(this.extensions.values());
    }

    public CoreExtension getCoreExtension(String id)
    {
        return this.extensions.get(id);
    }

    public List< ? extends CoreExtension> getExtensions(int nb, int offset)
    {
        return getCoreExtensions().subList(offset, offset + nb);
    }
}

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
package org.xwiki.velocity;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.exception.VelocityException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.apache.velocity.util.ExtProperties;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;

/**
 * {@link Environment} based {@link ResourceLoader}. Mostly used to load macros.vm templates from the WAR.
 *
 * @version $Id$
 * @since 10.6RC1
 */
public class XWikiWebappResourceLoader extends ResourceLoader
{
    private static final String SLASH = "/";

    /**
     * The root paths for templates (relative to webapp's root).
     */
    private String[] paths;

    private Environment environment;

    private String rootPath;

    private Map<String, String> templatePathsCache = new ConcurrentHashMap<>();

    @Override
    public void init(ExtProperties configuration)
    {
        this.environment = getEnvironment();

        // Get configured paths
        this.paths = configuration.getStringArray("path");
        if (this.paths == null || this.paths.length == 0) {
            this.paths = new String[1];
            this.paths[0] = SLASH;
        } else {
            for (int i = 0; i < this.paths.length; i++) {
                this.paths[i] = StringUtils.appendIfMissing(this.paths[i], SLASH);

                this.log.info("Added template path [{}]", this.paths[i]);
            }
        }

        // Find root path
        if (this.environment instanceof ServletEnvironment) {
            this.rootPath = ((ServletEnvironment) this.environment).getServletContext().getRealPath(SLASH);
        } else {
            URL root = this.environment.getResource(SLASH);
            if (root != null && root.getProtocol().equals("file")) {
                try {
                    this.rootPath = new File(root.toURI()).toString();
                } catch (URISyntaxException e) {
                    this.log.warn("Failed to find real path for root resource");
                }
            }
        }
    }

    /**
     * @return the Environment component implementation retrieved from the Component Manager
     */
    private Environment getEnvironment()
    {
        try {
            return getComponentManager().getInstance(Environment.class);
        } catch (ComponentLookupException e) {
            throw new VelocityException(
                "Cannot initialize Velocity subsystem: missing Environment component implementation");
        }
    }

    /**
     * @return the Component Manager component implementation retrieved from Velocity Engine's Application Attributes
     */
    private ComponentManager getComponentManager()
    {
        ComponentManager cm = (ComponentManager) this.rsvc.getApplicationAttribute(ComponentManager.class.getName());
        if (cm == null) {
            throw new VelocityException(
                "Cannot initialize Velocity subsystem: missing Component Manager in Velocity Application Attribute");
        }
        return cm;
    }

    // Velocity Tools

    private String cleanName(String source)
    {
        String name = source;

        int index = -1;
        while (name.length() > (index + 1) && name.charAt(index + 1) == '/') {
            ++index;
        }
        if (index > -1) {
            name = name.substring(index + 1);
        }

        return name;
    }

    @Override
    public Reader getResourceReader(String source, String encoding) throws ResourceNotFoundException
    {
        if (StringUtils.isEmpty(source)) {
            throw new ResourceNotFoundException("WebappResourceLoader: No template name provided");
        }

        // Make sure the name does not start with /
        String name = cleanName(source);

        Exception exception = null;
        for (int i = 0; i < this.paths.length; i++) {
            String path = this.paths[i] + name;
            try {
                InputStream stream = this.environment.getResourceAsStream(path);

                if (stream != null) {
                    // Remember the path
                    this.templatePathsCache.put(name, this.paths[i]);

                    return new InputStreamReader(stream, encoding);
                }
            } catch (Exception e) {
                // Remember the first exception
                if (exception == null) {
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("Could not load [{}]", path, e);
                    }

                    exception = e;
                }
            }
        }

        // No template found
        String msg = "Resource [" + name + "] not found.";

        if (exception == null) {
            throw new ResourceNotFoundException(msg);
        } else {
            throw new ResourceNotFoundException(msg, exception);
        }
    }

    private File getCachedFile(String rootPath, String resourceName)
    {
        String name = cleanName(resourceName);

        String savedPath = this.templatePathsCache.get(name);

        return new File(rootPath + savedPath, name);
    }

    /**
     * Checks to see if a resource has been deleted, moved or modified.
     *
     * @param resource Resource The resource to check for modification
     * @return boolean True if the resource has been modified
     */
    @Override
    public boolean isSourceModified(Resource resource)
    {
        if (this.rootPath == null) {
            // rootPath is null if the servlet container cannot translate the
            // virtual path to a real path for any reason (such as when the
            // content is being made available from a .war archive)
            return false;
        }

        // Try getting the previously found file
        File cachedFile = getCachedFile(this.rootPath, resource.getName());
        if (!cachedFile.exists()) {
            // Resource we know but which does not exist anymore -> modified
            return true;
        }

        // Check to see if the file can now be found elsewhere before it is found in the previously saved path
        File currentFile = null;
        for (int i = 0; i < this.paths.length; i++) {
            currentFile = new File(this.rootPath + this.paths[i], resource.getName());
            if (currentFile.canRead()) {
                // Stop at the first resource found (just like in getResourceStream())
                break;
            }
        }

        if (cachedFile.equals(currentFile) && cachedFile.canRead()) {
            // Resource we know and which still exist -> compare the dates
            return cachedFile.lastModified() != resource.getLastModified();
        } else {
            // Resource we don't already know -> modified
            return true;
        }
    }

    @Override
    public long getLastModified(Resource resource)
    {
        if (this.rootPath == null) {
            // rootPath is null if the servlet container cannot translate the
            // virtual path to a real path for any reason (such as when the
            // content is being made available from a .war archive)
            return 0;
        }

        File cachedFile = getCachedFile(this.rootPath, resource.getName());
        if (cachedFile.canRead()) {
            return cachedFile.lastModified();
        } else {
            return 0;
        }
    }
}

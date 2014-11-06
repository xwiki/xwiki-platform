package com.xpn.xwiki.internal.skin;

import java.io.InputStream;
import java.net.URI;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.environment.Environment;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputSource;

public class FileSystemSkin extends AbstractSkin
{
    @Inject
    private Environment environment;

    @Inject
    private Logger logger;

    @Override
    public InputSource getSkinResourceInputSource(String resource)
    {
        return getResourceAsInputSource("/skins/" + this.id + '/', resource);
    }

    private InputSource getResourceAsInputSource(String suffixPath, String resource)
    {
        String resourcePath = getResourcePath(suffixPath, resource, false);

        InputStream inputStream = this.environment.getResourceAsStream(resourcePath);
        if (inputStream != null) {
            return new DefaultInputStreamInputSource(inputStream, true);
        }

        return null;
    }

    private String getResourcePath(String suffixPath, String resource, boolean testExist)
    {
        String resourcePath = suffixPath + resource;

        // Prevent inclusion of templates from other directories
        String normalizedResource = URI.create(resourcePath).normalize().toString();
        if (!normalizedResource.startsWith(suffixPath)) {
            this.logger.warn("Direct access to template file [{}] refused. Possible break-in attempt!",
                normalizedResource);

            return null;
        }

        if (testExist) {
            // Check if the resource exist
            if (this.environment.getResource(resourcePath) == null) {
                return null;
            }
        }

        return resourcePath;
    }
}

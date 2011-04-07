package org.xwiki.extension.test;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionException;

public class ResourceExtension extends AbstractExtension
{
    ResourceExtension(ResourceExtensionRepository repository, Extension extension)
    {
        super(repository, extension);
    }

    public void download(File file) throws ExtensionException
    {
        try {
            FileUtils.copyInputStreamToFile(getResourceExtensionRepository().getResourceAsStream(getId(), getType()),
                file);
        } catch (IOException e) {
            throw new ExtensionException("Failed to copy resource containing extension [" + getId() + "] to file ["
                + file + "]", e);
        }
    }

    private ResourceExtensionRepository getResourceExtensionRepository()
    {
        return (ResourceExtensionRepository) getRepository();
    }
}

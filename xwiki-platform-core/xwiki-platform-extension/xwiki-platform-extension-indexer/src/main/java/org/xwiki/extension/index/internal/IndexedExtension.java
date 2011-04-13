package org.xwiki.extension.index.internal;

import java.io.File;

import org.xwiki.extension.AbstractExtension;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.ExtensionRepository;

public class IndexedExtension extends AbstractExtension
{
    public IndexedExtension()
    {
        super(null, null, null);
    }

    @Override
    public void setId(ExtensionId id)
    {
        super.setId(id);
    }

    @Override
    public void setRepository(ExtensionRepository repository)
    {
        super.setRepository(repository);
    }

    @Override
    public void setType(String type)
    {
        super.setType(type);
    }

    @Override
    public void putProperty(String key, Object value)
    {
        super.putProperty(key, value);
    }
    
    public void download(File file) throws ExtensionException
    {
        // Do nothing
    }
}

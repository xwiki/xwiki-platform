package org.xwiki.extension.repository;

public abstract class AbstractExtensionRepository implements ExtensionRepository
{
    private ExtensionRepositoryId id;

    public AbstractExtensionRepository(ExtensionRepositoryId id)
    {
        this.id = new ExtensionRepositoryId(id.getId(), id.getType(), id.getURI());
    }

    public ExtensionRepositoryId getId()
    {
        return id;
    }
}

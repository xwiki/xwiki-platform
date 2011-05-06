package org.xwiki.extension.repository;

public abstract class AbstractExtensionRepository implements ExtensionRepository
{
    private ExtensionRepositoryId id;

    public AbstractExtensionRepository(ExtensionRepositoryId id)
    {
        this.id = id;
    }

    public ExtensionRepositoryId getId()
    {
        return id;
    }
}

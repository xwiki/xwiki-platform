package org.xwiki.extension.repository;

/**
 * Base class for {@link ExtensionRepository} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionRepository implements ExtensionRepository
{
    /**
     * The repository identifier.
     */
    private ExtensionRepositoryId id;

    /**
     * @param id the repository identifier
     */
    public AbstractExtensionRepository(ExtensionRepositoryId id)
    {
        this.id = new ExtensionRepositoryId(id.getId(), id.getType(), id.getURI());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.repository.ExtensionRepository#getId()
     */
    public ExtensionRepositoryId getId()
    {
        return id;
    }
}

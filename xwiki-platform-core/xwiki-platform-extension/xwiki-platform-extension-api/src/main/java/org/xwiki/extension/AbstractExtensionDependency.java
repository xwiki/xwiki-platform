package org.xwiki.extension;

/**
 * Base class for {@link ExtensionDependency} implementations.
 * 
 * @version $Id$
 */
public abstract class AbstractExtensionDependency implements ExtensionDependency
{
    /**
     * @see #getId()
     */
    private String id;

    /**
     * @see #getVersion()
     */
    private String version;

    /**
     * @param id the id of the extension
     * @param version the version of the extension
     */
    public AbstractExtensionDependency(String id, String version)
    {
        this.id = id;
        this.version = version;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.ExtensionDependency#getId()
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the extension id
     * @see #getId()
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.ExtensionDependency#getVersion()
     */
    public String getVersion()
    {
        return this.version;
    }

    /**
     * @param version the extension version
     * @see #getVersion()
     */
    public void setVersion(String version)
    {
        this.version = version;
    }
}

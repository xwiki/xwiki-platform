package org.xwiki.extension;

public abstract class AbstractExtensionDependency implements ExtensionDependency
{
    private String id;

    private String version;

    public AbstractExtensionDependency()
    {
    }

    public AbstractExtensionDependency(String id, String version)
    {
        this.id = id;
        this.version = version;
    }

    public String getId()
    {
        return this.id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getVersion()
    {
        return this.version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }
}

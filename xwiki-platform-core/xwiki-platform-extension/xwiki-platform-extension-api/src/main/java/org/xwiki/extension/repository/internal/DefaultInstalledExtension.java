package org.xwiki.extension.repository.internal;

import java.util.HashSet;
import java.util.Set;

public class DefaultInstalledExtension
{
    private DefaultLocalExtension extension;

    private String namespace;

    private Set<DefaultLocalExtension> backwardDependencies = new HashSet<DefaultLocalExtension>();

    public DefaultInstalledExtension(DefaultLocalExtension extension, String namespace)
    {
        this.extension = extension;
        this.namespace = namespace;
    }

    public DefaultLocalExtension getExtension()
    {
        return this.extension;
    }

    public String getNamespace()
    {
        return this.namespace;
    }

    public Set<DefaultLocalExtension> getBackwardDependencies()
    {
        return this.backwardDependencies;
    }

    public void addBackwardDependency(DefaultLocalExtension localExtension)
    {
        this.backwardDependencies.add(localExtension);
    }
}

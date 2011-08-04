package org.xwiki.extension.repository.internal;

import java.util.HashSet;
import java.util.Set;

/**
 * Contains informations about extension installed on a specific namespace.
 * 
 * @version $Id$
 */
public class DefaultInstalledExtension
{
    /**
     * @see #getExtension()
     */
    private DefaultLocalExtension extension;

    /**
     * @see #getFeature()
     */
    private String feature;

    /**
     * @see #getNamespace()
     */
    private String namespace;

    /**
     * @see #getBackwardDependencies()
     */
    private Set<DefaultLocalExtension> backwardDependencies = new HashSet<DefaultLocalExtension>();

    /**
     * @param extension the extension
     * @param feature the feature
     * @param namespace the nsmaspace
     */
    public DefaultInstalledExtension(DefaultLocalExtension extension, String feature, String namespace)
    {
        this.extension = extension;
        this.feature = feature;
        this.namespace = namespace;
    }

    /**
     * @return the corresponding local extension object.
     */
    public DefaultLocalExtension getExtension()
    {
        return this.extension;
    }

    /**
     * @return the corresponding feature.
     */
    public String getFeature()
    {
        return feature;
    }

    /**
     * @return the corresponding namespace.
     */
    public String getNamespace()
    {
        return this.namespace;
    }

    /**
     * @return the extension which depends on it
     */
    public Set<DefaultLocalExtension> getBackwardDependencies()
    {
        return this.backwardDependencies;
    }

    /**
     * @param localExtension a backward dependency
     */
    public void addBackwardDependency(DefaultLocalExtension localExtension)
    {
        this.backwardDependencies.add(localExtension);
    }
}

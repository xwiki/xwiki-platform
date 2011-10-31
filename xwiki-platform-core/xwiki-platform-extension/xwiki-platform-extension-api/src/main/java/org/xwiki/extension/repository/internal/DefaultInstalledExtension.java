/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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

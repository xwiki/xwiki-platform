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
package org.xwiki.extension.job.plan.internal;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.extension.Extension;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.version.VersionConstraint;

/**
 * An action to perform as part of an extension plan.
 * 
 * @version $Id$
 */
public class DefaultExtensionPlanAction implements ExtensionPlanAction
{
    /**
     * @see #getExtension()
     */
    private Extension extension;

    /**
     * @see #getPreviousExtension()
     */
    private LocalExtension previousExtension;

    /**
     * @see Action
     */
    private Action action;

    /**
     * @see #getNamespace()
     */
    private String namespace;

    /**
     * @see #getVersionConstraint()
     */
    private VersionConstraint versionConstraint;

    /**
     * @param extension the extension on which to perform the action
     * @param previousExtension the currently installed extension. Used when upgrading
     * @param action the action to perform
     * @param namespace the namespace in which the action should be executed
     * @param versionConstraint the version constraint that has been used to resolve the extension
     */
    public DefaultExtensionPlanAction(Extension extension, LocalExtension previousExtension, Action action,
        String namespace, VersionConstraint versionConstraint)
    {
        this.extension = extension;
        this.previousExtension = previousExtension;
        this.action = action;
        this.namespace = namespace;
        this.versionConstraint = versionConstraint;
    }

    @Override
    public Extension getExtension()
    {
        return this.extension;
    }

    @Override
    public LocalExtension getPreviousExtension()
    {
        return this.previousExtension;
    }

    @Override
    public Action getAction()
    {
        return this.action;
    }

    @Override
    public String getNamespace()
    {
        return this.namespace;
    }

    @Override
    public VersionConstraint getVersionConstraint()
    {
        return this.versionConstraint;
    }

    // Object

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(this.extension);
        builder.append(this.namespace);

        return builder.toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }

        boolean equals;

        if (obj instanceof ExtensionPlanAction) {
            ExtensionPlanAction epa = (ExtensionPlanAction) obj;
            equals =
                this.extension.equals(epa.getExtension()) && ObjectUtils.equals(this.namespace, epa.getNamespace());
        } else {
            equals = false;
        }

        return equals;
    }
}

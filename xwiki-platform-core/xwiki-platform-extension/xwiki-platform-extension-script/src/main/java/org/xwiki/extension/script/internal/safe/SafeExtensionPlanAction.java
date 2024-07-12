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
package org.xwiki.extension.script.internal.safe;

import java.util.Collection;

import org.xwiki.extension.Extension;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.script.safe.ScriptSafeProvider;
import org.xwiki.script.wrap.AbstractWrappingObject;

/**
 * Provide a public script access to an extension plan action.
 * 
 * @version $Id$
 * @since 4.0M2
 */
public class SafeExtensionPlanAction extends AbstractWrappingObject<ExtensionPlanAction> implements ExtensionPlanAction
{
    /**
     * The provider of instances safe for public scripts.
     */
    private ScriptSafeProvider<Object> safeProvider;

    /**
     * @param action the wrapped action
     * @param safeProvider the provider of instances safe for public scripts
     */
    public SafeExtensionPlanAction(ExtensionPlanAction action, ScriptSafeProvider<Object> safeProvider)
    {
        super(action);

        this.safeProvider = safeProvider;
    }

    @Override
    public Extension getExtension()
    {
        return this.safeProvider.get(getWrapped().getExtension());
    }

    @Override
    @Deprecated
    public InstalledExtension getPreviousExtension()
    {
        return this.safeProvider.get(getWrapped().getPreviousExtension());
    }

    @Override
    public Collection<InstalledExtension> getPreviousExtensions()
    {
        return this.safeProvider.get(getWrapped().getPreviousExtensions());
    }

    @Override
    public Action getAction()
    {
        return getWrapped().getAction();
    }

    @Override
    public String getNamespace()
    {
        return getWrapped().getNamespace();
    }

    @Override
    public boolean isDependency()
    {
        return getWrapped().isDependency();
    }
}

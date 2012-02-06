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
package org.xwiki.extension.job.plan;

import org.xwiki.extension.Extension;
import org.xwiki.extension.LocalExtension;

/**
 * An action to perform as part of an extension plan.
 * 
 * @version $Id$
 */
public interface ExtensionPlanAction
{
    /**
     * The action to execute.
     * 
     * @version $Id$
     */
    public enum Action
    {
        /**
         * Nothing to do. Just here for information as why nothing is done here.
         */
        NONE,

        /**
         * Install the extension.
         */
        INSTALL,

        /**
         * Upgrade the extension.
         */
        UPGRADE,

        /**
         * Uninstall the extension.
         */
        UNINSTALL
    }

    /**
     * @return the extension on which to perform the action
     */
    Extension getExtension();

    /**
     * @return the currently installed extension. Used when upgrading
     */
    LocalExtension getPreviousExtension();

    /**
     * @return the action to perform
     */
    Action getAction();

    /**
     * @return the namespace in which the action should be executed
     */
    String getNamespace();

    /**
     * @return indicate indicate if the extension is a dependency of another one only in the plan
     */
    boolean isDependency();

}

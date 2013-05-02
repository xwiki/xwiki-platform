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
package org.xwiki.security.authorization;

import org.xwiki.component.annotation.Role;
import org.xwiki.component.util.DefaultParameterizedType;

import javax.inject.Provider;
import java.lang.reflect.Type;

/**
 * Interface for disabling the privileged mode in the authorization context.
 *
 * @version $Id$
 * @since 4.3M1
 */
@Role
public interface PrivilegedModeController
{

    /** Type object for Provider<PrivilegedModeController>. */
    Type PROVIDER_TYPE = new DefaultParameterizedType(null, Provider.class, PrivilegedModeController.class);

    /**
     * Disable the privileged mode. (I.e., programming rights may not be granted.)  This method does nothing if the
     * privileged mode is already disabled.
     */
    void disablePrivilegedMode();


    /**
     * Restore a privileged mode that have been disabled by this particular privileged mode controller.  This method
     * does nothing if the privileged mode is already enabled or have been disabled by another privileged mode
     * controller.
     */
    void restorePrivilegedMode();

    /**
     * Disable the privileged mode in the current execution context.  This will not affect the privileged mode in any
     * other execution context that might be activated in the request cycle.  Also note that 'restorePrivilegedMode'
     * will not restore the privileged mode in the current execution context after this call.
     */
    void disablePrivilegedModeInCurrentExecutionContext();

}
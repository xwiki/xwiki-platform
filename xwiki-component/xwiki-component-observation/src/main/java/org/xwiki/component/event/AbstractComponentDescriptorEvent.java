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
package org.xwiki.component.event;

/**
 * Base class for events about components descriptors.
 * 
 * @version $Id$
 * @since 2.6RC2
 */
public abstract class AbstractComponentDescriptorEvent implements ComponentDescriptorEvent
{
    /**
     * Component role.
     */
    private Class< ? > role;

    /**
     * Component role hint.
     */
    private String roleHint;

    /**
     * Watches all roles (whenever a component is added it'll trigger this event).
     */
    public AbstractComponentDescriptorEvent()
    {
        this.role = null;
    }

    /**
     * @param role the component role to watch (all components matching this role will trigger this event)
     */
    public AbstractComponentDescriptorEvent(Class< ? > role)
    {
        this.role = role;
    }

    /**
     * @param role the component role to watch
     * @param roleHint the component rolehint to watch
     */
    public AbstractComponentDescriptorEvent(Class< ? > role, String roleHint)
    {
        this.role = role;
        this.roleHint = roleHint;
    }

    /**
     * @return the component's role being watched or null if all components registrations are watched
     */
    public Class< ? > getRole()
    {
        return this.role;
    }

    /**
     * @return the component's role hint being watched or null if all role's components registrations are watched
     */
    public String getRoleHint()
    {
        return this.roleHint;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.event.Event#matches(java.lang.Object)
     */
    public boolean matches(Object otherEvent)
    {
        boolean result = false;

        if (otherEvent instanceof AbstractComponentDescriptorEvent) {
            // If we're watching all roles return a match
            if (getRole() == null) {
                result = true;
            } else {
                ComponentDescriptorEvent event = (ComponentDescriptorEvent) otherEvent;
                // It's possible Class reference are not the same when it coming for different ClassLoader so we
                // compare class names
                if (getRole().getName().equals(event.getRole().getName())) {
                    result = getRoleHint() == null || getRoleHint().equals(event.getRoleHint());
                }
            }
        }

        return result;
    }
}

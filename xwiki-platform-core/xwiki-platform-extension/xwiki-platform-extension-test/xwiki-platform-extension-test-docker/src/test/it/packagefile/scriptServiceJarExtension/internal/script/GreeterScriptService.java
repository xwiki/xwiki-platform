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
package packagefile.scriptServiceJarExtension.internal.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;

import packagefile.scriptServiceJarExtension.Greeter;

/**
 * Script service for {@link Greeter}.
 */
//It is a static registration but it's a generated jar
@Component(staticRegistration = false)
@Named("greeter")
@Singleton
public class GreeterScriptService implements ScriptService
{
    /**
     * The greeter.
     */
    @Inject
    private Greeter greeter;

    /**
     * The component manager, used to perform dynamic component lookups.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Greets the specified person.
     * 
     * @param name the name of the person to greet
     * @return the greet message
     */
    public String greet(String name)
    {
        // Test component injection.
        return greeter.greet(name);
    }

    /**
     * Greets the specified person.
     * 
     * @param name the name of the person to greet
     * @return the greet message
     */
    public String greet(String name, String hint)
    {
        try {
            // Test dynamic component lookup.
            Greeter greeter = componentManager.getInstance(Greeter.class, hint);
            return greeter.greet(name);
        } catch (ComponentLookupException e) {
            return null;
        }
    }
}

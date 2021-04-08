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
package org.xwiki.officeimporter.internal.openoffice;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.officeimporter.openoffice.OpenOfficeManager;
import org.xwiki.officeimporter.openoffice.OpenOfficeManagerVelocityBridge;
import org.xwiki.script.service.ScriptService;
import org.xwiki.velocity.VelocityContextInitializer;

/**
 * Puts a reference to {@link OpenOfficeManager} in newly created velocity contexts.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
@Component
@Named("oomanager")
@Singleton
public class OpenOfficeManagerVelocityContextInitializer implements VelocityContextInitializer
{
    /**
     * The key to use for openoffice server manager in the velocity context.
     */
    public static final String VELOCITY_CONTEXT_KEY = "oomanager";

    @Inject
    private Execution execution;

    @Inject
    @Named("officemanager")
    private ScriptService officeManagerScriptService;

    @Override
    public void initialize(VelocityContext context)
    {
        // Put the office manager script service on the execution context to be available to the velocity bridge.
        this.execution.getContext().setProperty("OpenOfficeManagerScriptService", this.officeManagerScriptService);
        // The first two parameters are not needed anymore since the velocity bridge is now just a wrapper of the office
        // manager script service which is taken from the execution context.
        context.put(VELOCITY_CONTEXT_KEY, new OpenOfficeManagerVelocityBridge(null, null, this.execution));
    }
}

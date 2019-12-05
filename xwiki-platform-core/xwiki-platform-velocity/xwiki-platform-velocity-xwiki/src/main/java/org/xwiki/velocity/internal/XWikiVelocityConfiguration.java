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
package org.xwiki.velocity.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.InitializationException;

/**
 * Override the default {@link org.xwiki.velocity.VelocityConfiguration} implementation in order to replace some of the
 * Velocity Tools by customized versions to properly handle locales (the default Velocity Tools can only have a single
 * locale configured and in XWiki we need to set the locale from the executing XWiki Context).
 *
 * @version $Id$
 * @since 8.2RC1
 */
@Component
@Singleton
public class XWikiVelocityConfiguration extends DefaultVelocityConfiguration
{
    @Inject
    private XWikiNumberTool numberTool;

    @Inject
    private XWikiDateTool dateTool;

    @Inject
    private XWikiMathTool mathTool;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        // Override some tools
        this.defaultTools.put("numbertool", this.numberTool);
        this.defaultTools.put("datetool", this.dateTool);
        this.defaultTools.put("mathttool", this.mathTool);

        if (this.componentManager.hasComponent(ResourceLoaderInitializer.class)) {
            try {
                // Try to find a ResourceLoaderInitializer implementation
                ResourceLoaderInitializer resourceLoader =
                    this.componentManager.getInstance(ResourceLoaderInitializer.class);

                // Initialize the ResourceLoader
                resourceLoader.initialize(this.defaultProperties);
            } catch (ComponentLookupException e) {
                throw new InitializationException("Failed to lookup the ResourceLoader implementation", e);
            }
        } else {
            this.logger.debug("Could not find any ResourceLoader implementation");
        }
    }
}

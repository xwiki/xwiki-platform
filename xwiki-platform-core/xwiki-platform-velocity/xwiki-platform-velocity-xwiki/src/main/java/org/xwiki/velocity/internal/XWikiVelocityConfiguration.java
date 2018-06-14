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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.velocity.runtime.RuntimeConstants;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.velocity.XWikiWebappResourceLoader;

import com.xpn.xwiki.XWikiContext;

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
    private static final String RESOURCE_LOADER_ID = "xwiki";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        // Override some tools
        this.defaultTools.put("numbertool", new XWikiNumberTool(this.contextProvider));
        this.defaultTools.put("datetool", new XWikiDateTool(this.contextProvider));
        this.defaultTools.put("mathttool", new XWikiMathTool(this.contextProvider));

        this.defaultProperties.setProperty(RuntimeConstants.RESOURCE_LOADER, RESOURCE_LOADER_ID);
        this.defaultProperties.setProperty(RESOURCE_LOADER_ID + '.' + RuntimeConstants.RESOURCE_LOADER + ".class",
            XWikiWebappResourceLoader.class.getName());
        this.defaultProperties.put(RuntimeConstants.VM_LIBRARY, "/templates/macros.vm");
    }
}

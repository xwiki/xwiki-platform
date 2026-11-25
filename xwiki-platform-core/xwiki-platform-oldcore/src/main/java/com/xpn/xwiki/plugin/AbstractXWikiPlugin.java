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
package com.xpn.xwiki.plugin;

import jakarta.inject.Inject;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.stability.Unstable;

/**
 * Base class for the plugins registered as {@link XWikiPluginInterface} components, which the plugin manager loads
 * without them being listed in {@code xwiki.cfg} (for example when they are provided by an installed extension).
 * <p>
 * The plugin name is the component hint, so a plugin implementation only needs to be declared as a component:
 *
 * <pre>
 * &#64;Component
 * &#64;Named("myplugin")
 * &#64;Singleton
 * public class MyPlugin extends AbstractXWikiPlugin
 * {
 *     ...
 * }
 * </pre>
 *
 * The component also needs to be listed in {@code META-INF/components.txt}, like any other component.
 *
 * @version $Id$
 * @since 18.10.0
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Unstable
@Deprecated(since = "18.10.0")
// Deprecated like the rest of the plugin technology it helps using, and to be removed along with it
@SuppressWarnings("java:S1133")
public abstract class AbstractXWikiPlugin extends XWikiDefaultPlugin
{
    @Inject
    private ComponentDescriptor<XWikiPluginInterface> descriptor;

    /**
     * Default constructor.
     */
    protected AbstractXWikiPlugin()
    {
        // The name is the component hint, see getName()
        super(null, null, null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The name of the plugin is the hint of the component ({@code null} when the plugin was not created by a component
     * manager).
     */
    @Override
    public final String getName()
    {
        return this.descriptor != null ? this.descriptor.getRoleHint() : null;
    }
}

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
package org.xwiki.blocknote.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.webjars.WebJarsUrlFactory;

/**
 * Publishes the BlockNote editor's stylesheet as a RequireJS module (to be loaded with the CSS RequireJS plugin).
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@Component
@Named(BlockNoteCSSRequireJSModuleUIExtension.ID)
@Singleton
public class BlockNoteCSSRequireJSModuleUIExtension implements UIExtension, Initializable
{
    /**
     * The id of the UI extension.
     */
    public static final String ID = "org.xwiki.platform.requirejs.module.blocknote.css";

    @Inject
    private WebJarsUrlFactory webJarsUrlFactory;

    private Map<String, String> parameters;

    @Override
    public void initialize() throws InitializationException
    {
        this.parameters = new HashMap<>();
        this.parameters.put("id", "xwiki-blocknote-css");
        this.parameters.put("path", this.webJarsUrlFactory.url("org.xwiki.platform:xwiki-platform-blocknote-webjar",
            "xwiki-platform-blocknote.css"));
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.requirejs.module";
    }

    @Override
    public Map<String, String> getParameters()
    {
        return this.parameters;
    }

    @Override
    public Block execute()
    {
        return new XDOM(List.of());
    }
}

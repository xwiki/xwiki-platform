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
package org.xwiki.internal.authentication;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.template.TemplateManager;
import org.xwiki.uiextension.UIExtension;

/**
 * Injects the register link in the top bar.
 *
 * @version $Id$
 */
@Component(RegisterLinkUIExtension.ROLE_HINT)
@Singleton
public class RegisterLinkUIExtension implements UIExtension
{
    /**
     * The role hint.
     */
    public static final String ROLE_HINT = "org.xwiki.platform.oldcore.registerlink";

    @Inject
    private TemplateManager templateManager;

    @Override
    public Block execute()
    {
        return this.templateManager.executeNoException("registerLink.vm");
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.topmenu.right";
    }

    @Override
    public String getId()
    {
        return ROLE_HINT;
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Map.of("order", "40500");
    }
}

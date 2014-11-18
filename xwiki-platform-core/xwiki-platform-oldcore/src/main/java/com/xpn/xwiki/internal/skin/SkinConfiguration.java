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
package com.xpn.xwiki.internal.skin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWiki;

/**
 * @version $Id$
 * @since 6.4M1
 */
@Component(roles = SkinConfiguration.class)
@Singleton
public class SkinConfiguration
{
    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    public String getDefaultBaseSkinId()
    {
        String baseskin = this.xwikicfg.getProperty("xwiki.defaultbaseskin");

        return StringUtils.isNotEmpty(baseskin) ? baseskin : null;
    }
    
    public String getDefaultSkin()
    {
        String skin = this.xwikicfg.getProperty("xwiki.defaultskin", XWiki.DEFAULT_SKIN);

        return StringUtils.isNotEmpty(skin) ? skin : null;
    }
}

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

/**
 * @version $Id$
 * @since 6.4M1
 */
@Component(roles = InternalSkinConfiguration.class)
@Singleton
public class InternalSkinConfiguration
{
    public static final String DEFAULT_SKIN = "flamingo";

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikicfg;

    public String getDefaultParentSkinId()
    {
        return getDefaultParentSkinId(null);
    }

    public String getDefaultParentSkinId(String def)
    {
        String baseskin = this.xwikicfg.getProperty("xwiki.defaultbaseskin", def);

        return StringUtils.isNotEmpty(baseskin) ? baseskin : null;
    }

    public String getDefaultSkinId()
    {
        return getDefaultSkinId(DEFAULT_SKIN);
    }

    public String getDefaultSkinId(String def)
    {
        String skin = this.xwikicfg.getProperty("xwiki.defaultskin", def);

        return StringUtils.isNotEmpty(skin) ? skin : null;
    }
}

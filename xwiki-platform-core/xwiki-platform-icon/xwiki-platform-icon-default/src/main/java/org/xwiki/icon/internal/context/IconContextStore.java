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
package org.xwiki.icon.internal.context;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.internal.concurrent.AbstractContextStore;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetManager;

/**
 * Save and restore well icon related information.
 * 
 * @version $Id$
 * @since 10.11.1
 * @since 11.0RC1
 */
@Component
@Singleton
@Named("icon")
public class IconContextStore extends AbstractContextStore
{
    /**
     * Name of the entry containing the icon set.
     */
    public static final String PROP_ICON_THEME = "icon.theme";

    @Inject
    private IconSetManager iconSetManager;

    @Inject
    private IconSetContext iconSetContext;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public IconContextStore()
    {
        super(PROP_ICON_THEME);
    }

    @Override
    public void save(Map<String, Serializable> contextStore, Collection<String> entries)
    {
        if (entries.contains(PROP_ICON_THEME)) {
            try {
                IconSet currentIconSet = this.iconSetManager.getCurrentIconSet();
                if (currentIconSet != null) {
                    contextStore.put(PROP_ICON_THEME, currentIconSet.getName());
                }
            } catch (IconException e) {
                this.logger.error("Unexcepted error when getting current icon set", e);
            }
        }
    }

    @Override
    public void restore(Map<String, Serializable> contextStore)
    {
        String iconSetName = (String) contextStore.get(PROP_ICON_THEME);

        if (iconSetName != null) {
            try {
                this.iconSetContext.setIconSet(this.iconSetManager.getIconSet(iconSetName));
            } catch (IconException e) {
                this.logger.error("Unexcepted error when getting icon set with name [{}]", e);
            }
        }
    }
}

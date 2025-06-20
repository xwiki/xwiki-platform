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
package org.xwiki.extension.distribution.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Configuration for the Distribution Wizard.
 *  
 * @version $Id$
 * @since 7.1RC1
 */
@Component(roles = DistributionConfiguration.class)
@Singleton
public class DistributionConfiguration
{
    @Inject
    private ConfigurationSource configurationSource;

    /**
     * @return true if the automatic launch of DW is enabled on main wiki
     */
    public boolean isAutoDistributionWizardEnabledForMainWiki()
    {
        return configurationSource.getProperty("distribution.automaticStartOnMainWiki", true);
    }

    /**
     * @return true if the automatic launch of DW is enabled on wiki
     */
    public boolean isAutoDistributionWizardEnabledForWiki()
    {
        return configurationSource.getProperty("distribution.automaticStartOnWiki", true);
    }

    /**
     * @return true if the distribution wizard is interactive on main wiki
     * @since 17.4.0RC1
     */
    public boolean isInteractiveDistributionWizardEnabledForMainWiki()
    {
        return configurationSource.getProperty("distribution.job.interactive", true);
    }

    /**
     * @return true if the distribution wizard is interactive on subwikis
     * @since 17.4.0RC1
     */
    public boolean isInteractiveDistributionWizardEnabledForWiki()
    {
        return configurationSource.getProperty("distribution.job.interactive.wiki", true);
    }
}

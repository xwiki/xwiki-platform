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
package org.xwiki.distributionwizard;

import org.xwiki.stability.Unstable;

/**
 * Defines UI information for a distribution wizard step.
 * That definition allows to either use a VueJS component loaded from the given module, or to rely on some velocity
 * script rendered to html. In that latter case the module name might still be needed to load javascript callback or
 * intialization functions.
 *
 * @param uiComponentName the VueJS component name to use if it exists
 * @param uiModuleName the webjar module name to use if it exists
 * @param html the actual HTML to be injected if there's no VueJS component
 * @param requiredSkinExtension the skin extensions to be injected
 * @since 18.4.0RC1
 */
@Unstable
public record DistributionWizardUIDefinition(String uiComponentName,
                                             String uiModuleName,
                                             String html,
                                             String requiredSkinExtension)
{
}

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
package org.xwiki.distributionwizard.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.distributionwizard.DistributionWizardException;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.distribution.internal.DistributionManager;
import org.xwiki.extension.distribution.internal.job.DistributionJob;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
class FlavorHelperTest
{
    @InjectMockComponents
    private FlavorHelper flavorHelper;

    @MockComponent
    private DistributionManager distributionManager;

    @MockComponent
    private ExtensionManager extensionManager;

    @Test
    void handleFlavorAnswer() throws ResolveException, DistributionWizardException
    {
        String selectedFlavor = "org.xwiki.platform:xwiki-platform-distribution-flavor-mainwiki:::18.4.0-SNAPSHOT";
        DistributionJob distributionJob = mock(DistributionJob.class);
        when(distributionManager.getCurrentDistributionJob()).thenReturn(distributionJob);

        ExtensionId extensionId = new ExtensionId("org.xwiki.platform:xwiki-platform-distribution-flavor-mainwiki",
            "18.4.0-SNAPSHOT");
        Extension extension = mock(Extension.class);
        when(extensionManager.resolveExtension(extensionId)).thenReturn(extension);

        this.flavorHelper.handleFlavorAnswer(selectedFlavor);
        verify(distributionJob).setProperty("flavor.selected", extension);
    }
}
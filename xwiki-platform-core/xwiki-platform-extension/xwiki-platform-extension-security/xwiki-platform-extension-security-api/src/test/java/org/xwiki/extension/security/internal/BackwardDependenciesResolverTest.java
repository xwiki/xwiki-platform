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
package org.xwiki.extension.security.internal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.test.LogLevel.WARN;

/**
 * Test of {@link BackwardDependenciesResolver}.
 *
 * @version $Id$
 */
@ComponentTest
class BackwardDependenciesResolverTest
{
    @InjectMockComponents
    private BackwardDependenciesResolver backwardDependenciesResolver;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(WARN);

    @Test
    void getExplicitlyInstalledBackwardDependencies() throws Exception
    {
        InstalledExtension e1 = mock(InstalledExtension.class);
        InstalledExtension e2 = mock(InstalledExtension.class);
        InstalledExtension e3 = mock(InstalledExtension.class);
        when(e2.isDependency("ns1")).thenReturn(true);
        when(e1.getId()).thenReturn(new ExtensionId("e.1", "7.3"));
        when(e2.getId()).thenReturn(new ExtensionId("e.2", "1.0"));
        when(e3.getId()).thenReturn(new ExtensionId("e.3", "4.5"));

        ExtensionId extensionId = new ExtensionId("e.4", "2.0");
        when(this.installedExtensionRepository.getBackwardDependencies(extensionId, true)).thenReturn(Map.of(
            "ns1", List.of(e2),
            "ns2", List.of(e3)
        ));
        when(this.installedExtensionRepository.getBackwardDependencies("e.2", "ns1", true))
            .thenReturn(List.of(e1));
        assertEquals(Map.of(
            e1, Set.of("ns1"),
            e3, Set.of("ns2")
        ), this.backwardDependenciesResolver.getExplicitlyInstalledBackwardDependencies(extensionId));
    }
}

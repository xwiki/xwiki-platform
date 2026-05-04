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
package org.xwiki.tool.provision;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionId;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Unit tests for {@link InstallMojo}.
 *
 * @version $Id$
 */
@WireMockTest(httpPort = 8888)
class InstallMojoTest
{
    @Test
    void install()
    {
        stubFor(put(urlEqualTo("/xwiki/rest/jobs?jobType=install&async=false"))
            .willReturn(aResponse()
                .withBodyFile("jobStatus.xml")));

        InstallMojo mojo = new InstallMojo();
        ReflectionUtils.setFieldValue(mojo, "xwikiRESTURL", "http://localhost:8888/xwiki/rest");
        ReflectionUtils.setFieldValue(mojo, "username", "superadmin");
        ReflectionUtils.setFieldValue(mojo, "password", "pass");
        ReflectionUtils.setFieldValue(mojo, "extensionIds",
            Collections.singletonList(new ExtensionId().withId("id").withVersion("version")));

        assertDoesNotThrow(mojo::execute);
    }
}

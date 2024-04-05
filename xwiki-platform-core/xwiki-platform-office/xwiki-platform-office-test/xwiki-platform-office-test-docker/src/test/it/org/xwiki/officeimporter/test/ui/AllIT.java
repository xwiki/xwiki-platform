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
package org.xwiki.officeimporter.test.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;

/**
 * All UI tests for the office extension.
 *
 * @version $Id$
 */
@UITest(
    // The servlet needs to be explicitly specified in AllIT too because otherwise ServletEngine#JETTY_STANDALONE is 
    // used, leading to a configurations merge conflict.
    servletEngine = ServletEngine.TOMCAT
)
public class AllIT
{
    @Nested
    @DisplayName("Office Importer tests")
    class NestedOfficeImporterIT extends OfficeImporterIT
    {
    }

    @Nested
    @DisplayName("Office Exporter tests")
    class NestedOfficeExporterIT extends OfficeExporterIT
    {
    }

}

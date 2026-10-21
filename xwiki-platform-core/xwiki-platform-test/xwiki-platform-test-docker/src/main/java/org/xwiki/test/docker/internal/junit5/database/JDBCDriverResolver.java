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
package org.xwiki.test.docker.internal.junit5.database;

import java.io.File;
import java.util.Properties;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.database.Database;
import org.xwiki.test.integration.maven.MavenResolver;

/**
 * Resolve the JDBC driver JAR to use for the database of the test.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
public final class JDBCDriverResolver
{
    private JDBCDriverResolver()
    {
        // Utility class, no instantiation
    }

    /**
     * @param testConfiguration the configuration of the test (database, JDBC driver version, etc)
     * @param mavenResolver the resolver to read Maven POMs and to resolve Maven artifacts
     * @return the JDBC driver JAR matching the database of the test
     * @throws Exception if the JDBC driver artifact cannot be resolved
     */
    public static File resolve(TestConfiguration testConfiguration, MavenResolver mavenResolver) throws Exception
    {
        Database database = testConfiguration.getDatabase();

        // Note: If the JDBC driver version is specified as "pom" or null then extract the information from the current
        // POM.
        Properties pomProperties = mavenResolver.getPropertiesFromCurrentPOM();
        String driverVersion = isJDBCDriverSpecified(testConfiguration.getJDBCDriverVersion())
            ? testConfiguration.getJDBCDriverVersion()
            : getPropertyForDatabase("version", database, pomProperties);
        String groupId = getPropertyForDatabase("groupId", database, pomProperties);
        String artifactId = getPropertyForDatabase("artifactId", database, pomProperties);

        Artifact artifact = new DefaultArtifact(groupId, artifactId, "jar", driverVersion);
        return mavenResolver.getArtifactResolver().resolveArtifact(artifact).getArtifact().getFile();
    }

    private static String getPropertyForDatabase(String propertyName, Database database, Properties properties)
    {
        String value = properties.getProperty(String.format("%s.%s", database.getPomPropertyPrefix(), propertyName));
        if (value == null) {
            throw new RuntimeException(
                String.format("Failed to get JDBC property [%s] for database [%s]. Database may not be supported yet!",
                    propertyName, database));
        }
        return value;
    }

    private static boolean isJDBCDriverSpecified(String jdbcDriverVersion)
    {
        return jdbcDriverVersion != null && !"pom".equalsIgnoreCase(jdbcDriverVersion);
    }
}

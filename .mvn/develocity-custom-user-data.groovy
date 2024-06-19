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
import com.gradle.develocity.agent.maven.adapters.BuildScanApiAdapter

/**
 * Captures the Maven active profiles and add them as tags to the Build Scan. The goal is to make it simpler to
 * filter builds on <a href="ge.xwiki.org">https://ge.xwiki.org</a> by filtering on Maven profiles.
 */

buildScan.executeOnce('tag-profiles') { BuildScanApiAdapter buildScanApi ->

    // Add all maven profile names as tags
    project.activeProfiles.each { buildScanApi.tag(it.id) }

    // Add all system properties starting with "xwiki" as custom values
    System.getProperties().entrySet()
        .findAll { it.key.startsWith('xwiki') }
        .each { buildScanApi.value(it.key, it.value) }

    // Add specific tags to make it easy to recognize with which environment a docker functional tests has been executed
    def servletContainer = System.getProperty('xwiki.test.ui.servletEngine')
    def servletContainerTag = System.getProperty('xwiki.test.ui.servletEngineTag')
    if (servletContainer && servletContainerTag) {
        buildScanApi.tag("${servletContainer.toLowerCase()} ${servletContainerTag.toLowerCase()}")
    }
    def database = System.getProperty('xwiki.test.ui.database')
    def databaseTag = System.getProperty('xwiki.test.ui.databaseTag')
    if (database && databaseTag) {
        buildScanApi.tag("${database.toLowerCase()} ${databaseTag.toLowerCase()}")
    }
    def browser = System.getProperty('xwiki.test.ui.browser')
    if (browser) {
        buildScanApi.tag(browser.toLowerCase())
    }
}

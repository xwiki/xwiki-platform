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

// It's assumed that Jenkins has been configured to implicitly load the vars/xwikiModule.groovy library which exposes
// the "xwikiModule" global function/DSL.
// Note that the version used is the one defined in Jenkins but it can be overridden as follows:
// @Library("XWiki@<branch, tag, sha1>") _
// See https://github.com/jenkinsci/workflow-cps-global-lib-plugin for details.

// Docker is required for the functional tests
node('docker') {
  xwikiBuild {
    // Force the latest Java version in order to be able to run the functional tests using a recent version of XWiki.
    // We have to do this since we can depend on an old XWiki parent pom version that can be using an old java version.
    javaTool = 'official'        
    profiles = 'quality,legacy,integration-tests,docker'
    // We need a display for the CKBuilder. Note that we don't need xvnc for the functional tests themselves since they
    // execute inside Docker containers.
    xvnc = true
  }
}

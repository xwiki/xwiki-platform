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

// It's assumed that Jenkins has been configured to implicitly load the vars/*.groovy libraries.
// Note that the version used is the one defined in Jenkins but it can be overridden as follows:
// @Library("XWiki@<branch, tag, sha1>") _
// See https://github.com/jenkinsci/workflow-cps-global-lib-plugin for details.

// Definitions of all builds
def builds = [
  'Main' : {
    build(
      name: 'Main',
      profiles: 'legacy,integration-tests,snapshot',
      properties:
        '-Dxwiki.checkstyle.skip=true -Dxwiki.surefire.captureconsole.skip=true -Dxwiki.revapi.skip=true -DskipITs'
    )
  },
  // Can be used to manually trigger the main build with integration tests on the CI.
  'Main with Integration Tests' : {
    build(
      name: 'Main with Integration Tests',
      profiles: 'legacy,integration-tests,snapshot',
      properties:
        '-Dxwiki.checkstyle.skip=true -Dxwiki.surefire.captureconsole.skip=true -Dxwiki.revapi.skip=true'
    )
  },
  'Distribution' : {
    build(
      name: 'Distribution',
      profiles: 'legacy,integration-tests,snapshot',
      pom: 'xwiki-platform-distribution/pom.xml'
    )
  },
  'Flavor Test - POM' : {
    buildFunctionalTest(
      name: 'Flavor Test - POM',
      pom: 'pom.xml',
      properties: '-N'
    )
  },
  'Flavor Test - PageObjects' : {
    buildFunctionalTest(
      name: 'Flavor Test - PageObjects',
      pom: 'xwiki-platform-distribution-flavor-test-pageobjects/pom.xml'
    )
  },
  'Flavor Test - UI' : {
    buildFunctionalTest(
      name: 'Flavor Test - UI',
      pom: 'xwiki-platform-distribution-flavor-test-ui/pom.xml',
    )
  },
  'Flavor Test - Misc' : {
    buildFunctionalTest(
      name: 'Flavor Test - Misc',
      pom: 'xwiki-platform-distribution-flavor-test-misc/pom.xml'
    )
  },
  'Flavor Test - Storage': {
    buildFunctionalTest(
      name: 'Flavor Test - Storage',
      pom: 'xwiki-platform-distribution-flavor-test-storage/pom.xml'
    )
  },
  'Flavor Test - Escaping' : {
    buildFunctionalTest(
      name: 'Flavor Test - Escaping',
      pom: 'xwiki-platform-distribution-flavor-test-escaping/pom.xml'
    )
  },
  'Flavor Test - Upgrade' : {
    buildFunctionalTest(
      name: 'Flavor Test - Upgrade',
      pom: 'xwiki-platform-distribution-flavor-test-upgrade/pom.xml'
    )
  },
  'Flavor Test - Webstandards' : {
    buildFunctionalTest(
      name: 'Flavor Test - Webstandards',
      pom: 'xwiki-platform-distribution-flavor-test-webstandards/pom.xml',
      mavenOpts: '-Xmx2048m -Xms512m -XX:ThreadStackSize=2048'
    )
  },
  'Flavor Test - Security' : {
    buildFunctionalTest(
      name: 'Flavor Test - Security',
      pom: 'xwiki-platform-distribution-flavor-test-security/pom.xml'
    )
  },
  'TestRelease': {
    build(
      name: 'TestRelease',
      goals: 'clean install',
      profiles: 'release,hsqldb,jetty,legacy,integration-tests,standalone,flavor-integration-tests,distribution,docker',
      properties: '-DskipTests -DperformRelease=true -Dgpg.skip=true -Dxwiki.checkstyle.skip=true -Dxwiki.revapi.skip=true -Dxwiki.enforcer.skip=true -Dxwiki.spoon.skip=true -Ddoclint=all'
    )
  },
  'Quality' : {
    // Run the quality checks.
    // Sonar notes:
    // - we need sonar:sonar to perform the analysis and push the results to Sonarcloud
    // - we need jacoco:report to execute jacoco and compute test coverage
    // - we need -Pcoverage and -Dxwiki.jacoco.itDestFile to tell Jacoco to compute a single global Jacoco
    //   coverage for the full reactor (so that the coverage percentage computed takes into account module tests
    //   which cover code in other modules)
    build(
      name: 'Quality',
      goals: 'clean install jacoco:report sonar:sonar',
      profiles: 'quality,legacy,coverage',
      properties: '-Dxwiki.jacoco.itDestFile=`pwd`/target/jacoco-it.exec'
    )
  }
]

// Let the user select the build to execute if manually triggered. Otherwise, build everything.
stage('Platform Builds') {
  def choices = builds.collect { k,v -> "$k" }.join('\n')
  // askUser() will check if the build was triggered manually or not. If not, it'll default to 'All'.
  def selection = askUser(choices)
  if (selection == 'All') {
    buildStandardAll(builds)
  } else {
    buildStandardSingle(builds[selection])
  }
}

private void buildStandardSingle(build)
{
  build.call()
}

private void buildStandardAll(builds)
{
  parallel(
    'main': {
      // Build, skipping quality checks and integration tests (but execute unit tests) so that the result of the build
      // can be sent as fast as possible to the devs. Note that we skip integration tests by using the FailSafe plugin
      // property "DskipITs".
      // In addition, we want the generated artifacts to be deployed to our remote Maven repository so that developers
      // can benefit from them even though some quality checks have not yet passed.
      // In // we start a build with the quality profile that executes various quality checks, and we run all the
      // integration tests just after this build.
      //
      // Note: We configure the snapshot extension repository in XWiki (-Psnapshots) in the generated
      // distributions to make it easy for developers to install snapshot extensions when they do manual tests.
      builds['Main'].call()

      parallel(
        'integration-tests' : {
          // Run all integration tests, with each module in its own node to parallelize the work.
          runIntegrationTests()
        },
        'distribution' : {
          // Note: We want the following behavior:
          // - if an error occurs during the previous build we don't want the subsequent builds to execute. This will
          //   happen since Jenkins will throw an exception and we don't catch it.
          // - if the previous build has failures (e.g. test execution failures), we want subsequent builds to execute
          //   since failures can be test flickers for ex, and it could still be interesting to get a distribution to
          //   test xwiki manually.

          // Build the distributions
          builds['Distribution'].call()

          // Building the various functional tests, after the distribution has been built successfully.

          // Build the Flavor Test POM, required for the pageobjects module below.
          builds['Flavor Test - POM'].call()

          // Build the Flavor Test PageObjects required by the functional test below that need an XWiki UI
          builds['Flavor Test - PageObjects'].call()

          // Now run all tests in parallel
          parallel(
            'flavor-test-ui': {
              // Run the Flavor UI tests
              builds['Flavor Test - UI'].call()
            },
            'flavor-test-misc': {
              // Run the Flavor Misc tests
              builds['Flavor Test - Misc'].call()
            },
            'flavor-test-storage': {
              // Run the Flavor Storage tests
              builds['Flavor Test - Storage'].call()
            },
            'flavor-test-escaping': {
              // Run the Flavor Escaping tests
              builds['Flavor Test - Escaping'].call()
            },
            'flavor-test-webstandards': {
              // Run the Flavor Webstandards tests
              // Note: -XX:ThreadStackSize=2048 is used to prevent a StackOverflowError error when using the HTML5 Nu
              // Validator (see https://bitbucket.org/sideshowbarker/vnu/issues/4/stackoverflowerror-error-when-running)
              builds['Flavor Test - Webstandards'].call()
            },
            'flavor-test-upgrade': {
              // Run the Flavor Upgrade tests
              builds['Flavor Test - Upgrade'].call()
            },
            'flavor-test-security': {
              // Run the Flavor Security tests
              builds['Flavor Test - Security'].call()
            }
          )
        }
      )
    },
    'testrelease': {
      // Simulate a release and verify all is fine, in preparation for the release day.
      builds['TestRelease'].call()
    },
    'quality': {
      // Run the quality checks
      builds['Quality'].call()
    }
  )
}

private void runIntegrationTests()
{
  def itModuleList
  node() {
    // Checkout platform to find all IT modules so that we can then parallelize executions across Jenkins agents.
    checkout skipChangeLog: true, scm: scm
    itModuleList = itModules()
  }

  xwikiITBuild {
    modules = itModuleList
  }
}

private void build(map)
{
  node(map.node ?: '') {
    buildInsideNode(map)
  }
}

private void buildInsideNode(map)
{
    // We want to get a memory dump on OOM errors.
    // Make sure the memory dump directory exists (see below)
    // Note that the user used to run the job on the agent must have the permission to create these directories
    // Verify existence of /home/hudsonagent/jenkins_root so that we only set the oomPath if it does
    def heapDumpPath = ''
    def exists = fileExists '/home/hudsonagent/jenkins_root'
    if (exists) {
        def oomPath = "/home/hudsonagent/jenkins_root/oom/maven/${env.JOB_NAME}-${currentBuild.id}"
        sh "mkdir -p \"${oomPath}\""
        heapDumpPath = "-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=\"${oomPath}\""
    }

    xwikiBuild(map.name) {
      mavenOpts = map.mavenOpts ?: "-Xmx2048m -Xms512m ${heapDumpPath}"
      javadoc = false
      if (map.goals != null) {
        goals = map.goals
      }
      if (map.profiles != null) {
        profiles = map.profiles
      }
      if (map.properties != null) {
        properties = map.properties
      }
      // Keep builds for 30 days since we want to be able to see all builds if there are a lot at a given time, to be
      // able to identify flickers, etc.
      daysToKeepStr = '30'
      if (map.pom != null) {
        pom = map.pom
      }
      if (map.mavenFlags != null) {
        mavenFlags = map.mavenFlags
      }
      if (map.sonar != null) {
        sonar = map.sonar
      }
      if (map.skipCheckout != null) {
        skipCheckout = map.skipCheckout
      }
      if (map.xvnc != null) {
        xvnc = map.xvnc
      }
      if (map.skipMail != null) {
        skipMail = map.skipMail
      }
      // Avoid duplicate changelogs in jenkins job execution UI page
      if (map.name != 'Main') {
        skipChangeLog = true
      }
      if (map.javaTool != null) {
        javaTool = map.javaTool
      }
    }
}

private void buildFunctionalTest(map)
{
  def sharedPOMPrefix =
    'xwiki-platform-distribution/xwiki-platform-distribution-flavor/xwiki-platform-distribution-flavor-test'

  build(
    name: map.name,
    profiles: 'legacy,integration-tests,jetty,hsqldb,firefox',
    mavenOpts: map.mavenOpts,
    pom: "${sharedPOMPrefix}/${map.pom}",
    properties: map.properties
  )
}


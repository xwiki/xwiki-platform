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
      properties: '-Dxwiki.checkstyle.skip=true -Dxwiki.surefire.captureconsole.skip=true -Dxwiki.revapi.skip=true',
      daysToKeepStr: env.BRANCH_NAME == 'master' ? '30' : null
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
      daysToKeepStr: env.BRANCH_NAME == 'master' ? '30' : null
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
  'Flavor Test - Selenium' : {
    buildFunctionalTest(
      name: 'Flavor Test - Selenium',
      pom: 'xwiki-platform-distribution-flavor-test-selenium/pom.xml',
      daysToKeepStr: env.BRANCH_NAME == 'master' ? '30' : null
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
  'TestRelease': {
    build(
      name: 'TestRelease',
      goals: 'clean install',
      profiles: 'hsqldb,jetty,legacy,integration-tests,standalone,flavor-integration-tests,distribution,docker',
      properties: '-DskipTests -DperformRelease=true -Dgpg.skip=true -Dxwiki.checkstyle.skip=true -Ddoclint=all'
    )
  },
  'Quality' : {
    build(
      name: 'Quality',
      goals: 'clean install jacoco:report',
      profiles: 'quality,legacy'
    )
  },
  'Sonar' : {
    // Note: ideally, this should be part of the 'Quality' build but last time we tried it was using too much memory
    // so until we make it work, we're testing Sonar in a separate build to not fail the 'Quality' one.
    build(
      name: 'Sonar',
      goals: 'clean install jacoco:report sonar:sonar',
      profiles: 'legacy',
      sonar: true
    )
  }
]

// Decide whether to execute the standard builds or the Docker ones.
// See the build() method below and the definition of the "type" job parameter.
if (!params.type || params.type == 'standard') {
  stage('Platform Builds') {
    def choices = builds.collect { k,v -> "$k" }.join('\n')
    // Add the docker scheduled jobs so that we can trigger them manually too
    choices = "${choices}\nDocker Latest\nDocker All\nDocker Unsupported"
    def selection = askUser(choices)
    if (selection == 'All') {
      buildStandardAll(builds)
    } else if (selection == 'Docker Latest') {
      buildDocker('docker-latest')
    } else if (selection == 'Docker All') {
      buildDocker('docker-all')
    } else if (selection == 'Docker Unsupported') {
      buildDocker('docker-unsupported')
    } else {
      buildStandardSingle(builds[selection])
    }
  }
} else {
  buildDocker(params.type)
}

private void buildStandardSingle(build)
{
  build.call()
}

private void buildStandardAll(builds)
{
  parallel(
    'main': {
      // Build, skipping quality checks so that the result of the build can be sent as fast as possible to the devs.
      // In addition, we want the generated artifacts to be deployed to our remote Maven repository so that developers
      // can benefit from them even though some quality checks have not yet passed. In // we start a build with the
      // quality profile that executes various quality checks.
      //
      // Note: We configure the snapshot extension repository in XWiki (-Psnapshots) in the generated
      // distributions to make it easy for developers to install snapshot extensions when they do manual tests.
      builds['Main'].call()

      // Note: We want the following behavior:
      // - if an error occurs during the previous build we don't want the subsequent builds to execute. This will
      //   happen since Jenkins will throw an exception and we don't catch it.
      // - if the previous build has failures (e.g. test execution failures), we want subsequent builds to execute
      //   since failures can be test flickers for ex, and it could still be interesting to get a distribution to test
      // xwiki manually.

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
        'flavor-test-selenium': {
          // Run the Flavor Selenium tests
          builds['Flavor Test - Selenium'].call()
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
    },
    'sonar': {
      // Sonar analysis + push to Sonarcloud.io
      builds['Sonar'].call()
    }
  )
}

private void buildDocker(type)
{
  def dockerConfigurationList
  def dockerModuleList
  def customJobProperties
  node() {
    // Checkout platform to find all docker configurations and test modules so that we can then parallelize executions
    // of configs and modules across Jenkins agents.
    checkout skipChangeLog: true, scm: scm
    dockerConfigurationList = dockerConfigurations(type)
    if (type == 'docker-unsupported') {
      dockerModuleList = ['xwiki-platform-core/xwiki-platform-menu']
    } else {
      dockerModuleList = dockerModules()
    }
    customJobProperties = getCustomJobProperties()
  }

  xwikiDockerBuild {
    configurations = dockerConfigurationList
    modules = dockerModuleList
    // Make sure that we don't reset the job properties!
    jobProperties = customJobProperties
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
      jobProperties = getCustomJobProperties()
      if (map.goals != null) {
        goals = map.goals
      }
      if (map.profiles != null) {
        profiles = map.profiles
      }
      if (map.properties != null) {
        properties = map.properties
      }
      if (map.daysToKeepStr != null) {
        daysToKeepStr = map.daysToKeepStr
      }
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

private def getCustomJobProperties()
{
  // Define a scheduler job to execute the Docker-based functional tests at regular intervals. We do this since they
  // take time to execute and thus we cannot run them all the time.
  // This scheduler job will pass the "type" parameter to this Jenkinsfile when it executes, allowing us to decide if
  // we run the standard builds or the docker ones.
  // Note: it's the xwikiBuild() calls from the standard builds that will set the jobProperties and thus set up the
  // job parameter + the crons. It would be better to set the properties directly in this Jenkinsfile but we haven't
  // found a way to merge properties and calling the properties() step will override any pre-existing properties.
  return [
    parameters([string(defaultValue: 'standard', description: 'Job type', name: 'type')]),
    pipelineTriggers([
      parameterizedCron('''@midnight %type=docker-latest
@weekly %type=docker-all
@monthly %type=docker-unsupported'''),
      cron("@monthly")
    ])
  ]
}


/*
 * See the LICENSE file distributed with this work for additional
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

pipeline {
    options { disableConcurrentBuilds() }
    agent {
      label 'dockernodejs'
    }
    environment {
      NX_CACHE_DIRECTORY = '/tmp/.nxcache'
    }
    stages {
        stage('Install') {
            steps {
                sh 'pnpm install'
            }
        }
        stage('Lint') {
            steps {
                sh 'pnpm lint'
            }
        }
        stage('Build') {
            steps {
                 sh 'pnpm build'
            }
        }
        stage('Unit Tests') {
            steps {
                sh 'pnpm test'
            }
        }
        stage('E2E Tests') {
            steps {
                sh 'pnpm run --filter ./web test:e2e'
            }
        }
    }
    post {
        always {
            junit testResults: '**/unit-tests.xml', skipPublishingChecks: true
            junit testResults: '**/e2e-tests.xml', skipPublishingChecks: true

        }
        failure {
          archiveArtifacts artifacts: 'web/test-results'
        }
    }
}


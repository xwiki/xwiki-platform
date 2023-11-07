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
pipeline {
    agent {
      label 'dockernodejs'
    }
    environment {
        BROWSERSTACK_USERNAME   = credentials('BROWSERSTACK_USERNAME')
        BROWSERSTACK_ACCESS_KEY = credentials('BROWSERSTACK_ACCESS_KEY')
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
                sh 'pnpm test:unit:ci'
            }
        }
        stage('End to End Tests') {
            steps {
                sh 'pnpm test:e2e:browserstack'
            }
        }
        stage('Pack') {
            steps {
               sh 'pnpm pack'
            }
        }
    }
    post {
        always {
            archiveArtifacts artifacts: '*.tgz', fingerprint: true
            junit 'unit-tests.xml'
            junit 'e2e-tests.xml'
        }
    }
}


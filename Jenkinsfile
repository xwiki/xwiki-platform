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
    agent any
    stages {
        stage('Debug') {
            steps {
                sh 'id'
                sh 'pwd'
            }
        }
        stage('Install') {
            steps {
                nvm('v20.9.0') {
                    sh 'npm ci'
                }
            }
        }
        stage('Lint') {
            steps {
                nvm('v20.9.0') {
                    sh 'npm run lint'
                }
            }
        }
        stage('Build') {
            steps {
                nvm('v20.9.0') {
                    sh 'npm run build'
                }
            }
        }
        stage('Unit Tests') {
            steps {
                nvm('v20.9.0') {
                    sh 'npm run test:unit:ci'
                }
            }
        }
        stage('End to End Tests') {
            steps {
                nvm('v20.9.0') {
                    sh 'npx playwright install'
                    sh 'npm run test:e2e'
                }
            }
        }
        stage('Pack') {
            steps {
                nvm('v20.9.0') {
                    sh 'npm pack'
                }
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


#!/usr/bin/env groovy

pipeline {
    agent any

    environment {
        APPLICATION_NAME = 'altinnkanal-2'
        FASIT_ENV = 'q1'
        ZONE = 'fss'
        NAMESPACE = 'default'
    }

    stages {
        stage('prepare') {
            steps {
                ciSkip action: 'check'
            }
        }
        stage('initialize') {
            steps {
                script {
                    sh './gradlew clean'
                    applicationVersionGradle = sh(script: './gradlew -q printVersion', returnStdout: true).trim()
                    gitVars = utils.gitVars(env.APPLICATION_NAME)
                    env.APPLICATION_VERSION = "${applicationVersionGradle}"
                    env.COMMIT = gitVars.commitHashShort
                    env.COMMIT_FULL = gitVars.commitHash
                    if (applicationVersionGradle.endsWith('-SNAPSHOT')) {
                        env.APPLICATION_VERSION = "${applicationVersionGradle}.${env.BUILD_ID}-${gitVars.commitHashShort}"
                    }
                    githubStatus status: 'pending'
                    slackStatus status: 'started', changeLog: "${gitVars.changeLog}"
                }
            }
        }
        stage('build') {
            steps {
                script {
                    sh './gradlew build -x test'
                }
            }
        }
        stage('run tests (unit & intergration)') {
            steps {
                script {
                    sh './gradlew test'
                }
                slackStatus status: 'passed'
            }
        }
        stage('deploy schemas to maven repo') {
            steps {
                script {
                    sh './gradlew publish'
                }
            }
        }
        stage('extract application files') {
            steps {
                script {
                    sh './gradlew installDist'
                }
            }
        }
        stage('push docker image') {
            steps {
                dockerUtils action: 'createPushImage'
            }
        }
        stage('validate & upload nais.yaml to nexus m2internal') {
            steps {
                nais action: 'validate'
                nais action: 'upload'
            }
        }
        stage('deploy to nais') {
            steps {
                script {
                    def jiraIssueId = nais action: 'jiraDeploy'
                    currentBuild.description = "<a href=\"https://jira.adeo.no/browse/$jiraIssueId\">$jiraIssueId</a>"
                    slackStatus status: 'deploying', jiraIssueId: "${jiraIssueId}"
                    try {
                        timeout(time: 1, unit: 'HOURS') {
                            input id: "deploy", message: "Waiting for remote Jenkins server to deploy the application..."
                        }
                    } catch (Exception exception) {
                        currentBuild.description = "Deploy failed, see <a href=\"https://jira.adeo.no/browse/$jiraIssueId\">$jiraIssueId</a>"
                        throw exception
                    }
                }
            }
        }
    }
    post {
        always {
            ciSkip action: 'postProcess'
            dockerUtils action: 'pruneBuilds'
            script {
                if (currentBuild.result == 'ABORTED') {
                    slackStatus status: 'aborted'
                }
            }
        }
        success {
            junit '**/build/test-results/junit-platform/*.xml'
            archive '**/build/libs/*'
            archive '**/build/install/*'
            deleteDir()
            githubStatus status: 'success'
            slackStatus status: 'success'
        }
        failure {
            githubStatus status: 'failure'
            slackStatus status: 'failure'
            deleteDir()
        }
    }
}

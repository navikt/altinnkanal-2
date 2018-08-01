#!/usr/bin/env groovy

pipeline {
    agent any

    environment {
        APPLICATION_NAME = 'altinnkanal-2'
        FASIT_ENV = 'q1'
        ZONE = 'fss'
        NAMESPACE = 'default'
        COMMIT_HASH_SHORT = gitVars 'commitHashShort'
        COMMIT_HASH = gitVars 'commitHash'
    }

    stages {
        stage('initialize') {
            steps {
                ciSkip 'check'
                sh './gradlew clean'
                script {
                    applicationVersionGradle = sh(script: './gradlew -q :altinnkanal:printVersion', returnStdout: true).trim()
                    env.APPLICATION_VERSION = "${applicationVersionGradle}"
                    if (applicationVersionGradle.endsWith('-SNAPSHOT')) {
                        env.APPLICATION_VERSION = "${applicationVersionGradle}.${env.BUILD_ID}-${env.COMMIT_HASH_SHORT}"
                    } else {
                        env.DEPLOY_TO = 'production'
                    }
                    changeLog = utils.gitVars(env.APPLICATION_NAME).changeLog.toString()
                    githubStatus 'pending'
                    slackStatus status: 'started', changeLog: "${changeLog}"
                }
            }
        }
        stage('build') {
            steps {
                sh './gradlew build -x test'
            }
        }
        stage('run tests (unit & intergration)') {
            steps {
                sh './gradlew test'
                slackStatus status: 'passed'
            }
        }
        stage('extract application files') {
            steps {
                sh './gradlew installDist'
            }
        }
        stage('push docker image') {
            steps {
                dockerUtils 'createPushImage'
            }
        }
        stage('validate & upload nais.yaml to nexus m2internal') {
            steps {
                nais action: 'validate'
                nais action: 'upload'
            }
        }
        stage('deploy to preprod') {
            steps {
                createJiraIssue('jiraDeploy')
                waitForCallback()
            }
        }
        stage('deploy to production') {
            when { environment name: 'DEPLOY_TO', value: 'production' }
            environment {
                FASIT_ENV = 'p'
                APPLICATION_SERVICE = 'CMDB-32744'
                APPLICATION_COMPONENT = 'CMDB-190474'
            }
            steps {
                script {
                    createJiraIssue('jiraDeploy')
                    createJiraIssue('jiraDeployProd')
                    waitForCallback()
                    githubStatus 'tagRelease'
                }
            }
        }
    }
    post {
        always {
            ciSkip 'postProcess'
            dockerUtils 'pruneBuilds'
            script {
                if (currentBuild.result == 'ABORTED') {
                    slackStatus status: 'aborted'
                }
            }
            junit '**/build/test-results/test/*.xml'
            archiveArtifacts artifacts: '**/build/libs/*', allowEmptyArchive: true
            archiveArtifacts artifacts: '**/build/install/*', allowEmptyArchive: true
            deleteDir()
        }
        success {
            githubStatus 'success'
            slackStatus status: 'success'
        }
        failure {
            githubStatus 'failure'
            slackStatus status: 'failure'
        }
    }
}

void createJiraIssue(String action) {
    def jiraIssueId = nais action: action
    slackStatus status: 'deploying', jiraIssueId: "${jiraIssueId}"
}

void waitForCallback() {
    try {
        timeout(time: 1, unit: 'HOURS') {
            input id: "deploy", message: "Waiting for remote Jenkins server to deploy the application..."
        }
    } catch (Exception exception) {
        currentBuild.description = "Deploy failed, see " + currentBuild.description
        throw exception
    }
}
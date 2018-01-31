#!/usr/bin/env groovy

pipeline {
	agent any
	tools {
		maven 'default'
	}

    environment {
        APPLICATION_NAME = 'altinnkanal-2'
        FASIT_ENV = 't4'
        ZONE = 'fss'
        NAMESPACE = 'default'
    }

	stages {
		stage('initialize') {
			steps {
				script {
					pom = readMavenPom file: 'pom.xml'
					gitVars = gitVars(env.APPLICATION_NAME, env.APPLICATION_NAME)
					applicationVersion = "${pom.version}.${env.BUILD_ID}-${gitVars.commitHashShort}"
					applicationFullName = "${env.APPLICATION_NAME}:${applicationVersion}"

					def title = "Build Started :partyparrot:"
					def fallback = "Build Started: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
					def customFieldText = gitVars.changeLog
					def customField = ["title": "Commit(s)", "value": customFieldText.toString(), "short": false]
					def color = "#D4DADF"
					slackMessageAttachments(env.APPLICATION_NAME, title, "", fallback, color, customField)
				}
			}
		}
		stage('build') {
			steps {
				mvnBuild()
			}
		}
		stage('run tests (unit & intergration)') {
			steps {
				script {
                    sh 'mvn verify'
					def title = "Build Passed :rightparrot:"
					def text = "Build passed in ${currentBuild.durationString.replace(' and counting', '')}"
					def fallback = "Build Passed: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
					def color = "#FFFE89"
					slackMessageAttachments(env.APPLICATION_NAME, title, text, fallback, color)
				}
			}
		}

        stage('deploy schemas to maven repo') {
            steps {
                script {
                    dir('altinnkanal-schemas') {
                        sh 'mvn deploy'
                    }
                }
            }
        }

		stage('push docker image') {
			steps {
				dockerCreatePushImage(applicationFullName, gitVars.commitHashShort)
			}
		}
		stage('validate & upload nais.yaml to nexus m2internal') {
			steps {
				naisUploadYaml(env.APPLICATION_NAME, applicationVersion)
			}
		}
		stage('deploy to nais') {
			steps {
				script {
					response = naisDeployJira(env.APPLICATION_NAME, applicationVersion, env.FASIT_ENV, env.NAMESPACE, env.ZONE)
					def jiraIssueId = readJSON([text: response.content])["key"]
					currentBuild.description = "Waiting for <a href=\"https://jira.adeo.no/browse/$jiraIssueId\">$jiraIssueId</a>"

					def title = "Deploying... :slowparrot:"
					def fallback = "Deploying...: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
					def text = "Waiting for deployment via Jira."
					def customFieldText = "<https://jira.adeo.no/browse/$jiraIssueId|${jiraIssueId}>"
					def customField = ["title": "Jira Issue", "value": customFieldText.toString(), "short": false]
					def color = "#FFFE89"
					slackMessageAttachments(env.APPLICATION_NAME, title, text, fallback, color, customField)

					try {
						timeout(time: 1, unit: 'HOURS') {
							input id: "deploy", message: "Waiting for remote Jenkins server to deploy the application..."
						}
						currentBuild.description = ""
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
			junit '**/target/surefire-reports/*.xml'
			junit '**/target/failsafe-reports/*.xml'
			archive '**/target/*.jar'
			deleteDir()
			dockerPruneBuilds()
			script {
				if (currentBuild.result == 'ABORTED') {
					def title = "Build Aborted :confusedparrot:"
					def fallback = "Build Aborted: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
					def color = "#FF9FA1"
					slackMessageAttachments(env.APPLICATION_NAME, title, "", fallback, color)
				}
			}
		}
		success {
			script {
				def title = "Deploy Success :ultrafastparrot:"
				def text = "Successfully deployed in ${currentBuild.durationString.replace(' and counting', '')}"
				def fallback = "Deploy Success: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
				def color = "#BDFFC3"
				slackMessageAttachments(env.APPLICATION_NAME, title, text, fallback, color)
			}
		}
		failure {
			script {
				def title = "Build Failed :explodyparrot:"
				def text = "Something went wrong."
				def fallback = "Build Failed: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
				def color = "#FF9FA1"
				slackMessageAttachments(env.APPLICATION_NAME, title, text, fallback, color)
			}
		}
	}
}

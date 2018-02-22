#!/usr/bin/env groovy

pipeline {
	agent any
	tools {
		maven 'default'
	}

    environment {
        APPLICATION_NAME = 'altinnkanal-2'
		GIT_PROJECT = 'INT'
        FASIT_ENV = 'q4'
        ZONE = 'fss'
        NAMESPACE = 'default'
    }

	stages {
		stage('initialize') {
			steps {
				script {
					pom = readMavenPom file: 'pom.xml'
					gitVars = utils.gitVars(env.GIT_PROJECT, env.APPLICATION_NAME)
					applicationVersion = "${pom.version}.${env.BUILD_ID}-${gitVars.commitHashShort}"
					applicationFullName = "${env.APPLICATION_NAME}:${applicationVersion}"

					def title = "Build Started :party_parrot:"
					def fallback = "Build Started: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
					def customFieldText = gitVars.changeLog
					def customField = ["title": "Commit(s)", "value": customFieldText.toString(), "short": false]
					def color = "#D4DADF"
					//utils.slackMessageAttachments(env.APPLICATION_NAME, title, "", fallback, color, customField)
				}
			}
		}
		stage('build') {
			steps {
				script {
					utils.mvnBuild()
				}
			}
		}
		stage('run tests (unit & intergration)') {
			steps {
				script {
                    //sh 'mvn verify'
					def title = "Build Passed :right_parrot:"
					def text = "Build passed in ${currentBuild.durationString.replace(' and counting', '')}"
					def fallback = "Build Passed: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
					def color = "#FFFE89"
					//utils.slackMessageAttachments(env.APPLICATION_NAME, title, text, fallback, color)
				}
			}
		}

        stage('deploy schemas to maven repo') {
            steps {
                script {
					sh 'mvn deploy -DskipTests'
                }
            }
        }

		stage('push docker image') {
			steps {
				script {
					utils.dockerCreatePushImage(applicationFullName, gitVars.commitHashShort)
				}
			}
		}
		stage('validate & upload nais.yaml to nexus m2internal') {
			steps {
				script {
					deploy.naisUploadYaml(env.APPLICATION_NAME, applicationVersion)
				}
			}
		}
		stage('deploy to nais') {
			steps {
				script {
					withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: 'nais-user-post', usernameVariable: "NAIS_USERNAME", passwordVariable: "NAIS_PASSWORD"]]) {
						def postBody = [
								application: "${env.APPLICATION_NAME}",
								environment: "${env.FASIT_ENV}",
								version    : "${applicationVersion}",
								username   : "${env.NAIS_USERNAME}",
								password   : "${env.NAIS_PASSWORD}",
								zone       : "${env.ZONE}",
								namespace  : "${env.NAMESPACE}"
						]
						def naisdPayload = groovy.json.JsonOutput.toJson(postBody)

						echo naisdPayload

						def response = httpRequest([
								url                   : "https://daemon.nais.preprod.local/deploy",
								consoleLogResponseBody: true,
								contentType           : "APPLICATION_JSON",
								httpMode              : "POST",
								requestBody           : naisdPayload,
								ignoreSslErrors       : true
						])

						echo "$response.status: $response.content"

						if (response.status != 200) {
							currentBuild.description = "Failed - $response.content"
							currentBuild.result = "FAILED"
						}
					}
					/*response = deploy.naisDeployJira(env.APPLICATION_NAME, applicationVersion, env.FASIT_ENV, env.NAMESPACE, env.ZONE)
					def jiraIssueId = readJSON([text: response.content])["key"]
					currentBuild.description = "Waiting for <a href=\"https://jira.adeo.no/browse/$jiraIssueId\">$jiraIssueId</a>"

					def title = "Deploying... :slow_parrot:"
					def fallback = "Deploying...: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
					def text = "Waiting for deployment via Jira."
					def customFieldText = "<https://jira.adeo.no/browse/$jiraIssueId|${jiraIssueId}>"
					def customField = ["title": "Jira Issue", "value": customFieldText.toString(), "short": false]
					def color = "#FFFE89"
					utils.slackMessageAttachments(env.APPLICATION_NAME, title, text, fallback, color, customField)

					try {
						timeout(time: 1, unit: 'HOURS') {
							input id: "deploy", message: "Waiting for remote Jenkins server to deploy the application..."
						}
						currentBuild.description = ""
					} catch (Exception exception) {
						currentBuild.description = "Deploy failed, see <a href=\"https://jira.adeo.no/browse/$jiraIssueId\">$jiraIssueId</a>"
						throw exception
					}*/
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
			script {
				utils.dockerPruneBuilds()
				if (currentBuild.result == 'ABORTED') {
					def title = "Build Aborted :confused_parrot:"
					def fallback = "Build Aborted: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
					def color = "#FF9FA1"
					//utils.slackMessageAttachments(env.APPLICATION_NAME, title, "", fallback, color)
				}
			}
		}
		success {
			script {
				def title = "Deploy Success :ultrafast_parrot:"
				def text = "Successfully deployed in ${currentBuild.durationString.replace(' and counting', '')}"
				def fallback = "Deploy Success: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
				def color = "#BDFFC3"
				//utils.slackMessageAttachments(env.APPLICATION_NAME, title, text, fallback, color)
			}
		}
		failure {
			script {
				def title = "Build Failed :explody_parrot:"
				def text = "Something went wrong."
				def fallback = "Build Failed: #${env.BUILD_NUMBER} of ${env.APPLICATION_NAME} - ${env.BUILD_URL}"
				def color = "#FF9FA1"
				//utils.slackMessageAttachments(env.APPLICATION_NAME, title, text, fallback, color)
			}
		}
	}
}

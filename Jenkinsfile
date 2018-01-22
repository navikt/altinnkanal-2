#!/usr/bin/env groovy

pipeline {
	agent any
	tools {
		maven 'default'
	}
	stages {
		stage('initialize') {
			steps {
				script {
					pom = readMavenPom file: 'pom.xml'
					applicationName = "${pom.artifactId}"
					gitVars = gitVars(args.gitProject, applicationName)
					applicationVersion = "${pom.version}.${env.BUILD_ID}-${gitVars.commitHashShort}"
					applicationFullName = "${applicationName}:${applicationVersion}"

					def title = "Build Started :partyparrot:"
					def fallback = "Build Started: #${env.BUILD_NUMBER} of ${applicationName} - ${env.BUILD_URL}"
					def customFieldText = "`<${gitVars.commitUrl}|${gitVars.commitHashShort}>`: ${gitVars.commitMessage} - ${gitVars.committer}"
					def customField = ["title": "Commit", "value": customFieldText.toString(), "short": false]
					def color = "#D4DADF"
					slackMessageAttachments(applicationName, title, "", fallback, color, customField)
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
				mvnTestVerify(args.get("envVars"))
				script {
					def title = "Build Passed :rightparrot:"
					def text = "Build passed in ${currentBuild.durationString.replace(' and counting', '')}"
					def fallback = "Build Passed: #${env.BUILD_NUMBER} of ${applicationName} - ${env.BUILD_URL}"
					def color = "#FFFE89"
					slackMessageAttachments(applicationName, title, text, fallback, color)
				}
			}
		}

        stage('deploy schemas to maven repo') {
            steps {
                script {
                    sh 'mvn deploy'
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
				naisUploadYaml(applicationName, applicationVersion)
			}
		}
		stage('deploy to nais') {
			steps {
				script {
					response = naisDeployJira(applicationName, applicationVersion, args.environment, args.namespace, args.zone)
					def jiraIssueId = readJSON([text: response.content])["key"]
					currentBuild.description = "Waiting for <a href=\"https://jira.adeo.no/browse/$jiraIssueId\">$jiraIssueId</a>"

					def title = "Deploying... :slowparrot:"
					def fallback = "Deploying...: #${env.BUILD_NUMBER} of ${applicationName} - ${env.BUILD_URL}"
					def text = "Waiting for deployment via Jira."
					def customFieldText = "<https://jira.adeo.no/browse/$jiraIssueId|${jiraIssueId}>"
					def customField = ["title": "Jira Issue", "value": customFieldText.toString(), "short": false]
					def color = "#FFFE89"
					slackMessageAttachments(applicationName, title, text, fallback, color, customField)

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
					def fallback = "Build Aborted: #${env.BUILD_NUMBER} of ${applicationName} - ${env.BUILD_URL}"
					def color = "#FF9FA1"
					slackMessageAttachments(applicationName, title, "", fallback, color)
				}
			}
		}
		success {
			script {
				def title = "Deploy Success :ultrafastparrot:"
				def text = "Successfully deployed in ${currentBuild.durationString.replace(' and counting', '')}"
				def fallback = "Deploy Success: #${env.BUILD_NUMBER} of ${applicationName} - ${env.BUILD_URL}"
				def color = "#BDFFC3"
				slackMessageAttachments(applicationName, title, text, fallback, color)
			}
		}
		failure {
			script {
				def title = "Build Failed :explodyparrot:"
				def text = "Something went wrong."
				def fallback = "Build Failed: #${env.BUILD_NUMBER} of ${applicationName} - ${env.BUILD_URL}"
				def color = "#FF9FA1"
				slackMessageAttachments(applicationName, title, text, fallback, color)
			}
		}
	}
}
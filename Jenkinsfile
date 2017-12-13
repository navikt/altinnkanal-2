#!/usr/bin/env groovy

pipeline {
	agent any
	tools {
		maven 'default'
	}
	environment {
		GIT_PROJECT='INT'
		APPLICATION_NAME='altinnkanal-2'
		FASIT_ENV='t4'
		VERSION_MAJOR='1'
		VERSION_MINOR='0'
		NAIS_CREDENTIALS_ID='nais-user' // refers to Jenkins credentials id
		ZONE='fss'
		APPLICATION_NAMESPACE='default'

		// env vars for tests
		LDAP_URL='ldap://ldapgw.test.local'
		LDAP_USER_BASEDN='ou=NAV,ou=BusinessUnits,dc=test,dc=local'
		SPRING_DATASOURCE_PASSWORD='root'
		SPRING_DATASOURCE_URL='jdbc:mysql://localhost/altinnkanal'
		SOAP_USERNAME='test'
		SOAP_PASSWORD='test'
	}
	stages {
		stage('initialize'){
			steps {
				script {
					commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
		            commitHashShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
		            commitUrl = "http://stash.devillo.no/projects/${env.GIT_PROJECT}/repos/${env.APPLICATION_NAME}/commits/${commitHash}"
		            committer = sh(script: 'git log -1 --pretty=format:"%an"', returnStdout: true).trim()
		            appVersion = "${env.VERSION_MAJOR}.${env.VERSION_MINOR}.${env.BUILD_ID}-${commitHashShort}"
		            slackMessage = "<${env.BUILD_URL}|#${env.BUILD_NUMBER}> (<${commitUrl}|${commitHashShort}>) of ${env.GIT_PROJECT}/${env.APPLICATION_NAME}@master by ${committer}"
				}
				slackSend color: "#7C2491", message: "[STARTED] ${slackMessage} :fastparrot:"
			}
		}
		stage('build') {
			steps {
				sh 'mvn -B -DskipTests clean package'
			}
		}
		stage('run tests (unit & intergration)') {
			steps {
				sh 'mvn verify'
				slackSend color: "#1E90FF", message: "[BUILD SUCCESS] Build passed in ${currentBuild.durationString.replace(' and counting', '')} :rightparrot:"
			}
		}
		
		stage('push docker image') {
			steps {
				script {
					checkout scm
					docker.withRegistry('https://docker.adeo.no:5000/') {
						def image = docker.build("integrasjon/${env.APPLICATION_NAME}:${appVersion}")
						image.push()
						image.push 'latest'
					}
				}
			}
		}
		stage('validate & upload nais.yaml to nexus m2internal') {
			steps {
				script {
					withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexus-user', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD']]) {
						sh "nais validate"
						sh "nais upload --app ${env.APPLICATION_NAME} -v ${appVersion}"
					}
				}
			}
		}
		stage('confirmation: deploy to prod?') {
			when {
				environment name: 'FASIT_ENV', value: 'p'
			}
			steps {
				milestone 1
				slackSend color: "warning", message: "[DEPLOY - User Input Needed] ${slackMessage} :parrot:"
				script {
					userInput = input(message: "Continue to Deploy?", submitterParameter: "submitter")
				}
				milestone 2 
				slackSend color: "warning", message: "[DEPLOY - Approved by ${userInput}] ${slackMessage} :aussieparrot:"
			}
		}
		stage('deploy to nais') {
			steps {
				script {
			        def postBody = [
	                    fields: [
	                        project          : [key: "DEPLOY"],
	                        issuetype        : [id: "14302"],
	                        customfield_14811: [value: "${env.FASIT_ENV}"],
	                        customfield_14812: "${env.APPLICATION_NAME}:${appVersion}",
	                        customfield_17410: "${env.BUILD_URL}input/Deploy/",
	                        customfield_19015: [id: "22707", value: "Yes"],
	                        customfield_19413: "${env.APPLICATION_NAMESPACE}",
	                        customfield_19610: [value: "${env.ZONE}"],
	                        summary          : "Automatisk deploy av ${env.APPLICATION_NAME}:${appVersion} til ${env.FASIT_ENV}"
	                    ]
	                ]

			        def jiraPayload = groovy.json.JsonOutput.toJson(postBody)
			        
			        echo jiraPayload

			        def response = httpRequest([
		                url                   : "https://jira.adeo.no/rest/api/2/issue/",
		               	authentication        : "nais-user",
		                consoleLogResponseBody: true,
		                contentType           : "APPLICATION_JSON",
		                httpMode              : "POST",
		                requestBody           : jiraPayload
			        ])

		            def jiraIssueId = readJSON([text: response.content])["key"]
		            currentBuild.description = "Waiting for <a href=\"https://jira.adeo.no/browse/$jiraIssueId\">${jiraIssueId}</a>"

		            slackSend (color: "#1E90FF", message: "[IN PROGRESS] Waiting for deployment via Jira. See issue: <https://jira.adeo.no/browse/$jiraIssueId|${jiraIssueId}> :slowparrot:")
		            try {
		                input id: "deploy", message: "Waiting for remote Jenkins server to deploy the application..."
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
			junit 'target/surefire-reports/*.xml'
			junit 'target/failsafe-reports/*.xml'
        	archive 'target/*.jar'
			deleteDir()
			script {
				// clean up Docker builds
				sh "docker system prune -af"
				if (currentBuild.result == 'ABORTED') {
					slackSend color: "warning", message: "[${currentBuild.currentResult}] ${slackMessage} :confusedparrot:"
				}
			}
        }
        success {
        	slackSend color: "good", message: "[DEPLOY SUCCESS] ${slackMessage} successfully deployed in ${currentBuild.durationString.replace(' and counting', '')} :feelsrareman:"
        }
		failure {
			script {
				if (currentBuild.description != null) {
					slackMessage = "${slackMessage}." + "${currentBuild.description}"
				}
			}
			slackSend color: "danger", message: "[${currentBuild.currentResult}] ${slackMessage} :feelsohwait:"
		}
    }
}

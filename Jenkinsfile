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
		MAVEN_OPTS='-Djavax.net.ssl.trustStore=preprod.truststore.jks -Djavax.net.ssl.trustStorePassword=password'
	}
	stages {
		stage('build') {
			steps {
				script {
					commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
		            commitHashShort = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
		            commitUrl = "http://stash.devillo.no/projects/${env.GIT_PROJECT}/repos/${env.APPLICATION_NAME}/commits/${commitHash}"
		            committer = sh(script: 'git log -1 --pretty=format:"%an"', returnStdout: true).trim()
		            slackMessage = "<${env.BUILD_URL}|#${env.BUILD_NUMBER}> (<${commitUrl}|${commitHashShort}>) of ${env.GIT_PROJECT}/${env.APPLICATION_NAME}@master by ${committer}"
		            sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot\" -import -file certs/preprod/B27_issuing_intern.crt -noprompt"
					sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot2\" -import -file certs/preprod/D26_issuing_intern.crt -noprompt"
				}
				slackSend message: "[STARTED] ${slackMessage} :fastparrot:"
				sh 'mvn -B -DskipTests clean package'
			}
		}
		stage('test') {
			steps {
				sh 'mvn verify'
			}
		}
		stage('deploy docker image') {
			steps {
				milestone 1
				slackSend color: "warning", message: "[DEPLOY - User Input Needed] ${slackMessage} :parrot:"
				script {
					userInput = input(message: "Continue to Deploy?", submitterParameter: "submitter")
				}
				milestone 2
				slackSend color: "warning", message: "[DEPLOY - Approved by ${userInput}] ${slackMessage} :aussieparrot:"
				script {
					checkout scm
					docker.withRegistry('https://docker.adeo.no:5000/') {
						def image = docker.build("integrasjon/${env.APPLICATION_NAME}:${env.VERSION_MAJOR}.${env.VERSION_MINOR}.${env.BUILD_ID}")
						image.push()
						image.push 'latest'
					}
				}
			}
		}
		stage('deploy nais.yaml to nexus m2internal') {
			steps {
				script {
					withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexus-user', usernameVariable: 'NEXUS_USERNAME', passwordVariable: 'NEXUS_PASSWORD']]) {
						sh "nais validate"
						sh "nais upload --app ${env.APPLICATION_NAME} -v ${env.VERSION_MAJOR}.${env.VERSION_MINOR}.${env.BUILD_ID}"
					}
				}
			}
		}
		stage('deploy to nais') {
			steps {
				script {
   					withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: NAIS_CREDENTIALS_ID, usernameVariable: "NAIS_USERNAME", passwordVariable: "NAIS_PASSWORD"]]) {
			            def postBody = [
			                    application: "${env.APPLICATION_NAME}",
			                    environment: "${env.FASIT_ENV}",
			                    version    : "${env.VERSION_MAJOR}.${env.VERSION_MINOR}.${env.BUILD_ID}",
			                    username   : "${env.NAIS_USERNAME}",
			                    password   : "${env.NAIS_PASSWORD}",
			                    zone       : "${env.ZONE}",
			                    namespace  : "${env.APPLICATION_NAMESPACE}"
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
				}
			}
		}
	}
	post {
        always {
			junit 'target/surefire-reports/*.xml'
			junit 'target/failsafe-reports/*.xml'
        	archive 'target/*.jar'
        	archive 'preprod.truststore.jks'
			deleteDir()
			script {
				if (currentBuild.result == 'ABORTED') {
					slackSend color: "warning", message: "[ABORTED] ${slackMessage} :confusedparrot:"
				}
			}
        }
        success {
        	slackSend color: "good", message: "[SUCCESS] ${slackMessage} :feelsrareman:"
        }
		failure {
			deleteDir()
			slackSend color: "danger", message: "[FAILURE] ${slackMessage} :feelsohwait:"
		}
    }
}

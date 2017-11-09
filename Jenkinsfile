#!/usr/bin/env groovy

pipeline {
	agent any
	tools {
		maven 'default'
	}
	environment {
		LDAP_URL="ldapgw.test.local"
	}
	stages {
		stage('build') {
			steps {
				//sh 'openssl req -new -x509 -keyout ca-key -out ca-cert -days 365 -nodes -subj "/C=NO/ST=Oslo/L=Oslo/O=NAV/OU=NAV IT/CN=test.local"'
				//sh 'keytool -keystore kafka.client.truststore.jks -alias CARoot -import -file ca-cert'
				sh 'keytool -genkeypair -alias CARoot -keystore kafka.client.truststore.jks -storepass password -keypass password -dname "C=NO, ST=Oslo, L=Oslo, O=NAV, OU=NAV IT, CN=test.local" -deststoretype pkcs12'
				sh 'mvn -B -DskipTests clean package'
			}
		}
		stage('test') {
			steps {
				sh 'mvn test'
				junit 'target/surefire-reports/*.xml'
			}
		}
		stage('deploy docker image') {
			steps {
				script {
					checkout scm
					docker.withRegistry('https://docker.adeo.no:5000/') {
						def image = docker.build("integrasjon/altinnkanal:${env.BUILD_ID}")
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
						sh "nais upload --app altinnkanal-2 -v 1.0.0"
					}
				}
			}
		}
		/*stage('deploy to nais') {
			steps {
				script {
   					withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: 'nais-user', usernameVariable: "NAIS_USERNAME", passwordVariable: "NAIS_PASSWORD"]]) {
			            def postBody = [
			                    application: "altinnkanal-2",
			                    environment: "preprod",
			                    version    : "1.0.0",
			                    username   : "${env.NAIS_USERNAME}",
			                    password   : "${env.NAIS_PASSWORD}",
			                    zone       : "FSS",
			                    namespace  : "default"
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
		}*/
	}
	post {
        always {
			deleteDir()
        }
        success {
        	archive 'target/*.jar'
        }
    }
}
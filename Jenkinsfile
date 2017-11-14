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
				script {
					sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot\" -import -file certs/preprod/B27_issuing_intern.crt -noprompt"
					sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot2\" -import -file certs/preprod/B27_root_ca.crt -noprompt"
					sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot3\" -import -file certs/preprod/B27_issuing.crt -noprompt"
					sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot4\" -import -file certs/preprod/B27_sub_ca.crt -noprompt"
				}
				sh 'mvn -B -DskipTests clean package'
			}
		}
		/*stage('test') {
			steps {
				sh 'mvn test'
				junit 'target/surefire-reports/*.xml'
			}
		}*/
		stage('deploy docker image') {
			steps {
				script {
					checkout scm
					docker.withRegistry('https://docker.adeo.no:5000/') {
						def image = docker.build("integrasjon/altinnkanal:1.0.${env.BUILD_ID}")
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
						sh "nais upload --app altinnkanal-2 -v 1.0.${env.BUILD_ID}"
					}
				}
			}
		}
		stage('deploy to nais') {
			steps {
				script {
   					withCredentials([[$class: "UsernamePasswordMultiBinding", credentialsId: 'nais-user', usernameVariable: "NAIS_USERNAME", passwordVariable: "NAIS_PASSWORD"]]) {
			            def postBody = [
			                    application: "altinnkanal-2",
			                    environment: "q4",
			                    version    : "1.0.${env.BUILD_ID}",
			                    username   : "${env.NAIS_USERNAME}",
			                    password   : "${env.NAIS_PASSWORD}",
			                    zone       : "fss",
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
		}
	}
	post {
        always {
        	archive 'target/*.jar'
        	archive 'preprod.truststore.jks'
			deleteDir()
        }
    }
}

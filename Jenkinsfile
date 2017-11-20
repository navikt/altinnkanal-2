#!/usr/bin/env groovy



pipeline {
	agent any
	tools {
		maven 'default'
	}
	environment {
		APPLICATION_NAME='altinnkanal-2'
		FASIT_ENV='t4'
		VERSION_MAJOR='1'
		VERSION_MINOR='0'
		NAIS_CREDENTIALS_ID='nais-user' // refers to Jenkins credentials id
		ZONE='fss'
		APPLICATION_NAMESPACE='default'

		LDAP_URL='ldap://ldapgw.test.local'
		LDAP_USER_BASEDN='ou=NAV,ou=BusinessUnits,dc=test,dc=local'
		SPRING_DATASOURCE_PASSWORD='root'
		SPRING_DATASOURCE_URL='jdbc:mysql://localhost/altinnkanal'
	}
	stages {
		stage('build') {
			steps {
				script {
					sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot\" -import -file certs/preprod/B27_issuing_intern.crt -noprompt"
					//sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot2\" -import -file certs/preprod/B27_root_ca.crt -noprompt"
					//sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot3\" -import -file certs/preprod/B27_issuing.crt -noprompt"
					//sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot4\" -import -file certs/preprod/B27_sub_ca.crt -noprompt"
					sh "keytool -keystore preprod.truststore.jks -storepass password -keypass password -alias \"CARoot2\" -import -file certs/preprod/D26_issuing_intern.crt -noprompt"
				}
				sh 'mvn -B -DskipTests clean package'
			}
		}
		stage('test') {
			steps {
				sh 'mvn verify'
				junit 'target/surefire-reports/*.xml'
				junit 'target/failsafe-reports/*.xml'
			}
		}
		stage('deploy docker image') {
			steps {
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
        	archive 'target/*.jar'
        	archive 'preprod.truststore.jks'
			deleteDir()
        }
    }
}

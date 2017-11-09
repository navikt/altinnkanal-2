#!/usr/bin/env groovy

pipeline {
	agent any
	tools {
		maven 'default'
	}
	stages {
		stage('build') {
			steps {
				sh 'mvn -B -DskipTests clean package'
			}
		}
		stage('test') {
			steps {
				sh 'mvn test'
			}
		}
		stage('deploy') {
			steps {
				// build and push docker image
				script {
					checkout scm
					docker.withRegistry('https://docker.adeo.no:5000/') {
						def image = docker.build("integrasjon/altinnkanal:${env.BUILD_ID}")
						image.push 'latest'
					}
				}
				// curl POST to naisd? which cluster? environment?
				// more steps? secrets, env vars, Fasit resources, etc.
			}
		}
	}
	post {
        always {
			archive 'target/*.jar'
			junit 'target/surefire-reports/*.xml'
			deleteDir()
        }
    }
}
#!/usr/bin/env groovy

pipeline {
	agent {
		docker {
			image 'maven:3.5.2-jdk-8-alpine'
			args '-v $HOME/.m2:/root/.m2' // TODO: Add custom settings.xml to Jenkins host directory; ${HOME}
		}
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
			// only run the whole pipeline on push to master branch? i.e. only run the previous stages for pull requests in other branches
			when {
				branch 'master'
			}
			steps {
				// build and push docker image
				script {
					checkout scm
					// ref https://jenkins.io/doc/book/pipeline/docker/#custom-registry
					docker.withRegistry('https://docker.adeo.no:5000', 'credentials-id') {
						def image = docker.build("altinnkanal:${env.BUILD_ID}")
						image.push()
					}
				}

				// use spotify dockerfile-maven plugin instead? i.e. mvn deploy -> need to add credentials/settings for registry
				// enables deployment using maven, although it doesn't work on windows...
				// sh 'mvn deploy'

				// curl POST to naisd? which cluster? environment?
				// more steps? secrets, env vars, Fasit resources, etc.
			}
		}
	}
	post {
        always {
            echo 'This will always run'
			archive 'target/*.jar'
			junit 'target/surefire-reports/*.xml'
			deleteDir()
        }
        success {
            echo 'This will run only if successful'
        }
        failure {
            echo 'This will run only if failed'
			// notify by slack/email/other? ditto for other post states.
        }
        unstable {
            echo 'This will run only if the run was marked as unstable'
        }
        changed {
            echo 'This will run only if the state of the Pipeline has changed'
            echo 'For example, if the Pipeline was previously failing but is now successful'
        }
    }
}
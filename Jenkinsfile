pipeline {
    agent any
    tools {
            jdk 'JDK21'
    }

    environment {
            TAG = "${env.BUILD_NUMBER}"
            DOCKER_IMAGE_NAME = 'goorm-space/MUNOVA-api'
//             DOCKER_CREDENTIALS_ID = 'dockerhub-access'
            WEBHOOK_URL = credentials("MUNOVA-Jenkins-webhook")
        }


    stages {
        stage('Checkout') {
            steps {
                git branch: 'dev',
                    url: 'https://github.com/goorm-space/MUNOVA.git',
                    credentialsId: 'MUNOVA-Access-Token'
            }
        }

        stage('Copy application.properties') {
            steps {
                withCredentials([file(credentialsId: 'MUNOVA-SECRET', variable: 'SECRET_FILE')]) {
                    script {
                         sh '''
                            rm -f src/main/resources/application.properties
                            mkdir -p src/main/resources
                            cp $SECRET_FILE src/main/resources/application.properties
                         '''
                    }
                }
            }
        }

        stage('Build') {
            steps {
                // Gradle clean build
                sh './gradlew clean build'
            }
        }

//         stage('Docker Build') {
//             steps {
//                 script {
//                     // Docker 이미지 생성
//                     def dockerImageVersion = "${env.BUILD_NUMBER}"
//                     sh "docker build --no-cache -t ${DOCKER_IMAGE_NAME}:${dockerImageVersion} ./"
//                     sh "docker image inspect ${DOCKER_IMAGE_NAME}:${dockerImageVersion}"
//                 }
//             }
//         }
    }

    post {
        success {
            discordSend description: "빌드가 성공했습니다! ✅",
            footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
            link: env.BUILD_URL,
            result: currentBuild.currentResult,
            title: "Jenkins CI/CD - 성공",
            webhookURL: env.WEBHOOK_URL
        }
        failure {
            discordSend description: "빌드가 실패했습니다! ❌",
            footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
            link: env.BUILD_URL,
            result: currentBuild.currentResult,
            title: "Jenkins CI/CD - 실패",
            webhookURL: env.WEBHOOK_URL
        }
    }
}
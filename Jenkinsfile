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
    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'mergeTo', value: '$.pull_request.base.ref', defaultValue: 'null'],
                [key: 'mergeFrom', value: '$.pull_request.head.ref', defaultValue: 'null'],
                [key: 'prHtmlLink', value: '$.pull_request.html_url', defaultValue: 'null']
            ],
            tokenCredentialId: 'MUNOVA-webhook-secret',
            regexpFilterText: '$mergeTo',
            regexpFilterExpression: '.*'  // 모든 이벤트 허용, Pipeline에서 머지 여부 체크
        )
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
           script {
               def commitMsg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
               def gitUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
               def commitHash = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
               def commitUrl = gitUrl.replace('.git','') + "/commit/" + commitHash

               discordSend(
                   webhookURL: env.WEBHOOK_URL,
                   description: "빌드가 성공했습니다! ✅\n커밋 메시지: ${commitMsg}\n[커밋 바로가기](${commitUrl})",
                   title: "Jenkins CI/CD - 성공",
                   footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                   link: env.BUILD_URL,
                   result: currentBuild.currentResult
               )
           }
       }
       failure {
           script {
               def commitMsg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
               def gitUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
               def commitHash = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
               def commitUrl = gitUrl.replace('.git','') + "/commit/" + commitHash

               discordSend(
                   webhookURL: env.WEBHOOK_URL,
                   description: "빌드가 실패했습니다! ❌\n커밋 메시지: ${commitMsg}\n[커밋 바로가기](${commitUrl})",
                   title: "Jenkins CI/CD - 실패",
                   footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                   link: env.BUILD_URL,
                   result: currentBuild.currentResult
               )
           }
       }
   }
}
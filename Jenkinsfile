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
           script {
               // 커밋 메시지 및 URL
               def commitMsg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
               def commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
               def commitUrl = "${env.GIT_URL.replace('.git','')}/commit/${commitHash}"

               // 머지 브랜치 정보
               def headBranch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
               def baseBranch = env.GIT_BRANCH ?: 'unknown'

               // PR 스타일 표시
               def prDisplay = "merge : ${headBranch} ➡️ ${baseBranch}"
               def prUrl = "${env.GIT_URL.replace('.git','')}/compare/${baseBranch}...${headBranch}"

               discordSend(
                   webhookURL: env.WEBHOOK_URL,
                   description: "빌드가 성공했습니다! ✅\n커밋 메시지: ${commitMsg}\n[커밋 바로가기](${commitUrl})\n${prDisplay}\n[머지 결과 바로가기](${prUrl})",
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
               def commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
               def commitUrl = "${env.GIT_URL.replace('.git','')}/commit/${commitHash}"

               def headBranch = sh(script: "git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
               def baseBranch = env.GIT_BRANCH ?: 'unknown'

               def prDisplay = "merge : ${headBranch} ➡️ ${baseBranch}"
               def prUrl = "${env.GIT_URL.replace('.git','')}/compare/${baseBranch}...${headBranch}"

               discordSend(
                   webhookURL: env.WEBHOOK_URL,
                   description: "빌드가 실패했습니다! ❌\n커밋 메시지: ${commitMsg}\n[커밋 바로가기](${commitUrl})\n${prDisplay}\n[머지 결과 바로가기](${prUrl})",
                   title: "Jenkins CI/CD - 실패",
                   footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                   link: env.BUILD_URL,
                   result: currentBuild.currentResult
               )
           }
       }
   }
}
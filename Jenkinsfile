pipeline {
    agent any
    tools {
        jdk 'JDK21'
    }

    environment {
        TAG = "${env.BUILD_NUMBER}"
        DOCKER_IMAGE_NAME = 'goorm-space/MUNOVA-api'
        WEBHOOK_URL = credentials("MUNOVA-Jenkins-webhook")
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'devtest',
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
                sh './gradlew clean build'
            }
        }
    }

    post {
        success {
            script {
                // 커밋 메시지 및 URL
                def commitMsg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                def commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                def commitUrl = "${env.GIT_URL.replace('.git','')}/commit/${commitHash}"

                def sourceBranch = env.CHANGE_BRANCH ?: "unknown"
                def targetBranch = env.CHANGE_TARGET ?: "unknown"

                def prId = env.CHANGE_ID ?: "unknown"
                def prUrl = "https://github.com/goorm-space/MUNOVA/pull/${prId}"
                def prDisplay = "merge : ${sourceBranch} ➡️ ${targetBranch}"

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

                def sourceBranch = env.CHANGE_BRANCH ?: "unknown"
                def targetBranch = env.CHANGE_TARGET ?: "unknown"
                def prId = env.CHANGE_ID ?: "unknown"
                def prUrl = "https://github.com/goorm-space/MUNOVA/pull/${prId}"

                def prDisplay = "merge : ${sourceBranch} ➡️ ${targetBranch}"

                discordSend(
                    webhookURL: env.WEBHOOK_URL,
                    description: "빌드가 실패했습니다! ❌\n커밋 메시지 : ${commitMsg}\n[커밋 바로가기](${commitUrl})\n${prDisplay}\n[머지 결과 바로가기](${prUrl})",
                    title: "Jenkins CI/CD - 실패",
                    footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                    link: env.BUILD_URL,
                    result: currentBuild.currentResult
                )
            }
        }
    }
}
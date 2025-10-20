import groovy.json.JsonOutput
import groovy.json.JsonSlurper

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

    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'repository', value: '$.repository.name', defaultValue: 'null'],
                [key: 'repositoryLink', value: '$.repository.html_url', defaultValue: 'null'],
                [key: 'action', value: '$.action', defaultValue: 'null'],
                [key: 'prIsMerged', value: '$.pull_request.merged', defaultValue: 'false'],
                [key: 'prNumber', value: '$.pull_request.number', defaultValue: '0'],
                [key: 'prHtmlLink', value: '$.pull_request.html_url', defaultValue: 'null'],
                [key: 'prTitle', value: '$.pull_request.title', defaultValue: 'null'],
                [key: 'prRequester', value: '$.pull_request.user.login', defaultValue: 'null'],
                [key: 'prLabelList', value: '$.pull_request.labels', defaultValue: '[]'],
                [key: 'mergeTo', value: '$.pull_request.base.ref', defaultValue: 'null'],
                [key: 'mergeFrom', value: '$.pull_request.head.ref', defaultValue: 'null']
            ],
            token: "${env.pr_token}",
            tokenCredentialId: 'MUNOVA-Github-Webhook-Token',
            printContributedVariables: false,
            printPostContent: false,
            silentResponse: false,
            causeString: 'Triggered by GitHub Pull Request',
            regexpFilterText: '$action',
            regexpFilterExpression: '^(opened|reopened|synchronize|closed)$'
        )
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
                    sh '''
                        rm -f src/main/resources/application.properties
                        mkdir -p src/main/resources
                        cp $SECRET_FILE src/main/resources/application.properties
                    '''
                }
            }
        }

        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }

        stage('Send Discord Notification (PR Info)') {
            when {
                expression { return env.action != null && env.action != "null" }
            }
            steps {
                script {
                    def jsonSlurper = new JsonSlurper()
                    def labelObjects = []
                    try {
                        labelObjects = jsonSlurper.parseText(prLabelList)
                    } catch (e) {
                        echo "No labels found"
                    }

                    def labels = labelObjects.collect { it.name }.join(', ') ?: '없음'
                    def emoji = ':fire:'
                    def status = ''

                    switch(action) {
                        case 'opened': emoji = ':sparkles:'; status = 'Pull Request 생성'; break
                        case 'reopened': emoji = ':recycle:'; status = 'Pull Request 재오픈'; break
                        case 'synchronize': emoji = ':arrows_counterclockwise:'; status = 'PR 내용 업데이트'; break
                        case 'closed':
                            if (prIsMerged == "true") {
                                emoji = ':tada:'; status = '병합 완료'
                            } else {
                                emoji = ':x:'; status = 'PR 닫힘'
                            }
                            break
                    }

                    def message = """
${emoji} **${status}**
> 📘 [#${prNumber}](${prHtmlLink}) ${prTitle}
> 👤 작성자: ${prRequester}
> 🧩 라벨: ${labels}
> 🔀 브랜치: ${mergeFrom} → ${mergeTo}
> 📦 저장소: [${repository}](${repositoryLink})
"""

                    discordSend(
                        webhookURL: env.WEBHOOK_URL,
                        title: "GitHub Pull Request 알림",
                        description: message.trim(),
                        footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                        result: currentBuild.currentResult
                    )
                }
            }
        }
    }

    post {
        success {
            script {
                def commitMsg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
                def commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                def commitUrl = "${env.GIT_URL.replace('.git','')}/commit/${commitHash}"

                discordSend(
                    webhookURL: env.WEBHOOK_URL,
                    title: "빌드 성공 ✅",
                    description: "커밋 메시지: ${commitMsg}\n[커밋 바로가기](${commitUrl})",
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

                discordSend(
                    webhookURL: env.WEBHOOK_URL,
                    title: "빌드 실패 ❌",
                    description: "커밋 메시지: ${commitMsg}\n[커밋 바로가기](${commitUrl})",
                    footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                    link: env.BUILD_URL,
                    result: currentBuild.currentResult
                )
            }
        }
    }
}
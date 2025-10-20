import groovy.json.JsonSlurper

properties([
    pipelineTriggers([
        [
            $class: 'GenericTrigger',
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
            tokenCredentialId: 'MUNOVA-Github-Webhook-Token',
            printContributedVariables: true,
            printPostContent: false,
            regexpFilterText: '$action',
            regexpFilterExpression: '^(opened|reopened|synchronize|closed)$',
            causeString: 'Triggered by GitHub Pull Request'
        ]
    ])
])

node {

    stage('Print Webhook Variables') {
        steps {
            script {
                echo "===== 🔔 GitHub Webhook Variables ====="
                echo "repository: ${env.repository}"
                echo "repositoryLink: ${env.repositoryLink}"
                echo "action: ${env.action}"
                echo "prIsMerged: ${env.prIsMerged}"
                echo "prNumber: ${env.prNumber}"
                echo "prHtmlLink: ${env.prHtmlLink}"
                echo "prTitle: ${env.prTitle}"
                echo "prRequester: ${env.prRequester}"
                echo "prLabelList: ${env.prLabelList}"
                echo "mergeTo: ${env.mergeTo}"
                echo "mergeFrom: ${env.mergeFrom}"
                echo "======================================="
            }
        }
    }
    stage('Setup') {
        env.TAG = "${env.BUILD_NUMBER}"
        env.DOCKER_IMAGE_NAME = 'goorm-space/MUNOVA-api'
        env.WEBHOOK_URL = credentials('MUNOVA-Jenkins-webhook')
    }

    stage('Checkout') {
        git branch: 'devtest',
            url: 'https://github.com/goorm-space/MUNOVA.git',
            credentialsId: 'MUNOVA-Access-Token'
    }

    stage('Copy application.properties') {
        withCredentials([file(credentialsId: 'MUNOVA-SECRET', variable: 'SECRET_FILE')]) {
            sh '''
                rm -f src/main/resources/application.properties
                mkdir -p src/main/resources
                cp $SECRET_FILE src/main/resources/application.properties
            '''
        }
    }

    stage('Build') {
        sh './gradlew clean build'
    }

    stage('Discord PR Notification') {
        script {
            if (env.action && env.action != "null") {
                def jsonSlurper = new JsonSlurper()
                def labelObjects = []
                try {
                    labelObjects = jsonSlurper.parseText(env.prLabelList)
                } catch (e) {
                    echo "No labels found"
                }

                def labels = labelObjects.collect { it.name }.join(', ') ?: '없음'
                def emoji = ':fire:'
                def status = ''

                switch(env.action) {
                    case 'opened': emoji = ':sparkles:'; status = 'Pull Request 생성'; break
                    case 'reopened': emoji = ':recycle:'; status = 'Pull Request 재오픈'; break
                    case 'synchronize': emoji = ':arrows_counterclockwise:'; status = 'PR 내용 업데이트'; break
                    case 'closed':
                        if (env.prIsMerged == "true") {
                            emoji = ':tada:'; status = '병합 완료'
                        } else {
                            emoji = ':x:'; status = 'PR 닫힘'
                        }
                        break
                }

                def message = """
${emoji} **${status}**
> 📘 [#${env.prNumber}](${env.prHtmlLink}) ${env.prTitle}
> 👤 작성자: ${env.prRequester}
> 🧩 라벨: ${labels}
> 🔀 브랜치: ${env.mergeFrom} → ${env.mergeTo}
> 📦 저장소: [${env.repository}](${env.repositoryLink})
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

    stage('Post Build Notification') {
        script {
            def commitMsg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()
            def commitHash = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
            def commitUrl = "${env.GIT_URL.replace('.git','')}/commit/${commitHash}"

            if (currentBuild.currentResult == 'SUCCESS') {
                discordSend(
                    webhookURL: env.WEBHOOK_URL,
                    title: "빌드 성공 ✅",
                    description: "커밋 메시지: ${commitMsg}\n[커밋 바로가기](${commitUrl})",
                    footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                    link: env.BUILD_URL,
                    result: currentBuild.currentResult
                )
            } else {
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
import groovy.json.JsonSlurper

pipeline {
    agent any

    environment {
        jobNum = "${BUILD_NUMBER}"
        buildUrl = "${BUILD_URL}console"
        jobName = "${JOB_NAME}"
        resultMsg = "success"
        WEBHOOK_URL = credentials("MUNOVA-Jenkins-webhook") // Discord Webhook
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
                [key: 'prReviewLink', value: '$.review.html_url', defaultValue: 'null'],
                [key: 'prLabelList', value: '$.pull_request.labels', defaultValue: 'null'],
                [key: 'mergeTo', value: '$.pull_request.base.ref', defaultValue: 'null'],
                [key: 'mergeFrom', value: '$.pull_request.head.ref', defaultValue: 'null']
            ],
            tokenCredentialId: 'tadak-github-pull-request',
            printContributedVariables: false,
            printPostContent: false,
            silentResponse: false,
            causeString: 'using GitHub webhook',
            regexpFilterText: '$action',
            regexpFilterExpression: '^(review_requested|opened|reopened|closed|submitted|synchronize)$'
        )
    }

    stages {

        stage('PR Request Message') {
            when {
                expression {
                    action == "review_requested" || action == "opened" || action == "reopened" || action == "closed"
                }
            }
            steps {
                script {
                    if(prIsMerged != "true") {
                        resultMsg = headerMessage()
                    }
                }
            }
        }

        stage('PR Review Message') {
            when {
                expression { action == "submitted" }
            }
            steps {
                script {
                    def headerMsg = headerMessage()
                    def reviewMsg = ":star: 코드리뷰 완료! <${prReviewLink} | (확인)>"
                    resultMsg = "${headerMsg}\n${reviewMsg}"
                }
            }
        }

        stage('PR Merge Message') {
            when {
                expression { action == "synchronize" || prIsMerged == "true" }
            }
            steps {
                script {
                    def headerMsg = headerMessage()
                    def barcnhUrl = "${repositoryLink}/tree"
                    def mergeMsg = ":star2: <${barcnhUrl}/${mergeTo} | ${mergeTo}> merged by <${barcnhUrl}/${mergeFrom} | ${mergeFrom}>"
                    if(mergeFrom != "develop") {
                        mergeMsg += "\n:bomb: ${mergeFrom} - branch 삭제 바랍니다"
                    }
                    resultMsg = "${headerMsg}\n${mergeMsg}"
                }
            }
        }

        // ===========================
        // 실제 빌드 프로세스
        // ===========================

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
    }

    post {
        success {
            script{
                sendDiscordMessage("${resultMsg}", "good")
            }
        }
        failure {
            script{
                def errorMessage = "${jobName} - #${jobNum} 빌드오류!!! <${buildUrl} | (확인)>"
                sendDiscordMessage(errorMessage, "danger")
            }
        }
    }
}

// ===============================
// Discord 전송용 함수
// ===============================

def sendDiscordMessage(message, color) {
    def discordColor = (color == "good") ? 65280 : 16711680 // 초록: success, 빨강: fail
    discordSend(
        webhookURL: env.WEBHOOK_URL,
        description: message,
        color: discordColor
    )
}

// ===============================
// Helper Functions
// ===============================

def getLabels() {
    def jsonSlurper = new JsonSlurper()
    def labelObjects = jsonSlurper.parseText("${prLabelList}")
    return labelObjects.collect { label ->
        label.description ?: ''
    }.join(' / ')
}

def getUserName() {
    def name = "<!here>"
    switch("${prRequester}") {
        case 'jeondoh':
            name = '<@U058YT0AJ9M>'
            break;
        case 'ahnsozero':
            name = '<@U059DAA1ZL3>'
            break;
        case 'itmdeveloper':
            name = '<@U058YTJSFHD>'
            break;
    }
    return name;
}

def headerMessage() {
    def pjNm = "${repository} - <${buildUrl} | #${jobNum}> build Success"
    def emoji = ":open_hands:"
    def prStr = "Pull-Request "

    switch("${action}") {
        case 'opened':
        case 'review_requested':
            emoji = ":fire:"
            prStr += "요청"
            break;
        case 'reopened':
            emoji = ":recycle:"
            prStr += "재오픈"
            break;
        case 'closed':
            if (prIsMerged == "true") {
                emoji = ':tada:'
                prStr += "병합완료"
                break;
            }
            emoji = ':no_entry_sign:'
            prStr += "닫음"
            break;
        case 'submitted':
            emoji = ':book:'
            prStr += "코드리뷰"
            break;
        case 'synchronize':
            emoji = ':tada:'
            prStr += "병합완료"
            break;
    }

    def labels = getLabels()
    def userName = getUserName()
    return "${pjNm}\n\n${emoji} ${prStr}\n\n:bookmark: <${prHtmlLink} | #${prNumber}> ${prTitle} (${labels}) by ${userName}"
}
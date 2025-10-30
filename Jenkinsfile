pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    triggers {
        GenericTrigger(
            genericVariables: [
                // 저장소명
                [key: 'repository', value: '$.repository.name', defaultValue: 'null'],
                // 저장소 URL
                [key: 'repositoryLink', value: '$.repository.html_url', defaultValue: 'null'],
                // pr 상태
                [key: 'action', value: '$.action', defaultValue: 'null'],
                // pr merged 여부
                [key: 'prIsMerged', value: '$.pull_request.merged', defaultValue: 'false'],
                // pr 번호
                [key: 'prNumber', value: '$.pull_request.number', defaultValue: '0'],
                // pr 링크
                [key: 'prHtmlLink', value: '$.pull_request.html_url', defaultValue: 'null'],
                // pr 제목
                [key: 'prTitle', value: '$.pull_request.title', defaultValue: 'null'],
                // pr 요청자
                [key: 'prRequester', value: '$.pull_request.user.login', defaultValue: 'null'],
                // merge 대상 브런치
                [key: 'mergeTo', value: '$.pull_request.base.ref', defaultValue: 'null'],
                // merge from
                [key: 'mergeFrom', value: '$.pull_request.head.ref', defaultValue: 'null'],
            ],
            tokenCredentialId: 'MUNOVA-jenkins-Hook',
            regexpFilterText: '$prIsMerged',
            regexpFilterExpression: '^true$'
        )
    }


    tools {
           jdk 'JDK21'
    }

    environment {
            TAG = "${env.BUILD_NUMBER}"
            APP_NAME     = "MUNOVA-api"
            JAR_NAME     = "munova-${env.BUILD_NUMBER}.jar"
            ZIP_NAME     = "munova-${env.BUILD_NUMBER}.zip"
            S3_BUCKET    = 'munova-be-bucket'
            IMAGE_NAME    = "goorm-space/munova-api"

            // S3 접속 크레덴셜
            AWS_ACCESS_KEY = credentials('aws_access_credential')
            AWS_SECRET_KEY = credentials('aws_access_credential')

            // Generic Trigger에서 넘어온 값을 빌드 환경 변수로 확실히 저장
            ENV_PR_TITLE = "${prTitle}"
            ENV_PR_NUMBER = "${prNumber}"
            ENV_MERGE_FROM = "${mergeFrom}"
            ENV_MERGE_TO = "${mergeTo}"
            ENV_PR_HTML_LINK = "${prHtmlLink}"

            WEBHOOK_DISORD_URL = credentials("MUNOVA-dico-Hook")
            SECRET_FILE = credentials('MUNOVA_APPLICATION_PROPERTIES') // application.properties
        }


    stages {

        stage('Checkout') {
            steps {
                git branch: 'deploy',
                    url: 'https://github.com/goorm-space/MUNOVA.git',
                    credentialsId: 'MUNOVA-Access-Token'
            }
        }

        stage('Prepare application.properties') {
            steps {
                withCredentials([file(credentialsId: 'MUNOVA_APPLICATION_PROPERTIES', variable: 'SECRET_FILE')]) {
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

        stage('Build Jar') {
            steps {
                // Gradle clean build
                sh './gradlew clean build'
            }
        }

        stage('Docker Build') {
            steps {
                script {
                    sh """
                        docker build --no-cache -t ${IMAGE_NAME}:${TAG} .
                    """
                }
            }
        }
        stage('Save & Zip Docker Image') {
            steps {
                script {
                    sh """
                        docker save -o ${DOCKER_TAR} ${IMAGE_NAME}:${TAG}
                        zip -r ${ZIP_NAME} ${DOCKER_TAR}
                    """
                }
            }
        }

//         stage('Package for CodeDㅇㅇㅇeploy') {
//             steps {
//                 sh """
//                     mkdir -p deploy
//                     cp build/libs/*.jar deploy/${JAR_NAME}
//                     cd deploy
//                     zip -r ${ZIP_NAME} ${JAR_NAME}
//                 """
//             }
//         }

        stage('Upload to S3') {
            steps {
                sh '''
                    # 환경 변수 설정
                    export AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY
                    export AWS_SECRET_ACCESS_KEY=$AWS_SECRET_KEY

                    # S3 업로드 예시
                    aws s3 cp my-docker-image.tar s3://my-bucket/
                '''
            }
        }

        post {
            success { notifyDiscord("빌드가 성공했습니다! ✅") }
            failure { notifyDiscord("빌드가 실패했습니다! ❌") }
        }
    }

   post {
       success {
           script {
               def gitUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
               def commitHash = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
               def commitUrl = gitUrl.replace('.git','') + "/commit/" + commitHash

               def fromTo = "Merge From: ${env.ENV_MERGE_FROM} ➡️ Merge To: ${env.ENV_MERGE_TO}"
               def prInfo = prHtmlLink != "null" ? "<${env.ENV_PR_HTML_LINK} | PR #${env.ENV_PR_NUMBER}>" : "PR 없음"

               // 최종 메시지
               def finalMsg = """빌드가 성공했습니다! ✅

                   PR 제목: ${env.ENV_PR_TITLE}

                   커밋 바로가기: ${commitUrl}

                   ${fromTo}

                   PR 링크: ${prInfo}"""

               discordSend(
                   webhookURL: env.WEBHOOK_URL,
                   description: finalMsg,
                   title: "Jenkins CI/CD - 성공",
                   footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                   link: env.BUILD_URL,
                   result: currentBuild.currentResult
               )
           }
       }
       failure {
           script {
               def gitUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
               def commitHash = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
               def commitUrl = gitUrl.replace('.git','') + "/commit/" + commitHash

               def fromTo = "Merge From: ${env.ENV_MERGE_FROM} ➡️ Merge To: ${env.ENV_MERGE_TO}"
               def prInfo = prHtmlLink != "null" ? "<${env.ENV_PR_HTML_LINK} | PR #${env.ENV_PR_NUMBER}>" : "PR 없음"

               // 최종 메시지
               def errorMessage = """빌드가 실패했습니다! ❌

                   PR 제목: ${env.ENV_PR_TITLE}

                   커밋 바로가기: ${commitUrl}

                   ${fromTo}

                   PR 링크: ${prInfo}"""

               discordSend(
                   webhookURL: env.WEBHOOK_URL,
                   description: errorMessage,
                   title: "Jenkins CI/CD - 실패",
                   footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                   link: env.BUILD_URL,
                   result: currentBuild.currentResult
               )
           }
       }
   }
}
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
               def gitUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
               def commitHash = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
               def commitUrl = gitUrl.replace('.git','') + "/commit/" + commitHash

               def fromTo = "Merge From: ${env.ENV_MERGE_FROM} ➡️ Merge To: ${env.ENV_MERGE_TO}"
               def prInfo = prHtmlLink != "null" ? "<${env.ENV_PR_HTML_LINK} | PR #${env.ENV_PR_NUMBER}>" : "PR 없음"

               // 최종 메시지
               def finalMsg = """빌드가 성공했습니다! ✅

                   PR 제목: ${env.ENV_PR_TITLE}

                   커밋 바로가기: ${commitUrl}

                   ${fromTo}

                   PR 링크: ${prInfo}"""

               discordSend(
                   webhookURL: env.WEBHOOK_URL,
                   description: finalMsg,
                   title: "Jenkins CI/CD - 성공",
                   footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                   link: env.BUILD_URL,
                   result: currentBuild.currentResult
               )
           }
       }
       failure {
           script {
               def gitUrl = sh(script: "git config --get remote.origin.url", returnStdout: true).trim()
               def commitHash = sh(script: "git rev-parse HEAD", returnStdout: true).trim()
               def commitUrl = gitUrl.replace('.git','') + "/commit/" + commitHash

               def fromTo = "Merge From: ${env.ENV_MERGE_FROM} ➡️ Merge To: ${env.ENV_MERGE_TO}"
               def prInfo = prHtmlLink != "null" ? "<${env.ENV_PR_HTML_LINK} | PR #${env.ENV_PR_NUMBER}>" : "PR 없음"

               // 최종 메시지
               def errorMessage = """빌드가 실패했습니다! ❌

                   PR 제목: ${env.ENV_PR_TITLE}

                   커밋 바로가기: ${commitUrl}

                   ${fromTo}

                   PR 링크: ${prInfo}"""

               discordSend(
                   webhookURL: env.WEBHOOK_URL,
                   description: errorMessage,
                   title: "Jenkins CI/CD - 실패",
                   footer: "Job: ${env.JOB_NAME} | Build #${env.BUILD_NUMBER}",
                   link: env.BUILD_URL,
                   result: currentBuild.currentResult
               )
           }
       }
   }
}
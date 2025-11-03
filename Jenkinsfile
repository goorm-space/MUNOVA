pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }
    
    triggers {
        GenericTrigger(
            genericVariables: [
                [key: 'action', value: '$.action', defaultValue: 'none'],
                [key: 'repository', value: '$.repository.full_name', defaultValue: 'null'],
                [key: 'repositoryLink', value: '$.repository.html_url', defaultValue: 'null'],
                [key: 'prIsMerged', value: '$.pull_request.merged', defaultValue: 'false'],
                [key: 'prNumber', value: '$.pull_request.number', defaultValue: '0'],
                [key: 'prHtmlLink', value: '$.pull_request.html_url', defaultValue: 'null'],
                [key: 'prTitle', value: '$.pull_request.title', defaultValue: 'null'],
                [key: 'prRequester', value: '$.pull_request.user.login', defaultValue: 'null'],
                [key: 'mergeTo', value: '$.pull_request.base.ref', defaultValue: 'null'],
                [key: 'mergeFrom', value: '$.pull_request.head.ref', defaultValue: 'null'],
            ],
            tokenCredentialId: 'MUNOVA-jenkins-Hook',
            regexpFilterText: '${action}',
            regexpFilterExpression: '^closed$'
        )
    }
    tools {
        jdk 'JDK21'
    }
    environment {
        TAG        = "${env.BUILD_NUMBER}"
        APP_NAME   = "MUNOVA-api"
        JAR_NAME   = "munova-${env.BUILD_NUMBER}.jar"
        ZIP_NAME   = "munova-${env.BUILD_NUMBER}.zip"
        DOCKER_TAR = "munova-${env.BUILD_NUMBER}.tar"
        S3_BUCKET  = 'munova-be-bucket'
        IMAGE_NAME = "goorm-space/munova-api"

        // AWS 크레덴셜
        AWS_ACCESS_KEY = credentials('aws_access_credential')
        AWS_SECRET_KEY = credentials('aws_access_credential')

        // EC2 크레덴셜
        EC2_ACCESS_PEM = credentials('munova-ec2-access-pem')

        // Git PR 정보
        ENV_PR_TITLE    = "${prTitle}"
        ENV_PR_NUMBER   = "${prNumber}"
        ENV_MERGE_FROM  = "${mergeFrom}"
        ENV_MERGE_TO    = "${mergeTo}"
        ENV_PR_HTML_LINK= "${prHtmlLink}"

        WEBHOOK_DISCORD_URL = credentials("MUNOVA-dico-Hook")
        SECRET_FILE         = credentials('MUNOVA_APPLICATION_PROPERTIES')
    }

    stages {
        stage('Clean Workspace') {
            steps {
                deleteDir() // workspace 안의 모든 파일 삭 제
            }
        }


        stage('Checkout') {
            steps {
                git branch: 'dev',
                    url: 'https://github.com/goorm-space/MUNOVA.git',
                    credentialsId: 'MUNOVA-Access-Token'
            }
        }

//          stage('Check Webhook Payload') {
//                 steps {
//                     echo "===== 🔍 Webhook Payload Debug Info ====="
// //                     echo "Action: ${params.action}"
//                     echo "Repository: ${repository}"
//                     echo "Repository Link: ${repositoryLink}"
//                     echo "PR Merged?: ${prIsMerged}"
//                     echo "PR Number: ${prNumber}"
//                     echo "PR Title: ${prTitle}"
//                     echo "PR Requester: ${prReq uester}"
//                     echo "PR Link: ${prHtmlLink}"
//                     echo "Merge From: ${mergeFrom}"
//                     echo "Merge To: ${mergeTo}"
//                     echo "========================================"
//                 }
//         }


        stage('Prepare application.properties') {
            steps {
                withCredentials([file(credentialsId: 'MUNOVA_APPLICATION_PROPERTIES', variable: 'SECRET_FILE')]) {
                    sh '''
                        rm -f src/main/resources/application.properties
                        mkdir -p src/main/resources
                        cp $SECRET_FILE src/main/resources/application.properties
                    '''
                }
            }
        }

        stage('Build Jar') {
            steps {
                sh './gradlew clean build'
            }
        }

        stage('Docker Build') {
            steps {
                sh "docker -v"
                sh "docker build --no-cache -t ${IMAGE_NAME}:${TAG} ."
            }
        }

        stage('Save & Zip Docker Image') {
             steps{
                sh """
                    docker save -o ${DOCKER_TAR} ${IMAGE_NAME}:${TAG}
                """
             }
        }

       stage('Upload to S3') {
           steps {
               withCredentials([[
                   $class: 'AmazonWebServicesCredentialsBinding',
                   credentialsId: 'aws_access_credential',
                   accessKeyVariable: 'AWS_ACCESS_KEY',
                   secretKeyVariable: 'AWS_SECRET_KEY'
               ]]) {
                   script {
                       echo "=== Start Upload Stage ==="

                       echo "AWS_ACCESS_KEY is set: ${env.AWS_ACCESS_KEY ? 'YES' : 'NO'}"
                       echo "AWS_SECRET_KEY is set: ${env.AWS_SECRET_KEY ? 'YES' : 'NO'}"

                       sh """
                           echo "Uploading ${env.DOCKER_TAR} to S3..."
                           aws s3 cp ${env.DOCKER_TAR} s3://${env.S3_BUCKET}/${env.DOCKER_TAR} --region ap-northeast-2
                       """
                       echo "✅ S3 업로드 완료: ${env.S3_BUCKET}/${env.DOCKER_TAR}"
                   }
               }
           }
       }

       stage('Deploy to EC2') {
           steps {
               withCredentials([file(credentialsId: 'munova-ec2-access-pem', variable: 'PEM_FILE')]) {
                   sh(script: '''
                       echo "🔑 Testing SSH connection..."
                       ssh -i "$PEM_FILE" -o StrictHostKeyChecking=no ubuntu@3.34.65.134 "echo Connected"
                       ssh -i "$PEM_FILE" -o StrictHostKeyChecking=no ubuntu@3.34.65.134 "bash /home/ubuntu/deploy/deploy.sh ${BUILD_NUMBER}"
                   ''')
               }
           }
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

                def finalMsg = """빌드가 성공했습니다! ✅
                                PR 제목: ${env.ENV_PR_TITLE}
                                커밋 바로가기: ${commitUrl}
                                ${fromTo}
                                PR 링크: ${prInfo}"""

                discordSend(
                    webhookURL: env.WEBHOOK_DISCORD_URL,
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

                def errorMessage = """빌드가 실패했습니다! ❌
                                        PR 제목: ${env.ENV_PR_TITLE}
                                        커밋 바로가기: ${commitUrl}
                                        ${fromTo}
                                        PR 링크: ${prInfo}"""

                discordSend(
                    webhookURL: env.WEBHOOK_DISCORD_URL,
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

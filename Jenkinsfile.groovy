pipeline {
    // Define no top-level agent. Each stage will define its own context.
    agent none

    environment {
        // Set your Docker Hub username.
        DOCKER_USER = 'ahmedatef1095'
        // Define the image tag using the build number for unique, traceable builds.
        IMAGE_TAG = "build-${BUILD_NUMBER}"
    }

    stages {
        // STAGE 1: Run all main tasks inside the correct Docker container environment.
        stage('Build, Scan, and Deploy') {
            agent {
                docker {
                    image 'cytopia/ansible-docker:2.10-0.3.3'
                    // Mount the host's Docker socket to allow running Docker commands from inside the container.
                    args '-v /var/run/docker.sock:/var/run/docker.sock'
                }
            }
            stages {
                stage('Checkout Code') {
                    steps {
                        checkout scm
                    }
                }

                stage('Build and Push') {
                    steps {
                        script {
                            withCredentials([string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD')]) {
                                echo "Building and pushing image: ${DOCKER_USER}/counter-app:${IMAGE_TAG}"
                                // Pass the unique image tag to the playbook
                                sh "ansible-playbook ansible/build-push.yml --extra-vars 'docker_user=${DOCKER_USER} docker_password=$DOCKER_PASSWORD image_tag=${IMAGE_TAG}'"
                            }
                        }
                    }
                }

                // DevSecOps: This stage is commented out as it requires a custom agent image with Trivy.
                // stage('Scan Image') {
                //     steps {
                //         echo "Scanning Docker image for vulnerabilities..."
                //         sh "trivy image --exit-code 1 --severity HIGH,CRITICAL ${DOCKER_USER}/counter-app:${IMAGE_TAG}"
                //     }
                // }

                stage('Deploy') {
                    steps {
                        script {
                            withCredentials([
                                string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD'),
                                sshUserPrivateKey(credentialsId: 'app-server-ssh-key', keyFileVariable: 'SSH_KEY_FILE', usernameVariable: 'SSH_USER')
                            ]) {
                                echo "Deploying image: ${DOCKER_USER}/counter-app:${IMAGE_TAG} to the app server."
                                // Pass the unique image tag to the deploy playbook
                                sh "ansible-playbook -i ansible/inventory ansible/deploy.yml --extra-vars 'docker_user=${DOCKER_USER} docker_password=$DOCKER_PASSWORD image_tag=${IMAGE_TAG}'"
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            // The 'post' section does not use an 'agent' directive.
            // Instead, we wrap our steps in a 'node' block to get a workspace.
            steps {
                echo 'Pipeline finished. Logging out from Docker Hub...'
                // This block now runs correctly within a 'node' context.
                withCredentials([string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD')]) {
                    // We must checkout the code again to access the logout playbook.
                    // This requires a workspace, which the 'node' block provides.
                    checkout scm
                    sh 'ansible-playbook ansible/logout.yml'
                }
            }
        }
    }
}
pipeline {
    // Define no top-level agent. Each stage will define its own context.
    agent none

    environment {
        // Set your Docker Hub username.
        // For better practice, this could also be a global variable in Jenkins.
        DOCKER_USER = 'ahmedatef1095'
    }

    stages {
        // STAGE 1: Diagnose the agent machine itself.
        stage('Debug Agent Environment') {
            agent any
            steps {
                echo "--- Diagnosing Jenkins Agent Environment ---"
                sh 'echo "1. Current User:"; whoami'
                sh 'echo "2. User Groups:"; id'
                sh 'echo "3. Docker command location:"; which docker || echo "docker not found in PATH"'
                sh 'echo "4. Docker daemon status:"; docker info || echo "ERROR: Failed to connect to Docker daemon"'
                echo "------------------------------------------"
            }
        }

        // STAGE 2: Run all main tasks inside the Docker container environment.
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

                // DevSecOps: Add a security scanning stage (Note: requires Trivy in the agent image)
                // stage('Scan Image') {
                //     steps {
                //         echo 'Scanning Docker image for vulnerabilities...'
                //         sh "trivy image --exit-code 1 --severity HIGH,CRITICAL ${DOCKER_USER}/counter-app:latest"
                //     }
                // }

                stage('Build and Push') {
                    steps {
                        script {
                            withCredentials([string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD')]) {
                                echo 'Building and pushing the Docker image...'
                                sh 'ansible-playbook ansible/build-push.yml --extra-vars "docker_user=${DOCKER_USER} docker_password=$DOCKER_PASSWORD"'
                            }
                        }
                    }
                }

                stage('Deploy') {
                    steps {
                        script {
                            withCredentials([
                                string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD'),
                                sshUserPrivateKey(credentialsId: 'app-server-ssh-key', keyFileVariable: 'SSH_KEY_FILE', usernameVariable: 'SSH_USER')
                            ]) {
                                echo "Deploying the application to the server..."
                                sh 'ansible-playbook -i ansible/inventory ansible/deploy.yml --extra-vars "docker_user=${DOCKER_USER} docker_password=$DOCKER_PASSWORD"'
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            // Define an agent for this post-build stage to run on.
            agent any
            steps {
                script {
                    echo 'Pipeline finished. Logging out from Docker Hub...'
                    withCredentials([string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD')]) {
                        // We need to checkout the code again to access the logout playbook.
                        checkout scm
                        sh 'ansible-playbook ansible/logout.yml --extra-vars "docker_password=$DOCKER_PASSWORD"'
                    }
                }
            }
        }
    }
}
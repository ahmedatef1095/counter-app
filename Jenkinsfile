pipeline {
    // Use a Docker container as the build agent.
    // This image contains both Ansible and the Docker client.
    // This makes the build environment consistent and removes the need for the 'tools' directive.
    agent {
        docker {
            // Pin the agent image to a specific version for reproducible and secure builds.
            image 'cytopia/ansible-docker:2.10-0.3.3'
            // We need to mount the host's Docker socket so we can run Docker commands from inside the container.
            args '-v /var/run/docker.sock:/var/run/docker.sock'
        }
    }

    environment {
        // Set your Docker Hub username.
        // For better practice, this could also be a global variable in Jenkins.
        DOCKER_USER = 'ahmedatef1095'
    }
    stages {
        // Stage 1: Checkout code from your version control system
        stage('Checkout') {
            steps {
                // This will check out the code from the repository where this Jenkinsfile is located.
                checkout scm
            }
        }

        // DevSecOps: Add a security scanning stage
        stage('Scan Image') {
            steps {
                echo 'Scanning Docker image for vulnerabilities...'
                // This command uses Trivy (installed in the agent) to scan the image for vulnerabilities.
                sh "trivy image --exit-code 1 --severity HIGH,CRITICAL ${DOCKER_USER}/counter-app:latest"
            }
        }

        // Stage 2: Build the Docker image and push it to Docker Hub
        stage('Build and Push') {
            steps {
                script {
                    // Use the Jenkins Credentials plugin to securely handle your Docker Hub password.
                    // 'dockerhub-password' is the ID of the "Secret text" credential you set up in Jenkins.
                    withCredentials([string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD')]) {
                        echo 'Building and pushing the Docker image...'                        
                        // Use single quotes to prevent Groovy from interpolating the secret.
                        // The shell will safely expand the $DOCKER_PASSWORD environment variable.
                        sh 'ansible-playbook ansible/build-push.yml --extra-vars "docker_user=${DOCKER_USER} docker_password=$DOCKER_PASSWORD"'
                    }
                }
            }
        }

        // Stage 3: Deploy the application to the server
        stage('Deploy') {
            steps {
                script {
                    // This step requires two credentials:
                    // 1. The Docker Hub password to pull the private image on the server.
                    // 2. The SSH private key to connect to your application server ('app-server-ssh-key').
                    withCredentials([
                        string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD'),
                        sshUserPrivateKey(credentialsId: 'app-server-ssh-key', keyFileVariable: 'SSH_KEY_FILE', usernameVariable: 'SSH_USER')
                    ]) {
                        echo "Deploying the application to the server..."
                        // Use single quotes here as well for the same security reasons.
                        // The -i flag points to the inventory file.
                        sh 'ansible-playbook -i ansible/inventory ansible/deploy.yml --extra-vars "docker_user=${DOCKER_USER} docker_password=$DOCKER_PASSWORD"'
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                echo 'Pipeline finished. Logging out from Docker Hub...'
                // This ensures the Docker Hub logout happens even if a stage fails.
                withCredentials([string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD')]) {
                    sh 'ansible-playbook ansible/logout.yml'
                }
            }
        }
    }
}
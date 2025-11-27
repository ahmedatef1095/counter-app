pipeline {
    // Define an agent. This should be a Jenkins agent with Ansible and Docker installed.
    agent any

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

        // Stage 2: Build the Docker image and push it to Docker Hub
        stage('Build and Push') {
            steps {
                script {
                    // Use the Jenkins Credentials plugin to securely handle your Docker Hub password.
                    // 'dockerhub-password' is the ID of the "Secret text" credential you set up in Jenkins.
                    withCredentials([string(credentialsId: 'dockerhub-password', variable: 'DOCKER_PASSWORD')]) {
                        echo 'Building and pushing the Docker image...'
                        // Execute the build-push.yml playbook
                        sh "ansible-playbook ansible/build-push.yml --extra-vars 'docker_user=${DOCKER_USER} docker_password=${DOCKER_PASSWORD}'"
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
                        // Execute the deploy.yml playbook.
                        // This assumes you have an 'inventory' file in your 'ansible' directory for target hosts.
                        sh "ansible-playbook -i ansible/inventory ansible/deploy.yml --extra-vars 'docker_user=${DOCKER_USER} docker_password=${DOCKER_PASSWORD}'"
                    }
                }
            }
        }
    }
}
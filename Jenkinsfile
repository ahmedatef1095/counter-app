pipeline {
    agent any

    environment {
        // Define the remote server credentials and path
        // Ensure you have a 'SSH Username with private key' credential in Jenkins with this ID
        REMOTE_CREDENTIALS_ID = 'App_Server_SSH_Key' 
        REMOTE_USER_HOST = 'ec2-user@16.171.26.62'
        // The absolute path to your project directory on the target server
        REMOTE_PROJECT_PATH = '/counter-app'
    }

    stages {
        stage('Checkout') {
            steps {
                // This will check out the code from the repository configured in the Jenkins job
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Deploy to Target Server') {
            steps {
                script {
                    // Use the sshagent wrapper to securely connect to the remote server
                    sshagent(credentials: [REMOTE_CREDENTIALS_ID]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${REMOTE_USER_HOST} '''
                                echo "Connected to target server."
                                
                                // Navigate to the project directory
                                cd ${REMOTE_PROJECT_PATH}
                                
                                // Pull the latest changes from the git repository
                                echo "Pulling latest code..."
                                git pull

                                // Stop and remove old containers, networks, and volumes
                                echo "Bringing down existing containers..."
                                docker-compose down

                                // Build and run the application in detached mode
                                echo "Building and starting new containers..."
                                docker-compose up --build -d
                            '''
                        """
                    }
                }
            }
        }
    }
}
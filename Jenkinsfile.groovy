pipeline {
    agent any

    environment {
        // Jenkins credential ID for SSH access to the target server
        REMOTE_CREDENTIALS_ID = 'App_Server_SSH_Key' 
        // Target server user and IP/hostname
        REMOTE_USER_HOST = 'ec2-user@16.171.26.62'
        // Absolute path to the project directory on the target server
        // Use '~' as a shortcut for the user's home directory (e.g., /home/ec2-user)
        REMOTE_PROJECT_PATH = '~/counter-app'
        // The URL of your Git repository - IMPORTANT: UPDATE THIS URL
        GIT_REPO_URL = 'https://github.com/your-username/your-repo.git'
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
                                # Ensure the project directory exists and navigate into it
                                mkdir -p ${REMOTE_PROJECT_PATH}
                                cd ${REMOTE_PROJECT_PATH}
                                
                                # Check if this is a git repository. If not, clone it.
                                if [ ! -d ".git" ]; then
                                    echo "Git repository not found. Cloning from ${GIT_REPO_URL}..."
                                    git clone ${GIT_REPO_URL} .
                                else
                                    echo "Git repository found. Pulling latest code..."
                                    # Reset any local changes and pull the latest from the main branch
                                    git fetch origin
                                    git reset --hard origin/main # Or your default branch
                                    git pull origin main
                                fi

                                # Stop and remove old containers to ensure a clean start
                                echo "Bringing down existing Docker containers..."
                                docker-compose down

                                # Build new images and start the services in detached mode
                                echo "Building and starting new Docker containers..."
                                docker-compose up --build -d
                            '''
                        """
                    }
                }
            }
        }
    }
}

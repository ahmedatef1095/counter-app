pipeline {
    agent any

    environment {
        // Jenkins credential ID for SSH access to the target server
        REMOTE_CREDENTIALS_ID = 'App_Server_SSH_Key' 
        // Target server user and IP/hostname
        REMOTE_USER_HOST = 'ec2-user@16.171.26.62'
        // Absolute path to the project directory on the target server
        REMOTE_PROJECT_PATH = '/app'
        // The URL of your Git repository
        GIT_REPO_URL = 'https://github.com/your-username/your-repo.git' // <-- TODO: UPDATE THIS URL
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
                                # Ensure the project directory exists
                                mkdir -p ${REMOTE_PROJECT_PATH}
                                cd ${REMOTE_PROJECT_PATH}

                                # Check if a .git directory exists. If not, clone the repo.
                                if [ ! -d ".git" ]; then
                                    echo "Git repository not found. Cloning repository..."
                                    git clone ${GIT_REPO_URL} .
                                else
                                    echo "Git repository found. Pulling latest code..."
                                    # Stash any local changes and pull the latest from the main branch
                                    git stash
                                    git pull origin main
                                fi

                                # Stop and remove old containers
                                echo "Bringing down existing Docker containers..."
                                docker-compose down

                                # Build and run the application
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
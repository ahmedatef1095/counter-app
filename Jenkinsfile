pipeline {
    agent any

    stages {
        stage('Verify Docker Installation') {
            steps {
                echo "--- Checking for Docker on the Jenkins Agent ---"
                sh '''
                    echo "Step 1: Checking if 'docker' command exists in PATH..."
                    which docker
                '''
            }
        }
    }
}
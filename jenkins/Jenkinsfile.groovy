pipeline {
    agent any

    environment {
        DOCKER_IMAGE = "ashiyanas/demo-app:${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/ayeshasayed25/infra-cicd-k8s-pipeline_new.git'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh 'docker build -t $DOCKER_IMAGE ./app'
            }
        }

        stage('Push Docker Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                    sh 'echo $PASS | docker login -u $USER --password-stdin'
                    sh 'docker push $DOCKER_IMAGE'
                }
            }
        }

        stage('Deploy to Kubernetes') {
            steps {
                sh 'kubectl apply -f k8s/deployment.yml'
                sh 'kubectl apply -f k8s/service.yml'
            }
        }

        stage('Setup Monitoring') {
            steps {
                sh '''
                helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
                helm repo add grafana https://grafana.github.io/helm-charts
                helm repo update
                helm install prometheus prometheus-community/prometheus --namespace monitoring --create-namespace || true
                helm install grafana grafana/grafana --namespace monitoring || true
                '''
            }
        }
    }

    post {
        success {
            echo "CI/CD Pipeline executed successfully!"
        }
    }
}

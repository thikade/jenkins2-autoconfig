pipeline {
    agent any
    tools {
        maven 'M361'
        // jdk 'jdk8'
    }
    options {
        skipDefaultCheckout true
    }    
    stages {
        stage('nvm tests') {
            steps {
                nvm('v10.16.0') {
                    echo "hello nvm"
                    sh "node --version"
                    sh "npm install gulp"
                }
            }
        }
        
        stage('maven tests') {
            steps {
                    sh "mvn --version"
            }
        }
        
        stage('alternate maven version tests') {
            tools {
                maven 'M354'
            }
            steps {
                    sh "mvn --version"
            }
        }
        
    }
}
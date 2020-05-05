def args = [
        Bob  : 42,
        Foo  : "bar",
        Hugo : "isst am liebsten Wolf"
]

// Ansible additional commandline args
def extraArgs = ""

pipeline {
    agent any
    // tools { }
    parameters {
        choice(name: 'PLAYBOOK', choices: ['test-playbook.yaml', 'main-setup-projects.yaml'], description: 'Ansible Playbook to run.') 
        string(name: 'ANSIBLE_CMD_OPTIONS', defaultValue: extraArgs, description: 'Ansible commandline options')
    }
    options {
        skipDefaultCheckout true
    }    
    environment {
        TEST="123"
    }

    stages {

        stage('Prepare') {
            steps {
                banner STAGE_NAME
                deleteDir()
                checkout scm
                
                banner 'PRINT ENV'
                sh "printenv | sort"
                // script {
                //     files = findFiles(glob: '**/*')
                //     files.each{ 
                //         f -> println("File=${f.path} isDir=${f.directory}")    
                //     }
                // }                
            }
        }


        stage('Run Playbook') {
            steps {
                dir("ansible") {
                    ansiColor('xterm') {
                        banner STAGE_NAME
                        // ansiblePlaybook(credentialsId: 'private_key', inventory: 'localhost', playbook: 'my_playbook.yml')
                        ansiblePlaybook(playbook: params.PLAYBOOK, colorized: true, extraVars: args, extras: params.ANSIBLE_CMD_OPTIONS)
                        // ansibleVault
                    }
                }
            }
        }
        
        
    }
}


def banner(String bannerText = "") {
    def text = "S T A G E : "
    for (c in bannerText.toUpperCase()) {
        text = text + c + " "
    }
    println "##############################################################################"
    println "#"
    println "#   " + text
    println "#"
    println "##############################################################################"
}



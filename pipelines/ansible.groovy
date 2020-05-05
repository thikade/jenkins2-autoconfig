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
        choice(name: 'PLAYBOOK', choices: ['main-setup-projects.yaml', 'test-playbook.yaml'], description: 'Ansible Playbook to run.') 
        string(name: 'ANSIBLE_CMD_OPTIONS', defaultValue: extraArgs, description: 'additional Ansible command-line options')
        booleanParam(name: 'DEBUG', defaultValue: false, description: '')
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
                script {
                    if (params.DEBUG) {
                        banner 'PRINT ENVIRONMENT'
                        sh "printenv | sort"
                        params.ANSIBLE_CMD_OPTIONS = "${params.ANSIBLE_CMD_OPTIONS} -v"
                    }
                }
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



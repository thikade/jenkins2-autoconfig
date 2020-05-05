// default variables
String PREFIX = "abc"
List STAGES = [ "int", "int2", "uat", "prod"]
String PRJ_ID_DESCRIPTION = 'Projekt-ID - damit werden mehrere Openshift Projekte nach dem Muster ${PREFIX}-<PROJEKT_ID>-<STAGE-NAME> angelegt.\nStages: ${STAGES}'


// Ansible Extra Variables: added via -e KEY=VALUE -e KEY2=VALUE2
def ansibleExtraVars = [
        Bob   : 42,
        Foo   : "bar",
        Hugo  : "The quick brown Fox ...",
        someList : [ "a", "b", "c"]
]


// Ansible additional commandline args
String extraCmdArgDefaults = ""
String extraCmdArgs = ""

pipeline {
    agent any
    // tools { }
    parameters {
        string(name: 'PRJ_ID', defaultValue: "app01", description: PRJ_ID_DESCRIPTION, trim: true)
        choice(name: 'PLAYBOOK', choices: ['main-setup-projects.yaml', 'test-playbook.yaml'], description: 'Ansible Playbook to run.') 
        string(name: 'ANSIBLE_CMD_OPTIONS', defaultValue: extraCmdArgDefaults, description: 'additional Ansible command-line options', trim: true)
        booleanParam(name: 'DEBUG', defaultValue: false, description: 'enable Debug mode')
    }
    options {
        skipDefaultCheckout true
    }    
    environment {
        TEST="123"
    }

    stages {

        stage('Prepare') {
            environment {
                PROJECT_BASE_NAME="${PREFIX}-${params.PRJ_ID}".toLowerCase()
            }            
            steps {
                banner STAGE_NAME
                deleteDir()
                checkout scm
                script {
                    extraCmdArgs = params.ANSIBLE_CMD_OPTIONS
                    if (params.DEBUG) {
                        banner 'PRINT ENVIRONMENT'
                        sh "printenv | sort"
                        extraCmdArgs = "-v ${extraCmdArgs}"
                    }
                }

                script {
                    List projects = [] 
                    // build project list
                    STAGES.each{ stage ->
                        projects << "${PROJECT_BASE_NAME}-${stage}"
                    }
                    echo "project list: ${projects}"
                }
            }
        }


        stage('Run Playbook') {
            steps {
                banner STAGE_NAME
                dir("ansible") {
                    ansiColor('xterm') {
                        ansiblePlaybook(playbook: params.PLAYBOOK, colorized: true, extraVars: ansibleExtraVars, extras: extraCmdArgs)
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


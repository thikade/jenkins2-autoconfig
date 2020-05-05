import groovy.json.JsonOutput

// default variables
String PREFIX = "stiu"

// main data structure -  will be read from Ansible 
List STAGES = [ "uat", "prod"]

    'stiu_stages' : STAGES,

    'uat' : [
        'namespace':    'UNDEFINED',
        'build':        'true',
        'tag':          'latest',
        'cluster':      'local'
    ],

    'prod' : [
        'namespace':    'UNDEFINED',
        'stage-type':   'int',
        'build':        'false',
        'copy-from':    'uat'
        'tag':          'prod'
        'cluster':      'local'
    ],
    
] 

// Ansible Extra Variables: added via -e KEY=VALUE -e KEY2=VALUE2
def ansibleExtraVars = [
    "Bob"   : 42,
    "Foo"   : "bar",
    "Hugo"  : 'The quick brown Fox ...',
]
// def jsonExtraVars = null


// Ansible additional commandline args
String extraCmdArgDefaults = ""
String extraCmdArgs = ""

pipeline {
    agent any
    // tools { }
    parameters {
        string(name: 'PRJ_ID', defaultValue: "app01", description: 'Projekt-ID - damit werden mehrere Openshift Projekte nach dem Muster ${PREFIX}-<PROJEKT_ID>-<STAGE-NAME> angelegt.\nStages: ${STAGES}', trim: true)
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

            }
        }


        stage('Run Playbook') {
            environment {
                PROJECT_BASE_NAME="${PREFIX}-${params.PRJ_ID}".toLowerCase()
            }              
            steps {
                banner STAGE_NAME
                dir("ansible") {
                    ansiColor('xterm') {
                        script {
                            // build project list
                            List projects = [] 
                            STAGES.each{ stage ->
                                ansibleVariables[stage].name = "${PROJECT_BASE_NAME}-${stage}"
                            }
                            writeJSON(file: 'variables.json', json: ansibleVariables)
                        }


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
    println "##############################################################################"
    println "#   " + text
    println "#"
}


def l_toJsonString(o) {
    return JsonOutput.toJson(o)
}
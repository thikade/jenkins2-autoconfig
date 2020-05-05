import groovy.json.JsonOutput

// default variables
String PREFIX = "stiu"

// main data structure -  will be read from Ansible 
List STAGES = [ "uat", "prod"]

Map ansibleVariables = [
    'repo_url'     : '',
    'repo_branch'  : '',
    'repo_context' : '',

    'stiu_stages'  : STAGES,
    
    'uat' : [
        'cluster':      'local',
        'namespace':    'will be defined in later stage!',
        'build':        'true',
        'tag-to':       'latest',
    ],

    'prod' : [
        'cluster':      'local',
        'namespace':    'will be defined in later stage!',
        'build':        'false',
        'tag-from':     'uat',
        'tag-to':       'prod',
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
        string(name: 'REPO_URL', defaultValue: "", description: 'Git Repo URL', trim: true)
        string(name: 'REPO_BRANCH', defaultValue: "uniqa", description: 'Git Repo BRANCH', trim: true)
        string(name: 'REPO_CONTEXT', defaultValue: ".", description: 'Git Repo Context directory', trim: true)
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
                            ansibleVariables.repo_url = params.REPO_URL
                            ansibleVariables.repo_branch = params.REPO_BRANCH
                            ansibleVariables.repo_context = params.REPO_CONTEXT
                            
                            writeJSON(file: 'variables.json', json: ansibleVariables)
                            sh "cat variables.json"
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
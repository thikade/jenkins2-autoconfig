import groovy.json.JsonOutput

// Namespace Prefix
String PREFIX = "stiu"

// main data structure -  will be read from Ansible 
List STAGES = [ "uat", "prod"]

Map ansibleVariables = [
    'repo': [
        'url'     : '',
        'branch'  : '',
        'context' : '',
    ],

    'project_annotations' : [
        'openshift.io/node-selectorXXX' : 'color=blue',
        'mySpecialAnnotationX' : 'color=green',
    ],

    'project_labels' : [:],

    'stages'  : STAGES,
    
    'uat' : [
        'cluster':         'default',
        'namespace':       'will be defined in stage(Run Playbook)',
        'maven_build':     'true',
        'image_tag':       ':latest',
        'has_jenkins':     'true',
    ],

    'prod' : [
        'cluster':         'default',
        'namespace':       'will be defined in stage(Run Playbook)',
        'maven_build':     'false',
        'image_tag':       ':prod',
        'copy_from_stage': 'uat',
        'has_jenkins':     'false',
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
        string(name: 'PRJ_ID', defaultValue: "app01", description: "Projekt-ID - damit werden mehrere Openshift Projekte nach dem Muster ${PREFIX}-<PROJEKT_ID>-<STAGE-NAME> angelegt.\nStages: ${STAGES}", trim: true)
        string(name: 'REPO_URL', defaultValue: "https://github.com/thikade/jenkins2-autoconfig.git", description: 'Git Repo URL', trim: true)
        string(name: 'REPO_BRANCH', defaultValue: "uniqa", description: 'Git Repo BRANCH', trim: true)
        string(name: 'REPO_CONTEXT', defaultValue: ".", description: 'Git Repo Context directory', trim: true)
        choice(name: 'PLAYBOOK', choices: ['000-main-setup-projects.yaml', 'test-playbook.yaml'], description: 'Ansible Playbook to run.') 
        string(name: 'ANSIBLE_CMD_OPTIONS', defaultValue: extraCmdArgDefaults, description: 'additional Ansible command-line options', trim: true)
        booleanParam(name: 'DEBUG', defaultValue: true, description: 'enable Debug mode')
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
                banner "PARAMS"
                echo "${params}"
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
                                ansibleVariables[stage].namespace = "${PROJECT_BASE_NAME}-${stage}"
                            }
                            ansibleVariables.repo.url = params.REPO_URL ?: "http://localhost:1234"
                            ansibleVariables.repo.branch = params.REPO_BRANCH
                            ansibleVariables.repo.context = params.REPO_CONTEXT
                            // echo "${ansibleVariables}"
                            
                            banner "JSON_CONVERSION"
                            // def jsonOut = readJSON text: l_toJsonString(ansibleVariables)
                            def jsonOut = readJSON text: JsonOutput.toJson(ansibleVariables)
                            writeJSON file: 'variables.json', json: jsonOut, pretty: 4
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
import groovy.json.JsonOutput

// Namespace Prefix
String PREFIX = "stiu"

// main data structure -  will be read from Ansible 
List STAGES = [ "test", "uat", "int" ]

Map ansibleVariables = [

    'repo': [
        'url'     : '',     // will be set by a later stage 
        'branch'  : '',
        'context' : '',
    ],


    // =====================================================
    // PROJECT & STAGE SETUP
    // =====================================================

    // needs to be a map ( or even an empty map = [:] !)
    'project_annotations' : [
        'openshift.io/node-selectorXXX' : 'region=stiu',
        'mySpecialAnnotation' : 'color=green',
    ],

    // needs to be a map ( or even an empty map = [:] !)
    'project_labels' : [:],

    // these is the list of stage names; each item requires a map entry below!
    'stages'  : [ "test", "uat", "int" ],
    
    // define in which stage the primary Jenkins will be deployed
    'jenkins_stage' : 'test',

    // map of all stages and their properties 
    'test' : [
        'cluster':         'testcloud',
        'namespace':       '-',         // will be set by a later stage 
        'maven_build':     'true',
        'image_tag':       'latest',
    ],
    
    'uat' : [
        'cluster':         'testcloud',
        'namespace':       '-',         // will be set by a later stage 
        'maven_build':     'true',
        'image_tag':       'latest',
    ],

    'int' : [
        'cluster':         'testcloud',
        'namespace':       '-',         // will be set by a later stage 
        'maven_build':     'false',
        'image_tag':       'int',
        'copy_from_stage': 'uat',
    ],
    
    // =====================================================
    // AUTHENTICATION section - CHANGE AT YOUR OWN RISK!
    // =====================================================
    'cluster_authentication' : [
        'testcloud' : [
            'url'    :  'https://openshift.default.svc.cluster.local',
            'secret' :  'jenkins-token-test',
            'skipTLS':  'true',
        ],
        'production': [
            'url'    :  'https://openshift.default.svc.cluster.local',
            'secret' :  'jenkins-token-prod',
            'skipTLS':  'true',
        ],
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
                echo "PARAMETERS:\n${params}\n"
                deleteDir()
                checkout scm
                script {
                    extraCmdArgs = params.ANSIBLE_CMD_OPTIONS
                    if (params.DEBUG) {
                        banner 'PRINT ENVIRONMENT'
                        sh "printenv | sort"
                        extraCmdArgs = "-v ${extraCmdArgs}"
                    }

                    // build project list
                    STAGES.each{ stage ->
                        if (ansibleVariables[stage] == null) { error "stage \"${stage}\" not found in 'ansibleVariables' Map!" }
                        ansibleVariables[stage].namespace = "${PROJECT_BASE_NAME}-${stage}"
                    }
                    ansibleVariables.repo.url = params.REPO_URL ?: "http://localhost:1234"
                    ansibleVariables.repo.branch = params.REPO_BRANCH
                    ansibleVariables.repo.context = params.REPO_CONTEXT
                    // echo "${ansibleVariables}"
                    
                    banner "JSON_CONVERSION"
                    def jsonOut = readJSON text: JsonOutput.toJson(ansibleVariables)
                    writeJSON file: 'ansible/variables.json', json: jsonOut, pretty: 4
                    sh "cat ansible/variables.json"
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
    println "##############################################################################"
    println "#   " + text
    println "#"
}


// def l_toJsonString(o) {
//     return JsonOutput.toJson(o)
// }
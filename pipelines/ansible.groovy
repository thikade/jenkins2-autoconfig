import groovy.json.JsonOutput

// Namespace Prefix
String PREFIX = "stiu"

// main data structure -  will be read from Ansible 
Map ansibleVariables = [

    'repository': [
        'url'     : '',     // will be set by a later stage 
        'branch'  : '',
        'context' : '',
    ],


    // =====================================================
    // PROJECT & STAGE SETUP
    // =====================================================

    // these is the list of stage names; each item requires a map entry below! (REQUIRED)
    'stages'  : [ "aaa", "bbb", "ccc" ],
    
    // define in which stage the primary Jenkins will be deployed (REQUIRED)
    'jenkins_stage' : 'aaa',

    // map of all stages and their properties (definitely REQUIRED)
    'aaa' : [
        'cluster':         'testcloud',
        'namespace':       '-',         // will be set by a later stage 
        'maven_build':     'true',
        'image_tag':       'latest',
    ],
    
    'bbb' : [
        'cluster':         'testcloud',
        'namespace':       '-',         // will be set by a later stage 
        'maven_build':     'true',
        'image_tag':       'latest',
    ],

    'ccc' : [
        'cluster':         'testcloud',
        'namespace':       '-',         // will be set by a later stage 
        'maven_build':     'false',
        'image_tag':       'ccc',
        'copy_from_stage': 'bbb',
    ],

    //
    // ANNOTATIONS & LABELS
    //
    // needs to be a map ( or even an empty map = [:] !) (OPTIONAL)
    'project_annotations' : [
        'openshift.io/node-selectorXXX' : 'region=stiu',
        'mySpecialAnnotation' : 'color=green',
    ],

    // needs to be a map ( or even an empty map = [:] !) (OPTIONAL)
    'project_labels' : [:],


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

// Ansible additional commandline args
String extraCmdArgDefaults = ""
String extraCmdArgs = ""



pipeline {

    agent any
    // tools { }
    parameters {
        string(name: 'PRJ_ID', defaultValue: "app01", description: "Projekt-ID - damit werden mehrere Openshift Projekte nach dem Muster \"${PREFIX}-<PROJEKT_ID>-<STAGE-NAME>\" angelegt.\nStages: ${ansibleVariables.stages}", trim: true)
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

    stages {

        stage('Prepare') {
            environment {
                PROJECT_BASE_NAME="${PREFIX}-${params.PRJ_ID}".toLowerCase()
            }              
            steps {
                banner STAGE_NAME
                script {
                    if (! params.PRJ_ID) {
                        error "Job parameter \"PRJ_ID\" is required, but was empty!"
                    }

                    echo "PARAMETERS:\n${params}\n"
                    deleteDir()
                    checkout scm

                    extraCmdArgs = params.ANSIBLE_CMD_OPTIONS
                    if (params.DEBUG) {
                        banner 'PRINT ENVIRONMENT'
                        sh "printenv | sort"
                        extraCmdArgs = "-v ${extraCmdArgs}"
                    }

                    // build project list
                    ansibleVariables.stages.each{ stage ->
                        if (ansibleVariables[stage] == null) { error "stage \"${stage}\" not found in 'ansibleVariables' Map!" }
                        ansibleVariables[stage].namespace = "${PROJECT_BASE_NAME}-${stage}"
                    }
                    ansibleVariables.repository.url = params.REPO_URL ?: "http://localhost:1234"
                    ansibleVariables.repository.branch = params.REPO_BRANCH
                    ansibleVariables.repository.context = params.REPO_CONTEXT
                    // echo "${ansibleVariables}"
                    
                    banner "JSON_CONVERSION"
                    def jsonOut = readJSON text: JsonOutput.toJson(ansibleVariables)
                    writeJSON file: 'ansible/variables.json', json: jsonOut, pretty: 4
                    sh "cat ansible/variables.json"
                }
            }
        }

        stage('Run Playbook') {
            environment {
                TEST="123"
            }
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

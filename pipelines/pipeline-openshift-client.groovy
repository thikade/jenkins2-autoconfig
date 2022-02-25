pipeline {
    agent any

    options {
        skipDefaultCheckout true
    }    
    parameters {
        // string( name: 'credential', defaultValue: "", description: 'credentials to use for openshift-client', trim: true )
        choice(
            name: 'credential',    
            choices: [  "synced-jenkins-token", 
                        "manually-created-token", 
                        "openshift-client-token-sync", 
                        "openshift-client-token-sync-akram", 
                        "ocp-client-token-choice-1", 
                        "ocp-client-token-choice-2", 
                        "ocp-client-token-choice-3", 
                        "non-existent-token", 
                        "no-token"
                    ], 
            description: 'select a credential ID to use'
        )}

    stages {
        stage('show params') {
            steps {
                script {
                    echo "--- PIPELINE PARAMETERS: ----------------"
                    def p2 = params.sort()
                    p2.each { p -> println "${p}" }
                    echo "-----------------------------------------"
                }
            }
        }

        stage('client-plugin-tests') {
            // when { expression { params.credential != "" }}

            steps {
                script {
                    String credentialID = params.credential
                    // special case "null" 
                    if (!params.credential || params.credential == "no-token") { 
                        println "credentialID is set to NULL!"
                        credentialID=null 
                    } 

                    println "credentialID used is: ${credentialID}"
                    currentBuild.description = "credentialID used: ${credentialID}"

                    openshift.withCluster("default") {
                        openshift.withProject("jenkinsbuild") {
                            openshift.withCredentials(credentialID) {
                                def x = openshift.raw("whoami", "--loglevel=5")
                                def user = x.out.trim()
                                println "running as user: \"${user}\"  in project: \"${openshift.project()}\"  on cluster \"${openshift.cluster()}\""
                            }
                        }
                    }
                }
            }
        }
    }
}
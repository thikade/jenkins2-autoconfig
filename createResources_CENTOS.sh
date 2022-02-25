#!/bin/bash

NS=${NAMESPACE:-jenkinsbuild}
JENKINS_BASE_IMAGESTREAM=openshift/jenkins:2

REPO_JENKINS=$(git ls-remote --get-url)

BRANCH=openshift4
# BRANCH=master

# create namespace if it does not exist
oc get namespace $NS -o name  || oc new-project $NS

### this annotation will automatically add the secret to all builds refering to gitlab!!
### oc -n $NS annotate secret gitlab 'build.openshift.io/source-secret-match-uri-1=https://git.example.com/*'
### # alternatively, use: oc set buildsecret --source


oc -n $NS  new-build "$JENKINS_BASE_IMAGESTREAM~${REPO_JENKINS}#${BRANCH}" \
 --context-dir=base-image --strategy=source \
 --name=jenkins-base -l app=jenkins-base \
 --dry-run -o yaml | oc -n $NS  apply -f -


# to try out / debug our new Jenkins base-image (non-persistent)!
# oc -n $NS new-app --template=openshift/jenkins-ephemeral  \
#   -p JENKINS_SERVICE_NAME=jenkins-base \
#   -p JNLP_SERVICE_NAME=jenkins-base-jnlp \
#   -p NAMESPACE=$NS \
#   -p JENKINS_IMAGE_STREAM_TAG=jenkins-base:latest \
#   -l app=jenkins-base \
#   --allow-missing-imagestream-tags \
#   --dry-run -o yaml | oc -n $NS  apply -f -



# use docker build to build our own jenkins image with add. packages etc
oc -n $NS new-build "${REPO_JENKINS}#${BRANCH}"  \
 --context-dir=docker-build --strategy=docker \
 --name=jenkins-autoconfig -l app=jenkins-autoconfig \
 --image-stream=jenkins-base:latest  \
 --allow-missing-imagestream-tags \
 --dry-run -o yaml | oc -n $NS apply -f -

# create CASC (Jenkins Plugin: Configuration-As-Code)configmap from file
oc -n $NS create configmap jenkins-casc --from-file=jenkins.yaml=jenkins-casc.yaml --dry-run=client -o yaml | oc -n $NS apply -f -
# oc -n $NS create configmap jenkins-casc --from-file=jenkins.yaml=jenkins-casc.yaml --dry-run=client -o yaml | oc -n $NS replace -f -

# secret token 
oc -n $NS create sa jenkins
oc -n $NS create secret generic synced-jenkins-token --from-literal=token=$(oc -n $NS sa get-token jenkins) --dry-run=client -o yaml | oc -n $NS apply -f -

# have secret synced into Jenkins credential store:
oc -n $NS label secret synced-jenkins-token credential.sync.jenkins.openshift.io=true
oc -n $NS annotate secret synced-jenkins-token jenkins.openshift.io/secret.name=synced-jenkins-token

# process template and run  jenkins-autoconfig
oc -n $NS process -f templates/jenkins.tpl.yaml \
 -p NAMESPACE=$NS \
 -p MEMORY_LIMIT="2048M" \
 -p JENKINS_IMAGE_STREAM_TAG=jenkins-autoconfig:latest \
 -o yaml  \
 | oc -n $NS apply -f -

# create the test pipeline build
oc -n $NS process -f templates/bc-pipeline.tpl.yaml \
 -p NAME=plugintest-pipeline \
 -p REPO_URL=$REPO_JENKINS \
 -p BRANCH=${BRANCH} \
 -p CONTEXT_DIR=. \
 -p JENKINSFILEPATH=pipelines/pipeline-validate-Jenkins.groovy \
 -o yaml  \
 | oc -n $NS apply -f -

# create the Ansible  pipeline build
oc -n $NS process -f templates/bc-pipeline.tpl.yaml \
 -p NAME=ansible-runner \
 -p REPO_URL=$REPO_JENKINS \
 -p BRANCH=${BRANCH} \
 -p CONTEXT_DIR=. \
 -p JENKINSFILEPATH=pipelines/ansible.groovy \
 -o yaml  \
 | oc -n $NS apply -f -

 # create the Openshift client plugin test pipeline buildconfig
oc -n $NS process -f templates/bc-pipeline.tpl.yaml \
 -p NAME=client-plugin-test \
 -p REPO_URL=$REPO_JENKINS \
 -p BRANCH=${BRANCH} \
 -p CONTEXT_DIR=. \
 -p JENKINSFILEPATH=pipelines/pipeline-openshift-client.groovy \
 -o yaml  \
 | oc -n $NS apply -f -
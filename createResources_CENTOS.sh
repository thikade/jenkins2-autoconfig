#!/bin/bash

NS=${NAMESPACE:-jenkinsbuild}
JENKINS_BASE_IMAGESTREAM=openshift/jenkins-2-centos7:v3.11

REPO_JENKINS=$(git ls-remote --get-url)


### this annotation will automatically add the secret to all builds refering to gitlab!!
### oc -n $NS annotate secret gitlab 'build.openshift.io/source-secret-match-uri-1=https://git.example.com/*'
### # alternatively, use: oc set buildsecret --source


oc -n $NS  new-build $JENKINS_BASE_IMAGESTREAM~${REPO_JENKINS} \
 --context-dir=base-image --strategy=source \
 --name=jenkins-base -l app=jenkins-base \
 --dry-run -o yaml | oc -n $NS  apply -f -


# to try out our new base image! not persistent!
# oc -n $NS new-app --template=openshift/jenkins-ephemeral  \
#   -p JENKINS_SERVICE_NAME=jenkins-base \
#   -p JNLP_SERVICE_NAME=jenkins-base-jnlp \
#   -p NAMESPACE=$NS \
#   -p JENKINS_IMAGE_STREAM_TAG=jenkins-base:latest \
#   -l app=jenkins-base \
#   --allow-missing-imagestream-tags \
#   --dry-run -o yaml | oc -n $NS  apply -f -



# use docker build to build our own jenkins image with add. packages etc
oc -n $NS new-build ${REPO_JENKINS}  \
 --context-dir=docker-build --strategy=docker \
 --name=jenkins-custom -l app=jenkins-custom \
 --image-stream=jenkins-base:latest   \
 --allow-missing-imagestream-tags \
 --dry-run -o yaml | oc -n $NS apply -f -

# create CASC configmap from file
oc -n $NS create configmap jenkins-casc --from-file=jenkins.yaml=jenkins-casc.yaml --dry-run -o yaml | oc -n $NS apply -f -

# secret token 
oc -n $NS create secret generic jenkins-sa-token --from-literal=token=$(oc -n $NS sa get-token jenkins) --dry-run -o yaml | oc -n $NS apply -f -

# process template and run persistent jenkins
oc -n $NS process -f templates/jenkins.tpl.yaml \
 -p NAMESPACE=$NS \
 -p MEMORY_LIMIT="1024M" \
 -p JENKINS_IMAGE_STREAM_TAG=jenkins-custom:latest \
 -o yaml  \
 | oc -n $NS apply -f -

# create the test pipeline build
oc -n $NS process -f templates/bc-pipeline.tpl.yaml \
 -p REPO_URL=$REPO_JENKINS \
 -p CONTEXT_DIR=. \
 -p JENKINSFILEPATH=pipelines/pipeline-validate-Jenkins \
 -o yaml  \
 | oc -n $NS apply -f -
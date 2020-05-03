#!/bin/bash

NS=${NAMESPACE:-jenkinsbuild-rhel}
JENKINS_BASE_IMAGESTREAM=jenkins-2-rhel7:v3.11

REPO_JENKINS=$(git ls-remote --get-url)


: ${REGISTRY_USERNAME?Error: env var not set}
: ${REGISTRY_PASSWORD?Error: env var not set}

### PREREQUS: RG PRIVATE REGISTRY AUTH
#  docker login registry.redhat.io
#  oc delete secret rh-registry
#  ### secret MUST be created with <generic> type, and not <docker-registry> !!!!!!! Else it simply does not work!
#  oc create secret generic rh-registry --from-file=.dockerconfigjson=$HOME/.docker/config.json --type=kubernetes.io/dockerconfigjson
#  oc secrets link default rh-registry  --for=pull
#  oc secrets link builder rh-registry  --for=pull
#  oc import-image jenkins-2-rhel7:v3.11 --from registry.redhat.io/openshift3/jenkins-2-rhel7:v3.11 --confirm
#  oc -n openshift import-image jenkins-2-rhel7 --from registry.redhat.io/openshift3/jenkins-2-rhel7 --confirm


# oc -n $NS  create secret generic gitlab \
#     --from-literal=username=$GITLAB_USER \
#     --from-literal=password="$GITLAB_PASS" \
#     --from-file=ca.crt=jenkins-build/cert/ca02.cert \
#     --type=kubernetes.io/basic-auth \
#     --dry-run -o yaml | oc apply -f -

### this annotation will automatically add the secret to all builds refering to gitlab!!
### oc -n $NS annotate secret gitlab 'build.openshift.io/source-secret-match-uri-1=https://git.example.com/*'
### # alternatively, use: oc set buildsecret --source


oc -n $NS  new-build $JENKINS_BASE_IMAGESTREAM~${REPO_JENKINS} \
 --context-dir=base-image --strategy=source \
 --name=jenkins-base -l app=jenkins-base \
 --dry-run -o yaml | oc -n $NS  apply -f -

# to try out our new base image! not persistent!
oc -n $NS new-app --template=openshift/jenkins-ephemeral  \
  -p JENKINS_SERVICE_NAME=jenkins-base \
  -p JNLP_SERVICE_NAME=jenkins-base-jnlp \
  -p NAMESPACE=$NS \
  -p JENKINS_IMAGE_STREAM_TAG=jenkins-base:latest \
  -l app=jenkins-base \
  --dry-run -o yaml | oc -n $NS  apply -f -


# use docker build to build our own jenkins image
oc -n $NS new-build ${REPO_JENKINS}  \
 --context-dir=docker-build --strategy=docker \
 --name=jenkins-custom -l app=jenkins-custom \
 --image-stream=jenkins-base:latest   \
 --allow-missing-imagestream-tags \
 --dry-run -o yaml | oc -n $NS apply -f -


# create CASC (Jenkins Plugin: Configuration-As-Code)configmap from file
oc -n $NS create configmap jenkins-casc --from-file=jenkins.yaml=jenkins-casc.yaml --dry-run -o yaml | oc -n $NS apply -f -

# secret token 
oc -n $NS create secret generic demo-secret-token --from-literal=token=$(oc -n $NS sa get-token jenkins) --dry-run -o yaml | oc -n $NS apply -f -

# process template and run jenkins
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
#!/bin/sh

NS=${NAMESPACE:-jenkinsbuild}
# JENKINS_BASE_IMAGESTREAM=openshift/jenkins-2-centos7
JENKINS_BASE_IMAGESTREAM=jenkins-2-rhel7:v3.11

REPO_JENKINS=https://github.com/thikade/jenkins2-autoconfig.git


: ${REGISTRY_USERNAME?Error: env var not set}
: ${REGISTRY_PASSWORD?Error: env var not set}

### PREREQUS: RG PRIVATE REGISTRY AUTH
#  docker login registry.redhat.io
#  oc delete secret rh-registry
#  ### secret MUST be created with <generic> type, and not <docker-registry> !!!!!!! Else it simply does not work!
#  oc create secret generic rh-registry --from-file=.dockerconfigjson=~/.docker/config.json --type=kubernetes.io/dockerconfigjson
#  oc secrets link default rh-registry  --for=pull
#  oc secrets link builder rh-registry  --for=pull
#  oc import-image jenkins-2-rhel7:v3.11 --from registry.redhat.io/openshift3/jenkins-2-rhel7:v3.11 --confirm


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

# use docker build to build our own jenkins image
oc -n $NS new-build ${REPO_JENKINS}  \
 --context-dir=docker-build --strategy=docker \
 --name=jenkins-custom -l app=jenkins-custom \
 --image-stream=jenkins-base:latest   \
 --allow-missing-imagestream-tags \
 --dry-run -o yaml | oc -n $NS apply -f -

# process template and run jenkins
oc -n $NS process -f templates/xxx.yaml \
 -p NAMESPACE=$NS \
 -p JENKINS_PV_NAME=dummy123 \
 -p JENKINS_IMAGE_STREAM_TAG=jenkins-blueocean:latest \
 | oc -n $NS apply -f -

oc -n $NS apply -f bc-pipelineTest.yml 




#!/bin/sh

EXECDIR=$(dirname $0)


# set Namespace string: either from arg $1 or default.
TNS=${1?You need to specify a NAMESPACE parameter when calling $0 .}

echo -e "\n- Working on namespace: $TNS\n"

# add secrets required by Jenkins
echo -e "\n- Updating synced Jenkins secrets:"

#################################
# create and sync new secrets 
#################################

# secret type 'openshift-client-token' used by Openshift CLient Plugin
SECRET=jenkins-client-plugin-token-${TNS}
TOKEN=$(oc -n $TNS sa get-token jenkins)
oc -n $TNS create secret generic $SECRET --from-literal=openshift-client-token=$TOKEN --dry-run -o yaml | oc -n $TNS apply -f - 1>/dev/null
sh ${EXECDIR}/mkSyncedSecret.sh $SECRET ${TNS}

# secret type 'token' used by Skopeo
SECRET=jenkins-token-${TNS}
TOKEN=$(oc -n $TNS sa get-token jenkins)
oc -n $TNS create secret generic $SECRET --from-literal=secrettext=$TOKEN --dry-run -o yaml | oc -n $TNS apply -f - 1>/dev/null
sh ${EXECDIR}/mkSyncedSecret.sh $SECRET ${TNS}




# create and apply the Jenkins CASC Plugin configmap that will be volume-mounted to $JENKINS_HOME/jenkins.yaml
echo -e "\n- Applying resource: Jenkins CASC Configmap (newly generated)"
oc -n $TNS delete configmap jenkins-casc 1>/dev/null && \
oc -n $TNS create configmap jenkins-casc  \
   --from-file=jenkins.yaml=${EXECDIR}/jenkins-casc-config.yaml  \
   --dry-run -o yaml  \
   | oc -n $TNS apply -f - 1>/dev/null


#################################
# apply ALL templates
#################################

echo -e "\n- Processing templates from dir: ${EXECDIR}/"

for TPL in `ls ${EXECDIR}/*.TEMPLATE.yaml`; do
   # get TPL base name
   TPL_BASE=${TPL%.TEMPLATE.yaml}
   # need to allow for MULTIPLE parameter files !!
   # split filelist into array
   #### NOTE: DONT USE SPACES IN FILENAMES, or this code breaks!
   PARAM_FILES=($(ls ${TPL_BASE}.PARAMETERS*.yaml))
   echo "  processing ${#PARAM_FILES[@]} parameter files for $TPL "
   for idx in "${!PARAM_FILES[@]}"; do
      PARAMFILE=${PARAM_FILES[idx]}
      echo "     +  parameter file: $PARAMFILE"
      OUTPUT=${TPL_BASE}.GENERATED-${idx}.yaml
      oc -n $TNS process -f $TPL --param-file=${PARAMFILE} -o yaml > ${OUTPUT}
      oc -n $TNS apply -f $OUTPUT  1>/dev/null
   done 
   echo
done

#!/bin/sh

EXECDIR=$(dirname $0)

# set Namespace string: either from arg $1 or default.
TNS=${1?You need to specify a NAMESPACE parameter when calling $0 .}

echo -e "\n- Working on namespace: $TNS\n"

#################################
# apply Jenkins manifests
#################################

echo -e "\n- Applying manifests from dir: ${EXECDIR}/"
## parse file-list int array variable: YAMLS
YAMLS=($(find ${EXECDIR} -name "*.yaml"))
# echo "count: ${#YAMLS[@]} ."
if [[ ${#YAMLS[@]} > 0  ]]; then
  for F in ${YAMLS[@]}; do 
    echo -e "  Applying: $F"
    oc -n $TNS apply -f $F 1>/dev/null
  done
else
  echo "  no *.yaml files found."
fi
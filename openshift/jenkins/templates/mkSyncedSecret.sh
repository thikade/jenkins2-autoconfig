
if [ "$1" = "-h" -o "$1" = "--help" ]; then
   cat <<- EOM
   
   Usage: $0  - script to add labels/annotations so that SECRET will be synced into Jenkins credentials.
                Optional arg. CREDNETIAL-ID will override the ID of the secret as used by Jenkins!
                 
        $0 SECRET [ NAMESPACE ] [ CREDENTIAL-ID ]

EOM
   exit 0
fi

SECRET=${1?Error: missing argument SECRET}
shift 1
# get arg: Namespace (can be left empty)
SECRET_NAMESPACE=${1}
shift 1
# get arg: credential-id (can be left empty)
CRED_ID=${1:-$SECRET}
shift 1

cat << EOM
  adding secret to Jenkins credential store: ${SECRET_NAMESPACE:+$SECRET_NAMESPACE/}${SECRET} as $CRED_ID
EOM

SECRET_NAMESPACE=${SECRET_NAMESPACE:+ -n $SECRET_NAMESPACE}

oc ${SECRET_NAMESPACE} annotate secret $SECRET jenkins.openshift.io/secret.name=$CRED_ID --overwrite 1>/dev/null
oc ${SECRET_NAMESPACE} label secret $SECRET credential.sync.jenkins.openshift.io=true --overwrite    1>/dev/null


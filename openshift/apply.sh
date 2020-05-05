#!/bin/sh

THIS_NS=${1?You forgot to specify the namespace argument.}

# make that apply can be called with a path to script
EXECDIR=$(dirname $0)
TEMPLATE_DIR=$EXECDIR/templates
MANIFEST_DIR=$EXECDIR/manifests

echo -e "\n=== Processing TEMPLATE folder: $TEMPLATE_DIR  ==="
sh $TEMPLATE_DIR/apply.sh $THIS_NS

echo -e "\n=== Processing MANIFEST folder: $MANIFEST_DIR  ==="
sh $MANIFEST_DIR/apply.sh $THIS_NS
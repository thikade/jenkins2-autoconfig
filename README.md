# jenkins2-autoconfig

## About
This project showcases a few things:
1. Fully configured ephemeral Jenkins using the "Configuratio-As-Code" Plugin to load Jenkins config from a yaml file.
1. Automatic Jenkins Job creation by utilizing Openshift Sync Plugin and automatically syncing:
  - Openshift secrets
  - Openshift Pipeline Buildconfigs into Jenkins jobs.
1. Run Ansible Playbooks from a Jenkins pipeline  
   Our Playbook will create a few projects and setup initial project builds, deployments, etc. 

## Prerequisites
- To be able to create projects, Jenkins Serviceaccount requires the *self-provisioner* role.
  `oc adm policy add-cluster-role-to-user self-provisioner --rolebinding-name self-provisioner-jenkins -z jenkins -n <NAMESPACE>`
- tbd

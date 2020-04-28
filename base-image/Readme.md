
### Steps for using the image:
* execute 'oc create (or replace) -f template_digi-jenkins-v1.yaml -n openshift' to make the te template generally available in openshift (or update it)
* create a physical volume like the ones in folder test or prod - the name must be identical with the jenkins name specified in next step
* open the openshift webconsole, chose the jenkins digi template from the catalogue and follow the steps in the widget...

## Details on the Jenkins S2I Build Process
**Q:** "How can we add more plugins and configurations?  
**A:** Jenkins Image from Red Hat _registry.redhat.io/openshift3/jenkins-2-rhel7:v3.11_ is a S2I Image builder itself. That means you can use Jenkins to customize and build another Jenkins base image!
Read [Docs](https://docs.openshift.com/container-platform/3.11/using_images/other_images/jenkins.html) for more details.

1. Do a source build from Jenkins image *openshift3/jenkins-2-rhel7:v3.11*. You can put a few special files into your repository that will be recognized by S2I builder:
   - **plugins.txt** - List plugins an versions that should be downloaded and installed.  
   Format: `pluginId:pluginVersion` (one entry per line)  
   or   
   **plugins/** - This directory contains binary Jenkins plug-ins you want to install.  
   *Caution:  
   If you use a persistent Jenkins home, plugins will be copied & installed only on the first run during which the persistent volume is initialized by Jenkins.  
   To install or update plugins at a later time, you will need to set the env OVERRIDE_PV_PLUGINS_WITH_IMAGE_PLUGINS to true in your Jenkins DC!*

   - **configuration/someConfig.xml** - The contents of the configuration/ directory will be copied into the /var/lib/jenkins/ directory (effectively overwriting existing plugin configs). This will also be done only on first Jenkins run if you use a persistent volume. You will need to set *OVERRIDE_PV_CONFIG_WITH_IMAGE_CONFIG=true* **but use with caution!** You will probably overwrite your custom plugin configuration that is already stored on you PV!

   - **JENKINS_JAVA_OVERRIDES** - This env variable enables you to set custom Java Properties for Jenkins. Eg. `-Dfoo -Dbar`
   - **configuration/jobs** - Predefined Job definitions

## Details on the Jenkins Openshift Sync Plugin
Jenkins OpenShift Sync Plugin can sync Jobs and Secrets from watched Openshift namespaces. This is extremely useful to manage Jenkins Secrets in Openshift!  
[Details & Docs](https://github.com/openshift/jenkins-sync-plugin)

The type of secret will be mapped to the Jenkins credential type as follows:

- With *Opaque-type* `Secret` objects the plug-in looks for *username* and *password* in the data section and constructs a Jenkins UsernamePasswordCredentials credential. Remember, in OpenShift Container Platform the password field can be either an actual password or the user’s unique token. If those are not present, it will look for the ssh-privatekey field and create a Jenkins BasicSSHUserPrivateKey credential.

- "kubernetes.io/basic-auth" secrets map to Jenkins Username / Password credentials
- Opaque/generic secrets where the data has a *username* key and a *password* key map to Jenkins Username / Password credentials
- "kubernetes.io/ssh-auth" map to Jenkins SSH User credentials
- Opaque/generic secrets where the data has a *ssh-privatekey* map to Jenkins SSH User credentials
- Opaque/generic secrets where the data has a *secrettext* key map to Jenkins Secret Text credentials
- Opaque/generic secrets where the data has a *openshift-client-token* key map to **Jenkins OpenShift Client Plugin Token credentials**

### Usage examples
All secrets need the label `credential.sync.jenkins.openshift.io=true` to be watched by the Sync Plugin and registered with the Jenkins Credentials Plugin.  
The name of the credential can be set/overridden using the annotation `enkins.openshift.io/secret.name=mySecretNameinJenkins`.  

- **OpenShift Token**  
Create an Openshift Token credential in Jenkins used by eg Skopeo or `oc --token xxx`
```
SECRET=jenkins-token-prod-projectname
oc create   secret generic $SECRET  --from-literal=secrettext=$TOKEN
oc annotate secret $SECRET jenkins.openshift.io/secret.name=$SECRET
oc label    secret $SECRET credential.sync.jenkins.openshift.io=true
```

- **OpenShift Client Plugin Token**  
Create an Openshift Token credential in Jenkins used by eg Skopeo or `oc --token xxx`
```
SECRET=jenkins-token-prod-projectname
oc create   secret generic $SECRET  --from-literal=openshift-client-token=$TOKEN
oc annotate secret $SECRET jenkins.openshift.io/secret.name=$SECRET
oc label    secret $SECRET credential.sync.jenkins.openshift.io=true
```

- **Bitbucket Username & Password**  
Create a username/password Jenkins credential.
```
SECRET=bitbucket
oc create   secret generic $SECRET  --from-literal=username=USER --from-literal=password=PASS
oc annotate secret $SECRET jenkins.openshift.io/secret.name=$SECRET
oc label    secret $SECRET credential.sync.jenkins.openshift.io=true
```

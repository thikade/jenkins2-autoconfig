FROM dummy

USER root

RUN yum -y update
RUN yum -y install epel-release
RUN yum -y install skopeo libselinux-utils python-pip ansible java-11-openjdk-devel.x86_64
# RUN yum clean all && \
RUN rm -f /etc/localtime && ln -s /usr/share/zoneinfo/Europe/Vienna /etc/localtime

# install custom Maven
RUN cd /opt && curl -L -o maven.tgz  http://www-eu.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz && \
    tar xzf maven.tgz && ln -s apache-maven-3.5.4 maven && \
    mkdir -p ${JENKINS_HOME}/tools/hudson.tasks.Maven_MavenInstallation/ && ln -s /opt/apache-maven-3.5.4 ${JENKINS_HOME}/tools/hudson.tasks.Maven_MavenInstallation/Maven-3.5.0 && \
    chgrp -R root maven* ${JENKINS_HOME}


# COPY cert/*.cer /etc/pki/ca-trust/source/anchors/
# RUN update-ca-trust extract

USER 1000

FROM dummy

USER root

ENV ANSIBLE_VER=2.9.27
ENV PY_VER=39
ENV PY_DOT_VER=3.9

# RUN yum repolist all 
# RUN yum module list 
# RUN yum module list --enabled 
# RUN yum module info python${PY_VER}:${PY_DOT_VER}

# RUN yum -y update \

RUN yum install -y skopeo libselinux-utils java-11-openjdk-devel.x86_64

RUN yum module enable -y python${PY_VER}:${PY_DOT_VER} \
    && yum module install -y python${PY_VER}:${PY_DOT_VER} \
    && pip${PY_DOT_VER} --version 
    
RUN update-alternatives --set python  /usr/bin/python${PY_DOT_VER} \
    && python --version && pip${PY_DOT_VER} --version

RUN pip${PY_DOT_VER} install ansible==${ANSIBLE_VER} && ansible --version

# RUN yum clean all 

RUN rm -f /etc/localtime && ln -s /usr/share/zoneinfo/Europe/Vienna /etc/localtime

# install custom Maven
RUN cd /opt && curl -L -o maven.tgz  http://www-eu.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz && \
    tar xzf maven.tgz && ln -s apache-maven-3.5.4 maven && \
    mkdir -p ${JENKINS_HOME}/tools/hudson.tasks.Maven_MavenInstallation/ && ln -s /opt/apache-maven-3.5.4 ${JENKINS_HOME}/tools/hudson.tasks.Maven_MavenInstallation/Maven-3.5.0 && \
    chgrp -R root maven* ${JENKINS_HOME}


# COPY cert/*.cer /etc/pki/ca-trust/source/anchors/
# RUN update-ca-trust extract

USER 10001

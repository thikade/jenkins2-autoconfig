FROM dummy

USER root

RUN yum update
RUN yum -y install skopeo atomic-openshift-clients maven libselinux-utils firefox && \
# RUN yum clean all && \
RUN rm -f /etc/localtime && ln -s /usr/share/zoneinfo/Europe/Vienna /etc/localtime

# install custom Maven
RUN cd /opt && curl -L -o maven.tgz  http://www-eu.apache.org/dist/maven/maven-3/3.5.4/binaries/apache-maven-3.5.4-bin.tar.gz && \
    tar xzf maven.tgz && ln -s apache-maven-3.5.4 maven && \
    mkdir -p /var/jenkins_home/tools/hudson.tasks.Maven_MavenInstallation/ && ln -s /opt/apache-maven-3.5.4 /var/jenkins_home/tools/hudson.tasks.Maven_MavenInstallation/Maven-3.5.0 && \
    chgrp -R root maven* /var/jenkins_home

RUN curl -L https://github.com/optiopay/klar/releases/download/v2.4.0/klar-2.4.0-linux-amd64 -o /usr/local/bin/klar && \
    chmod +x /usr/local/bin/klar


# COPY cert/*.cer /etc/pki/ca-trust/source/anchors/
# RUN update-ca-trust extract

ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0

USER 1000
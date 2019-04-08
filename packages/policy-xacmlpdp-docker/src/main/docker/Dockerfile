FROM alpine:3.9

ARG HTTP_PROXY=${HTTP_PROXY}
ARG HTTPS_PROXY=${HTTPS_PROXY}
ARG BUILD_VERSION=${BUILD_VERSION}
ARG POLICY_LOGS=/var/log/onap/policy/pdpx

ENV http_proxy $HTTP_PROXY
ENV https_proxy $HTTPS_PROXY
ENV BUILD_VERSION ${BUILD_VERSION}
ENV POLICY_LOGS ${POLICY_LOGS}

ENV POLICY_HOME=/opt/app/policy/pdpx

RUN \
    apk add --no-cache --update busybox-extras bash nss procps coreutils findutils grep \
                                zip unzip curl wget openssh maven openjdk8 jq httpie py-pip

RUN addgroup policy && \
    adduser -S --shell /bin/bash -G policy policy

RUN mkdir -p ${POLICY_HOME} ${POLICY_LOGS} ${POLICY_HOME}/etc/ssl ${POLICY_HOME}/bin ${POLICY_HOME}/apps && \
    chown  -R policy:policy ${POLICY_HOME} ${POLICY_LOGS}

RUN mkdir /packages
COPY /maven/* /packages
RUN tar xvfz /packages/policy-xacmlpdp.tar.gz --directory ${POLICY_HOME} && \
    rm /packages/policy-xacmlpdp.tar.gz

WORKDIR ${POLICY_HOME}
COPY policy-pdpx.sh  bin/.
RUN chown -R policy:policy * && chmod +x bin/*.sh

USER policy
WORKDIR ${POLICY_HOME}/bin
ENTRYPOINT [ "bash", "./policy-pdpx.sh" ]

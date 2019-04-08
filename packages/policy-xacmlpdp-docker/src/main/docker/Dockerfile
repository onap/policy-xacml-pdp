FROM alpine:3.9

ARG HTTP_PROXY=${HTTP_PROXY}
ARG HTTPS_PROXY=${HTTPS_PROXY}
ARG BUILD_VERSION=${BUILD_VERSION}
ARG POLICY_LOGS=/var/log/onap/policy/pdpx

ENV http_proxy $HTTP_PROXY
ENV https_proxy $HTTPS_PROXY
ENV BUILD_VERSION ${BUILD_VERSION}
ENV POLICY_LOGS ${POLICY_LOGS}

ENV POLICY_HOME=/opt/app/policy
ENV POLICY_PDPX_HOME=${POLICY_HOME}/pdpx
ENV POLICY_PDPX_APPS=${POLICY_PDPX_HOME}/apps

RUN \
    apk add --no-cache --update busybox-extras bash nss procps coreutils findutils grep \
                                zip unzip curl wget openssh maven openjdk8 jq httpie py-pip


RUN addgroup policy && \
    adduser -S --shell /bin/bash -G policy policy

RUN mkdir -p ${POLICY_PDPX_HOME} ${POLICY_LOGS} ${POLICY_HOME}/etc/ssl ${POLICY_PDPX_HOME}/bin ${POLICY_PDPX_APPS} && \
    chown  -R policy:policy ${POLICY_HOME} ${POLICY_PDPX_HOME} ${POLICY_LOGS}

RUN mkdir /packages
COPY /maven/* /packages
RUN tar xvfz /packages/policy-xacmlpdp.tar.gz --directory ${POLICY_PDPX_HOME} && \
    rm /packages/policy-xacmlpdp.tar.gz

WORKDIR ${POLICY_PDPX_HOME}
COPY policy-pdpx.sh  bin/.
RUN chown -R policy:policy * && chmod +x bin/*.sh && \
    cp ${POLICY_PDPX_HOME}/etc/ssl/* ${POLICY_HOME}/etc/ssl && chown policy:policy ${POLICY_HOME}/etc/ssl/* && \
    cp ${POLICY_PDPX_APPS}/. ${POLICY_HOME}/apps && chown policy:policy ${POLICY_HOME}/apps

USER policy
WORKDIR ${POLICY_PDPX_HOME}/bin
ENTRYPOINT [ "bash", "./policy-pdpx.sh" ]

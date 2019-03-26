FROM onap/policy-common-alpine

LABEL maintainer="Policy Team"

ARG POLICY_LOGS=/var/log/onap/policy/pdpx

ENV POLICY_LOGS ${POLICY_LOGS}
ENV POLICY_PDPX_HOME=${POLICY_HOME}/pdpx

RUN mkdir -p ${POLICY_PDPX_HOME} ${POLICY_LOGS} ${POLICY_PDPX_HOME}/bin && \
    chown  -R policy:policy ${POLICY_PDPX_HOME} ${POLICY_LOGS}

RUN mkdir /packages
COPY /maven/* /packages
RUN tar xvfz /packages/policy-xacmlpdp.tar.gz --directory ${POLICY_PDPX_HOME} && \
    rm /packages/policy-xacmlpdp.tar.gz

WORKDIR ${POLICY_PDPX_HOME}
COPY policy-pdpx.sh  bin/.
RUN chown -R policy:policy * && chmod +x bin/*.sh && \
    cp ${POLICY_PDPX_HOME}/etc/ssl/* ${POLICY_HOME}/etc/ssl && chown policy:policy ${POLICY_HOME}/etc/ssl/*

USER policy
WORKDIR ${POLICY_PDPX_HOME}/bin
ENTRYPOINT [ "bash", "./policy-pdpx.sh" ]

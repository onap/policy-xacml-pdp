ARG BUILD_VERSION=${BUILD_VERSION}

FROM onap/policy-xacml-pdp:${BUILD_VERSION}

ARG BUILD_NAME=${BUILD_NAME}
ARG BUILD_VERSION=${BUILD_VERSION}

ENV BUILD_NAME $BUILD_NAME
ENV BUILD_VERSION $BUILD_VERSION

ADD maven/$BUILD_NAME-$BUILD_VERSION.jar /opt/app/policy/pdpx/lib/$BUILD_NAME-$BUILD_VERSION.jar

RUN mkdir -p /opt/app/policy/pdpx/apps/tutorial

COPY --chown=policy:policy xacml.properties /opt/app/policy/pdpx/apps/tutorial

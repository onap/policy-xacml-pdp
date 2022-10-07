# Running Docker XACML Tutorial

## Getting XACML Tutorial Images Up

 1. cd /policy-xacml-pdp/tutorials/tutorial-xacml-application
 2. Run 'mvn clean install -Dmaven.test.skip=true -DskipTests -DskipIntegrationTests -DskipUnitTests -Dcheckstyle.skip -P docker'

## Setting Up and Starting from policy/docker components

 1. Clone https://git.onap.org/policy/docker/
 2. cd /docker/csit
 3. Run the following to set the containers location, project, branch, and version:
	- 'export CONTAINER_LOCATION=nexus3.onap.org:10001/'
	- 'export PROJECT=pap'
	- 'export GERRIT_BRANCH=master'
	- 'get-branch.sh'
	- 'get-versions.sh'
 4. Run 'docker image ls'
	- Take note of the REPOSITORY "onap/policy/xacml-tutorial" and its TAG
	- This refers to the image from our mvn clean install from above
 5. Edit docker-compose-all.sh
	- Replace xacml-pdp image with the format "REPOSITORY:TAG" as noted in Step 4
		- ex. image: onap/policy-xacml-tutorial:2.7.1-SNAPSHOT

## Running the Containers and Testing

Run 'docker-compose -f docker-compose-all.yml up xacml-pdp'
 
## Stopping the Containers

Run 'docker-compose -f docker-compose-all.yml down'

## Verification Example Calls

Verify that the components are accessible:

 1. curl -X POST http://0.0.0.0:3904/events/POLICY-PDP-PAP
	- Should return JSON similar to this: {"serverTimeMs":0,"count":0}

 2. curl -k -u 'healthcheck:zb!XztG34' 'https://0.0.0.0:6969/policy/pdpx/v1/healthcheck'
	- Should return JSON similar to this: {"name":"Policy Xacml PDP","url":"self","healthy":true,"code":200,"message":"alive"}

 3. curl -k -u 'healthcheck:zb!XztG34' 'https://0.0.0.0:6767/policy/api/v1/healthcheck'
	- Should return JSON similar to this: {"name": "Policy API","url": "policy-api","healthy": true,"code": 200,"message": "alive"}

 4. curl -k -u 'healthcheck:zb!XztG34' 'https://0.0.0.0:6868/policy/pap/v1/healthcheck'
	- Should return JSON similar to this: {"name": "Policy PAP","url": "policy-pap","healthy": true,"code": 200,"message": "alive"}

## POSTMAN Collection

You can find the collection under /policy-xacml-pdp/tutorials-tutorial-xacml-application/postman/

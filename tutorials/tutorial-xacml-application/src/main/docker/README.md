# Running Docker XACML Tutorial

## Building XACML Tutorial Docker Image

 1. ```cd /policy-xacml-pdp/tutorials/tutorial-xacml-application```
 2. ```mvn clean install -Dmaven.test.skip=true -DskipTests -DskipIntegrationTests -DskipUnitTests -Dcheckstyle.skip -P docker```

## Running the Tutorial (Automatically)
```./run-tutorial.sh```

## Running the Tutorial (Manually)
### Setting Up and Starting from policy/docker components

 1. Clone ```https://git.onap.org/policy/docker/```
 2. ```cd /docker```
 3. Run the following to set the containers location, project, branch, and version:
	- ```export CONTAINER_LOCATION=nexus3.onap.org:10001/```
	- ```export PROJECT=pap```
	- ```export GERRIT_BRANCH=master```
	- ```get-branch.sh```
	- ```get-versions.sh```
 4. Run ```docker image ls```
	- Take note of the REPOSITORY ```onap/policy/xacml-tutorial``` and its ```TAG```
	- This refers to the image from our `mvn clean install` from above
 5. Edit ```compose.yaml```
	- Replace xacml-pdp image with the format "REPOSITORY:TAG" as noted in Step 4
		- ex. image: ```onap/policy-xacml-tutorial:3.1.1-SNAPSHOT```

### Running the Containers and Testing

Run ```./compose/start-compose.sh xacml-pdp```

### Triggering policy notification update

Run python script ```kafka_producer.py``` under docker repository with topic name and message as parameters.
i.e. ```python3 /docker/csit/resources/tests/kafka_producer.py POLICY-PDP-PAP "message"```
(update "message" to the usual json body)

## Verification Example Calls

Verify that the components are accessible:

 1. ```curl -k -u 'healthcheck:zb!XztG34' 'https://0.0.0.0:6969/policy/pdpx/v1/healthcheck'```
    - Should return JSON similar to this: ```{"name":"Policy Xacml PDP","url":"self","healthy":true,"code":200,"message":"alive"}```

 2. ```curl -k -u 'healthcheck:zb!XztG34' 'https://0.0.0.0:6767/policy/api/v1/healthcheck'```
    - Should return JSON similar to this: ```{"name": "Policy API","url": "policy-api","healthy": true,"code": 200,"message": "alive"}```

 3. ```curl -k -u 'healthcheck:zb!XztG34' 'https://0.0.0.0:6868/policy/pap/v1/healthcheck'```
    - Should return JSON similar to this: ```{"name": "Policy PAP","url": "policy-pap","healthy": true,"code": 200,"message": "alive"}```

## POSTMAN Collection

You can find the collection under ```/policy-xacml-pdp/tutorials/tutorial-xacml-application/postman/```

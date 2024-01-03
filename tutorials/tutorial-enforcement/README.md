Copyright 2020 AT&T Intellectual Property. All rights reserved.

This file is licensed under the CREATIVE COMMONS ATTRIBUTION 4.0 INTERNATIONAL LICENSE
Full license text at https://creativecommons.org/licenses/by/4.0/legalcode

# Build the Tutorial

The Policy Enforcement Tutorial can be built:

    mvn clean install

# Start the Policy Framework components

Be sure to start the Policy Framework application components in *docker* if you are not testing this in a lab.
- Ensure you have docker and docker-compose installed
- Check out the *policy/docker* repo from the ONAP gerrit or from github: https://github.com/onap/policy-docker
- In a console, change directory into the *compose* directory of the *policy/docker* repo
- Start *xacml-pdp* by running the *start-compose.sh* script

    start-compose.sh xacml-pdp

- Run python script ```kafka_producer.py``` under docker repository with topic name and message as parameters.
  i.e. ```python3 /docker/csit/resources/tests/kafka_producer.py POLICY-PDP-PAP "message"```
  (update "message" to the usual json body)

- Run the xacml-pdp health check

    curl -u 'policyadmin:zb!XztG34' 'http://0.0.0.0:30441/policy/pdpx/v1/healthcheck'
    Should return JSON similar to this:
    {"name":"Policy Xacml PDP","url":"self","healthy":true,"code":200,"message":"alive"}

- Run the api health check

    curl -u 'policyadmin:zb!XztG34' 'http://0.0.0.0:30440/policy/api/v1/healthcheck'
    Should return JSON similar to this:
    {
        "name": "Policy API",
        "url": "policy-api",
        "healthy": true,
        "code": 200,
        "message": "alive"
    }

- Run the pap health check

    curl -u 'policyadmin:zb!XztG34' 'http://0.0.0.0:30442/policy/pap/v1/healthcheck'
    Should return JSON similar to this:
    {
        "name": "Policy PAP",
        "url": "policy-pap",
        "healthy": true,
        "code": 200,
        "message": "alive"
    }

# Run the Tutorial

You can run the application via code by running the *App.main* method with command line argument with IP then Port
of the XACML PDP, followed by the IP then Port of kafka.

    App.main(new String[] {"0.0.0.0", "6969", "0.0.0.0", "9092"});

or from Eclipse by right-clicking App.java and selecting *Run As* and select *Java Application*. Edit the
configuration by adding these command line arguments: "0.0.0.0" "6969" "0.0.0.0" "9092"

Quit the application by typing 'q' into stdin.

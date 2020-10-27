Copyright 2020 AT&T Intellectual Property. All rights reserved.
This file is licensed under the CREATIVE COMMONS ATTRIBUTION 4.0 INTERNATIONAL LICENSE
Full license text at https://creativecommons.org/licenses/by/4.0/legalcode

The Policy Enforcement Tutorial can be built:

mvn clean install

Be sure to start the Policy Framework application components if you are not testing this in a lab. See
src/main/docker/README.txt for details to run local instances of the components.

You can run the application via code by running the App.main method with command line argument with IP then Port
of the XACML PDP, followed by the IP then Port of Dmaap.

App.main(new String[] {"0.0.0.0", "6969", "0.0.0.0", "3904"});

or from Eclipse by right-clicking App.java and selecting "Run As" and select "Java Application". Edit the
configuration by adding these command line arguments: "0.0.0.0" "6969" "0.0.0.0" "3904"

Quit the application by typing 'q' into stdin.

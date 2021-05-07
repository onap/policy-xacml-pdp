*** Settings ***
Library     Collections
Library     RequestsLibrary
Library     OperatingSystem
Library     Process
Library     json

*** Test Cases ***
Healthcheck
     [Documentation]  Verify policy xacml-pdp health check
     ${resp}=  PeformGetRequest  /policy/pdpx/v1/healthcheck  200
     Should Be Equal As Strings    ${resp.json()['code']}  200

Statistics
     [Documentation]  Verify policy xacml-pdp statistics
     ${resp}=  PeformGetRequest  /policy/pdpx/v1/statistics  200
     Should Be Equal As Strings    ${resp.json()['code']}  200

MakeTopics
     [Documentation]    Creates the Policy topics
     ${result}=     Run Process     ${SCR_DMAAP}/make_topic.sh   POLICY-PDP-PAP
     Should Be Equal As Integers        ${result.rc}    0

ExecuteXacmlPolicy
     Wait Until Keyword Succeeds    0 min   15 sec  CreateMonitorPolicy
     Wait Until Keyword Succeeds    0 min   15 sec  CreateOptimizationPolicy
     Wait Until Keyword Succeeds    0 min   15 sec  GetDefaultDecision
     Wait Until Keyword Succeeds    0 min   15 sec  DeployPolicies
     Wait Until Keyword Succeeds    0 min   15 sec  GetStatisticsAfterDeployed
     Wait Until Keyword Succeeds    0 min   15 sec  GetAbbreviatedDecisionResult
     Wait Until Keyword Succeeds    0 min   15 sec  GetMonitoringDecision
     Wait Until Keyword Succeeds    0 min   15 sec  GetNamingDecision
     Wait Until Keyword Succeeds    0 min   15 sec  GetOptimizationDecision
     Wait Until Keyword Succeeds    0 min   15 sec  GetStatisticsAfterDecision
     Wait Until Keyword Succeeds    0 min   15 sec  UndeployMonitorPolicy
     Wait Until Keyword Succeeds    0 min   15 sec  GetStatisticsAfterUndeploy

*** Keywords ***

CreateMonitorPolicy
     [Documentation]  Create a Monitoring policy
     CreatePolicy  /policy/api/v1/policytypes/onap.policies.monitoring.tcagen2/versions/1.0.0/policies  200  vCPE.policy.monitoring.input.tosca.json  onap.restart.tca  1.0.0

CreateOptimizationPolicy
     [Documentation]  Create an Optimization policy
     CreatePolicy  /policy/api/v1/policytypes/onap.policies.optimization.resource.AffinityPolicy/versions/1.0.0/policies  200  vCPE.policies.optimization.input.tosca.json  OSDF_CASABLANCA.Affinity_Default  1.0.0

GetDefaultDecision
    [Documentation]  Get Default Decision with no policies in Xacml PDP
     ${resp}=  PerformPostRequest  /policy/pdpx/v1/decision  abbrev=true  ${POLICY_PDPX_IP}  200  onap.policy.guard.decision.request.json  ${CURDIR}/data
     ${status}=  Get From Dictionary  ${resp.json()}  status
     Should Be Equal As Strings  ${status}  Permit

DeployPolicies
     [Documentation]   Runs Policy PAP to deploy a policy
     ${resp}=  PerformPostRequest  /policy/pap/v1/pdps/policies  null  ${POLICY_PAP_IP}  202  vCPE.policy.input.tosca.deploy.json  ${CURDIR}/data
     Sleep      5s
     ${result}=     Run Process    ${SCR_DMAAP}/wait_topic.sh    POLICY-PDP-PAP
     ...            responseTo    xacml    ACTIVE    onap.restart.tca

GetStatisticsAfterDeployed
     [Documentation]  Verify policy xacml-pdp statistics after policy is deployed
     ${resp}=  PeformGetRequest  /policy/pdpx/v1/statistics  200
     Should Be Equal As Strings  ${resp.json()['code']}  200
     Should Be Equal As Strings  ${resp.json()['totalPoliciesCount']}  3

GetAbbreviatedDecisionResult
    [Documentation]    Get Decision with abbreviated results from Policy Xacml PDP
     ${resp}=  PerformPostRequest  /policy/pdpx/v1/decision  abbrev=true  ${POLICY_PDPX_IP}  200  onap.policy.monitoring.decision.request.json  ${CURDIR}/data
     ${policy}=    Get From Dictionary    ${resp.json()['policies']}   onap.restart.tca
     Dictionary Should Contain Key    ${policy}    type
     Dictionary Should Contain Key    ${policy}    metadata
     Dictionary Should Not Contain Key    ${policy}    type_version
     Dictionary Should Not Contain Key    ${policy}    properties
     Dictionary Should Not Contain Key    ${policy}    name
     Dictionary Should Not Contain Key    ${policy}    version

GetMonitoringDecision
    [Documentation]    Get Decision from Monitoring Policy Xacml PDP
     ${resp}=  PerformPostRequest  /policy/pdpx/v1/decision  null  ${POLICY_PDPX_IP}  200  onap.policy.monitoring.decision.request.json  ${CURDIR}/data
     ${policy}=    Get From Dictionary    ${resp.json()['policies']}   onap.restart.tca
     Dictionary Should Contain Key    ${policy}    type
     Dictionary Should Contain Key    ${policy}    metadata
     Dictionary Should Contain Key    ${policy}    type_version
     Dictionary Should Contain Key    ${policy}    properties
     Dictionary Should Contain Key    ${policy}    name
     Dictionary Should Contain Key    ${policy}    version

GetNamingDecision
    [Documentation]    Get Decision from Naming Policy Xacml PDP
     ${resp}=  PerformPostRequest  /policy/pdpx/v1/decision  null  ${POLICY_PDPX_IP}  200  onap.policy.naming.decision.request.json  ${CURDIR}/data
     ${policy}=    Get From Dictionary    ${resp.json()['policies']}   SDNC_Policy.ONAP_NF_NAMING_TIMESTAMP
     Dictionary Should Contain Key    ${policy}    type
     Dictionary Should Contain Key    ${policy}    type_version
     Dictionary Should Contain Key    ${policy}    properties
     Dictionary Should Contain Key    ${policy}    name

GetOptimizationDecision
    [Documentation]    Get Decision from Optimization Policy Xacml PDP
     ${resp}=  PerformPostRequest  /policy/pdpx/v1/decision  null  ${POLICY_PDPX_IP}  200  onap.policy.optimization.decision.request.json  ${CURDIR}/data
     ${policy}=    Get From Dictionary    ${resp.json()['policies']}   OSDF_CASABLANCA.Affinity_Default
     Dictionary Should Contain Key    ${policy}    type
     Dictionary Should Contain Key    ${policy}    type_version
     Dictionary Should Contain Key    ${policy}    properties
     Dictionary Should Contain Key    ${policy}    name

GetStatisticsAfterDecision
     [Documentation]    Runs Policy Xacml PDP Statistics after Decision request
     ${resp}=  PeformGetRequest  /policy/pdpx/v1/statistics  200
     Should Be Equal As Strings    ${resp.json()['code']}  200
     Should Be Equal As Strings    ${resp.json()['permitDecisionsCount']}     4
     Should Be Equal As Strings    ${resp.json()['notApplicableDecisionsCount']}     1

UndeployMonitorPolicy
     [Documentation]    Runs Policy PAP to undeploy a policy
     PeformDeleteRequest  /policy/pap/v1/pdps/policies/onap.restart.tca  202

GetStatisticsAfterUndeploy
     [Documentation]    Runs Policy Xacml PDP Statistics after policy is undeployed
     ${resp}=  PeformGetRequest  /policy/pdpx/v1/statistics  200
     Should Be Equal As Strings    ${resp.json()['code']}  200
     Should Be Equal As Strings    ${resp.json()['totalPoliciesCount']}     2
     
CreatePolicy
     [Arguments]  ${url}  ${expectedstatus}  ${jsonfile}  ${policyname}  ${policyversion}
     [Documentation]  Create the specific policy
     ${resp}=  PerformPostRequest  ${url}  null  ${POLICY_API_IP}  ${expectedstatus}  ${jsonfile}  ${DATA2}
     Run Keyword If  ${expectedstatus}==200  Dictionary Should Contain Key  ${resp.json()['topology_template']['policies'][0]}  ${policyname}
     Run Keyword If  ${expectedstatus}==200  Should Be Equal As Strings  ${resp.json()['topology_template']['policies'][0]['${policyname}']['version']}  ${policyversion}
     
PerformPostRequest
     [Arguments]  ${url}  ${params}  ${hostname}  ${expectedstatus}  ${jsonfile}  ${filepath}
     ${auth}=  Create List  healthcheck  zb!XztG34
     ${postjson}=  Get file  ${filepath}/${jsonfile}
     Log  Creating session https://${hostname}:6969
     ${session}=  Create Session  policy  https://${hostname}:6969  auth=${auth}
     ${headers}=  Create Dictionary  Accept=application/json  Content-Type=application/json
     ${resp}=  POST On Session  policy  ${url}  params=${params}  data=${postjson}  headers=${headers}  expected_status=${expectedstatus}
     Log  Received response from policy ${resp.text}
     [return]  ${resp}

PeformGetRequest
     [Arguments]  ${url}  ${expectedstatus}
     ${auth}=  Create List  healthcheck  zb!XztG34
     Log  Creating session https://${POLICY_PDPX_IP}:6969
     ${session}=  Create Session  policy  https://${POLICY_PDPX_IP}:6969  auth=${auth}
     ${headers}=  Create Dictionary  Accept=application/json  Content-Type=application/json
     ${resp}=  GET On Session  policy  ${url}  headers=${headers}  expected_status=${expectedstatus}
     Log  Received response from policy ${resp.text}
     [return]  ${resp}

PeformDeleteRequest
     [Arguments]  ${url}  ${expectedstatus}
     ${auth}=  Create List  healthcheck  zb!XztG34
     Log  Creating session https://${POLICY_PAP_IP}:6969
     ${session}=  Create Session  policy  https://${POLICY_PAP_IP}:6969  auth=${auth}
     ${headers}=  Create Dictionary  Accept=application/json  Content-Type=application/json
     ${resp}=  DELETE On Session  policy  ${url}  headers=${headers}  expected_status=${expectedstatus}
     Log  Received response from policy ${resp.text}

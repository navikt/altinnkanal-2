#!/usr/bin/env groovy

naisPipelineJava {
	gitProject = 'INT' // refers to Stash projects
	environment = 't4'
	zone ='fss'
	namespace ='default'
	envVars = 
		["LDAP_URL=ldap://ldapgw.test.local",
		"LDAP_USER_BASEDN=ou=NAV,ou=BusinessUnits,dc=test,dc=local",
        "SPRING_PROFILES_ACTIVE=local"
		]
}

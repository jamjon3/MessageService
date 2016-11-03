node {
  // stage 'Build and Test'
  def mvnHome = tool 'maven3'
  env.JAVA_HOME = tool 'jdk7'
  env.GRAILS_HOME = tool 'grails2.2.5'

  env.PATH = "${mvnHome}/bin:${env.GRAILS_HOME}/bin:./:${env.PATH}"
  checkout scm
  
  stage('Test') {
      // Run the maven test
      // sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/main.yml --extra-vars 'java_home=${env.JAVA_HOME}' -t 'test'"
  }  
  
  stage('Build') {
      // Run the maven build
      sh "ansible-playbook -i 'localhost,' -c local --vault-password-file=${env.USF_ANSIBLE_VAULT_KEY} ansible/main.yml --extra-vars 'java_home=${env.JAVA_HOME}' -t 'build'"
  }
}

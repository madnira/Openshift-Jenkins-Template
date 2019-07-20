import hudson.model.*;
import jenkins.model.*;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.impl.*;
import org.jenkinsci.plugins.plaincredentials.*;
import org.jenkinsci.plugins.plaincredentials.impl.*;
import org.apache.commons.fileupload.*;

println "--> Creating Credentials"

// Impersonate user to aquire the right privileges
hudson.security.ACL.impersonate(hudson.security.ACL.SYSTEM)

// Register Jenkins users
createUser('nexusid', 'nexusid', 'Nexus Reader')
createUser('sonarid', 'sonarid', 'Sonar Reader')
//createUser('finance-builder-nexus-id', 'finance_builder', 'Finance Builder Nexus')
//createUser('merchandising-builder-nexus-id', 'merchandising_builder', 'Merchandising Builder Nexus')
//createUser('sourcing-builder-nexus-id', 'sourcing_builder', 'Souring Builder Nexus')
//createUser('format-builder-nexus-id', 'format_builder', 'Format Builder Nexus')
//createUser('suppy-chain-builder-nexus-id', 'suppy-chain_builder', 'Supply-Chain Builder Nexus')


createSshUser('jenkins-ssh', 'jenkins-ssh', 'Jenkins Reader', 'ssh-privatekey')
//createSshUser('marketing-builder-id', 'marketing-builder', 'Marketing Builder', 'marketing_builder_rsa')
//createSshUser('finance-builder-id', 'finance-builder', 'Finance Builder', 'finance_builder_rsa')
//createSshUser('merchandising-builder-id', 'merchandising-builder', 'Merchandising Builder', 'merchandising_builder_rsa')
//createSshUser('sourcing-builder-id', 'sourcing-builder', 'Souring Builder', 'sourcing_builder_rsa')
//createSshUser('format-builder-id', 'format-builder', 'Format Builder', 'format_builder_rsa')
//createSshUser('suppy-chain-builder-id', 'suppy-chain-builder', 'Supply-Chain Builder', 'supply-chain_builder_rsa')
//createSshUser('common-builder-id', 'common-builder', 'Common Builder', 'common_builder_rsa')


def createUser(userId, userName, userDescription) {
    def env = System.getenv()
    def password = env[userName]
    def instance = Jenkins.getInstance()
    def system_credentials_provider = SystemCredentialsProvider.getInstance()

    // Add ssh key as private key type
    ssh_credentials_exist = false
    system_credentials_provider.getCredentials().each {
        credentials = (com.cloudbees.plugins.credentials.Credentials) it
        if ( credentials.getId() == userId) {
            ssh_credentials_exist = true
            println("Found existing credentials: " + userId)
        }
    }
    if(!ssh_credentials_exist) {
        println "--> Registering Username/Password Credentials for user " + userName
        Credentials c = (Credentials) new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, userId, userDescription, "admin", password)
        SystemCredentialsProvider.getInstance().getStore().addCredentials(com.cloudbees.plugins.credentials.domains.Domain.global(), c)
    }
    instance.save()
}

def createSshUser(userId, userName, userDescription, keyFile) {
    def env = System.getenv()
    def jenkins_home = env['JENKINS_HOME']
    def instance = Jenkins.getInstance()

    def system_credentials_provider = SystemCredentialsProvider.getInstance()

    def ssh_key_description = userDescription
    def ssh_key_scope = CredentialsScope.GLOBAL
    def ssh_key_id = userId
    def ssh_key_username = userName
    def keyfile = jenkins_home + '/user-keys/' + keyFile
    String fileContents = new File(keyfile).text
    //def ssh_key_private_key_source = new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource(keyfile)
    def ssh_key_private_key_source = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(fileContents)
    def ssh_key_passphrase = null

     // Add ssh key as private key type
    ssh_credentials_exist = false
    system_credentials_provider.getCredentials().each {
        credentials = (com.cloudbees.plugins.credentials.Credentials) it
        if ( credentials.getDescription() == ssh_key_description) {
            ssh_credentials_exist = true
            println("Found existing credentials: " + ssh_key_description)
        }
    }

    if(!ssh_credentials_exist) {
        println "--> Registering SSH Credentials for user " + userName
        def ssh_key_domain = com.cloudbees.plugins.credentials.domains.Domain.global()
        def ssh_key_creds = new BasicSSHUserPrivateKey(ssh_key_scope,ssh_key_id,ssh_key_username,ssh_key_private_key_source,ssh_key_passphrase,ssh_key_description)

        system_credentials_provider.addCredentials(ssh_key_domain,ssh_key_creds)
    }
    instance.save()
}

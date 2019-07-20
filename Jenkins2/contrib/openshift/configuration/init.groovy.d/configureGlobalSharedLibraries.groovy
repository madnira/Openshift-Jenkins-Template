import jenkins.model.Jenkins
import jenkins.plugins.git.GitSCMSource
import jenkins.plugins.git.traits.BranchDiscoveryTrait
import org.jenkinsci.plugins.workflow.libs.GlobalLibraries
import org.jenkinsci.plugins.workflow.libs.LibraryConfiguration
import org.jenkinsci.plugins.workflow.libs.SCMSourceRetriever
import net.sf.json.JSONObject

println 'Configuring Pipeline Global Shared Libraries...\n'

/**
  Function to compare if the two global shared libraries are equal.
 */
boolean isLibrariesEqual(List lib1, List lib2) {
    //compare returns true or false
    lib1.size() == lib2.size() &&
    !(
        false in [lib1, lib2].transpose().collect { l1, l2 ->
            def s1 = l1.retriever.scm
            def s2 = l2.retriever.scm
            l1.retriever.class == l2.retriever.class &&
            l1.name == l2.name &&
            l1.defaultVersion == l2.defaultVersion &&
            l1.implicit == l2.implicit &&
            l1.allowVersionOverride == l2.allowVersionOverride &&
            l1.includeInChangesets == l2.includeInChangesets &&
            s1.remote == s2.remote &&
            s1.credentialsId == s2.credentialsId &&
            s1.traits.size() == s2.traits.size() &&
            !(
                false in [s1.traits, s2.traits].transpose().collect { t1, t2 ->
                    t1.class == t2.class
                }
            )
        }
    )
}

pipeline_shared_libraries = [
    'java': [
        'defaultVersion': 'master',
        'implicit': false,
        'allowVersionOverride': true,
        'includeInChangesets': true,
        'scm': [
            'remote': 'ssh://git@bitbucket-lan.ah.nl:7999/aut/jenkins.git',
            'credentialsId': 'jenkins-ssh'
        ]
    ]
]

if(!binding.hasVariable('pipeline_shared_libraries')) {
    pipeline_shared_libraries = [:]
}

if(!pipeline_shared_libraries in Map) {
    throw new Exception("pipeline_shared_libraries must be an instance of Map but instead is instance of: ${pipeline_shared_libraries.getClass()}")
}

pipeline_shared_libraries = pipeline_shared_libraries as JSONObject

List libraries = [] as ArrayList
pipeline_shared_libraries.each { name, config ->
    if(name && config && config in Map && 'scm' in config && config['scm'] in Map && 'remote' in config['scm'] && config['scm'].optString('remote')) {
        def scm = new GitSCMSource(config['scm'].optString('remote'))
        scm.credentialsId = config['scm'].optString('credentialsId')
        scm.traits = [new BranchDiscoveryTrait()]
        def retriever = new SCMSourceRetriever(scm)
        def library = new LibraryConfiguration(name, retriever)
        library.defaultVersion = config.optString('defaultVersion')
        library.implicit = config.optBoolean('implicit', false)
        library.allowVersionOverride = config.optBoolean('allowVersionOverride', true)
        library.includeInChangesets = config.optBoolean('includeInChangesets', true)
        libraries << library
    }
}

def global_settings = Jenkins.instance.getExtensionList(GlobalLibraries.class)[0]

if(libraries && !isLibrariesEqual(global_settings.libraries, libraries)) {
    global_settings.libraries = libraries
    global_settings.save()
    println 'Configured Pipeline Global Shared Libraries:\n    ' + global_settings.libraries.collect { it.name }.join('\n    ')
}
else {
    if(pipeline_shared_libraries) {
        println 'Nothing changed.  Pipeline Global Shared Libraries already configured.'
    }
    else {
        println 'Nothing changed.  Skipped configuring Pipeline Global Shared Libraries because settings are empty.'
    }
}

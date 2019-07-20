#!groovy

import jenkins.model.*

def env = System.getenv()
def instance = Jenkins.getInstance()

final def pc = new hudson.ProxyConfiguration("proxy.ah.nl", 8080, null, null, env['no_proxy'])
instance.proxy = pc
instance.save()
println "Proxy settings updated!"
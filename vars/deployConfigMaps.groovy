// vars/deployConfigMaps.groovy
def call(String environment, String namespace) {
    def configFile = "configmaps/${environment}/values.yaml"

    // Load and apply the ConfigMap
    script {
        echo "Deploying ConfigMap for environment: ${environment} in namespace: ${namespace}"
        sh "kubectl apply -f ${configFile} -n ${namespace}"
    }
}

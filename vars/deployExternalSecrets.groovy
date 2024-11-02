// vars/deployExternalSecrets.groovy
def call(String environment, String namespace) {
    def clusterSecretStoreFile = "ExternalSecrets/cluster-secret-store.yaml"
    def valuesFile = "ExternalSecrets/values/values-${environment}.yaml"

    // Apply ClusterSecretStore and deploy ExternalSecret
    script {
        echo "Deploying ExternalSecrets for environment: ${environment} in namespace: ${namespace}"

        // Apply ClusterSecretStore
        sh "kubectl apply -f ${clusterSecretStoreFile}"

        // Deploy ExternalSecret using Helm
        sh """
        helm upgrade --install petclinic-${environment}-external-secrets ExternalSecrets \
            -f ${valuesFile} \
            --namespace ${namespace} --create-namespace
        """
    }
}

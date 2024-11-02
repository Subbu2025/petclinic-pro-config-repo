def deployConfigsAndSecrets(String environment, String namespace) {
    // Initialize Helm and apply ClusterSecretStore
    sh "helm repo add external-secrets https://charts.external-secrets.io"
    sh "helm repo update"
    sh "kubectl apply -f ExternalSecrets/cluster-secret-store.yaml"

    // Deploy ExternalSecrets and ConfigMaps for the specified environment
    def valuesFile = "ExternalSecrets/values/values-${environment}.yaml"
    sh """
        helm upgrade --install petclinic-config-${environment} ./ExternalSecrets \
            -f ${valuesFile} \
            --namespace ${namespace} \
            --create-namespace
        kubectl apply -f environments/${environment}/configmaps/ -n ${namespace}
        kubectl apply -f environments/${environment}/secrets/ -n ${namespace}
    """
}

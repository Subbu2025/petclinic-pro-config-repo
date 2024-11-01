// vars/testDockerImageWithMySQL.groovy

def call(Map params = [:]) {
    def mysqlContainerName = "mysql-test"
    def petclinicContainerName = "petclinic-test"
    def petclinicImage = params.get('dockerImage', 'your-default-image') // Replace with a default image if not provided
    def mysqlUser = params.get('mysqlUser', 'petclinic')
    def mysqlPassword = params.get('mysqlPassword', 'petclinic')
    def mysqlDatabase = params.get('mysqlDatabase', 'petclinic')
    def mysqlRootPassword = params.get('mysqlRootPassword', 'root')
    def maxRetries = params.get('maxRetries', 10)
    def retryInterval = params.get('retryInterval', 10)
    
    try {
        // Start MySQL container
        sh """
        docker run -d --name ${mysqlContainerName} \
            -e MYSQL_ROOT_PASSWORD=${mysqlRootPassword} \
            -e MYSQL_DATABASE=${mysqlDatabase} \
            -e MYSQL_USER=${mysqlUser} \
            -e MYSQL_PASSWORD=${mysqlPassword} \
            mysql:8.4
        """
        
        // Wait for MySQL to become ready
        def isMysqlReady = false
        for (int i = 0; i < maxRetries; i++) {
            echo "Waiting for MySQL to be ready (Attempt ${i + 1}/${maxRetries})..."
            def mysqlStatus = sh(script: "docker exec ${mysqlContainerName} mysqladmin ping -u root -p${mysqlRootPassword}", returnStatus: true)
            if (mysqlStatus == 0) {
                isMysqlReady = true
                echo "MySQL is ready."
                break
            }
            sleep retryInterval
        }

        if (!isMysqlReady) {
            error('MySQL container did not become ready.')
        }
        
        // Start PetClinic container linked to MySQL
        sh """
        docker run -d --name ${petclinicContainerName} \
            --link ${mysqlContainerName}:mysql \
            -e MYSQL_URL='jdbc:mysql://mysql:3306/${mysqlDatabase}?useSSL=false&allowPublicKeyRetrieval=true' \
            -e MYSQL_USER=${mysqlUser} \
            -e MYSQL_PASSWORD=${mysqlPassword} \
            -e MYSQL_ROOT_PASSWORD=${mysqlRootPassword} \
            -p 8082:8081 ${petclinicImage}
        """
        
        // Check PetClinic health
        def petclinicHealth = false
        for (int i = 0; i < maxRetries; i++) {
            echo "Checking PetClinic health (Attempt ${i + 1}/${maxRetries})..."
            def healthStatus = sh(script: "curl -s http://localhost:8082/actuator/health | grep UP", returnStatus: true)
            if (healthStatus == 0) {
                petclinicHealth = true
                echo "PetClinic is healthy."
                break
            }
            sleep retryInterval
        }

        if (!petclinicHealth) {
            echo 'Collecting logs from PetClinic container...'
            sh "docker logs ${petclinicContainerName}"
            error('PetClinic application did not become healthy.')
        }

        echo "PetClinic and MySQL containers are running and healthy."
    } finally {
       /* // Clean up containers
        sh "docker stop ${mysqlContainerName} ${petclinicContainerName} || true"
        sh "docker rm ${mysqlContainerName} ${petclinicContainerName} || true" */
    }
}

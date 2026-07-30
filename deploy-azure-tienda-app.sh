#!/bin/bash
set -euo pipefail

# NOTA: Este script asume que se tiene instalado y configurado Azure CLI
# Se puede instalar desde https://docs.microsoft.com/en-us/cli/azure/install-azure-cli
# Y que se ha iniciado sesión:
# Si se está en un entorno sin interfaz gráfica (GUI), se puede usar el comando:
# az login --use-device-code
# que permite completar el proceso de inicio de sesión de forma completamente manual en un dispositivo diferente.
# 
# También se debe establecer la suscripción por defecto:
# az account set --subscription "id-de-suscripción"
# Se puede listar las suscripciones disponibles con:
# az account list --output table

# Variables personalizables
resourceGroup="rg-unir-tienda-app"
# Puede cambiar esto por "northcentralus", "centralus", "southcentralus", etc. Depende si esta disponible Container Apps en la region
location="northcentralus"
environment="env-unir-tienda-app"

# Versión de la imagen de docker
versionImage="3.0"

# Imagenes de docker de los contenedores
eurekaImage="docker.io/osgol/discovery-service:$versionImage"
gatewayImage="docker.io/osgol/gateway-service:$versionImage"
orderImage="docker.io/osgol/order-service:$versionImage"
productImage="docker.io/osgol/product-service:$versionImage"
userImage="docker.io/osgol/user-service:$versionImage"

# Variables de entorno de elasticsearch
elasticSearchHost="vibrant-sassafras-1qpqwpy9.us-east-1.bonsaisearch.net:443"
elasticSearchUser="e8a1040b53"
elasticSearchPass="acb720dba11a489a781b"

# Variables de entorno de la base de datos
databaseOrderUrl="postgresql://thomas.proxy.rlwy.net:50761/railway"
databaseOrderUser="postgres"
databaseOrderPass="AEAefRaAwwZFUEusILEpzUWPkNgwHiNf"

databaseUserUrl="postgresql://sakura.proxy.rlwy.net:38034/railway"
databaseUserrUser="postgres"
databaseUserPass="hCNqPqtDcwFmYXgiGkzVYRtQQbygrtPo"

# Eliminar grupo de recursos si ya existe
if az group exists --name $resourceGroup | grep -q true; then
  echo "Eliminando grupo de recursos existente: $resourceGroup"
  az group delete --name $resourceGroup --yes
fi

echo "Creando grupo de recursos: $resourceGroup en la región $location"
az group create --name $resourceGroup --location $location

echo "Creando Log Analytics Workspace..."
workspaceName="workspace-$resourceGroup"
az monitor log-analytics workspace create \
  --resource-group $resourceGroup \
  --workspace-name $workspaceName \
  --location $location

workspaceId=$(az monitor log-analytics workspace show \
  --resource-group $resourceGroup \
  --workspace-name $workspaceName \
  --query customerId -o tsv)

workspaceKey=$(az monitor log-analytics workspace get-shared-keys \
  --resource-group $resourceGroup \
  --workspace-name $workspaceName \
  --query primarySharedKey -o tsv)

echo "Creando entorno de Container Apps..."
az containerapp env create \
  --name $environment \
  --resource-group $resourceGroup \
  --location $location \
  --logs-workspace-id $workspaceId \
  --logs-workspace-key $workspaceKey

echo "Desplegando Eureka Server..."
az containerapp create \
  --name eureka-server \
  --resource-group $resourceGroup \
  --environment $environment \
  --image $eurekaImage \
  --min-replicas 1 \
  --max-replicas 1 \
  --target-port 8761 \
  --ingress internal \
  --cpu 0.75 --memory 1.5Gi \
  --env-vars \
    EUREKA_CLIENT_REGISTER_WITH_EUREKA="false" \
    EUREKA_CLIENT_FETCH_REGISTRY="false"

echo "Consultando URI de Eureka Server..."
eureka_fqdn=$(az containerapp show \
  --name eureka-server \
  --resource-group $resourceGroup \
  --query properties.configuration.ingress.fqdn \
  -o tsv)

eurekaServer="https://$eureka_fqdn/eureka"

echo "Desplegando Gateway Service..."
az containerapp create \
  --name gateway-service \
  --resource-group $resourceGroup \
  --environment $environment \
  --image $gatewayImage \
  --target-port 8762 \
  --min-replicas 1 \
  --ingress external \
  --cpu 0.75 --memory 1.5Gi \
  --env-vars EUREKA_URL=$eurekaServer

echo "Desplegando Product Service..."
az containerapp create \
  --name product-service \
  --resource-group $resourceGroup \
  --environment $environment \
  --image $productImage \
  --target-port 8081 \
  --min-replicas 1 \
  --ingress internal \
  --cpu 0.75 --memory 1.5Gi \
  --env-vars \
    EUREKA_URL=$eurekaServer \
    ELASTICSEARCH_HOST=$elasticSearchHost \
    ELASTICSEARCH_USER=$elasticSearchUser \
    ELASTICSEARCH_PASS=$elasticSearchPass

echo "Desplegando Order Service..."
az containerapp create \
  --name order-service \
  --resource-group $resourceGroup \
  --environment $environment \
  --image $orderImage \
  --target-port 8082 \
  --min-replicas 1 \
  --ingress internal \
  --cpu 0.75 --memory 1.5Gi \
  --env-vars \
    EUREKA_URL=$eurekaServer \
    ORDERSERVICE_DB_URL=$databaseOrderUrl \
    ORDERSERVICE_DB_USER=$databaseOrderUser \
    ORDERSERVICE_DB_PASS=$databaseOrderPass \

echo "Desplegando User Service..."
az containerapp create \
  --name user-service \
  --resource-group $resourceGroup \
  --environment $environment \
  --image $userImage \
  --target-port 8083 \
  --min-replicas 1 \
  --ingress internal \
  --cpu 0.75 --memory 1.5Gi \
  --env-vars \
    EUREKA_URL=$eurekaServer \
    USERSERVICE_DB_URL=$databaseUserUrl \
    USERSERVICE_DB_USER=$databaseUserrUser \
    USERSERVICE_DB_PASS=$databaseUserPass \


echo "Despliegue completado. Resumen de recursos en el grupo de recursos $resourceGroup:"
az resource list \
  --resource-group $resourceGroup \
  --output table

echo "Consultando Endpoint del Gateway..."
gateway_fqdn=$(az containerapp show \
  --name gateway-service \
  --resource-group $resourceGroup \
  --query properties.configuration.ingress.fqdn \
  --output tsv)

gatewayUrl="https://$gateway_fqdn/actuator/gateway/routes"
echo "Gateway URL: $gatewayUrl"

#!/bin/bash

# NOTA: Este script asume que tienes instalado y configurado Azure CLI
# Puedes instalarlo desde https://docs.microsoft.com/en-us/cli/azure/install-azure-cli
# Y que has iniciado sesión:
# Si estás en un entorno sin interfaz gráfica (GUI), puedes usar el comando:
# az login --use-device-code
# que te permite completar el proceso de inicio de sesión de forma completamente manual en un dispositivo diferente.
# 
# También debes establecer la suscripción por defecto:
# az account set --subscription "id-de-tu-suscripción"
# Puedes listar las suscripciones disponibles con:
# az account list --output table

# Variables personalizables
resourceGroup="rg-unir-tienda-app"
# Puede cambiar esto por "westeurope", "centralus", etc. Depende si esta disponible Container Apps en la region
location="centralus"
environment="env-unir-tienda-app"

# Imagenes de docker de los contenedores
eurekaImage="docker.io/osgol/discovery-service:2.0"
gatewayImage="docker.io/osgol/gateway-service:2.0"
orderImage="docker.io/osgol/order-service:2.0"
productImage="docker.io/osgol/product-service:2.0"

# Variables de entorno de elasticsearch
elasticSearchHost="ogonzalezl-6089145741.us-east-1.bonsaisearch.net:443"
elasticSearchUser="orhmb20dea"
elasticSearchPass="91418fl69v"

# Variables de entorno de la base de datos
databaseUrl="jdbc:postgresql://shuttle.proxy.rlwy.net:22278/railway"
databaseUser="postgres"
databasePass="GJXkIMzUdyOflLANbCAoZfSjAGQxJJdT"

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
    ORDERSERVICE_DB_URL=$databaseUrl \
    ORDERSERVICE_DB_USER=$databaseUser \
    ORDERSERVICE_DB_PASS=$databasePass \

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

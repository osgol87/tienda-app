#!/bin/bash
set -euo pipefail

# NOTA: este script debe ejecutarse desde la raíz del repo (tienda-app/),
# ya que cada build usa la carpeta del microservicio como contexto.

# Versión de la imagen de docker
versionImage="3.0"

docker login

# Azure Container Apps requiere imágenes linux/amd64; buildx --push evita
# el paso intermedio de "docker tag" ya que publica el manifest directamente.
echo "Publicando la imagen de DiscoveryService..."
docker buildx build --platform linux/amd64 -t osgol/discovery-service:$versionImage --push ./DiscoveryService

echo "Publicando la imagen de GatewayService..."
docker buildx build --platform linux/amd64 -t osgol/gateway-service:$versionImage --push ./GatewayService

echo "Publicando la imagen de ProductServiceElastic..."
docker buildx build --platform linux/amd64 -t osgol/product-service:$versionImage --push ./ProductServiceElastic

echo "Publicando la imagen de OrderService..."
docker buildx build --platform linux/amd64 -t osgol/order-service:$versionImage --push ./OrderService

echo "Publicando la imagen de UserService..."
docker buildx build --platform linux/amd64 -t osgol/user-service:$versionImage --push ./UserService

echo "Publicación finalizada"

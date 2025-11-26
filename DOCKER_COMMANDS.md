# Comandos Docker para Backend API Extractor PDF

## 📦 Construcción de la Imagen

### Construir la imagen Docker
```bash
docker build -f deployment/Dockerfile -t backend-api-extractor-pdf:latest .
```

### Construir con un tag específico
```bash
docker build -f deployment/Dockerfile -t backend-api-extractor-pdf:1.0.0 .
```

### Construir sin usar caché
```bash
docker build --no-cache -f deployment/Dockerfile -t backend-api-extractor-pdf:latest .
```

---

## 🏷️ Etiquetado de Imágenes

### Tag para un registry local
```bash
docker tag backend-api-extractor-pdf:latest localhost:5000/backend-api-extractor-pdf:latest
```

### Tag para Docker Hub
```bash
docker tag backend-api-extractor-pdf:latest tuusuario/backend-api-extractor-pdf:latest
```

### Tag para otro registry (ej: AWS ECR, Google GCR, Azure ACR)
```bash
docker tag backend-api-extractor-pdf:latest registry.example.com/backend-api-extractor-pdf:latest
```

---

## 🚀 Ejecución del Contenedor

### Ejecutar en modo detached (background)
```bash
docker run -d -p 8080:8080 --name backend-extractor backend-api-extractor-pdf:latest
```

### Ejecutar en modo interactivo (ver logs en tiempo real)
```bash
docker run --rm -p 8080:8080 --name backend-extractor backend-api-extractor-pdf:latest
```

### Ejecutar con variables de entorno personalizadas
```bash
docker run -d -p 8080:8080 \
  -e JAVA_OPTS="-Xmx512m -Xms256m" \
  -e SERVER_PORT=8080 \
  --name backend-extractor \
  backend-api-extractor-pdf:latest
```

### Ejecutar con volumen montado (para logs o configuración)
```bash
docker run -d -p 8080:8080 \
  -v ${PWD}/logs:/app/logs \
  --name backend-extractor \
  backend-api-extractor-pdf:latest
```

### Ejecutar en una red específica
```bash
docker network create backend-network
docker run -d -p 8080:8080 \
  --network backend-network \
  --name backend-extractor \
  backend-api-extractor-pdf:latest
```

---

## 🔍 Gestión del Contenedor

### Ver contenedores en ejecución
```bash
docker ps
```

### Ver todos los contenedores (incluyendo detenidos)
```bash
docker ps -a
```

### Ver logs del contenedor
```bash
docker logs backend-extractor
```

### Ver logs en tiempo real (follow)
```bash
docker logs -f backend-extractor
```

### Ver las últimas 100 líneas de logs
```bash
docker logs --tail 100 backend-extractor
```

### Detener el contenedor
```bash
docker stop backend-extractor
```

### Iniciar el contenedor detenido
```bash
docker start backend-extractor
```

### Reiniciar el contenedor
```bash
docker restart backend-extractor
```

### Eliminar el contenedor (debe estar detenido)
```bash
docker rm backend-extractor
```

### Forzar eliminación del contenedor (aunque esté corriendo)
```bash
docker rm -f backend-extractor
```

### Ejecutar comandos dentro del contenedor
```bash
docker exec -it backend-extractor sh
```

### Ver estadísticas del contenedor en tiempo real
```bash
docker stats backend-extractor
```

### Inspeccionar detalles del contenedor
```bash
docker inspect backend-extractor
```

---

## 🖼️ Gestión de Imágenes

### Listar imágenes locales
```bash
docker images
```

### Ver imágenes filtradas
```bash
docker images | grep backend-api-extractor-pdf
```

### Eliminar una imagen
```bash
docker rmi backend-api-extractor-pdf:latest
```

### Eliminar imágenes sin usar (dangling)
```bash
docker image prune
```

### Eliminar todas las imágenes sin usar
```bash
docker image prune -a
```

### Ver historial de capas de la imagen
```bash
docker history backend-api-extractor-pdf:latest
```

### Ver el tamaño de la imagen
```bash
docker images backend-api-extractor-pdf:latest
```

---

## 📤 Push a Registry

### Push a Docker Hub
```bash
docker login
docker push tuusuario/backend-api-extractor-pdf:latest
```

### Push a registry privado
```bash
docker login registry.example.com
docker push registry.example.com/backend-api-extractor-pdf:latest
```

### Push a AWS ECR
```bash
# Login a AWS ECR
aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin 713881784063.dkr.ecr.us-east-2.amazonaws.com

# Construir la imagen con la ruta correcta del Dockerfile
docker build -f deployment/Dockerfile -t poliedro/extractor-pdf:latest .

# Tag para AWS ECR
docker tag poliedro/extractor-pdf:latest 713881784063.dkr.ecr.us-east-2.amazonaws.com/poliedro/extractor-pdf:latest

# Push a AWS ECR
docker push 713881784063.dkr.ecr.us-east-2.amazonaws.com/poliedro/extractor-pdf:latest
```

---

## 🧹 Limpieza

### Limpiar contenedores detenidos
```bash
docker container prune
```

### Limpiar imágenes sin usar
```bash
docker image prune -a
```

### Limpiar volúmenes sin usar
```bash
docker volume prune
```

### Limpiar redes sin usar
```bash
docker network prune
```

### Limpiar todo el sistema (contenedores, imágenes, volúmenes, redes)
```bash
docker system prune -a --volumes
```

---

## 🔬 Debug y Troubleshooting

### Ver procesos corriendo dentro del contenedor
```bash
docker top backend-extractor
```

### Copiar archivos desde el contenedor al host
```bash
docker cp backend-extractor:/app/logs/application.log ./logs/
```

### Copiar archivos desde el host al contenedor
```bash
docker cp ./config.yml backend-extractor:/app/config/
```

### Ver el health check del contenedor
```bash
docker inspect --format='{{json .State.Health}}' backend-extractor
```

### Ejecutar un health check manual
```bash
docker exec backend-extractor wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health
```

---

## 🌐 Endpoints Disponibles

Después de levantar el contenedor, los siguientes endpoints estarán disponibles:

- **Swagger UI**: http://localhost:8080/swagger-ui/index.html
- **API Docs**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Actuator Info**: http://localhost:8080/actuator/info

---

## 📋 Secuencia Completa de Comandos

### Para desarrollo local:
```bash
# 1. Construir la imagen
docker build -f deployment/Dockerfile -t backend-api-extractor-pdf:latest .

# 2. Ejecutar el contenedor
docker run -d -p 8080:8080 --name backend-extractor backend-api-extractor-pdf:latest

# 3. Ver logs
docker logs -f backend-extractor

# 4. Verificar health
curl http://localhost:8080/actuator/health

# 5. Abrir Swagger
start http://localhost:8080/swagger-ui/index.html

# 6. Detener y eliminar cuando termines
docker stop backend-extractor
docker rm backend-extractor
```

### Para producción (con registry):
```bash
# 1. Construir la imagen
docker build -f deployment/Dockerfile -t backend-api-extractor-pdf:1.0.0 .

# 2. Etiquetar para el registry
docker tag backend-api-extractor-pdf:1.0.0 registry.example.com/backend-api-extractor-pdf:1.0.0

# 3. Push al registry
docker push registry.example.com/backend-api-extractor-pdf:1.0.0

# 4. En el servidor de producción, hacer pull
docker pull registry.example.com/backend-api-extractor-pdf:1.0.0

# 5. Ejecutar en producción
docker run -d -p 8080:8080 \
  --name backend-extractor \
  --restart unless-stopped \
  -e JAVA_OPTS="-XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/./urandom" \
  registry.example.com/backend-api-extractor-pdf:1.0.0
```

---

## 🐳 Docker Compose (Alternativa)

Si prefieres usar Docker Compose, puedes usar el archivo `deployment/docker-compose.yml`:

```bash
# Levantar servicios
docker-compose -f deployment/docker-compose.yml up -d

# Ver logs
docker-compose -f deployment/docker-compose.yml logs -f

# Detener servicios
docker-compose -f deployment/docker-compose.yml down

# Reconstruir y levantar
docker-compose -f deployment/docker-compose.yml up -d --build
```

---

## 📝 Notas Importantes

1. **Puerto**: La aplicación expone el puerto `8080` por defecto
2. **Health Check**: El contenedor tiene un health check configurado en `/actuator/health`
3. **Usuario**: La aplicación corre con un usuario no-root (`appuser`) por seguridad
4. **Java Version**: Utiliza Java 24 (eclipse-temurin)
5. **Base Image**: Alpine Linux para un tamaño reducido

## ⚠️ Requisitos

- Docker instalado (versión 20.10 o superior recomendada)
- Puerto 8080 disponible en el host
- Al menos 512MB de RAM disponible para el contenedor

---

## ☁️ Deploy a AWS ECR (Configuración Específica)

### Secuencia completa para subir a AWS ECR:

```bash
# 1. Login a AWS ECR (región us-east-2)
aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin 713881784063.dkr.ecr.us-east-2.amazonaws.com

# 2. Construir la imagen con la ruta correcta del Dockerfile
docker build -f deployment/Dockerfile -t poliedro/extractor-pdf:latest .

# 3. Tag la imagen para AWS ECR
docker tag poliedro/extractor-pdf:latest 713881784063.dkr.ecr.us-east-2.amazonaws.com/poliedro/extractor-pdf:latest

# 4. Push la imagen a AWS ECR
docker push 713881784063.dkr.ecr.us-east-2.amazonaws.com/poliedro/extractor-pdf:latest
```

### Verificar la imagen en AWS ECR:
```bash
# Listar imágenes en el repositorio
aws ecr describe-images --repository-name poliedro/extractor-pdf --region us-east-2
```

### Deploy desde AWS ECR en un servidor:
```bash
# 1. Login en el servidor de destino
aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin 713881784063.dkr.ecr.us-east-2.amazonaws.com

# 2. Pull la imagen
docker pull 713881784063.dkr.ecr.us-east-2.amazonaws.com/poliedro/extractor-pdf:latest

# 3. Ejecutar el contenedor
docker run -d -p 8080:8080 \
  --name backend-extractor \
  --restart unless-stopped \
  713881784063.dkr.ecr.us-east-2.amazonaws.com/poliedro/extractor-pdf:latest
```

### Notas importantes para AWS ECR:
- **Región**: us-east-2
- **Registry ID**: 713881784063
- **Repositorio**: poliedro/extractor-pdf
- **Requisito**: AWS CLI configurado con credenciales válidas
- **Permisos necesarios**: `ecr:GetAuthorizationToken`, `ecr:BatchCheckLayerAvailability`, `ecr:GetDownloadUrlForLayer`, `ecr:BatchGetImage`, `ecr:PutImage`


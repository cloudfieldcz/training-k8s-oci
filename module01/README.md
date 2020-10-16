# 01 - Building application containers

For your development you can use docker-compose configuration which is ready there in subfolder `src/` / commands `docker-compose build` and `docker-compose up` can be used for local testing.

## Build image in Container Registry

script expecting that you have sourced environment variables from rc file.

```bash
# enter directory with source codes
cd module01

# build images ..
###################################
docker login -u "${DOCKER_USR}" -p "${DOCKER_KEY}" ${DOCKER_SRV}

# build SPA application 
echo v1 > ./src/myappspa/version
docker build --tag ${DOCKER_PATH}/myappspa:v1 ./src/myappspa
docker push ${DOCKER_PATH}/myappspa:v1

# for purpose of lab create v2 of your app by only changing from v1 to v2 in version file and build container with v2 tag
echo v2 > ./src/myappspa/version
docker build --tag ${DOCKER_PATH}/myappspa:v2 ./src/myappspa
docker push ${DOCKER_PATH}/myappspa:v2

# build JAVA microservice (spring-boot)
docker build --tag ${DOCKER_PATH}/myapptodo:v1 ./src/myapptodo
docker push ${DOCKER_PATH}/myapptodo:v1

# build NET CORE app for performance testing
docker build --tag ${DOCKER_PATH}/webstress:v1 ./src/webstress
docker push ${DOCKER_PATH}/webstress:v1

```


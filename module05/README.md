# CI/CD

```bash
# goto directory for this lab
cd ../module05
```

## Prepare to CI/CD with helm.

Helm is template engine (deployment engine) for kubernetes.

Please change IP address of your ingress controller and name of your Container Registry in `helm upgrade` command below.

```bash
# variables
. ../rc

# create namespace
kubectl create namespace myapp

```

## Setup access for Container Registry

```bash
# TEST docker login
docker login -u "${DOCKER_USR}" -p "${DOCKER_KEY}" ${DOCKER_SRV}

# apply credentials to cluster
kubectl create secret docker-registry ocirsecret --docker-server=${DOCKER_SRV} \
  --docker-username=${DOCKER_USR} --docker-password="${DOCKER_KEY}" --docker-email=test@test.it \
  --namespace myapp 
```

## prepare deployment infra

```bash

export DBUSER="todo"
export DBPASSWORD="pwd123..."

# deploy postgress
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update 

helm install mypostgress bitnami/postgresql --namespace='myapp' --set-string global.postgresql.postgresqlDatabase="todo",global.postgresql.postgresqlUsername="${DBUSER}",global.postgresql.postgresqlPassword="${DBPASSWORD}"

# secrets for app
POSTGRESQL_URL="jdbc:postgresql://mypostgress-postgresql:5432/todo?user=${DBUSER}&password=${DBPASSWORD}"
kubectl create secret generic myrelease-myapp \
  --from-literal=postgresqlurl="$POSTGRESQL_URL" \
  --namespace myapp

# Get ingress public IP
export INGRESS_IP=$(kubectl get service nginx-ingress-ingress-nginx-controller  -n nginx-ingress -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "You will be able to access application on this URL: http://${INGRESS_IP}.xip.io"

# helm deploy
helm upgrade --install myrelease helm/myapp --namespace='myapp' --set-string appspa.image.repository="${DOCKER_PATH}/myappspa",appspa.image.tag='v1',apptodo.image.repository="${DOCKER_PATH}/myapptodo",apptodo.image.tag='v1',apphost="${INGRESS_IP}.xip.io",appspa.imagePullSecrets="ocirsecret",apptodo.imagePullSecrets="ocirsecret"

# clean-up deployment
helm --namespace myapp delete myrelease
# delete namespace
kubectl delete namespace myapp
```

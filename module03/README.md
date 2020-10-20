# 03 - Deploy application to Container Engine for Kubernetes (OKE)

```bash
# goto directory for this lab
cd ../module03
```

## Create PostgreSQL service

```bash
#variables
. ../rc

# create namespace
kubectl create namespace myapp

export DBUSER="todo"
export DBPASSWORD="pwd123..."

# deploy postgress
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update 

helm install mypostgress bitnami/postgresql --namespace='myapp' --set-string global.postgresql.postgresqlDatabase="todo",global.postgresql.postgresqlUsername="${DBUSER}",global.postgresql.postgresqlPassword="${DBPASSWORD}"

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

## Deploy apps to K8S

Replace your image names (OCRI name) in files `myapp-deploy/myappspa-rs.yaml` and `myapp-deploy/myapptodo-rs.yaml`.

Replace public IP address of your nginx ingress controller for host rule in files `myapp-deploy/myappspa-ing.yaml` and `myapp-deploy/myapptodo-ing.yaml`. 

```bash
# Change yaml files to your ACR name
sed -i 's/YOUROCIRNAME/'$DOCKER_PATH'/g' myapp-deploy/*.yaml

# Get ingress public IP
export INGRESS_IP=$(kubectl get service nginx-ingress-ingress-nginx-controller -n nginx-ingress -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "You will be able to access application on this URL: http://${INGRESS_IP}.xip.io"

# Change YAML files for ingress
sed -i 's/YOURINGRESSIP/'$INGRESS_IP'/g' myapp-deploy/*.yaml
```

```bash

# create secrets
POSTGRESQL_URL="jdbc:postgresql://mypostgress-postgresql:5432/todo?user=${DBUSER}&password=${DBPASSWORD}"
kubectl create secret generic myapptodo-secret \
  --from-literal=postgresqlurl="$POSTGRESQL_URL" \
  --namespace myapp

# create deployment
kubectl apply -f myapp-deploy --namespace myapp
```

### Deploy canary (v2 of SPA application)

Now we will create canary deployment with version v2 and we will balance there 10% of traffic.

```bash
# Change yaml files to your ACR name
sed -i 's/YOUROCIRNAME/'$DOCKER_PATH'/g' myapp-deploy-canary/*.yaml

# Get ingress public IP
export INGRESS_IP=$(kubectl get service nginx-ingress-ingress-nginx-controller -n nginx-ingress -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "You will be able to access application on this URL: http://${INGRESS_IP}.xip.io"

# Change YAML files for ingress
sed -i 's/YOURINGRESSIP/'$INGRESS_IP'/g' myapp-deploy-canary/*.yaml
```

```bash
# create deployment
kubectl apply -f myapp-deploy-canary --namespace myapp
```

Is traffic really balanced to app versions? Let's find out.
```
while true; do curl http://${INGRESS_IP}.xip.io/info.txt; done
```

Enforce traffic routing only to canary based on HEADER values in requests.
```
while true; do curl -H "myappspa-canary-v2: always" http://${INGRESS_IP}.xip.io/info.txt; done
```

### Note

There are way more configurations options beyond scope of this workshop. To name a few:
* TLS encryption using certificate stored as Kubernetes secret
* Automation of certificate enrollment (eg. with Let's encrypt) using cert-manager project
* Rate limit on requests per minute
* Source IP filtering
* Basic authentication
* OAuth2
* Canary including complex ones such as by header or cookie
* Cors
* Redirect
* Proxy features such as url rewrite
* Buffering
* Lua rules

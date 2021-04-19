# 03 - Deploy application to Container Engine for Kubernetes (OKE)

```bash
# goto directory for this lab
cd ../module03
```

## Create namespace for application

```bash
#variables
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

## Deploy apps to K8S

Replace your image names (OCRI name) in files `myapp-deploy/myappspa-rs.yaml` and `myapp-deploy/myapptodo-rs.yaml`.

Replace public IP address of your nginx ingress controller for host rule in files `myapp-deploy/myappspa-ing.yaml` and `myapp-deploy/myapptodo-ing.yaml`. 

```bash
# Change yaml files to your ACR name
sed -i 's,YOUROCIRNAME,'$DOCKER_PATH',g' myapp-deploy/*.yaml

# Get ingress public IP
export INGRESS_IP=$(kubectl get service nginx-ingress-ingress-nginx-controller -n nginx-ingress -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
echo "You will be able to access application on this URL: http://${INGRESS_IP}.xip.io"

# Change YAML files for ingress
sed -i 's/YOURINGRESSIP/'$INGRESS_IP'/g' myapp-deploy/*.yaml
```

```bash

# create secrets -> change Oracle connection credentials there and use orawalet
export ORASQL_URL='jdbc:oracle:thin:@tcps://adb.eu-frankfurt-1.oraclecloud.com:1522/#####.adb.oraclecloud.com?oracle.net.ssl_server_cert_dn="CN=adwc.eucom-central-1.oraclecloud.com,OU=Oracle BMCS FRANKFURT,O=Oracle Corporation,L=Redwood City,ST=California,C=US"&javax.net.ssl.trustStore=/home/user/orawalet/truststore.jks&javax.net.ssl.trustStorePassword=#####&javax.net.ssl.keyStore=/home/user/orawalet/keystore.jks&javax.net.ssl.keyStorePassword=#####&user=admin&password=#####'

kubectl create secret generic myrelease-myapp \
  --from-literal=orasqlurl="$ORASQL_URL" \
  --namespace myapp

# configmap
# store oracle walet files in configmap
kubectl create configmap my-config -n myapp --from-file=/mnt/c/Src/sandbox/orawalet

# create deployment
kubectl apply -f myapp-deploy --namespace myapp
```

### Deploy canary (v2 of SPA application)

Now we will create canary deployment with version v2 and we will balance there 10% of traffic.

```bash
# Change yaml files to your ACR name
sed -i 's,YOUROCIRNAME,'$DOCKER_PATH',g' myapp-deploy-canary/*.yaml

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

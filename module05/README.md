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
# create secrets -> change Oracle connection credentials there and use orawalet
export ORASQL_URL='jdbc:oracle:thin:@tcps://adb.eu-frankfurt-1.oraclecloud.com:1522/#####.adb.oraclecloud.com?oracle.net.ssl_server_cert_dn="CN=adwc.eucom-central-1.oraclecloud.com,OU=Oracle BMCS FRANKFURT,O=Oracle Corporation,L=Redwood City,ST=California,C=US"&javax.net.ssl.trustStore=/home/user/orawalet/truststore.jks&javax.net.ssl.trustStorePassword=#####&javax.net.ssl.keyStore=/home/user/orawalet/keystore.jks&javax.net.ssl.keyStorePassword=#####&user=admin&password=#####'

kubectl create secret generic myapptodo-secret \
  --from-literal=orasqlurl="$ORASQL_URL" \
  --namespace myapp

# configmap
# store oracle walet files in configmap
kubectl create configmap my-config -n myapp --from-file=/mnt/c/Src/sandbox/orawalet

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

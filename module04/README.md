# Prepare deployment files
```bash
cd ../module04
sed -i 's/YOUROCIRNAME/'$DOCKER_PATH'/g' *.yaml
```

# Use ConfigMap to tweek nginx configuration
Currently for our probes we are accessing root of SPA application which is too big taking away network and storage IO. We might prefer some more lightweight probe to check health status. Also we want to standardize on single url for all apps (/health) and do not want to implement changes in code itself. NGINX allows for configuring such health paths itself.

We want to change NGINX configuration without rebuilding container image. There might more configuration options that we want to tweek during deployment perhaps providing different settings for dev, test and production environment. General rule is not to change container image between environments!

We will solve this by using ConfigMap in Kubernetes. It can consist of key value pair that we can map into our Pod as environmental variables. In our case configuration is actualy more complex configuration file. This is also possible with ConfigMap. First let's use configuration file healthvhost.conf and package it as ConfigMap. 

```bash
kubectl create configmap healthvhostconf --from-file=healthvhost.conf -n myapp
kubectl describe configmap healthvhostconf -n myapp
```

First we will use changed Deployment with ConfigMap mapped to file system to proper locaiton where nginx expects configuration file and check it works.

```bash
kubectl apply -f 01-myappspa-deploy.yaml -n myapp
```

Wait for Deployment to redeploy Pods and check our /health URL works.
```bash
curl http://$INGRESS_IP.xip.io/health
```

Looks good. We will now change our probes implementation to point to /health.
```bash
kubectl apply -f 02-myappspa-deploy.yaml -n myapp
```

# Use init container to create information file outside of main container startup script
Suppose now we need to know inside of our Pod what Kubernetes namespace it has been created in. More over we want to write it into file that will be accessible via URL. We will use passing this information via Downward API and also use init container to prepare file before running our main container.

We will add initContainer to our Pod definition. That container will be started before all other containers and Kubernetes will wait for it to finish first. This can used to preload cache or do any other preparations before you are ready to run your main container. We will also leverage Downward API to inject information about used image into Pod as environmental variable. For now init container will just print it on screen so we can capture it in logs.
```bash
kubectl apply -f 03-myappspa-deploy.yaml -n myapp
```

Checkout logs from our info container
```bash
kubectl logs myappspa-7b74455b84-rf2c6 -n myapp -c info   # Change to your Pod name
```

Should work. Now we want to put this information as file on our site so we need some way how init container can write to file system that main container can read from. We will use Volume for this, but this time it will not be implemented as outside resource, but rather is Volume valid only on Pod level mounted to both containers. Let's do it.
```bash
kubectl apply -f 04-myappspa-deploy.yaml -n myapp
```

Check it out
```bash
curl http://$INGRESS_IP.xip.io/info/namespace.txt
```

# Use CronJob to periodically extract data from Postgresql
In this example we will investigate how to use Kubernetes to run scheduled batch jobs. This is very useful for certain computation scenarios such as rendering or machine learning, but also for periodical tasks. In our demo we will shedule periodic task to dump data from postgresql into csv file stored on share in Oracle File storage.

We will use shared file system for exports.
```bash
# create FS
OFS=$(oci fs file-system create -c $(cat ../.compartment) --display-name ocik8s-fs \
  --availability-domain "${AVAILD}" \
  --query 'data.id' --raw-output )

# create FS mount target
OFSMT=$(oci fs mount-target create -c $(cat ../.compartment) --display-name ocik8s-fs-mt \
  --availability-domain "${AVAILD}" --subnet-id "${SUBNVM}" \
  --query 'data.id' --raw-output )

# get export target
OFSEXT=$(oci fs mount-target get --mount-target-id $OFSMT --query 'data."export-set-id"' --raw-output)

# get private IP for export
OFSEXTIP=$(oci network private-ip get --private-ip-id $(oci fs mount-target get --mount-target-id $OFSMT --query 'data."private-ip-ids"[0]' --raw-output) --query 'data."ip-address"' --raw-output)

# create FS export
OFSEXPORT=$(oci fs export create \
  --export-set-id "${OFSEXT}" --file-system-id "${OFS}" --path "/data" \
  --query 'data.id' --raw-output )

# create storage class
cat <<EOF | kubectl create -n myapp -f -
kind: StorageClass
apiVersion: storage.k8s.io/v1beta1
metadata:
  name: oci-fss
provisioner: oracle.com/oci-fss
parameters:
  mntTargetId: ${OFSMT}
EOF

# create persistent volume
cat <<EOF | kubectl create -n myapp -f -
apiVersion: v1
kind: PersistentVolume
metadata:
 name: oke-fsspv
spec:
 storageClassName: oci-fss
 capacity:
  storage: 10Gi
 accessModes:
  - ReadWriteMany
 mountOptions:
  - nosuid
 nfs:
  server: ${OFSEXTIP}
  path: "/data"
  readOnly: false
EOF

# create persistent volume claim
cat <<EOF | kubectl create -n myapp -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
 name: oke-fsspvc
spec:
 storageClassName: oci-fss
 accessModes:
  - ReadWriteMany
 resources:
  requests:
    storage: 10Gi
 volumeName: oke-fsspv
EOF

```

Than we need to gather connection details you used for creating database in previous modules and store them in Kubernetes secret with naming convention used by psql command line utility.
```bash
kubectl create secret generic psql -n myapp \
    --from-literal PGUSER=$DBUSER \
    --from-literal PGPASSWORD=$DBPASSWORD \
    --from-literal PGHOST=mypostgress-postgresql \
    --from-literal PGDATABASE=todo
```

Schedule job to run every 2 minutes.
```bash
kubectl apply -f 05-export.yaml -n myapp
```

Job will run every 2 minutes. After while check files in your storage account.

```bash
# show exports
kubectl exec -n myapp -ti ubuntu -- /bin/bash

# in container lets check exports ...
cd /exports
ls -al
cat <some file>

```

# HPA - Horizontal Pod Autoscaler

We will try to create horizontally auto-scaled solution for our .NetCore service.
```bash
# create namespace for experiment with HPA
kubectl create namespace perf
```

## Setup access for Container Registry

```bash
# TEST docker login
docker login -u "${DOCKER_USR}" -p "${DOCKER_KEY}" ${DOCKER_SRV}

# apply credentials to cluster
kubectl create secret docker-registry ocirsecret --docker-server=${DOCKER_SRV} \
  --docker-username=${DOCKER_USR} --docker-password="${DOCKER_KEY}" --docker-email=test@test.it \
  --namespace perf 
```

## instalations

Apply deployment of our webstress service
```bash
# install metrics server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.3.7/components.yaml

# deploy
kubectl apply -f 06-webstress.yaml -n perf

# get public IP of service for testing
kubectl get svc -n perf

# create HPA
kubectl autoscale deployment webstress --namespace perf --min=1 --max=8 --cpu-percent=50

# monitor HPA
kubectl get hpa --namespace perf -w
```

Send some "stress" traffic to service.
```bash
# you can run fet times this background task ...
curl "[IP ADDRESS]/perf?x=[0-10000]" 2> /dev/null > /dev/null &

# after test you can kill all curl processes
pkill curl
```

# Cleanup

```bash
kubectl delete namespace myapp
kubectl delete namespace perf
```
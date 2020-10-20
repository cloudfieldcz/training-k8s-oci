# k8s-workshop
This repo contains materials for one-day App Dev on Oracle Kubernetes training.
Repository content is based on original repository: https://github.com/cloudfieldcz/training-k8s-az

# Lab preparation

Before you will start please CLONE our repo `git@github.com:cloudfieldcz/training-k8s-oci.git`

## Clone repo

```bash
mkdir myexperiment
cd myexperiment

# clone repo first
git clone git@github.com:cloudfieldcz/training-k8s-oci.git

# and now lets step into folder with project files.
cd training-k8s-oci
```

## prerequirements
* installed oci
* access to Oracle CLoud Platform
* installed docker and docker tools

## Login to oci

```bash
# access configured via
 oci setup config
```

## Container Registry

https://www.oracle.com/webfolder/technetwork/tutorials/obe/oci/registry/index.html

Container registry is already prepared for your tenant, you have to collect registry information which we will need later on:
* registry endpoint (depends on your region - for Germany for example eu-frankfurt-1.ocir.io)
* tenancy-namespace
* generate authentication token for user

## Update environment file

Please provide information to `rc` file, update content and than run:

```bash
. rc
```

## Create Compartment for our experiment
```bash
oci iam compartment create -c "${TENANCY}" --description "Container Engine for our experiments" --name "TEST-CE" \
  --query 'data.id' --raw-output \
  --config-file ${CONFIGFILE} --profile ${PROFILE} --auth ${AUTH} > .compartment

```

## Create Container Engine for Kubernetes (OKE)

```bash
# Virtual Network
oci network vcn create -c $(cat .compartment)  --display-name ocik8s \
  --cidr-block "10.0.0.0/16" --dns-label ocik8s
  
# get ID of Virtual Network
VCNID=$(oci network vcn list -c $(cat .compartment) \
  --query 'data[0].id' --raw-output)

# Internet gateway
VCNINETGW=$(oci network internet-gateway create -c $(cat .compartment)  --display-name ocik8s-inetgw \
  --vcn-id "${VCNID}" --is-enabled=true --query 'data.id' --raw-output)

# NAT gateway
VCNNATGW=$(oci network nat-gateway create -c $(cat .compartment)  --display-name ocik8s-natgw \
  --vcn-id "${VCNID}" --query 'data.id' --raw-output)

# Route tables
VCNRTINETGW=$(oci network route-table create  -c $(cat .compartment)  --display-name ocik8s-rt-inetgw \
  --vcn-id "${VCNID}" --query 'data.id' --raw-output \
  --route-rules "[{\"cidrBlock\":\"0.0.0.0/0\",\"networkEntityId\":\"${VCNINETGW}\"}]")
VCNRTNATGW=$(oci network route-table create  -c $(cat .compartment)  --display-name ocik8s-rt-natgw \
  --vcn-id "${VCNID}" --query 'data.id' --raw-output \
  --route-rules "[{\"cidrBlock\":\"0.0.0.0/0\",\"networkEntityId\":\"${VCNNATGW}\"}]")

# Security list
VCNSECLISTLB=$(oci network security-list create  -c $(cat .compartment)  --display-name ocik8s-seclist-lb \
  --vcn-id "${VCNID}" --query 'data.id' --raw-output \
  --ingress-security-rules "[{\"source\": \"0.0.0.0/0\", \"protocol\": \"6\", \"isStateless\": true}]" \
  --egress-security-rules "[{\"destination\": \"0.0.0.0/0\", \"protocol\": \"6\", \"isStateless\": true}]" \
  )
VCNSECLISTWORKNOD=$(oci network security-list create  -c $(cat .compartment)  --display-name ocik8s-seclist-worknod \
  --vcn-id "${VCNID}" --query 'data.id' --raw-output \
  --ingress-security-rules "[{\"source\": \"10.0.10.0/24\", \"protocol\": \"all\", \"isStateless\": true},{\"source\": \"10.0.0.0/16\", \"protocol\": \"6\", \"isStateless\": false,\"tcpOptions\": {\"destinationPortRange\": {\"max\": 22, \"min\": 22}}}]" \
  --egress-security-rules "[{\"destination\": \"10.0.10.0/24\", \"protocol\": \"all\", \"isStateless\": true},{\"destination\": \"0.0.0.0/0\", \"protocol\": \"all\", \"isStateless\": false}]" \
  )

# Subnets for LB
SUBNLB=$(oci network subnet create -c $(cat .compartment) --display-name ocik8s-sub-lb \
  --route-table-id "${VCNRTINETGW}" --security-list-ids "[\"${VCNSECLISTLB}\"]" --prohibit-public-ip-on-vnic false \
  --vcn-id "${VCNID}" --cidr-block "10.0.20.0/24" --dns-label sublb --query 'data.id' --raw-output )

# Subnet for Work Nodes
SUBNVM=$(oci network subnet create -c $(cat .compartment) --display-name ocik8s-sub-worknod \
  --route-table-id "${VCNRTNATGW}" --security-list-ids "[\"${VCNSECLISTWORKNOD}\"]" --prohibit-public-ip-on-vnic true \
  --vcn-id "${VCNID}" --cidr-block "10.0.10.0/24" --dns-label subworknod --query 'data.id' --raw-output )

# Create K8S cluster
oci ce cluster create -c $(cat .compartment) \
  --kubernetes-version "v1.17.9" --name "ocik8s" --vcn-id "${VCNID}" \
  --service-lb-subnet-ids "[\"${SUBNLB}\"]" 

# Get cluster ID - wait few minutes - cluster must be created
OCE=$(oci ce cluster list -c $(cat .compartment) \
  --query "data[?\"name\"=='ocik8s'&&\"lifecycle-state\"=='ACTIVE'].id | [0]" --raw-output )
echo $OCE

# Node pool
AVAILD=$(oci iam availability-domain list --query 'data[0].name' --raw-output )
IMAGEID=$(oci ce node-pool-options get --node-pool-option-id all --query 'data.sources[0]."image-id"' --raw-output)
oci ce node-pool create -c $(cat .compartment) \
  --cluster-id "${OCE}" --kubernetes-version "v1.17.9" --name "default" \
  --size 2 --placement-configs "[{\"availabilityDomain\": \"${AVAILD}\",\"subnetId\": \"${SUBNVM}\"}]" \
  --node-shape "VM.Standard2.1" --node-image-id "${IMAGEID}" 

```

# kube config

```bash
# get config
oci ce cluster create-kubeconfig --cluster-id "${OCE}" 

```

# Labs

## [01 - Building application containers](module01/README.md)

## [02 - Introduction to Container Engine for Kubernetes (OKE)](module02/README.md)

## [03 - Deploy application to Container Engine for Kubernetes (OKE)](module03/README.md)

## [04 - Optimizing deployment in Kubernetes](module04/README.md)

## [05 - DevOps with OKE and introduction to helm](module05/README.md)


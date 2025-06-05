# A Quarkus CLI to manage locally a kubernetes cluster

The goal of this project is to manage using a Java CLI a Kubernetes cluster and to provision it with some Packages definition: ingress, argocd, gitea, etc. 

The Client proposes different commands such as: 
- Create/delete/start/stop a kubernetes cluster,
- Install/uninstall a package

**Note**: The cluster is installed within a container created by a docker engine (podman or docker) using the [kind container image](https://hub.docker.com/r/kindest/node/tags).

To allow to provision the cluster with the packages that you need: ingress, cert manager, keycloak, vault, gitea, etc, we install by default the `Platform` operator
supporting to install using Custom resources a Platform definition which includes the description of the packages to be installed using a Pipeline of steps - see the project: [Platform Operator](https://github.com/halkyonio/java-package-operator).

## How to play with the client

**Prerequisite**: Podman or docker desktop installed

Compile the project locally
```shell
mvn clean package
```
Define an alias pointing to the Quarkus App jar file
```shell
// bash
alias qk='java -jar target/quarkus-app/quarkus-run.jar'
// Fishell
alias qk 'java -jar target/quarkus-app/quarkus-run.jar'
```
Create a kind cluster
```shell
qk create my-kind
```
Export the kubeconfig file and access the cluster
```shell
// Bash
export KUBECONFIG=my-kind-kube.conf
// Fishell
set -x KUBECONFIG my-kind-kube.conf

kubectl get pods -A
NAMESPACE            NAME                                     READY   STATUS    RESTARTS   AGE
kube-system          coredns-76f75df574-hjg4z                 1/1     Running   0          7m22s
kube-system          coredns-76f75df574-pg9sd                 1/1     Running   0          7m22s
kube-system          etcd-my-kind                             1/1     Running   0          7m32s
kube-system          kindnet-qhst6                            1/1     Running   0          7m22s
kube-system          kube-apiserver-my-kind                   1/1     Running   0          7m32s
kube-system          kube-controller-manager-my-kind          1/1     Running   0          7m32s
kube-system          kube-proxy-9k4nl                         1/1     Running   0          7m22s
kube-system          kube-scheduler-my-kind                   1/1     Running   0          7m32s
local-path-storage   local-path-provisioner-55ff44bb8-ffjkc   1/1     Running   0          7m22s
platform             package-operator-859946fc66-x6vz9        1/1     Running   0          6m52s
```
Enjoy ;-)

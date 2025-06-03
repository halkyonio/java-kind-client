## A Quarkus CLI to manage locally a kubernetes cluster

The goal of this project is to manage using a Java CLI a Kubernetes cluster and to provision it with some packageCRS: ingress, argocd, gitea, etc. 

The Client proposes different commands such as: 
- Create/delete/start/stop a kubernetes cluster,
- Install/uninstall a package

**Note**: The cluster is installed within a container created by a docker engine (podman or docker) using the [kind container image](https://hub.docker.com/r/kindest/node/tags).
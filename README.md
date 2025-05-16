## A Quarkus CLI to manage locally a kubernetes cluster

The goal of this project is to manage using Java a Kubernetes cluster. The cluster is launched by a container engine using the [kind container image](https://hub.docker.com/r/kindest/node/tags). The Client proposes different commands to: `create/delete/start/stop` the cluster.
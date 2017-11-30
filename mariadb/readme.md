# Install a mariadb instance using a helm chart.

Requires kubectl and kubeconfigs setup as well as helm installed.

Installing the chart:

helm install -f values.yaml --name altinnkanal-db stable/mariadb 

Deleting the chart:

helm delete altinnkanal-db --purge


See: 

https://github.com/kubernetes/helm///github.com/kubernetes/helm/integrasjon
https://github.com/kubernetes/charts/tree/master/stable/mariadb



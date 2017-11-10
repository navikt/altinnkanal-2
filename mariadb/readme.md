# Install a mariadb instance using a helm chart.

Requires kubectl and kubeconfigs setup as well as helm installed.

Installing the chart:

helm install --namespace integrasjon -f values.yaml --name integrasjon  stable/mariadb 

Deleting the chart:

helm delete integrasjon --purge


See: 

https://github.com/kubernetes/helm///github.com/kubernetes/helm/integrasjon
https://github.com/kubernetes/charts/tree/master/stable/mariadb



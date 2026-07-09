output "cluster_id" {
  value = oci_containerengine_cluster.bloom.id
}

output "cluster_name" {
  value = oci_containerengine_cluster.bloom.name
}

output "node_pool_id" {
  value = oci_containerengine_node_pool.bloom_workers.id
}

output "vcn_id" {
  value = oci_core_vcn.bloom.id
}

# Commande prete a copier pour configurer kubectl.
output "kubeconfig_command" {
  value = join(" ", [
    "oci ce cluster create-kubeconfig",
    "--cluster-id", oci_containerengine_cluster.bloom.id,
    "--file $HOME/.kube/config",
    "--region", var.region,
    "--token-version 2.0.0",
    "--kube-endpoint PUBLIC_ENDPOINT"
  ])
}

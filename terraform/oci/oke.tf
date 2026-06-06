data "oci_identity_availability_domains" "ads" {
  compartment_id = var.tenancy_ocid
}

data "oci_containerengine_node_pool_option" "options" {
  node_pool_option_id = "all"
  compartment_id      = var.compartment_ocid
}

resource "oci_containerengine_cluster" "bloom" {
  compartment_id     = var.compartment_ocid
  name               = var.cluster_name
  kubernetes_version = var.kubernetes_version
  vcn_id             = oci_core_vcn.bloom.id
  type               = "BASIC_CLUSTER"

  endpoint_config {
    is_public_ip_enabled = true
    subnet_id            = oci_core_subnet.k8s_api.id
  }

  options {
    service_lb_subnet_ids = [oci_core_subnet.lb.id]
    kubernetes_network_config {
      pods_cidr     = "10.244.0.0/16"
      services_cidr = "10.96.0.0/16"
    }
  }
}

resource "oci_containerengine_node_pool" "bloom_workers" {
  compartment_id     = var.compartment_ocid
  cluster_id         = oci_containerengine_cluster.bloom.id
  name               = "${var.cluster_name}-pool"
  kubernetes_version = var.kubernetes_version
  node_shape         = var.node_pool_shape

  node_shape_config {
    ocpus         = var.node_ocpus
    memory_in_gbs = var.node_memory_gbs
  }

  node_source_details {
    source_type = "IMAGE"
    image_id = (
      var.node_image_ocid != "" ? var.node_image_ocid :
      [for s in data.oci_containerengine_node_pool_option.options.sources :
      s.image_id if can(regex("aarch64|ARM|Aarch", s.source_name))][0]
    )
  }

  node_config_details {
    size = var.node_count
    placement_configs {
      availability_domain = data.oci_identity_availability_domains.ads.availability_domains[0].name
      subnet_id           = oci_core_subnet.workers.id
    }
  }

  ssh_public_key = var.ssh_public_key != "" ? var.ssh_public_key : null
}

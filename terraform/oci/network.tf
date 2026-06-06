resource "oci_core_vcn" "bloom" {
  compartment_id = var.compartment_ocid
  display_name   = "${var.cluster_name}-vcn"
  cidr_blocks    = ["10.0.0.0/16"]
  dns_label      = "bloomvcn"
}

resource "oci_core_internet_gateway" "igw" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bloom.id
  display_name   = "${var.cluster_name}-igw"
  enabled        = true
}

resource "oci_core_nat_gateway" "nat" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bloom.id
  display_name   = "${var.cluster_name}-nat"
}

data "oci_core_services" "all_services" {
  filter {
    name   = "name"
    values = ["All .* Services In Oracle Services Network"]
    regex  = true
  }
}

resource "oci_core_service_gateway" "sgw" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bloom.id
  display_name   = "${var.cluster_name}-sgw"
  services {
    service_id = data.oci_core_services.all_services.services[0]["id"]
  }
}

resource "oci_core_route_table" "public" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bloom.id
  display_name   = "${var.cluster_name}-rt-public"
  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_internet_gateway.igw.id
  }
}

resource "oci_core_route_table" "private" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bloom.id
  display_name   = "${var.cluster_name}-rt-private"
  route_rules {
    destination       = "0.0.0.0/0"
    destination_type  = "CIDR_BLOCK"
    network_entity_id = oci_core_nat_gateway.nat.id
  }
  route_rules {
    destination       = data.oci_core_services.all_services.services[0]["cidr_block"]
    destination_type  = "SERVICE_CIDR_BLOCK"
    network_entity_id = oci_core_service_gateway.sgw.id
  }
}

resource "oci_core_security_list" "public" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bloom.id
  display_name   = "${var.cluster_name}-sl-public"

  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
  }
  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 6443
      max = 6443
    }
  }
  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 80
      max = 80
    }
  }
  ingress_security_rules {
    protocol = "6"
    source   = "0.0.0.0/0"
    tcp_options {
      min = 443
      max = 443
    }
  }
}

resource "oci_core_security_list" "private" {
  compartment_id = var.compartment_ocid
  vcn_id         = oci_core_vcn.bloom.id
  display_name   = "${var.cluster_name}-sl-private"

  egress_security_rules {
    destination = "0.0.0.0/0"
    protocol    = "all"
  }
  ingress_security_rules {
    protocol = "all"
    source   = "10.0.0.0/16"
  }
}

resource "oci_core_subnet" "k8s_api" {
  compartment_id             = var.compartment_ocid
  vcn_id                     = oci_core_vcn.bloom.id
  display_name               = "${var.cluster_name}-subnet-api"
  cidr_block                 = "10.0.0.0/28"
  route_table_id             = oci_core_route_table.public.id
  security_list_ids          = [oci_core_security_list.public.id]
  dns_label                  = "api"
  prohibit_public_ip_on_vnic = false
}

resource "oci_core_subnet" "lb" {
  compartment_id             = var.compartment_ocid
  vcn_id                     = oci_core_vcn.bloom.id
  display_name               = "${var.cluster_name}-subnet-lb"
  cidr_block                 = "10.0.1.0/24"
  route_table_id             = oci_core_route_table.public.id
  security_list_ids          = [oci_core_security_list.public.id]
  dns_label                  = "lb"
  prohibit_public_ip_on_vnic = false
}

resource "oci_core_subnet" "workers" {
  compartment_id             = var.compartment_ocid
  vcn_id                     = oci_core_vcn.bloom.id
  display_name               = "${var.cluster_name}-subnet-workers"
  cidr_block                 = "10.0.10.0/24"
  route_table_id             = oci_core_route_table.private.id
  security_list_ids          = [oci_core_security_list.private.id]
  dns_label                  = "workers"
  prohibit_public_ip_on_vnic = true
}

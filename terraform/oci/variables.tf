variable "tenancy_ocid" {
  type = string
}

variable "compartment_ocid" {
  type = string
}

variable "region" {
  type    = string
  default = "uk-london-1"
}

variable "cluster_name" {
  type    = string
  default = "bloom-oke"
}

variable "kubernetes_version" {
  type    = string
  default = "v1.33.1"
}

# VM.Standard.A1.Flex = ARM Ampere (Always Free)
# "arm" -> A1.Flex (Always Free, souvent "out of capacity")
# "amd" -> E4.Flex (couvert par les credits du free trial, capacite fiable)
variable "node_arch" {
  type    = string
  default = "arm"
}

variable "node_count" {
  type    = number
  default = 2
}

variable "node_ocpus" {
  type    = number
  default = 2
}

variable "node_memory_gbs" {
  type    = number
  default = 12
}

variable "node_image_ocid" {
  type    = string
  default = ""
}

variable "ssh_public_key" {
  type    = string
  default = ""
}

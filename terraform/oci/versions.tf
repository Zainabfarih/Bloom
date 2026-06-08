terraform {
  required_version = ">= 1.5.0"

  required_providers {
    oci = {
      source  = "oracle/oci"
      version = ">= 5.0.0"
    }
  }
}

# En Cloud Shell, l'auth est automatique (resource principal).
provider "oci" {
  region = var.region
}

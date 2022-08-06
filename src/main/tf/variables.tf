# export TF_VAR_azure_subscription_id=
# export TF_VAR_azure_tenant_id=

variable "azure_subscription_id" {
  type = string
}

variable "azure_tenant_id" {
  type = string
}

variable "azure_default_location" {
  type    = string
  default = "westeurope"
}

variable "azure_failover_location" {
  type    = string
  default = "northeurope"
}

# export TF_VAR_azure_subscription_id=
# export TF_VAR_azure_tenant_id=
# export TF_VAR_notification_alert_email=

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

variable "resource_group_name" {
  type    = string
  default = "weather-observations"
}

variable "notification_alert_email" {
  type = string
}
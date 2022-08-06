locals {
  project_name = "weather-observations"
}

resource "azurerm_resource_group" "this" {
  name     = local.project_name
  location = var.azure_default_location
}

resource "random_integer" "this" {
  min = 10000
  max = 99999
}

resource "azurerm_cosmosdb_account" "mongo" {
  location             = azurerm_resource_group.this.location
  name                 = "${local.project_name}-${random_integer.this.result}"
  resource_group_name  = azurerm_resource_group.this.name
  offer_type           = "Standard"
  kind                 = "MongoDB"
  mongo_server_version = "4.2"

  enable_automatic_failover     = true
  public_network_access_enabled = false

  backup {
    type                = "Periodic"
    interval_in_minutes = 720
    retention_in_hours  = 24
    storage_redundancy  = "Zone"
  }

  capabilities {
    name = "EnableMongo"
  }
  capabilities {
    name = "EnableServerless"
  }

  consistency_policy {
    consistency_level = "Eventual"
  }

  geo_location {
    failover_priority = 0
    location          = var.azure_default_location
  }

  lifecycle {
    ignore_changes = [
      name
    ]
  }
}

# App Service

resource "azurerm_storage_account" "function_app_storage" {
  account_replication_type = "LRS"
  account_tier             = "Standard"
  location                 = azurerm_resource_group.this.location
  name                     = "weatherfuncappsa${random_integer.this.result}"
  resource_group_name      = azurerm_resource_group.this.name
}

resource "azurerm_service_plan" "this" {
  location            = azurerm_resource_group.this.location
  name                = "${local.project_name}-service-plan"
  os_type             = "Linux"
  resource_group_name = azurerm_resource_group.this.name
  sku_name            = "Y1"
}

#resource "azurerm_linux_function_app" "this" {
#  location            = azurerm_service_plan.this.location
#  name                = "${local.project_name}-func-app-${random_integer.this.result}"
#  resource_group_name = azurerm_service_plan.this.resource_group_name
#  service_plan_id     = azurerm_service_plan.this.id
#  site_config {
#    app_scale_limit = 5
#  }
#}
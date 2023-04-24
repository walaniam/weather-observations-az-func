# weather-observations-az-func

## Archetype used to generate this project
```bash
mvn archetype:generate -DarchetypeGroupId=com.microsoft.azure -DarchetypeArtifactId=azure-functions-archetype -DjavaVersion=11
```

## Environment setup

### Terraform
#### About Terraform
https://developer.hashicorp.com/terraform/tutorials/azure-get-started
#### Install CLI
https://developer.hashicorp.com/terraform/tutorials/azure-get-started/install-cli  

### Azure CLI
https://learn.microsoft.com/en-us/cli/azure/functionapp/keys?view=azure-cli-latest

## Provision Azure environment
### Init Terraform
```bash
cd src/main/tf
terraform init
```

### Export variables
For each set proper value
```bash
export TF_VAR_azure_subscription_id=
export TF_VAR_azure_tenant_id=
export TF_VAR_notification_alert_email=
export TF_VAR_resource_group_name=
```

### Create environment
```bash
cd src/main/tf
terraform plan
terraform apply
```

## Work with Environment

### Export TF_RANDOM_INT
For further commands export resource group name and random int variable (from terraform state)
```bash
export TF_RANDOM_INT=$(cd src/main/tf/ && terraform state show random_integer.this |grep result | cut -d "=" -f2 |xargs)
export TF_VAR_resource_group_name=
```

### Manage function app keys
#### List keys
```bash
az functionapp keys list -g ${TF_VAR_resource_group_name} -n weather-observations-${TF_RANDOM_INT}-func-app
```
#### Set key
```bash
az functionapp keys set -g ${TF_VAR_resource_group_name} -n weather-observations-${TF_RANDOM_INT}-func-app --key-type functionKeys --key-name MyKeyName --key-value MyKeyValue
```
### Manage function keys
```bash
az functionapp function keys list -g ${TF_VAR_resource_group_name} -n weather-observations-${TF_RANDOM_INT}-func-app --function-name get-latest-observations-v1
```

## Build and run locally
```bash
mvn clean package -Dtf.random.int=$TF_RANDOM_INT
mvn azure-functions:run -Dtf.random.int=$TF_RANDOM_INT
```

## Deploy to Azure
```bash
az login
```
```bash
mvn clean package azure-functions:deploy -Dtf.random.int=$TF_RANDOM_INT -Dapp.resource.group=$TF_VAR_resource_group_name
```

### Check logs
```bash
func azure functionapp logstream weather-observations-${TF_RANDOM_INT}-func-app
```

### Show function
```bash
az functionapp function show -g ${TF_VAR_resource_group_name} -n weather-observations-${TF_RANDOM_INT}-func-app --function-name get-latest-observations-v1
```

## Properties
```bash
mvn help:evaluate
```
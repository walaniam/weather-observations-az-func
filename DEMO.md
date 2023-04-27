# Demo
## Provision Azure environment with Terraform

### Prepare tfvars
Fill in `src/main/tf/myenv.auto.tfvars` as described in [README](README.md)

### Login to Azure
```bash
az login
```

### Apply Terraform plan

```bash
cd src/main/tf
terraform init
terraform plan
terraform apply
```

### Wait for terraform completion

### Prepare shell variables
```bash
export FUNC_APP_PLAN=$(terraform state show azurerm_service_plan.this |grep name |grep -v resource_group_name |grep -v sku_name |cut -d "=" -f2 |xargs |tr -d '[:space:]')
export FUNC_APP=$(terraform state show azurerm_windows_function_app.this |grep name |grep -v hostname |grep -v resource_group_name |grep -v storage_account_name |head -n1 |cut -d "=" -f2 |xargs |tr -d '[:space:]')
export RG_NAME=$(cat myenv.auto.tfvars |grep resource_group_name |cut -d "=" -f2 |xargs |tr -d '[:space:]')
```

### Show environment
- in portal
- in az cli
```bash
az group list --output table --query "[?name == '$RG_NAME']"
az resource list -g $RG_NAME --output table
```
### Show terraform state
```bash
terraform state list
terraform state show azurerm_service_plan.this
```

### Function deployment
Show logs
```bash
az functionapp log deployment list -n $FUNC_APP -g $RG_NAME
```

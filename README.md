# weather-observations-az-func
## Archetype to generate this project
```bash
mvn archetype:generate -DarchetypeGroupId=com.microsoft.azure -DarchetypeArtifactId=azure-functions-archetype -DjavaVersion=11
```

## Provision Azure environment
### Init
```bash
cd src/main/tf
terraform init
```

### Create environment
```bash
cd src/main/tf
terraform plan
terraform apply
```

## Build and run locally
For further commands export random int variable from terraform state
```bash
export TF_RANDOM_INT=$(cd src/main/tf/ && terraform state show random_integer.this |grep result | cut -d "=" -f2 |xargs)
```

```bash
mvn clean package -Dtf.random.int=$TF_RANDOM_INT
mvn azure-functions:run -Dtf.random.int=$TF_RANDOM_INT
```

## Azure
### Deployment
```bash
az login
mvn azure-functions:deploy -Dtf.random.int=$TF_RANDOM_INT
```

### Find keys (host, master and system key)
```bash
az functionapp keys list -g weather-observations -n weather-observations-func-app
```

### Find function keys
```bash
az functionapp function keys list -g weather-observations -n weather-observations-func-app --function-name observations-v1
```

### Check logs
```bash
func azure functionapp logstream weather-observations-func-app
```

### Show function
```bash
az functionapp function show -g weather-observations -n weather-observations-func-app --function-name observations-v1
```

## Properties
```bash
mvn help:evaluate
```
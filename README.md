# weather-observations-az-func
## Archetype to generate this project
```bash
mvn archetype:generate -DarchetypeGroupId=com.microsoft.azure -DarchetypeArtifactId=azure-functions-archetype -DjavaVersion=11
```

## Build and run locally
```bash
source .env
mvn clean package -DfunctionNameRandom=${FUNCTION_NAME_RANDOM}
mvn azure-functions:run -DfunctionNameRandom=${FUNCTION_NAME_RANDOM}
```

## Azure
### Deployment
```bash
source .env
az login
mvn azure-functions:deploy -DfunctionNameRandom=${FUNCTION_NAME_RANDOM}
```

### Find keys (host, master and system key)
```bash
az functionapp keys list -g weather-observations -n weather-observations-az-func-${FUNCTION_NAME_RANDOM}
```

### Find function keys
```bash
az functionapp function keys list -g weather-observations -n weather-observations-az-func-${FUNCTION_NAME_RANDOM} --function-name HttpExample
```

### Check logs
```bash
source .env
func azure functionapp logstream weather-observations-az-func-${FUNCTION_NAME_RANDOM}
```

### Cleanup
```bash
az group delete --name weather-observations
```

## Properties
```bash
mvn help:evaluate -DfunctionNameRandom=${FUNCTION_NAME_RANDOM}
```
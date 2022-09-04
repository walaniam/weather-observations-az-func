# weather-observations-az-func
## Archetype to generate this project
```bash
mvn archetype:generate -DarchetypeGroupId=com.microsoft.azure -DarchetypeArtifactId=azure-functions-archetype -DjavaVersion=11
```

## Build and run locally
```bash
mvn clean package
mvn azure-functions:run
```

## Azure
### Deployment
```bash
az login
mvn azure-functions:deploy
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
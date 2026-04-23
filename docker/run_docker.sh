#!/bin/bash

# sh ./run_docker.sh <local|dev|uat|prod>

ENV=$1

if [ -z "$ENV" ]
then
  ENV="local"
  echo "No environment specified: local is used."
fi

pip3 install yq

if [ "$ENV" = "local" ]; then
  image="service-local:latest"
  ENV="dev"
else
  repository=$(yq -r '."microservice-chart".image.repository' ../helm/values-$ENV.yaml)
  image="${repository}:latest"
fi
export image=${image}

FILE=.env
if test -f "$FILE"; then
    rm .env
fi
config=$(yq  -r '."microservice-chart".envConfig' ../helm/values-$ENV.yaml)
IFS=$'\n'
for line in $(echo "$config" | jq -r '. | to_entries[] | select(.key) | "\(.key)=\(.value)"'); do
    echo "$line" >> .env
done

keyvault=$(yq  -r '."microservice-chart".keyvault.name' ../helm/values-$ENV.yaml)
secret=$(yq  -r '."microservice-chart".envSecret' ../helm/values-$ENV.yaml)
for line in $(echo "$secret" | jq -r '. | to_entries[] | select(.key) | "\(.key)=\(.value)"'); do
  IFS='=' read -r -a array <<< "$line"
  response=$(az keyvault secret show --vault-name $keyvault --name "${array[1]}")
  value=$(echo "$response" | jq -r '.value')
  echo "${array[0]}=$value" >> .env
done


stack_name=$(cd .. && basename "$PWD")
docker compose -p "${stack_name}" up -d --remove-orphans --force-recreate --build


# waiting the containers
printf 'Waiting for the service\n'

echo "=== Container status ==="
docker ps -a --filter "name=service" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo "=== .env contents (secrets redacted) ==="
cat .env | sed 's/=.*/=***/' || echo "No .env file"

attempt_counter=0
max_attempts=50
until $(curl --output /dev/null --silent --fail http://localhost:8080/info); do
    if [ ${attempt_counter} -eq ${max_attempts} ];then
      echo ""
      echo "=== Max attempts reached - dumping debug info ==="
      echo "=== Container status ==="
      docker ps -a --filter "name=service" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
      echo "=== Container logs (last 50 lines) ==="
      docker logs service --tail 50 2>&1
      echo "=== Curl response ==="
      curl -v http://localhost:8080/info 2>&1 || true
      exit 1
    fi

    if [ $((attempt_counter % 10)) -eq 0 ] && [ ${attempt_counter} -gt 0 ]; then
      echo ""
      echo "=== Debug at attempt ${attempt_counter} ==="
      docker ps -a --filter "name=service" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
      docker logs service --tail 10 2>&1
    fi

    printf '.'
    attempt_counter=$((attempt_counter+1))
    sleep 5
done
echo 'Service Started'

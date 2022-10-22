#!/bin/bash

set -e
run_cmd="dotnet run --project src/WebAPI/WebAPI.csproj --server.urls http://+:80"

until dotnet ef database update --project src/Infrastructure/Infrastructure.csproj --startup-project src/WebAPI/WebAPI.csproj --context Infrastructure.Persistence.AppDbContext; do
>&2 echo "MySQL is starting up"
sleep 1
done

>&2 echo "MySQL is up - executing command"
exec $run_cmd